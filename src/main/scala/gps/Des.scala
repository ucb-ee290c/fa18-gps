package gps

import chisel3._
import chisel3.experimental.FixedPoint
import chisel3.util.Decoupled
import chisel3.util._
import scala.math._
import dsptools.numbers._
//import freechips.rocketchip.diplomacy.LazyModule
//import freechips.rocketchip.subsystem.BaseSubsystem

/**
  * Base class for CORDIC parameters
  * These are type generic
  */
trait DesParams[T <: Data] {
  val proto: T
  val wADC: Int
  val nSample: Int
  val nbit_cnt = log2Ceil(nSample)
}


case class SIntDesParams(
                          wADC: Int = 3,
                          nSample: Int = 16,
                        ) extends DesParams[SInt] {

  val proto = SInt(wADC.W)
//  val nbit_cnt = log2Ceil(nSample)

}


class DesBundle[T <: Data](val params: DesParams[T]) extends Bundle {

  val bits: T = params.proto.cloneType //Vec(params.nSample, params.proto.cloneType)

  override def cloneType: this.type = DesBundle(params).asInstanceOf[this.type]
}
object DesBundle {
  def apply[T <: Data](params: DesParams[T]): DesBundle[T] = new DesBundle(params)
}


class DesIO[T <: Data](params: DesParams[T]) extends Bundle {

  val in = Flipped(Decoupled(Wire(Vec(DesBundle(params)))))
  val out = Decoupled(Wire(Vec(DesBundle(params))))
//  val in = Flipped(Decoupled(DesBundle(params)))
//  val out = Decoupled(DesBundle(params))

  override def cloneType: this.type = DesIO(params).asInstanceOf[this.type]
}
object DesIO {
  def apply[T <: Data](params: DesParams[T]): DesIO[T] =
    new DesIO(params)
}

class Buffer[T <: Data](val params: DesParams[T]) extends Module {

  require(params.nSample > 0)
  require(params.wADC > 0)

  val io = IO(DesIO(params))

  val reg_bits = Reg(Vec(params.nSample, params.proto))
  val reg_full = RegInit(Bool(), false.B)

  io.out.valid := reg_full
  io.in.ready := !reg_full

  reg_bits := Mux(io.in.fire() && !reg_full, io.in.bits, reg_bits)
  io.out.bits := reg_bits

  reg_full := Mux(io.in.fire(), true.B, Mux(io.out.fire(), false.B, reg_full))


}

class Des[T <: Data](val params: DesParams[T]) extends Module {

  require(params.nSample > 0)
  require(params.wADC > 0)

  val io = IO(new Bundle{
    val in = Input(SInt(params.wADC.W))
    val out = Output(Vec(params.nSample, params.proto))
    val ready = Input(Bool())
    val valid = Output(Bool())
    val offset = Input(UInt(params.nbit_cnt.W))
  })

  // the shift register and the buffer
  val reg_shift = Reg(Vec(params.nSample, params.proto))
  val reg_buffer = Reg(Vec(params.nSample, params.proto))

  // behavior of the shift register
  val i = 0
  for (i <- 1 until params.nSample) {reg_shift(i) := reg_shift(i-1)}
  reg_shift(0) := io.in

  // behavior of the counter
  val reg_cnt = RegInit(UInt(params.nbit_cnt.W), 0.U)
  reg_cnt := Mux(reg_cnt === (params.nSample-1).U, 0.U, reg_cnt+1.U)
  val reg_shifter_full = RegInit(Bool(), false.B)
  reg_shifter_full := Mux(reg_cnt === (params.nSample-1).U, true.B, reg_shifter_full)

  // control signal for the buffer
  val buffer_input_valid = (reg_cnt === io.offset) && reg_shifter_full

  // behavior of the buffer
  val reg_buffer_full = RegInit(Bool(), false.B)
  val buffer_output_valid = reg_buffer_full
  val buffer_input_fire = !reg_buffer_full && buffer_input_valid
  val buffer_output_fire = buffer_output_valid && io.ready

  reg_buffer_full := Mux(buffer_input_fire, true.B, Mux(buffer_output_fire, false.B, reg_buffer_full))
  reg_buffer := Mux(buffer_input_fire && !reg_buffer_full, reg_shift, reg_buffer)


  io.out := reg_buffer
  io.valid := buffer_output_valid



}