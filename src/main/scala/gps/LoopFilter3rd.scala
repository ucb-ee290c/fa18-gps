package gps

import chisel3._
import chisel3.experimental.FixedPoint
import chisel3.util._

import dsptools.numbers._

import scala.math._

/**
 * Base Class for 3rd order Loop Filter Parameters
 */
trait LoopFilter3rdParams[T <: Data] {
  val proto: T
  val fBandwidth: Double
  val pBandwidth: Double
  val width: Int
  val BPWidth: Int
  val a2: Double
  val a3: Double
  val b3: Double
}

/**
 * Loop Filter Parameters for a Fixed Point output
 */
case class FixedFilter3rdParams(
  val fBandwidth: Double = 3.0,
  val pBandwidth: Double = 17.0,
  val width: Int,
  val BPWidth: Int,
  val a2: Double = 1.414,
  val a3: Double = 1.1,
  val b3: Double = 2.4,
) extends LoopFilter3rdParams[FixedPoint] {
  val proto = FixedPoint(width.W, BPWidth.BP)
}

/**
  * Reference: Understanding GPS Ch5.5
  * For fll loop, a second order filter
  * H(s) = w0^2/s^2 + a2*w0/s
  *
 */
object GetLoopFilter3rdW0s {
  def apply[T <: Data](params: LoopFilter3rdParams[T]): (Double, Double) = {
    val w0f = params.fBandwidth / 0.53
    val w0p = params.pBandwidth / 0.7845
    (w0f, w0p)
  }
}
 
/**
 * Loop Filter 3rd order IO
 */
class LoopFilter3rdIO[T <: Data](params: LoopFilter3rdParams[T]) extends Bundle {
  val freqErr: T = Input(params.proto.cloneType)
  val phaseErr: T = Input(params.proto.cloneType)
  val intTime: T = Input(params.proto.cloneType)
  val valid = Input(Bool())
  val out: T = Output(params.proto.cloneType)
}
object LoopFilter3rdIO {
  def apply[T <: Data](params: LoopFilter3rdParams[T]): LoopFilter3rdIO[T] = new LoopFilter3rdIO(params)
}

/**
 * Loop Filter 3rd order module
 */
class LoopFilter3rd[T <: Data : Ring : ConvertableTo](params: LoopFilter3rdParams[T]) extends Module {
  val io = IO(LoopFilter3rdIO(params))
  val (w0f, w0p) = GetLoopFilter3rdW0s(params)

  val outReg = RegInit(params.proto.cloneType, Ring[T].zero)
  val alpha = RegInit(params.proto.cloneType, Ring[T].zero)
  val beta = RegInit(params.proto.cloneType, Ring[T].zero)

  val betaWire = Mux(io.valid, (ConvertableTo[T].fromDouble(w0f) * ConvertableTo[T].fromDouble(w0f) * io.freqErr +
      ConvertableTo[T].fromDouble(w0p) * ConvertableTo[T].fromDouble(w0p) * ConvertableTo[T].fromDouble(w0p) *
        io.phaseErr) * io.intTime + beta,
       Ring[T].zero)
  val alphaWire = Mux(io.valid, (ConvertableTo[T].fromDouble(params.a2) * ConvertableTo[T].fromDouble(w0f) * io.freqErr +
      ConvertableTo[T].fromDouble(params.a3) * ConvertableTo[T].fromDouble(w0p) * ConvertableTo[T].fromDouble(w0p) * io.phaseErr +
      (betaWire + beta) * ConvertableTo[T].fromDouble(0.5)) * io.intTime + alpha,
      Ring[T].zero)

  when(io.valid) {
    outReg := ConvertableTo[T].fromDouble(params.b3) * ConvertableTo[T].fromDouble(w0p) * io.phaseErr +
      (alphaWire + alpha) * ConvertableTo[T].fromDouble(0.5)
    beta := betaWire
    alpha := alphaWire
  }.otherwise{
    outReg := Ring[T].zero
  }

  io.out := outReg

}
