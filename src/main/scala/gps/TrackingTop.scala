
package gps

import chisel3._

trait TrackingTopParams {
  val adcWidth: Int
  val ncoWidth: Int
  val codeLength: Int //1023
}

class TrackingTopIO(params: TrackingTopParams) extends Bundle {
  val adcSample = Input(SInt(params.adcWidth.W))
  val svNumber = Input(UInt(6.W)) //fixed width due to number of satellites
}

object TrackingTopIO {
  def apply[T <: Data](params: TrackingTopParams): TrackingTopIO =
    new TrackingTopIO(params)
}


class TrackingTop (val params: TrackingTopParams) extends Module {
  val io = IO(TrackingTopIO(params))

//  val CAGen = Module(new CA(CAParams(fcoWidth, codeWidth)))
//  CAGen.io.satellite := io.svNumber
//  CAGen.io.fco := fromNCO
//  CAGen.io.fco2x := from NCO2

  //instantiate DLL, Costas Loop, NCO's, packetizer etc. //TODO: Instantiate remaining blocks

  //could make into a vec if wanted it to be more variable length and connections more concise
  val multI = Module(new Mul[SInt](SampledMulParams(params.adcWidth)))
  multI.io.in1 := io.adcSample
//  multI.io.in2 := costasLoopOut.cos

  val multQ = Module(new Mul[SInt](SampledMulParams(params.adcWidth)))
  multQ.io.in1 := io.adcSample
//  multQ.io.in2 := costasLoopOut.sin

  val multIE = Module(new Mul[SInt](SampledMulParams(2*params.adcWidth)))
  multIE.io.in1 := multI.io.out
  multIE.io.in2 := CAGen.io.early

  val multIP = Module(new Mul[SInt](SampledMulParams(2*params.adcWidth)))
  multIP.io.in1 := multI.io.out
//  multIP.io.in2 := CAGen.io.punctual

  val multIL = Module(new Mul[SInt](SampledMulParams(2*params.adcWidth)))
  multIL.io.in1 := multI.io.out
//  multIL.io.in2 := CAGen.io.late

  val multQE = Module(new Mul[SInt](SampledMulParams(2*params.adcWidth)))
  multQE.io.in1 := multQ.io.out
//  multQE.io.in2 := CAGen.io.early

  val multQP = Module(new Mul[SInt](SampledMulParams(2*params.adcWidth)))
  multQP.io.in1 := multQ.io.out
//  multQP.io.in2 := CAGen.io.punctual

  val multQL = Module(new Mul[SInt](SampledMulParams(2*params.adcWidth)))
  multQL.io.in1 := multQ.io.out
//  multQL.io.in2 := CAGen.io.late


  //again, could make into a vec? might make naming less clear
  val intDumpIE = Module(new IntDump[SInt](SampledIntDumpParams(params.adcWidth, params.codeLength))) //TODO: change bitwidth here to proper value (output of Mul or truncated)
  intDumpIE.io.in := multIE.io.out
//  intDumpIE.io.dump := dump

  val intDumpIP = Module(new IntDump[SInt](SampledIntDumpParams(params.adcWidth, params.codeLength)))
  intDumpIP.io.in := multIP.io.out
//  intDumpIP.io.dump := dump

  val intDumpIL = Module(new IntDump[SInt](SampledIntDumpParams(params.adcWidth, params.codeLength)))
  intDumpIL.io.in := multIL.io.out
//  intDumpIL.io.dump := dump

  val intDumpQE = Module(new IntDump[SInt](SampledIntDumpParams(params.adcWidth, params.codeLength)))
  intDumpQE.io.in := multQE.io.out
//  intDumpQE.io.dump := dump

  val intDumpQP = Module(new IntDump[SInt](SampledIntDumpParams(params.adcWidth, params.codeLength)))
  intDumpQP.io.in := multQP.io.out
//  intDumpQP.io.dump := dump

  val intDumpQL = Module(new IntDump[SInt](SampledIntDumpParams(params.adcWidth, params.codeLength)))
  intDumpQL.io.in := multQL.io.out
//  intDumpQL.io.dump := dump




}