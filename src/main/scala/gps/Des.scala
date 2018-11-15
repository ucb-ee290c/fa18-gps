
package gps

import chisel3._
import chisel3.experimental.FixedPoint
import chisel3.util.Decoupled
import chisel3.util._
import scala.math._
import dsptools._
import dsptools.numbers._
import dsptools.numbers.implicits._
//import freechips.rocketchip.diplomacy.LazyModule
//import freechips.rocketchip.subsystem.BaseSubsystem

/**
  * Base class for CORDIC parameters
  * These are type generic
  */
trait DesParams[T <: Data] {
  val proto: T
  val width: Int
  val nSample: Int
  val nLane: Int
  val nbit_cnt = log2Ceil(nSample)
}


case class SIntDesParams(
                          width: Int = 3,
                          nSample: Int = 16,
                          nLane: Int = 4,
                        ) extends DesParams[SInt] {

  val proto = SInt(width.W)
//  val nbit_cnt = log2Ceil(nSample)

}



//class DesBundle[T <: Data](val params: DesParams[T]) extends Bundle {
//
//  val bits: T = params.proto.cloneType //Vec(params.nSample, params.proto.cloneType)
//
//
//  override def cloneType: this.type = DesBundle(params).asInstanceOf[this.type]
//}
//object DesBundle {
//  def apply[T <: Data](params: DesParams[T]): DesBundle[T] = new DesBundle(params)
//}
//
//
//class DesIO[T <: Data](params: DesParams[T]) extends Bundle {
//
//  val in = Flipped(Decoupled(Wire(Vec(DesBundle(params)))))
//  val out = Decoupled(Wire(Vec(DesBundle(params))))
////  val in = Flipped(Decoupled(DesBundle(params)))
////  val out = Decoupled(DesBundle(params))
//
//  override def cloneType: this.type = DesIO(params).asInstanceOf[this.type]
//}
//object DesIO {
//  def apply[T <: Data](params: DesParams[T]): DesIO[T] =
//    new DesIO(params)
//}
//
//class Buffer[T <: Data](val params: DesParams[T]) extends Module {
//
//  require(params.nSample > 0)
//  require(params.wADC > 0)
//
//  val io = IO(DesIO(params))
//
//  val reg_bits = Reg(Vec(params.nSample, params.proto))
//  val reg_full = RegInit(Bool(), false.B)
//
//  io.out.valid := reg_full
//  io.in.ready := !reg_full
//
//  reg_bits := Mux(io.in.fire() && !reg_full, io.in.bits, reg_bits)
//  io.out.bits := reg_bits
//
//  reg_full := Mux(io.in.fire(), true.B, Mux(io.out.fire(), false.B, reg_full))
//
//
//}

class Des[T <: Data:Real:BinaryRepresentation](val params: DesParams[T]) extends Module {

  require(params.nSample > 0)
  require(params.width > 0)

  val io = IO(new Bundle{
    val in: T = Input(params.proto)
    val out: Vec[T] = Output(Vec(params.nLane, params.proto))
//    val out = Output(Vec(params.nLane, SInt(2.W)))
    val ready = Input(Bool())
    val valid = Output(Bool())
    val newreq = Input(Bool())
    val offset = Input(UInt(params.nbit_cnt.W))
    val state = Output(UInt(2.W))
    val cnt_buffer = Output(UInt(params.nbit_cnt.W))
    val start = Output(Bool())
    val end = Output(Bool())
  })

  val cnt_buffer_max = (params.nSample / params.nLane - 1).U

  // the shift register and the buffer
  val reg_shift = Reg(Vec(params.nSample, params.proto))
  val reg_buffer = Reg(Vec(params.nSample, params.proto))

  // behavior of the shift register
//  val i = 0
  for (i <- 0 until params.nSample-1) {reg_shift(i) := reg_shift(i+1)}
  reg_shift(params.nSample-1) := io.in

  // behavior of the counter
  val reg_cnt = RegInit(UInt(params.nbit_cnt.W), 0.U)
  reg_cnt := Mux(reg_cnt === (params.nSample-1).U, 0.U, reg_cnt+1.U)
  val reg_shifter_cleared = RegInit(Bool(), false.B)
  reg_shifter_cleared := Mux(reg_cnt === (params.nSample-1).U, false.B, Mux(io.newreq, true.B, reg_shifter_cleared))
  val reg_shifter_full = RegInit(Bool(), false.B)
//  reg_shifter_full := Mux(reg_cnt === (params.nSample-1).U, true.B, reg_shifter_full)
  reg_shifter_full := !reg_shifter_cleared && reg_cnt === (params.nSample-1).U


  // control signal for the buffer
  val buffer_input_valid = (reg_cnt === io.offset) && reg_shifter_full

  val idle = WireInit(UInt(2.W), 0.U)
  val lock = WireInit(UInt(2.W), 1.U)
  val stream = WireInit(UInt(2.W), 2.U)

  val reg_state_buffer = RegInit(UInt(2.W), idle)
  val reg_cnt_buffer = RegInit(UInt(params.nbit_cnt.W), 0.U)
  val stream_finished = reg_cnt_buffer === cnt_buffer_max

  val buffer_input_fire = reg_state_buffer === idle && buffer_input_valid


  reg_state_buffer := Mux(reg_state_buffer === idle,
                          Mux(buffer_input_fire, lock, idle),
                          Mux(reg_state_buffer === lock,
                              Mux(io.ready, stream, Mux(io.newreq, idle, lock)),
                              Mux(stream_finished, lock, stream)
                              )
                          )

  reg_cnt_buffer := Mux(reg_state_buffer =/= stream, 0.U, Mux(reg_cnt_buffer === cnt_buffer_max, 0.U, reg_cnt_buffer+1.U))


  val buffer_output_valid = reg_state_buffer === stream //|| (reg_state_buffer === lock && !io.newreq)
  val buffer_output_fire = buffer_output_valid && io.ready
  reg_buffer := Mux(buffer_input_fire, reg_shift, reg_buffer)


  io.valid := buffer_output_valid
  when (reg_state_buffer =/= stream) {
    for (i <- 0 until params.nLane) {
      io.out(i) := reg_buffer(i)
    }
  }
  .otherwise {
    for (i <- 0 until params.nLane) {
      io.out(i) := reg_buffer(reg_cnt_buffer*params.nLane.U+i.U)
    }
  }

  io.state := reg_state_buffer
  io.cnt_buffer := reg_cnt_buffer
  io.start := reg_state_buffer === stream && reg_cnt_buffer === 0.U
  io.end := reg_state_buffer === stream && reg_cnt_buffer === cnt_buffer_max

  // behavior of the buffer
//  val reg_buffer_full = RegInit(Bool(), false.B)
//  val buffer_output_valid = reg_buffer_full
//  val buffer_input_fire = !reg_buffer_full && buffer_input_valid
//  val buffer_output_fire = buffer_output_valid && io.ready

//  reg_buffer_full := Mux(buffer_input_fire, true.B, Mux(buffer_output_fire, false.B, reg_buffer_full))
//  reg_buffer := Mux(buffer_input_fire && !reg_buffer_full, reg_shift, reg_buffer)


//  io.out := reg_buffer
//  io.valid := buffer_output_valid



}

