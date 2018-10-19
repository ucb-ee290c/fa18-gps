package gps

import chisel3._
import chisel3.experimental.FixedPoint
import chisel3.util._

import dsptools.numbers._

case class PacketizerParams (
  val SubframeLength: Int,
  val WordLength: Int,
  val PreambleLength: Int,
  val Preamble: UInt
)

class Packetizer (
  Params: PacketizerParams
) extends Module {
  val io = IO(new Bundle{
    val I_in = Input(Bool())
  })

  val parser = Module(new Parser(Params))
  val parity_checker = Module(new ParityChecker(Params))

  parity_checker.io.subframe_valid := parser.io.subframe_valid
  parity_checker.io.data_in := parser.io.data_out
  parity_checker.io.d_star := parser.io.d_star
}

class Parser (
  Params: PacketizerParams
) extends Module {
  val io = IO(new Bundle{
    val I_in = Input(UInt(1.W))
    val subframe_valid = Output(Bool())
    val data_out = Output(Vec(Params.SubframeLength, UInt(Params.WordLength.W)))
    val d_star = Output(UInt(2.W))
  })

  val fifo = RegInit(0.U(Params.PreambleLength.W))

  fifo := (fifo << 1) + io.I_in
}

class ParityChecker (
  Params: PacketizerParams
) extends Module {
  val io = IO(new Bundle{
    val subframe_valid = Input(Bool())
    val data_in = Input(Vec(Params.SubframeLength, UInt(Params.WordLength.W)))
    val d_star = Input(UInt(2.W))
  })
}

class Test extends Module {
  val io = IO(new Bundle{})

  val preamble = "b10001011".U(8.W)
  val params = PacketizerParams(10, 30, 8, preamble)
  val pk = Module(new Packetizer(params))
}
