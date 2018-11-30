package gps

import chisel3._
import chisel3.util._
import dsptools.numbers._


/**
 * Lock Detector Parameters
 */
case class LockDetectParams[T <: Data : Real] (
  val proto: T,
  val min: Double, 
  val max: Double, 
  val lockCount: Int, 
)

/**
 * Lock Detectors
 */
class LockDetector[T <: Data : Real](params: LockDetectParams[T]) extends Module {
  val io = IO(new Bundle{
    val in = Flipped(Valid(params.proto.cloneType))
    val lock = Output(Bool())
  })

  val lockCount = RegInit(0.U(log2Ceil(params.lockCount).W))

  when (
    io.in.valid &&
    io.in.bits >= ConvertableTo[T].fromDouble(params.min) && 
    io.in.bits <= ConvertableTo[T].fromDouble(params.max)
  ) {
    lockCount := Mux(io.lock, lockCount, lockCount + 1.U)
  }.elsewhen(io.in.valid) {
    lockCount := 0.U
  }

  io.lock := lockCount >= params.lockCount.U
}
