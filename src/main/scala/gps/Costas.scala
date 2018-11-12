
package gps

import chisel3._
import chisel3.util._
import dsptools.numbers._

trait CostasParams[T <: Data] {
  val protoData: T
  val protoFreq: T
  val protoPhase: T
}

case class SampledCostasParams(dataWidth: Int, freqWidth:Int, phaseWidth: Int) extends CostasParams[SInt] {
  val protoData = SInt(dataWidth.W)
  val protoFreq = SInt(freqWidth.W)
  val protoPhase = SInt(phaseWidth.W)
}

class CostasIO[T <: Data](params: CostasParams[T]) extends Bundle {
  val Ip = Input(params.protoData)
  val Qp = Input(params.protoData)
  val freqBias = Input(params.protoFreq)
  val phaseCtrl = Output(params.protoPhase)
  val freqCtrl = Output(params.protoFreq)
}

object CostasIO {
  def apply[T <: Data](params: CostasParams[T]): CostasIO[T] =
    new CostasIO(params)
}

class Costas[T <: Data : Ring] (val params: CostasParams[T]) extends Module {
  val io = IO(CostasIO(params))

  // use fake value for now
  io.freqCtrl := io.freqBias
  io.phaseCtrl := 0.U
}