package gps

import chisel3._

trait TrackingTopParams {
  val adcWidth: Int
  val carrierNcoParams: NcoParams
  val caNcoParams: NcoParams
  val ca2xNcoParams: NcoParams
  val caParams: CAParams
  val mulParams: MulParams
  val intParams: IntParams
}
case class ExampleTrackingTopParams extends TrackingTopParams {
  // Assume ADC in is a 2bit signed number
  val adcWidth = 2
  // Carrier NCO is 32 bits wide and has a sin output
  val carrierNcoParams = FixedNcoParams(32, true)
  // Ca NCO is 32 bits wide and has no sin output
  val caNcoParams = FixedNcoParams(32, false)
  // Ca NCO 3x is only 31 bits wide to create double the frequency
  val Ca2xNcoParams = FixedNcoParams(31, false)
  // Ca Params
  val caParams = 
  // Multipliers are 2 bit in and 2 bit out


class TrackingTopIO(params: TrackingTopParams) extends Bundle {
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
  def apply[T <: Data](params: TrackingTopParams): TrackingTopIO =
    new TrackingTopIO(params)
}

class TrackingTop (val params: TrackingTopParams) extends Module {
  val io = IO(TrackingTopIO(params))

  val carrierNco = Module(new NCO[SInt](params.carrierNcoParams))
  carrierNCO.io.stepSize := io.costasIn
  val multI = Module(new Mul[SInt](params.mulParams))
  multI.io.in1 := io.adcSample
  multI.io.in2 := carrierNco.io.cos

  val multQ = Module(new Mul[SInt](params.mulParams))
  multQ.io.in1 := io.adcSample
  multQ.io.in2 := carrierNco.io.sin

  val caGen = Module(new CA(params.caParams))
  caGen.io.satellite := io.svNumber
  
  val caNco = Module(new NCO[SInt](params.caNcoParams))
  val caNco2x = Module(new NCO[SInt](params.ca2xNcoParams))
  caNco.io.stepSize := io.dllIn
  caNco.io.stepSize := io.dllIn
  caGen.io.fco := caNco.io.cos
  caGen.io.fco2x := caNco2x.io.cos

  val multIE = Module(new Mul[SInt](params.mulParams))
  multIE.io.in1 := multI.io.out
  multIE.io.in2 := caGen.io.early
  val intDumpIE = Module(new IntDump[SInt](params.intParams)))
  intDumpIE.io.in := multIE.io.out
  intDumpIE.io.dump := io.dump

  val multIP = Module(new Mul[SInt](params.mulParams))
  multIP.io.in1 := multI.io.out
  multIP.io.in2 := caGen.io.punctual
  val intDumpIP = Module(new IntDump[SInt](params.intParams)))
  intDumpIP.io.in := multIP.io.out
  intDumpIP.io.dump := io.dump

  val multIL = Module(new Mul[SInt](params.mulParams))
  multIL.io.in1 := multI.io.out
  multIL.io.in2 := caGen.io.late
  val intDumpIL = Module(new IntDump[SInt](params.intParams)))
  intDumpIL.io.in := multIL.io.out
  intDumpIL.io.dump := io.dump

  val multQE = Module(new Mul[SInt](params.mulParams))
  multQE.io.in1 := multQ.io.out
  multQE.io.in2 := caGen.io.early
  val intDumpQE = Module(new IntDump[SInt](params.intParams)))
  intDumpQE.io.in := multQE.io.out
  intDumpQE.io.in := io.dump

  val multQP = Module(new Mul[SInt](params.mulParams))
  multQP.io.in1 := multQ.io.out
  multQP.io.in2 := caGen.io.punctual
  val intDumpQP = Module(new IntDump[SInt](params.intParams)))
  intDumpQP.io.in := multQP.io.out
  intDumpQP.io.in := io.dump

  val multQL = Module(new Mul[SInt](params.mulParams))
  multQL.io.in1 := multQ.io.out
  multQL.io.in2 := caGen.io.late
  val intDumpQL = Module(new IntDump[SInt](params.intParams)))
  intDumpQL.io.in := multQL.io.out
  intDumpQP.io.in := io.dump
}
