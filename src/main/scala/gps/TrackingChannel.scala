package gps

import chisel3._
import dsptools.numbers._

trait TrackingChannelParams[T <: Data] {
  val adcWidth: Int
  val carrierNcoParams: NcoParams[T]
  val caNcoParams: NcoParams[T]
  val ca2xNcoParams: NcoParams[T]
  val caParams: CAParams
  val mulParams: MulParams[T]
  val intParams: IntDumpParams[T]
}
case class ExampleTrackingChannelParams() extends TrackingChannelParams[SInt] {
  // Assume ADC in is a 2bit signed number
  val adcWidth = 5
  // Carrier NCO is 32 bits wide counter, 2 bit out and has a sin output
  val carrierNcoParams = SIntNcoParams(30, 5, true)
  // Ca NCO is 32 bits wide and has no sin output, 1 bit output
  val caNcoParams = SIntNcoParams(30, 1, false)
  // Ca NCO 2x is only 31 bits wide to create double the frequency
  val ca2xNcoParams = SIntNcoParams(29, 1, false)
  // Ca Params
  val caParams = CAParams(1, 2)
  // Multipliers are 2 bit in and 2 bit out
  val mulParams = SampledMulParams(2)
  // Int Params 
  val intParams = SampledIntDumpParams(2, Math.pow(2, 20).toInt)
}

class TrackingChannelIO[T <: Data](params: TrackingChannelParams[T]) extends Bundle {
  val adcSample = Input(SInt(params.adcWidth.W))
  val svNumber = Input(UInt(6.W)) //fixed width due to number of satellites
  val dump = Input(Bool())
  val ie = Output(SInt(32.W))
  val ip = Output(SInt(32.W))
  val il = Output(SInt(32.W))
  val qe = Output(SInt(32.W))
  val qp = Output(SInt(32.W))
  val ql = Output(SInt(32.W))
  val dllIn = Input(UInt(32.W))
  val costasIn = Input(UInt(32.W))
  val caIndex = Output(UInt(32.W))

  override def cloneType: this.type =
    TrackingChannelIO(params).asInstanceOf[this.type]
}
object TrackingChannelIO {
  def apply[T <: Data](params: TrackingChannelParams[T]): TrackingChannelIO[T] =
    new TrackingChannelIO(params)
}

class TrackingChannel[T <: Data : Ring : Real](val params: TrackingChannelParams[T]) extends Module {
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
  io.ie := intDumpIE.io.integ

  val multIP = Module(new Mul[T](params.mulParams))
  multIP.io.in1 := multI.io.out
  multIP.io.in2 := caGen.io.punctual
  val intDumpIP = Module(new IntDump[T](params.intParams))
  intDumpIP.io.in := multIP.io.out
  intDumpIP.io.dump := io.dump
  io.ip := intDumpIP.io.integ

  val multIL = Module(new Mul[T](params.mulParams))
  multIL.io.in1 := multI.io.out
  multIL.io.in2 := caGen.io.late
  val intDumpIL = Module(new IntDump[T](params.intParams))
  intDumpIL.io.in := multIL.io.out
  intDumpIL.io.dump := io.dump
  io.il := intDumpIL.io.integ

  val multQE = Module(new Mul[T](params.mulParams))
  multQE.io.in1 := multQ.io.out
  multQE.io.in2 := caGen.io.early
  val intDumpQE = Module(new IntDump[T](params.intParams))
  intDumpQE.io.in := multQE.io.out
  intDumpQE.io.dump := io.dump
  io.qe := intDumpQE.io.integ

  val multQP = Module(new Mul[T](params.mulParams))
  multQP.io.in1 := multQ.io.out
  multQP.io.in2 := caGen.io.punctual
  val intDumpQP = Module(new IntDump[T](params.intParams))
  intDumpQP.io.in := multQP.io.out
  intDumpQP.io.dump := io.dump
  io.qp := intDumpQP.io.integ

  val multQL = Module(new Mul[T](params.mulParams))
  multQL.io.in1 := multQ.io.out
  multQL.io.in2 := caGen.io.late
  val intDumpQL = Module(new IntDump[T](params.intParams))
  intDumpQL.io.in := multQL.io.out
  intDumpQL.io.dump := io.dump
  io.ql := intDumpQL.io.integ
}
