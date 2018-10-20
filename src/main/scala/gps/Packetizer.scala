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
  val current_bit = RegInit(0.U(log2Ceil(params.wordLength).W))
  val current_word = RegInit(0.U(log2Ceil(params.subframeLength).W))
  val completeSubframe = RegInit(Vec(Seq.fill(10)(0.U(params.wordLength.W))))

  switch (state) {
    is(0.U) {
      when (params.preamble === fifo) {
        state := 1.U
        subframe(0) := Cat(0.U((params.wordLength - params.preambleLength).W), fifo)
        current_word := 0.U
        current_bit := (params.preambleLength).U
      }
    }
    is (1.U) {
      subframe(current_word) := (subframe(current_word) << 1) + io.iIn
      when (current_bit === (params.wordLength - 1).U) {
        when (current_word === (params.subframeLength - 1).U) {
          state := 2.U
        } .otherwise {
          current_bit := 0.U
          current_word := current_word + 1.U
        }
      } .otherwise {
        current_bit := current_bit + 1
      }
    }
    is (2.U) {
      dStar := (completeSubframe(params.subframeLength - 1))(params.wordLength - 2, params.wordLength - 1)
      completeSubframe := subframe
      for (word <- 0 until params.subframeLength) {
        subframe(word) := 0.U
      }
    }
  }

  fifo := (fifo << 1) + io.iIn
  io.dStarOut := dStar
  io.subframeValid := (state === 2.U)
  io.dataOut := completeSubframe
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
