package gps

import chisel3._
import dsptools.numbers._

trait MulParams[T <: Data] {
  val protoIn: T
  val protoOut: T
}

case class SampledMulParams(
  adcWidth: Int
) extends MulParams[SInt] {
  val protoIn = SInt(adcWidth.W)
  val protoOut = SInt((2*adcWidth).W)
}

class MulIO[T <: Data](params: MulParams[T]) extends Bundle {
  val in1 = Input(params.protoIn)
  val in2 = Input(params.protoIn)
  val out = Output(params.protoOut)
}

object MulIO {
  def apply[T <: Data](params: MulParams[T]): MulIO[T] =
    new MulIO(params)
}

class Mul[T <: Data : Ring] (val params: MulParams[T]) extends Module {
  val io = IO(MulIO(params))

  io.out := io.in1 * io.in2
}