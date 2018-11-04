package gps

import chisel3._
import dsptools.numbers._

trait TrackingTopParams[T <: Data] {
  val adcWidth: Int
  val carrierNcoParams: NcoParams[T]
  val caNcoParams: NcoParams[T]
  val ca2xNcoParams: NcoParams[T]
  val caParams: CAParams
  val mulParams: MulParams[T]
  val intParams: IntDumpParams[T]
}
case class ExampleTrackingTopParams() extends TrackingTopParams[SInt] {
  // Assume ADC in is a 2bit signed number
  val adcWidth = 2
  // Carrier NCO is 32 bits wide counter, 2 bit out and has a sin output
  val carrierNcoParams = SIntNcoParams(32, 2, true)
  // Ca NCO is 32 bits wide and has no sin output, 1 bit output
  val caNcoParams = SIntNcoParams(32, 1, false)
  // Ca NCO 2x is only 31 bits wide to create double the frequency
  val ca2xNcoParams = SIntNcoParams(31, 1, false)
  // Ca Params
  val caParams = CAParams(1, 2)
  // Multipliers are 2 bit in and 2 bit out
  val mulParams = SampledMulParams(2)
  // Int Params 
  val intParams = SampledIntDumpParams(2, Math.pow(2, 20).toInt)
}

class TrackingTopIO[T <: Data](params: TrackingTopParams[T]) extends Bundle {
  val adcSample = Input(SInt(params.adcWidth.W))
  val svNumber = Input(UInt(6.W)) //fixed width due to number of satellites
  val dump = Input(Bool())
  val ie = Output(SInt())
  val ip = Output(SInt())
  val il = Output(SInt())
  val qe = Output(SInt())
  val qp = Output(SInt())
  val ql = Output(SInt())
  val dllIn = Input(UInt())
  val costasIn = Input(UInt())
}
object TrackingTopIO {
  def apply[T <: Data](params: TrackingTopParams[T]): TrackingTopIO[T] =
    new TrackingTopIO(params)
}

class TrackingTop[T <: Data : Ring : Real](val params: TrackingTopParams[T]) extends Module {
  val io = IO(TrackingTopIO(params))

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
  
  val caNco = Module(new NCO[T](params.caNcoParams))
  val caNco2x = Module(new NCO[T](params.ca2xNcoParams))
  caNco.io.stepSize := io.dllIn
  caNco.io.stepSize := io.dllIn
  caGen.io.fco := caNco.io.cos
  caGen.io.fco2x := caNco2x.io.cos

  val multIE = Module(new Mul[T](params.mulParams))
  multIE.io.in1 := multI.io.out
  multIE.io.in2 := caGen.io.early
  val intDumpIE = Module(new IntDump[T](params.intParams))
  intDumpIE.io.in := multIE.io.out
  intDumpIE.io.dump := io.dump

  val multIP = Module(new Mul[T](params.mulParams))
  multIP.io.in1 := multI.io.out
  multIP.io.in2 := caGen.io.punctual
  val intDumpIP = Module(new IntDump[T](params.intParams))
  intDumpIP.io.in := multIP.io.out
  intDumpIP.io.dump := io.dump

  val multIL = Module(new Mul[T](params.mulParams))
  multIL.io.in1 := multI.io.out
  multIL.io.in2 := caGen.io.late
  val intDumpIL = Module(new IntDump[T](params.intParams))
  intDumpIL.io.in := multIL.io.out
  intDumpIL.io.dump := io.dump

  val multQE = Module(new Mul[T](params.mulParams))
  multQE.io.in1 := multQ.io.out
  multQE.io.in2 := caGen.io.early
  val intDumpQE = Module(new IntDump[T](params.intParams))
  intDumpQE.io.in := multQE.io.out
  intDumpQE.io.in := io.dump

  val multQP = Module(new Mul[T](params.mulParams))
  multQP.io.in1 := multQ.io.out
  multQP.io.in2 := caGen.io.punctual
  val intDumpQP = Module(new IntDump[T](params.intParams))
  intDumpQP.io.in := multQP.io.out
  intDumpQP.io.in := io.dump

  val multQL = Module(new Mul[T](params.mulParams))
  multQL.io.in1 := multQ.io.out
  multQL.io.in2 := caGen.io.late
  val intDumpQL = Module(new IntDump[T](params.intParams))
  intDumpQL.io.in := multQL.io.out
  intDumpQP.io.in := io.dump
}
