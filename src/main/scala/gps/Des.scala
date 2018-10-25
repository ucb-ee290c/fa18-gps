package gps

import chisel3._
import chisel3.experimental.FixedPoint
import chisel3.util.Decoupled
import chisel3.util._
import scala.math._
import dsptools.numbers._
import freechips.rocketchip.diplomacy.LazyModule
import freechips.rocketchip.subsystem.BaseSubsystem

/**
  * Base class for CORDIC parameters
  * These are type generic
  */
trait DesParams[T <: Data] {
  val proto: T
  val wADC: Int
  val nSample: Int
}


case class SIntDesParams(
                          wADC: Int = 3,
                          nSample: Int = 16,
                        ) extends DesParams[SInt] {

  val proto = Vec(nSample, SInt(wADC.W))
  val nbit_cnt = log2Ceil(nSample)

}


class DesBundle[T <: Data](val params: DesParams[T]) extends Bundle {

  val bits: T = params.proto.cloneType

  override def cloneType: this.type = DesBundle(params).asInstanceOf[this.type]
}
object DesBundle {
  def apply[T <: Data](params: DesParams[T]): DesBundle[T] = new DesBundle(params)
}


class DesIO[T <: Data](params: DesParams[T]) extends Bundle {

  val in = Flipped(Decoupled(DesBundle(params)))
  val out = Decoupled(DesBundle(params))

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

  val reg_bits = Reg(params.proto)
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
    val in = Input(params.proto)
    val out = Output(params.proto)
    val ready = Input(Bool())
    val valid = Output(Bool())
    val offset = Input(UInt(params.nbit_cnt.W))
  })

  val reg_cnt = RegInit(UInt(params.nbit_cnt.W), 0.U)
  reg_cnt := Mux(reg_cnt === (params.nSample-1).U, 0.U, reg_cnt+1.U)



  val reg_shifter = Reg(params.proto)


  buffer = Module(new Buffer(params))
  buffer.io.in.bits := reg_shifter
  buffer.io.in.valid := reg_cnt === offset
  //   := buffer.io.in.ready not needed here

  io.out := buffer.io.out.bits
  io.valid := buffer.io.out.valid
  buffer.io.out.ready := io.ready



}