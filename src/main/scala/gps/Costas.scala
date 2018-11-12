package gps

import chisel3._
import chisel3.experimental.FixedPoint
import dsptools.numbers._

trait CostasParams[T <: Data] {
  val protoData: T
  val protoFreq: T
  val protoPhase: T
  val protoCoeff: T
  val protoCordic: FixedCordicParams
}

case class SampledCostasParams(
  // width of input data
  dataWidth: Int,
  // width of freq ctrl
  freqWidth: Int,
  // width of phase ctrl
  phaseWidth: Int,
  // cordic xyWidth
  cordicXYWidth: Int,
  // cordic zWidth
  cordicZWidth: Int,
  // nStages
  cordicNStages: Int,
  // correct gain
  cordicCorrectGain: Boolean = true,
  // costas left shift bits
  costasLeftShift: Int,
  // fll right shift bits
  fllRightShift: Int,
  // filter coefficient bits
  lfCoeffWidth: Int
) extends CostasParams[SInt] {
  val protoData = SInt(dataWidth.W)
  val protoFreq = SInt(freqWidth.W)
  val protoPhase = SInt(phaseWidth.W)
  val protoCoeff = SInt(lfCoeffWidth.W)
  val protoCordic = FixedCordicParams(
        xyWidth = cordicXYWidth,
        zWidth = cordicZWidth,
        nStages = cordicNStages,
        correctGain = cordicCorrectGain,
        )
}

class lfCoeffIO[T <: Data](params: CostasParams[T]) extends Bundle {
  val phaseCoeff0 = Input(params.protoCoeff)
  val phaseCoeff1 = Input(params.protoCoeff)
  val phaseCoeff2 = Input(params.protoCoeff)
  val freqCoeff0 = Input(params.protoCoeff)
  val freqCoeff1 = Input(params.protoCoeff)
}

class CostasIO[T <: Data](params: CostasParams[T]) extends Bundle {
  val Ip = Input(params.protoData)
  val Qp = Input(params.protoData)
  val lfCoeff = new lfCoeffIO(params)
  val freqBias = Input(params.protoFreq)
  val freqCtrl = Output(params.protoFreq)
  val phaseCtrl = Output(params.protoPhase)

  // debug
  val xin = Output(params.protoCordic.protoXY)
  val yin = Output(params.protoCordic.protoXY)
  val zin = Output(params.protoCordic.protoZ)
  val xout = Output(params.protoCordic.protoXY)
  val yout = Output(params.protoCordic.protoXY)
  val zout = Output(params.protoCordic.protoZ)
}

object CostasIO {
  def apply[T <: Data](params: CostasParams[T]): CostasIO[T] =
    new CostasIO(params)
}

class costasDis(val params: SampledCostasParams) extends Module {
  val io = IO( new Bundle {
    val Ip = Input(params.protoData)
    val Qp = Input(params.protoData)
    val costasDisOut = Output(SInt((params.costasLeftShift + 2).W))

    // debug
    val xin = Output(params.protoCordic.protoXY)
    val yin = Output(params.protoCordic.protoXY)
    val zin = Output(params.protoCordic.protoZ)
    val xout = Output(params.protoCordic.protoXY)
    val yout = Output(params.protoCordic.protoXY)
    val zout = Output(params.protoCordic.protoZ)
  })
  // get cordic
  val cordic = Module(new Cordic1Cycle(params.protoCordic))

  // get extend type
  val extendType = FixedPoint(params.dataWidth.W, (params.cordicXYWidth-2).BP)
  // coridic input/output
  val xInCordic = Wire(extendType)
  val yInCordic = Wire(extendType)
  val xOutCordic = Wire(params.protoCordic.protoXY.cloneType)
  val yOutCordic = Wire(params.protoCordic.protoXY.cloneType)
  val zOutCordic = Wire(params.protoCordic.protoZ.cloneType)

  // fll Dis Mid
  val costasDisMid = Wire(FixedPoint(((params.costasLeftShift+2).W), (params.cordicZWidth-2).BP))

  // format input to FixedPoint
  xInCordic := io.Ip.asTypeOf(FixedPoint(params.dataWidth.W, (params.cordicXYWidth-2).BP)) >> (params.dataWidth-2)
  yInCordic := io.Qp.asTypeOf(FixedPoint(params.dataWidth.W, (params.cordicXYWidth-2).BP)) >> (params.dataWidth-2)
  // format to cordic Input
  cordic.io.in.x := xInCordic.asTypeOf(params.protoCordic.protoXY)
  cordic.io.in.y := yInCordic.asTypeOf(params.protoCordic.protoXY)
  cordic.io.in.z := 0.S.asTypeOf(params.protoCordic.protoZ)
  cordic.io.vectoring := true.B
  cordic.io.dividing := false.B
  // cordic output
  xOutCordic := cordic.io.out.x
  yOutCordic := cordic.io.out.y
  zOutCordic := cordic.io.out.z

  // costas discreminator
  costasDisMid := zOutCordic << params.costasLeftShift
  io.costasDisOut := costasDisMid.asSInt()

  io.xin := cordic.io.in.x
  io.yin := cordic.io.in.y
  io.zin := cordic.io.in.z
  io.xout := xOutCordic
  io.yout := yOutCordic
  io.zout := zOutCordic
}


class fllDis(val params: SampledCostasParams) extends Module {
  val io = IO( new Bundle {
    val Ip = Input(params.protoData)
    val Qp = Input(params.protoData)
    val fllDisOut = Output(SInt((2 * params.dataWidth).W))
  })
  // delayed version
  val IpDel = Reg(params.protoData.cloneType)
  val QpDel = Reg(params.protoData.cloneType)

  // before shift
  val fllDisMid = Wire(SInt((2*params.dataWidth).W))

  // delay
  IpDel := io.Ip
  QpDel := io.Qp
  // fll discreminator
  fllDisMid := IpDel * io.Qp - QpDel * io.Ip
  io.fllDisOut := fllDisMid >> params.fllRightShift
}


class loopFilter(val params: SampledCostasParams) extends Module {
  val io = IO( new Bundle {
    val costasDis = Input(SInt((params.costasLeftShift+2).W))
    val fllDis = Input(SInt((2 * params.dataWidth).W))
    val lfCoeff = new lfCoeffIO(params)
    val freqOut = Output(params.protoFreq)
  })

  val lfSumSum = RegInit(params.protoFreq.cloneType, 0.S)
  val lfSum = RegInit(params.protoFreq.cloneType, 0.S)

  lfSumSum := io.lfCoeff.phaseCoeff2 * io.costasDis + io.lfCoeff.freqCoeff1 * io.fllDis
  lfSum := io.lfCoeff.phaseCoeff1 * io.costasDis + io.lfCoeff.freqCoeff0 * io.fllDis + lfSumSum
  io.freqOut := io.lfCoeff.phaseCoeff0 * io.costasDis + lfSum

}


class CostasLoop(val params: SampledCostasParams) extends Module {
  val io = IO(CostasIO(params))

  // costas and fll
  val costas = Module(new costasDis(params))
  val fll = Module(new fllDis(params))
  val lf = Module(new loopFilter(params))

  // costas output
  val costasDisOut = Wire(SInt((params.costasLeftShift+2).W))
  // fll output
  val fllDisOut = Wire(SInt((2*params.dataWidth).W))
  //
  val freqMid = Wire(params.protoFreq.cloneType)

  // connect costas
  costas.io.Ip := io.Ip
  costas.io.Qp := io.Qp
  costasDisOut := costas.io.costasDisOut

  // connect fll
  fll.io.Ip := io.Ip
  fll.io.Qp := io.Qp
  fllDisOut := fll.io.fllDisOut

  // connect loop filter
  lf.io.costasDis := costasDisOut
  lf.io.fllDis := fllDisOut
  lf.io.lfCoeff := io.lfCoeff
  freqMid := lf.io.freqOut

  // use fake value for now
  io.freqCtrl := io.freqBias + freqMid
  io.phaseCtrl := costasDisOut

  // debug
  io.xin := costas.io.xin
  io.yin := costas.io.yin
  io.zin := costas.io.zin
  io.xout := costas.io.xout
  io.yout := costas.io.yout
  io.zout := costas.io.zout
}