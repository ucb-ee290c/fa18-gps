package gps

import chisel3._
import chisel3.experimental.FixedPoint
import chisel3.util._

import dsptools.numbers._

case class PacketizerParams (
  val subframeLength: Int,
  val wordLength: Int,
  val preambleLength: Int,
  val preamble: UInt
)

class Packetizer (
  Params: PacketizerParams
) extends Module {
  val io = IO(new Bundle{
    val I_in = Input(Bool())
  })

  val parser = Module(new Parser(Params))
  val parityChecker = Module(new ParityChecker(Params))

  parityChecker.io.subframeValid := parser.io.subframeValid
  parityChecker.io.dataIn := parser.io.dataOut
  parityChecker.io.dStarIn := parser.io.dStarOut
}

class Parser (
  params: PacketizerParams
) extends Module {
  val io = IO(new Bundle{
    val iIn = Input(UInt(1.W))
    val subframeValid = Output(Bool())
    val dataOut = Output(Vec(params.subframeLength, UInt(params.wordLength.W)))
    val dStarOut = Output(UInt(2.W))
  })

  val fifo = RegInit(0.U(params.preambleLength.W))
  val dStar = RegInit(0.U(2.W))
  val state = RegInit(0.U(2.W))
  val subframe = RegInit(Vec(Seq.fill(10)(0.U(params.wordLength.W))))

  switch (state) {
    is(0.U) {
      when (params.preamble === fifo) {
        state := 1.U
      }
    }
  }

  fifo := (fifo << 1) + io.iIn
  io.dStarOut := dStar
}

class ParityChecker (
  params: PacketizerParams
) extends Module {
  val io = IO(new Bundle{
    val subframeValid = Input(Bool())
    val dataIn = Input(Vec(params.subframeLength, UInt(params.wordLength.W)))
    val dStarIn = Input(UInt(2.W))
  })
}

class Test extends Module {
  val io = IO(new Bundle{})

  val preamble = "b10001011".U(8.W)
  val params = PacketizerParams(10, 30, 8, preamble)
  val pk = Module(new Packetizer(params))
}
