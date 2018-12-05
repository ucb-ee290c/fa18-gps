package gps

import chisel3._
import chisel3.experimental.FixedPoint
import chisel3.util._
import dsptools.numbers._

trait TrackingChannelParams[T <: Data] {
  val adcWidth: Int
  val ncoWidth: Int
  val intWidth: Int
  val carrierNcoParams: NcoParams[T]
  val caNcoParams: NcoParams[T]
  val ca2xNcoParams: NcoParams[T]
  val caParams: CAParams
  val mulParams: MulParams[T]
  val intParams: IntDumpParams[T]
}
case class ExampleTrackingChannelParams() extends 
  TrackingChannelParams[SInt] {
  val adcWidth = 5  // Assume ADC in is a 5bit signed number
  val ncoWidth = 30 // 30 bit wide NCO
  val intWidth = 32 // signed 32 bit integrated numbers
  // Carrier NCO is 32 bits wide counter, 2 bit out and has a sin output
  val carrierNcoParams = SIntNcoParams(ncoWidth, 5, true)
  // Ca NCO is 32 bits wide and has no sin output, 1 bit output
  val caNcoParams = SIntNcoParams(ncoWidth, 1, false)
  // Ca NCO 2x is only 31 bits wide to create double the frequency
  val ca2xNcoParams = SIntNcoParams(ncoWidth-1, 1, false)
  // Ca Params
  val caParams = CAParams(1, 2)
  // Multipliers are 5 bit in and 5 bit out
  val mulParams = SampledMulParams(5)
  // Int Params 
  val intParams = SampledIntDumpParams(5, 5*16*1023)
  // Phase Lock Detector Params Limit set to +-15deg
  val phaseLockParams = LockDetectParams(FixedPoint(20.W, 12.BP), -0.26, 0.26,100)
}

class EPLBundle[T <: Data](protoIn: T) extends Bundle {
  val ie: T = protoIn.cloneType
  val ip: T = protoIn.cloneType
  val il: T = protoIn.cloneType 
  val qe: T = protoIn.cloneType
  val qp: T = protoIn.cloneType
  val ql: T = protoIn.cloneType 

  override def cloneType: this.type = EPLBundle(protoIn).asInstanceOf[this.type]
}
object EPLBundle {
  def apply[T <: Data](protoIn: T): EPLBundle[T] = 
    new EPLBundle(protoIn)
}

class TrackingChannelIO[T <: Data](params: TrackingChannelParams[T]) extends Bundle {
  val adcSample = Input(SInt(params.adcWidth.W))
  val svNumber = Input(UInt(6.W)) //fixed width due to number of satellites
  val dump = Input(Bool())
  val toLoop = Output(EPLBundle(SInt(params.intWidth.W)))
  val dllIn = Input(UInt(params.ncoWidth.W))
  val costasIn = Input(UInt(params.ncoWidth.W))
  val caIndex = Output(UInt(32.W))
  val phaseErr = Flipped(Valid(FixedPoint(20.W, 12.BP)))
  val lock = Output(Bool())

  override def cloneType: this.type =
    TrackingChannelIO(params).asInstanceOf[this.type]
}
object TrackingChannelIO {
  def apply[T <: Data](
    params: TrackingChannelParams[T]
  ): TrackingChannelIO[T] =
    new TrackingChannelIO(params)
}

class TrackingChannel[T <: Data : Real](
  val params: TrackingChannelParams[T]
) extends Module {
  val io = IO(TrackingChannelIO(params))

  val carrierNco = Module(new NCO[T](params.carrierNcoParams))
  carrierNco.io.stepSize := io.costasIn
  val multI = Module(new Mul[T](params.mulParams))
  multI.io.in1 := io.adcSample
  multI.io.in2 := carrierNco.io.cos

  val multQ = Module(new Mul[T](params.mulParams))
  multQ.io.in1 := io.adcSample
  multQ.io.in2 := carrierNco.io.sin

  val caGen = Module(new CA(params.caParams))
  caGen.io.satellite := io.svNumber
  io.caIndex := caGen.io.currIndex 
  
  val caNco = Module(new NCO[T](params.caNcoParams))
  val caNco2x = Module(new NCO[T](params.ca2xNcoParams))
  caNco.io.stepSize := io.dllIn
  caNco2x.io.stepSize := io.dllIn
  // For the CA-NCO just connecting the truncated one bit out
  caGen.io.fco := caNco.io.truncateRegOut.asSInt
  caGen.io.fco2x := caNco2x.io.truncateRegOut.asSInt

  val multIE = Module(new Mul[T](params.mulParams))
  multIE.io.in1 := multI.io.out
  multIE.io.in2 := caGen.io.early
  val intDumpIE = Module(new IntDump[T](params.intParams))
  intDumpIE.io.in := multIE.io.out
  intDumpIE.io.dump := io.dump
  io.toLoop.ie := intDumpIE.io.integ

  val multIP = Module(new Mul[T](params.mulParams))
  multIP.io.in1 := multI.io.out
  multIP.io.in2 := caGen.io.punctual
  val intDumpIP = Module(new IntDump[T](params.intParams))
  intDumpIP.io.in := multIP.io.out
  intDumpIP.io.dump := io.dump
  io.toLoop.ip := intDumpIP.io.integ

  val multIL = Module(new Mul[T](params.mulParams))
  multIL.io.in1 := multI.io.out
  multIL.io.in2 := caGen.io.late
  val intDumpIL = Module(new IntDump[T](params.intParams))
  intDumpIL.io.in := multIL.io.out
  intDumpIL.io.dump := io.dump
  io.toLoop.il := intDumpIL.io.integ

  val multQE = Module(new Mul[T](params.mulParams))
  multQE.io.in1 := multQ.io.out
  multQE.io.in2 := caGen.io.early
  val intDumpQE = Module(new IntDump[T](params.intParams))
  intDumpQE.io.in := multQE.io.out
  intDumpQE.io.dump := io.dump
  io.toLoop.qe := intDumpQE.io.integ

  val multQP = Module(new Mul[T](params.mulParams))
  multQP.io.in1 := multQ.io.out
  multQP.io.in2 := caGen.io.punctual
  val intDumpQP = Module(new IntDump[T](params.intParams))
  intDumpQP.io.in := multQP.io.out
  intDumpQP.io.dump := io.dump
  io.toLoop.qp := intDumpQP.io.integ

  val multQL = Module(new Mul[T](params.mulParams))
  multQL.io.in1 := multQ.io.out
  multQL.io.in2 := caGen.io.late
  val intDumpQL = Module(new IntDump[T](params.intParams))
  intDumpQL.io.in := multQL.io.out
  intDumpQL.io.dump := io.dump
  io.toLoop.ql := intDumpQL.io.integ
}
