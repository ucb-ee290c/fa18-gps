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


trait FFTChainParams[T <: Data] {
  val width: Int
  val bp: Int
  val nSample: Int
  val nLane: Int
  val nStgFFT: Int
  val nStgIFFT: Int
  val nStgFFTMul: Int
  val config_fft: FFTConfig[T]
  val config_ifft: FFTConfig[T]
  val FFTMulParams: FFTMulParams[T]
  val pDataIn: T
  val pDataOut: T

}

case class FixedFFTChainParams(
                                val width: Int,
                                val bp: Int,
                                val nSample: Int,
                                val nLane: Int,
                                val nStgFFT: Int,
                                val nStgIFFT: Int,
                                val nStgFFTMul: Int,
                          ) extends FFTChainParams[FixedPoint] {

  val pDataIn = FixedPoint(width.W, bp.BP)
  val pDataOut = FixedPoint(width.W, bp.BP)


  val config_fft = FFTConfig[FixedPoint](
    genIn = DspComplex(FixedPoint(width.W, bp.BP)),
    genOut = DspComplex(FixedPoint(width.W, bp.BP)),
    n = nSample,
    pipelineDepth = nStgFFT,
    lanes = nLane,
    inverse = false,
    quadrature = false,
    unscrambleOut = false,
    unscrambleIn = false,
  )
  val config_ifft = FFTConfig[FixedPoint](
    genIn = DspComplex(FixedPoint(width.W, bp.BP)),
    genOut = DspComplex(FixedPoint(width.W, bp.BP)),
    pipelineDepth = nStgIFFT,
    lanes = nLane,
    inverse = true,
    quadrature = false,
    unscrambleOut = false,
    unscrambleIn = true,
  )
  val FFTMulParams = complexFFTMulParams(
    width = width,
    bp = bp,
    laneCount = nLane,
    pipeStageCount = nStgFFTMul
  )

}





// input interface within the acquisition loop
class FFTChainInputBundle[T <: Data](params: FFTChainParams[T]) extends Bundle {

  val ADC: Vec[T] = Input(Vec(params.nLane, params.pDataIn))
  val cos: Vec[T] = Input(Vec(params.nLane, params.pDataIn))
  val sin: Vec[T] = Input(Vec(params.nLane, params.pDataIn))
  val CA: Vec[T] = Input(Vec(params.nLane, params.pDataIn))
  val sync = Input(Bool())
  val valid = Input(Bool())



  override def cloneType: this.type = FFTChainInputBundle(params).asInstanceOf[this.type]
}
object FFTChainInputBundle {
  def apply[T <: Data](params: FFTChainParams[T]): FFTChainInputBundle[T] = new FFTChainInputBundle(params)
}


// output interface within the acquisition loop
class FFTChainOutputBundle[T <: Data](params: FFTChainParams[T]) extends Bundle {

  val corrReal: Vec[T] = Output(Vec(params.nLane, params.pDataOut))
  val corrImag: Vec[T] = Output(Vec(params.nLane, params.pDataOut))
  val sync = Output(Bool())
  val valid = Output(Bool())


  override def cloneType: this.type = FFTChainOutputBundle(params).asInstanceOf[this.type]
}
object FFTChainOutputBundle {
  def apply[T <: Data](params: FFTChainParams[T]): FFTChainOutputBundle[T] = new FFTChainOutputBundle(params)
}



class FFTChainIO[T <: Data](params: FFTChainParams[T]) extends Bundle {

  val in = FFTChainInputBundle(params)
  val out = FFTChainOutputBundle(params)

  override def cloneType: this.type = FFTChainIO(params).asInstanceOf[this.type]
}
object FFTChainIO {
  def apply[T <: Data](params: FFTChainParams[T]): FFTChainIO[T] = new FFTChainIO(params)
}



class FFTChain[T <: Data:Real:BinaryRepresentation] (val params: FFTChainParams[FixedPoint])
  ( implicit p: Parameters = null ) extends Module {

  val io = IO(FFTChainIO(params))

  val fft_ADC = Module(new FFT[FixedPoint](params.config_fft))
  val fft_CA = Module(new FFT[FixedPoint](params.config_fft))
  val ifft = Module(new FFT[FixedPoint](params.config_ifft))
  val fft_mul = Module(new FFTMul[FixedPoint](params.FFTMulParams))




  val adc_i = io.in.ADC.zip(io.in.cos).map{ case(x: FixedPoint, y: FixedPoint) => x * y }
  val adc_q = io.in.ADC.zip(io.in.sin).map{ case(x: FixedPoint, y: FixedPoint) => x * y }

  fft_ADC.io.in.valid := io.in.valid
  fft_ADC.io.in.sync := io.in.sync
  fft_ADC.io.data_set_end_clear := true.B
  fft_CA.io.in.valid := io.in.valid
  fft_CA.io.in.sync := io.in.sync
  fft_CA.io.data_set_end_clear := true.B


  // TODO: convert type here!!!
  for (i <- 0 until params.nLane) {
    fft_ADC.io.in.bits(i).real := adc_i(i)
    fft_ADC.io.in.bits(i).imag := adc_q(i)
    fft_CA.io.in.bits(i).real := io.in.CA(i)
    fft_CA.io.in.bits(i).imag := 0.U.asTypeOf(FixedPoint(params.width.W, params.bp.BP))
  }

  fft_mul.io.dataIn := fft_ADC.io.out
  fft_mul.io.caIn := fft_CA.io.out

  ifft.io.in := fft_mul.io.out
  ifft.io.data_set_end_clear := true.B



  for (i <- 0 until params.nLane) {
    io.out.corrReal(i) := ifft.io.out.bits(i).real
    io.out.corrImag(i) := ifft.io.out.bits(i).imag
  }

  io.out.valid := ifft.io.out.valid
  io.out.sync := ifft.io.out.sync



}





