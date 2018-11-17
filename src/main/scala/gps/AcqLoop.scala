package gps

import chisel3._
//import chisel3.util.Decoupled
import chisel3.util._
import scala.math._
import dsptools.numbers._
import dsptools.numbers.implicits._
import dsptools.numbers.DspComplex
import chisel3.experimental.FixedPoint
import freechips.rocketchip.amba.axi4stream._
import freechips.rocketchip.config.Parameters
import freechips.rocketchip.diplomacy._
import freechips.rocketchip.regmapper._
import freechips.rocketchip.tilelink._


trait ALoopParams[T1 <: Data, T2 <: Data] {
  val wADC: Int
  val wCA: Int
  val wNCOTct: Int
  val wNCORes: Int
  val wCorrelation: Int
  val nSample: Int
  val nLoop: Int
  val nFreq: Int
  val nLane: Int
  val nStgFFT: Int
  val nStgIFFT: Int
  val nStgFFTMul: Int
  val freqMin: Int
  val freqStep: Int
  val ACtrlParams: ACtrlParams[T2]
  val NCOParams_ADC: NcoParams[T1]
  val NCOParams_CA: NcoParams[T1]
  val DesParams_ADC: DesParams[T1]
  val DesParams_CA: DesParams[T1]
  val DesParams_NCO: DesParams[T1]
  val config_fft: FFTConfig[T2]
  val config_ifft: FFTConfig[T2]
  val FFTMulParams: FFTMulParams[T2]
  val CA_Params: CAParams
  val pADC: T1
  val pSate: UInt
}

case class EgALoopParams(
                            val wADC: Int,
                            val wCA: Int,
                            val wNCOTct: Int,
                            val wNCORes: Int,
                            val nSample: Int,
                            val nLoop: Int,
                            val nFreq: Int,
                            val nLane: Int,
                            val nStgFFT: Int,
                            val nStgIFFT: Int,
                            val nStgFFTMul: Int,
                            val freqMin: Int,
                            val freqStep: Int,
                          ) extends ALoopParams[SInt, FixedPoint] {

  val pADC = SInt(wADC.W)
  val pSate = UInt(5.W)

  val wCorrelation = wADC + wCA + wNCORes + log2Ceil(nSample) + 1

  val ACtrlParams = FixedACtrlParams (
    nLoop = nLoop,
    nFreq = nFreq,
    nSample = nSample,
    nLane = nLane,
    wCorrelation = wCorrelation,
    wLoop = log2Ceil(nLoop) + 1,
    wIdxFreq = log2Ceil(nFreq) + 1,
    wFreq = 32,
    wCodePhase = log2Ceil(nSample) + 1,
    wLane = log2Ceil(nLane) + 1,
    wADC = wADC,
    wSate = 6,
    freqMin = freqMin,
    freqStep = freqStep,
  )

  val NCOParams_ADC = SIntNcoParams (
    resolutionWidth = wNCORes,
    truncateWidth = wNCOTct,
    sinOut = true,
  )
  val NCOParams_CA = SIntNcoParams (
    resolutionWidth = wNCORes,
    truncateWidth = wNCOTct,
    sinOut = true,
  )

  val DesParams_ADC = SIntDesParams(
    width = wADC,
    nSample = nSample,
    nLane = nLane,
  )
  val DesParams_CA = SIntDesParams(
    width = wCA,
    nSample = nSample,
    nLane = nLane,
  )
  val DesParams_NCO = SIntDesParams(
    width = wNCOTct,
    nSample = nSample,
    nLane = nLane,
  )
  val config_fft = FFTConfig[FixedPoint](
    genIn = DspComplex(FixedPoint((wCorrelation+8).W, 8.BP)),
    genOut = DspComplex(FixedPoint((wCorrelation+8).W, 8.BP)),
    n = nSample,
    pipelineDepth = nStgFFT,
    lanes = nLane,
    inverse = false,
  )
  val config_ifft = FFTConfig[FixedPoint](
    genIn = DspComplex(FixedPoint((wCorrelation+8).W, 8.BP)),
    genOut = DspComplex(FixedPoint((wCorrelation+8).W, 8.BP)),
    n = nSample,
    pipelineDepth = nStgIFFT,
    lanes = nLane,
    inverse = true,
  )
  val FFTMulParams = FixedFFTMulParams(
    width = wCorrelation + 8,
    bp = 8,
    laneCount = nLane,
    pipeStageCount = nStgFFTMul
  )
  val CA_Params = CAParams(
    fcoWidth = wNCOTct,
    codeWidth = wCA
  )

}





// input interface within the acquisition loop
class ALoopInputBundle[T1 <: Data, T2 <: Data](params: ALoopParams[T1, T2]) extends Bundle {

  val ADC: T1 = Input(params.pADC)
  val idx_sate: UInt = Input(params.pSate)
  val ready = Output(Bool())
  val valid = Input(Bool())

  override def cloneType: this.type = ALoopInputBundle(params).asInstanceOf[this.type]
}
object ALoopInputBundle {
  def apply[T1 <: Data, T2 <: Data](params: ALoopParams[T1, T2]): ALoopInputBundle[T1, T2] = new ALoopInputBundle(params)
}


// output interface within the acquisition loop
class ALoopOutputBundle[T1 <: Data, T2 <: Data](params: ALoopParams[T1, T2]) extends Bundle {

  val iFreqOpt: UInt = Output(params.ACtrlParams.pIdxFreq.cloneType)
  val freqOpt: UInt = Output(params.ACtrlParams.pFreq.cloneType)
  val CPOpt: UInt = Output(params.ACtrlParams.pCodePhase.cloneType)
  val sateFound = Output(Bool())
  val ready = Input(Bool())
  val valid = Output(Bool())

  override def cloneType: this.type = ALoopOutputBundle(params).asInstanceOf[this.type]
}
object ALoopOutputBundle {
  def apply[T1 <: Data, T2 <: Data](params: ALoopParams[T1, T2]): ALoopOutputBundle[T1, T2] = new ALoopOutputBundle(params)
}


class ALoopDebugBundle[T1 <: Data, T2 <: Data](params: ALoopParams[T1, T2]) extends Bundle {

  val FreqNow: UInt = Output(params.ACtrlParams.pFreq.cloneType)
  val iFreqNow: UInt = Output(params.ACtrlParams.pIdxFreq.cloneType)
  val iFreqNext: UInt = Output(params.ACtrlParams.pIdxFreq.cloneType)
  val iLoopNow: UInt = Output(params.ACtrlParams.pLoop.cloneType)
  val iLoopNext: UInt = Output(params.ACtrlParams.pLoop.cloneType)
  val iCPNow: UInt = Output(params.ACtrlParams.pCodePhase.cloneType)
  val iCPNext: UInt = Output(params.ACtrlParams.pCodePhase.cloneType)
  val max: T2 = Output(params.ACtrlParams.pMax.cloneType)
  val reg_max: T2 = Output(params.ACtrlParams.pMax.cloneType)
  val reg_tag_CP = Output(Bool())
  val reg_tag_Loop = Output(Bool())
  val reg_tag_Freq = Output(Bool())

  val Correlation = Output(Vec(params.nLane, params.ACtrlParams.pCorrelation))
  val iFreqOptItm: UInt = Output(params.ACtrlParams.pIdxFreq.cloneType)
  val iFreqOptOut: UInt = Output(params.ACtrlParams.pIdxFreq.cloneType)
  val CPOptItm: UInt = Output(params.ACtrlParams.pCodePhase.cloneType)
  val CPOptOut: UInt = Output(params.ACtrlParams.pCodePhase.cloneType)
  val vec = Output(Vec(params.nSample, params.ACtrlParams.pCorrelation.cloneType))
  val state = Output(Bool())


  override def cloneType: this.type = ALoopDebugBundle(params).asInstanceOf[this.type]
}
object ALoopDebugBundle {
  def apply[T1 <: Data, T2 <: Data](params: ALoopParams[T1, T2]): ALoopDebugBundle[T1, T2] = new ALoopDebugBundle(params)
}



class ALoopIO[T1 <: Data, T2 <: Data](params: ALoopParams[T1, T2]) extends Bundle {

  val in = ALoopInputBundle(params)
  val out = ALoopOutputBundle(params)
  val debug = ALoopDebugBundle(params)

  override def cloneType: this.type = ALoopIO(params).asInstanceOf[this.type]
}
object ALoopIO {
  def apply[T1 <: Data, T2 <: Data](params: ALoopParams[T1, T2]): ALoopIO[T1, T2] =
    new ALoopIO(params)
}





class ALoop[T1 <: Data:Real:BinaryRepresentation, T2 <: Data:Real:BinaryRepresentation]
  (
//    val ACtrlParams: ACtrlParams[FixedPoint],
//    val NCOParams_ADC: NcoParams[SInt], val NCOParams_CA: NcoParams[SInt],
//    val DesParams_ADC: DesParams[SInt], val DesParams_CA: DesParams[SInt], val DesParams_NCO: DesParams[SInt],
//    val config_fft: FFTConfig[FixedPoint], val config_ifft: FFTConfig[FixedPoint],
//    val FFTMulParams: FFTMulParams[FixedPoint],
//    val CA_Params: CAParams,
    val params: ALoopParams[SInt, FixedPoint],
  )
  ( implicit p: Parameters = null )
extends Module {

  val io = IO(ALoopIO(params))


  val actrl = Module(new ACtrl[FixedPoint](params.ACtrlParams))
  val ca = Module(new CA(params.CA_Params))
  val nco_ADC = Module(new NCO[SInt](params.NCOParams_ADC))
  val nco_CA1x = Module(new NCO[SInt](params.NCOParams_CA))
  val nco_CA2x = Module(new NCO[SInt](params.NCOParams_CA))
  val des_ADC = Module(new Des[SInt](params.DesParams_ADC))
  val des_CA = Module(new Des[SInt](params.DesParams_CA))
  val des_cos = Module(new Des[SInt](params.DesParams_NCO))
  val des_sin = Module(new Des[SInt](params.DesParams_NCO))
  val fft_ADC = Module(new FFT[FixedPoint](params.config_fft))
  val fft_CA = Module(new FFT[FixedPoint](params.config_fft))
  val ifft = Module(new FFT[FixedPoint](params.config_ifft))
  val fft_mul = Module(new FFTMul[FixedPoint](params.FFTMulParams))


  val fsample = 16367600
  val stepSizeCoeff = pow(2, params.NCOParams_ADC.resolutionWidth) / fsample

  // TODO: need fix here
  val stepSize1x = actrl.io.Aout.freqNext.toSInt * ConvertableTo[SInt].fromDouble(stepSizeCoeff)
  val stepSize2x = stepSize1x * ConvertableTo[SInt].fromInt(2)





  ca.io.satellite := io.in.idx_sate
  ca.io.fco := nco_CA1x.io.sin
  ca.io.fco2x := nco_CA2x.io.sin

  nco_ADC.io.stepSize := stepSize1x
  nco_CA1x.io.stepSize := stepSize1x
  nco_CA2x.io.stepSize := stepSize2x

  val idle = WireInit(UInt(2.W), 0.U)
  val lock = WireInit(UInt(2.W), 1.U)
  val stream = WireInit(UInt(2.W), 2.U)
  val reg_freqNext = RegNext(actrl.io.Aout.freqNext, 0.U)
  val reg_loopNext = RegNext(actrl.io.Aout.loopNext, 0.U)



  // TODO: define des_out_ready and newreq here

  val all_des_valid = (des_ADC.io.valid && des_CA.io.valid) && (des_cos.io.valid && des_sin.io.valid)
  val des_out_ready = ifft.io.out.sync && all_des_valid
  val des_ADC_newreq = reg_loopNext =/= actrl.io.Aout.loopNext
  val des_CA_newreq = Mux(actrl.io.Tout.state === idle, true.B, false.B)
  val des_NCO_newreq = reg_freqNext =/= actrl.io.Aout.freqNext



  des_ADC.io.in := io.in.ADC
  des_ADC.io.ready := des_out_ready
  des_ADC.io.newreq := des_ADC_newreq
  des_ADC.io.offset := 0.U

  des_CA.io.in := ca.io.punctual
  des_CA.io.ready := des_out_ready
  des_CA.io.newreq := des_CA_newreq
  des_CA.io.offset := 0.U

  des_cos.io.in := nco_ADC.io.cos
  des_cos.io.ready := des_out_ready
  des_cos.io.newreq := des_NCO_newreq
  des_cos.io.offset := 0.U

  des_sin.io.in := nco_ADC.io.sin
  des_sin.io.ready := des_out_ready
  des_sin.io.newreq := des_NCO_newreq
  des_sin.io.offset := 0.U


  val all_des_isstream = (des_ADC.io.isstream && des_CA.io.isstream) && (des_cos.io.isstream && des_sin.io.isstream)
  val des_sync = des_ADC.io.end
  //  val cnt_buffer_max = (params.nSample / params.nLane - 1).U


  val adc_i = des_ADC.io.out.zip(des_cos.io.out).map{ case(x: SInt, y: SInt) => x * y }
  val adc_q = des_ADC.io.out.zip(des_sin.io.out).map{ case(x: SInt, y: SInt) => x * y }

  fft_ADC.io.in.valid := all_des_isstream
  fft_ADC.io.in.sync := des_sync
  fft_CA.io.in.valid := all_des_isstream
  fft_CA.io.in.sync := des_sync


  // TODO: convert type here!!!
  for (i <- 0 until params.DesParams_ADC.nLane) {
    fft_ADC.io.in.bits(i).real := adc_i(i)
    fft_ADC.io.in.bits(i).imag := adc_q(i)
    fft_CA.io.in.bits(i).real := des_CA.io.out(i)
    fft_CA.io.in.bits(i).imag := 0.U
  }

  fft_mul.io.dataIn := fft_ADC.io.out
  fft_mul.io.caIn := fft_CA.io.out

  ifft.io.in := fft_mul.io.out


  for (i <- 0 until params.DesParams_ADC.nLane) {
    actrl.io.Ain.Correlation(i) := ifft.io.out.bits(i).abssq()
  }
  actrl.io.Ain.valid := ifft.io.out.valid
  // TODO: should ready always be true?
  actrl.io.Aout.ready := true.B
  actrl.io.Tin.idx_sate := io.in.idx_sate
  actrl.io.Tin.valid := io.in.valid
  actrl.io.Tout.ready := io.out.ready

  io.out.iFreqOpt := actrl.io.Tout
  io.out.freqOpt := actrl.io.Tout.freqOpt
  io.out.CPOpt := actrl.io.Tout.CPOpt
  io.out.sateFound := actrl.io.Tout.sateFound
  io.out.valid := actrl.io.Tout.ready







//  actrl.io.Ain.Correlation := fft.io.***.abs()

//  nco.io.stepSize := actrl.io.Aout.


}





