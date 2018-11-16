package gps

import chisel3._
import chisel3.experimental.FixedPoint
import chisel3.util.{log2Ceil, Decoupled}

import dsptools.numbers._
import scala.math._


// xy integer bits should be more than 2 + log2(max_input)

/**
 * Base class for CORDIC parameters
 *
 * These are type generic
 */
trait CordicParams[T <: Data] {
  val xyWidth: Int
  val xyBPWidth: Int
  val zWidth: Int
  val zBPWidth: Int
  val protoXY: T
  val protoZ: T
  val nStages: Int
  val correctGain: Boolean
  val stagesPerCycle: Int
  val calAtan2: Boolean
  val dividing: Boolean
  // requireIsHardware(protoXY)
  // requireIsHardware(protoZ)
}

case class FixedCordicParams(
  xyWidth: Int,
  xyBPWidth: Int,
  zWidth: Int,
  zBPWidth: Int,
  correctGain: Boolean = true,
  stagesPerCycle: Int = 1,
  calAtan2: Boolean = true,
  dividing: Boolean = false,
) extends CordicParams[FixedPoint] {
  val protoXY = FixedPoint(xyWidth.W, xyBPWidth.BP)
  val protoZ = FixedPoint(zWidth.W, zBPWidth.BP)
  // number of stages needed to get LSBs of xy
  private val xyStages = xyWidth
  // number of stages needed to get LSBs of z
  private val zStages = {
    val minNumber = math.pow(2.0, -(zWidth-3))
    // number of cordic stages
    var n = 0
    while (breeze.numerics.tan(math.pow(2.0, -(n+1))) >= minNumber) {
      n += 1
    }
    n
  }
  val nStages = 20 //, xyStages.max(zStages)
}

class CordicBundle[T <: Data](val params: CordicParams[T]) extends Bundle {
  val x: T = params.protoXY.cloneType
  val y: T = params.protoXY.cloneType
  val z: T = params.protoZ.cloneType

  override def cloneType: this.type = CordicBundle(params).asInstanceOf[this.type]
}
object CordicBundle {
  def apply[T <: Data](params: CordicParams[T]): CordicBundle[T] = new CordicBundle(params)
}

class IterativeCordicIO[T <: Data](params: CordicParams[T]) extends Bundle {
  val in = Flipped(Decoupled(CordicBundle(params)))
  val out = Decoupled(CordicBundle(params))

  val vectoring = Input(Bool())

  override def cloneType: this.type = IterativeCordicIO(params).asInstanceOf[this.type]
}
object IterativeCordicIO {
  def apply[T <: Data](params: CordicParams[T]): IterativeCordicIO[T] =
    new IterativeCordicIO(params)
}

object AddSub {
  def apply(sel: Bool, a: FixedPoint, b: FixedPoint): FixedPoint = {
    Mux(sel, a + b, a - b)
  }
}

/**
  * The main part of the cordic algorithm expects to see vectors in the 1st and 4th quadrant (or angles with absolute
  * value < pi/2).
  *
  * This function transforms inputs into ranges that the main part of the cordic can deal with.
  */
object TransformInput {
  def apply(xyz: CordicBundle[FixedPoint], vectoring: Bool): CordicBundle[FixedPoint] = {
    val zBP = xyz.params.protoZ.binaryPoint
    val pi = math.Pi.F(zBP)
    val piBy2 = (math.Pi/2).F(zBP)
    val zBig = xyz.z >= piBy2
    val zSmall = xyz.z <= -piBy2
    val xNeg = xyz.x.isSignNegative()
    val yNeg = xyz.y.isSignNegative()

    val xyzTransformed = WireInit(xyz)
    if (xyz.params.dividing){
      when(xNeg){
        xyzTransformed.x := -xyz.x
      }.otherwise{
        xyzTransformed.x := xyz.x
      }
      when(yNeg) {
        xyzTransformed.y := -xyz.y >> (xyz.params.xyWidth - xyz.params.xyBPWidth)
      }.otherwise{
        xyzTransformed.y := xyz.y >> (xyz.params.xyWidth - xyz.params.xyBPWidth)
      }
      xyzTransformed.z := xyz.z
    }else {
      when(vectoring) {
        // When vectoring, if in quadrant 2 or 3 we rotate by pi
        when(xNeg) {
          xyzTransformed.x := -xyz.x
          xyzTransformed.y := -xyz.y
          if (xyz.params.calAtan2) {
            // if calculate atan2
            when(yNeg) {
              // if yNeg, then transformed y is positive
              // we'll have a positive z, so subtract pi
              xyzTransformed.z := xyz.z - pi
            }.otherwise {
              xyzTransformed.z := xyz.z + pi
            }
          } else {
            // if calculate atan
            xyzTransformed.z := xyz.z
          }
        }
      }.otherwise {
        // when rotating, if |z| > pi/2 rotate by pi/2 so |z| < pi/2
        when(zBig) {
          xyzTransformed.x := -xyz.y
          xyzTransformed.y := xyz.x
          xyzTransformed.z := xyz.z - piBy2
        }
        when(zSmall) {
          xyzTransformed.x := xyz.y
          xyzTransformed.y := -xyz.x
          xyzTransformed.z := xyz.z + piBy2
        }
      }
    }
    xyzTransformed
  }
}

class CordicStage(params: CordicParams[FixedPoint]) extends Module {
  val io = IO(new Bundle {
    val in = Input(CordicBundle(params))
    val vectoring = Input(Bool())
    val shift = Input(UInt())
    val romIn = Input(FixedPoint(params.zWidth.W, params.zBPWidth.BP))
    val romLinIn = Input(FixedPoint(params.zWidth.W, params.zBPWidth.BP))
    val out = Output(CordicBundle(params))
  })
  val xshift = io.in.x >> io.shift
  val yshift = io.in.y >> io.shift

  val d = Mux(io.vectoring,
    io.in.y.signBit(),
    !io.in.z.signBit()
  )

  io.out.y := AddSub( d, io.in.y, xshift)
  if (!params.dividing) {
    io.out.x := AddSub(!d, io.in.x, yshift)
    io.out.z := AddSub(!d, io.in.z, io.romIn)
  }else{
    io.out.x := io.in.x
    io.out.z := AddSub(!d, io.in.z, io.romLinIn)
  }
}

class FixedIterativeCordic(val params: CordicParams[FixedPoint]) extends Module {
  require(params.nStages > 0)
  require(params.stagesPerCycle > 0)
  require(params.nStages >= params.stagesPerCycle)
  require(params.nStages % params.stagesPerCycle == 0, "nStages must be multiple of stagesPerCycles")

  val io = IO(IterativeCordicIO(params))

  // Make states for state machine
  val sInit = 0.U(2.W)
  val sWork = 1.U(2.W)
  val sDone = 2.U(2.W)
  val state = RegInit(sInit)

  // Register to hold iterations of CORDIC
  val xyz = Reg(CordicBundle(params))
  // Counter for the current iteration
  val iter = RegInit(0.U(log2Ceil(params.nStages + 1).W))

  val table = CordicConstants.arctan(params.nStages)
  val tableLin = CordicConstants.linear(-(params.xyWidth-params.xyBPWidth),
      params.xyBPWidth)
  println(tableLin)
  val gain = (1/CordicConstants.gain(params.nStages)).F(params.xyWidth.W, params.xyBPWidth.BP)
  val rom = VecInit(table.map(_.F(params.zWidth.W, params.zBPWidth.BP)))
  val romLin = VecInit(tableLin.map(_.F((params.xyWidth+2).W, params.xyBPWidth.BP)))  // may need change
  val regVectoring = Reg(Bool())

  // Make the stages and connect everything except in and out
  val stages = for (i <- 0 until params.stagesPerCycle) yield {
    val idx = iter + i.U
    val stage = Module(new CordicStage(params))
    stage.io.vectoring := regVectoring
    stage.io.shift := idx
    stage.io.romIn := rom(idx)
    stage.io.romLinIn := romLin(idx)
    stage
  }
  // Chain the stages together
  val stageOut = stages.foldLeft(xyz) { case (in, stage) =>
      stage.io.in := in
      stage.io.out
  }

  when (state === sInit && io.in.fire()) {
    state := sWork
    iter := 0.U
    regVectoring := io.vectoring

    xyz := TransformInput(io.in.bits, io.vectoring)
  }
  when (state === sWork) {
    val iterNext = iter + params.stagesPerCycle.U
    iter := iterNext
    when (iterNext >= (params.nStages - 1).U) {
      state := sDone
    }

    xyz := stageOut
  }
  when (state === sDone && io.out.fire()) {
    state := sInit
  }

  io.in.ready := state === sInit
  io.out.valid := state === sDone

  if (params.correctGain && !params.dividing) {
    io.out.bits.x := xyz.x * gain
    io.out.bits.y := xyz.y * gain
  } else {
    io.out.bits.x := xyz.x
    io.out.bits.y := xyz.y
  }
  io.out.bits.z := xyz.z
}
