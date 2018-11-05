package gps

import breeze.numerics.{log, log10}
import chisel3._
import chisel3.util.RegEnable
import chisel3.experimental.FixedPoint
import dspblocks.ShiftRegisterWithReset
import dspjunctions.ValidWithSync
import dsptools.numbers._

import scala.collection._

trait FFTMulParams[T <: Data] {
  val protoData: T
  val lanes: Int // How many inputs
  val pipeStages: Int // How many pipelined output
}

case class FixedFFTMulParams(
  width: Int,
  bp: Int,
  laneCount: Int,
  pipeStageCount: Int,
) extends FFTMulParams[FixedPoint] {
  val protoData = FixedPoint(width.W, bp.BP)
  val lanes = laneCount
  val pipeStages = pipeStageCount
}

/**
  * Bundle type as IO for FIR Filter modules
  */
class FFTMulIO[T <: chisel3.Data : Ring](params: FFTMulParams[T]) extends Bundle {
  val dataIn = Input(ValidWithSync(Vec(params.lanes, params.protoData.cloneType)))
  val caIn = Input(ValidWithSync(Vec(params.lanes, params.protoData.cloneType)))
  val out = Output(ValidWithSync(Vec(params.lanes, params.protoData.cloneType)))
  //TODO: does it need extend bits?
  override def cloneType: this.type = FFTMulIO(params).asInstanceOf[this.type]
}
object FFTMulIO {
  def apply[T <: chisel3.Data : Ring](params: FFTMulParams[T]): FFTMulIO[T] =
    new FFTMulIO(params)
}

class FFTMul[T <: chisel3.Data : Ring](val params: FFTMulParams[T]) extends Module {
  require(params.lanes > 0, "Must have parallel input size greater than 1")
  require(params.pipeStages > 0, "pipeline stage numbers must greater than 1, should depends on FFT pipeline depth")
  val io = IO(FFTMulIO[T](params))
  val shift_en = Wire(Bool())
  val counter = RegInit(UInt( ((log10(params.lanes)/log10(2)).ceil.toInt+1).W ),0.U)


  when(io.dataIn.valid === true.B && io.caIn.valid === true.B) {
    shift_en := true.B
    io.out.bits.zip(io.dataIn.bits.zip(io.caIn.bits)).foreach{case (o,(data, ca)) => o := data * ca}
    io.out.valid := true.B
  } .otherwise{
    shift_en := false.B
    io.out.valid := false.B
  }

  io.out.sync := (ShiftRegisterWithReset(io.dataIn.valid, params.pipeStages, false.B, shift_en) && shift_en)

}