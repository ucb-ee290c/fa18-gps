package gps

import chisel3._
import chisel3.util.RegEnable
import chisel3.experimental.FixedPoint
import dspblocks.ShiftRegisterWithReset
import dspjunctions.ValidWithSync
import dsptools.numbers._

import scala.collection._

/**
  * Base class for FIR Filter parameters
  *
  * These are type generic
  */
trait FIRFilterParams[T <: Data] {
  val protoData: T
  val taps: Seq[T]
  //TODO: Implementation -> Direct or Transposed?
  //TODO: # of Pipeline Stages?
  //TODO: Data Buffer Type -> Linear Shift Register or Circular Buffer?
}

case class FixedFIRFilterParams(
                                 width: Int,
                                 bp: Int,
                                 tapSeq: Seq[FixedPoint]
) extends FIRFilterParams[FixedPoint] {
  val protoData = FixedPoint(width.W, bp.BP)
  val taps = tapSeq
}

/**
  * Bundle type as IO for FIR Filter modules
  */
class FIRFilterIO[T <: chisel3.Data : Ring](params: FIRFilterParams[T]) extends Bundle {
  val in = Flipped(ValidWithSync(params.protoData.cloneType))
  val out = ValidWithSync(params.protoData.cloneType)

  override def cloneType: this.type = FIRFilterIO(params).asInstanceOf[this.type]
}
object FIRFilterIO {
  def apply[T <: chisel3.Data : Ring](params: FIRFilterParams[T]): FIRFilterIO[T] =
    new FIRFilterIO(params)
}

class ConstantCoefficientFIRFilter[T <: chisel3.Data : Ring](val params: FIRFilterParams[T]) extends Module {
  require(params.taps.nonEmpty)
  val io = IO(FIRFilterIO[T](params))

  val shift_en = Wire(Bool())

  val regs = mutable.ArrayBuffer[T]()
  for(i <- params.taps.indices) {
    if(i == 0) regs += RegEnable(io.in.bits, Ring[T].zero, shift_en)
    else       regs += RegEnable(regs(i - 1), Ring[T].zero, shift_en)
  }

  val muls = mutable.ArrayBuffer[T]()
  for(i <- params.taps.indices) {
    muls += regs(i) * params.taps(i)
  }

  val accumulator = mutable.ArrayBuffer[T]()
  for(i <- params.taps.indices) {
    if (i == 0) accumulator += muls(i)
    else accumulator += muls(i) + accumulator(i - 1)
  }

  when(io.in.valid === true.B) {
    shift_en := true.B
  } .otherwise{
    shift_en := false.B
  }

  io.out.bits := accumulator.last
  io.out.valid := (ShiftRegisterWithReset(io.in.valid, params.taps.length, false.B, shift_en) && shift_en)
  io.out.sync := (ShiftRegisterWithReset(io.in.sync, params.taps.length, false.B, shift_en) && shift_en)

}

