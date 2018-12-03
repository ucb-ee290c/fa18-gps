package gps

import chisel3._
import chisel3.experimental.FixedPoint
import chisel3.util._

import dsptools.numbers._

import scala.math._

/**
 * Base Class for Loop Filter Parameters
 */
trait LoopFilterParams[T <: Data] {
  val proto: T
  val dcGain: Double
  val bandwidth: Double
  val sampleRate: Double
}

/**
 * Loop Filter Parameters for a Fixed Point output
 */
case class FixedFilterParams(
  inWidth: Int, 
  inBP: Int,
  dcGain: Double,
  bandwidth: Double,
  sampleRate: Double,
) extends LoopFilterParams[FixedPoint] {
  //FIXME: Fixed point widths hardcoded
  val proto = FixedPoint(20.W, 16.BP)
}

/**
 * Function to get Loop Filter coefficients from dcGain and bandwidth
 * The generated filter is a 1st order CT low pass filter that is 
 * Bilinear-transformed to find DT coefficients for the sample rate
 * H(s) = dcGain / (1 + s/(2*pi*bandwidth))
 * H(z) = a*(1 + z^-1)/(1 - b*z^-1)
 * This function calculates a and b so the above filters have the same
 * frequency response. 
 */
object GetLoopFilterCoeffs {
  def apply[T <: Data](params: LoopFilterParams[T]): (Double, Double) = {
    val tau = 1/(2*Pi*params.bandwidth)
    val time = 1/params.sampleRate
    ((params.dcGain/(1+2*tau/time)), ((1-2*tau/time)/(1+2*tau/time)))
  }
}
 
/**
 * Loop Filter IO
 */
class LoopFilterIO[T <: Data](params: LoopFilterParams[T]) extends Bundle {
  val in: T = Input(params.proto.cloneType)
  val out: T = Output(params.proto.cloneType)
  val valid = Input(Bool())
}
object LoopFilterIO {
  def apply[T <: Data](params: LoopFilterParams[T]): LoopFilterIO[T] = new LoopFilterIO(params)
}

/**
 * Loop Filter module
 */
class LoopFilter[T <: Data : Ring : ConvertableTo](params: LoopFilterParams[T]) extends Module {
  val io = IO(LoopFilterIO(params))
  val (a, b) = GetLoopFilterCoeffs(params) 
  val x = RegInit(params.proto.cloneType, Ring[T].zero)
  val y = RegInit(params.proto.cloneType, Ring[T].zero)
  
  when(io.valid) {
    io.out := ConvertableTo[T].fromDouble(a)*(io.in + x) -
      ConvertableTo[T].fromDouble(b)*y
    x := io.in
    y := io.out
  }.otherwise{
    io.out := Ring[T].zero
  }
}
