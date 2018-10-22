package gps

import chisel3._
import chisel3.experimental.FixedPoint
import chisel3.util._

import dsptools.numbers._

case class PacketizerParams (
  val subframeLength: Int,
  val wordLength: Int,
  val preambleLength: Int,
  val preamble: UInt,
  val parityLength: Int
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
    val stateOut = Output(UInt(2.W))
  })

  val fifo = RegInit(0.U(params.preambleLength.W))
  val fifoNext = Wire(UInt(params.preambleLength.W))
  val dStar = RegInit(0.U(2.W))
  val state = RegInit(0.U(2.W))
  val subframe = RegInit(Vec(Seq.fill(10)(0.U(params.wordLength.W))))
  val currentBit = RegInit(0.U(log2Ceil(params.wordLength).W))
  val currentWord = RegInit(0.U(log2Ceil(params.subframeLength).W))
  val completeSubframe = RegInit(Vec(Seq.fill(10)(0.U(params.wordLength.W))))
  val sIdle :: sRecording :: sDone :: Nil = Enum(3)

  fifoNext := (fifo << 1) + io.iIn.asUInt()

  switch (state) {
    is(sIdle) {
      when (params.preamble === fifoNext) {
        state := sRecording
        subframe(0) := Cat(0.U((params.wordLength - params.preambleLength).W), fifoNext)
        currentWord := 0.U
        currentBit := (params.preambleLength).U
      }
    }
    is (sRecording) {
      subframe(currentWord) := (subframe(currentWord) << 1) + io.iIn
      when (currentBit === (params.wordLength - 1).U) {
        when (currentWord === (params.subframeLength - 1).U) {
          state := sDone
          dStar := (completeSubframe(params.subframeLength - 1))(1, 0)
          completeSubframe := subframe
        } .otherwise {
          currentBit := 0.U
          currentWord := currentWord + 1.U
        }
      } .otherwise {
        currentBit := currentBit + 1
      }
    }
    is (sDone) {
      for (word <- 0 until params.subframeLength) {
        subframe(word) := 0.U
      }
      state := sIdle
    }
  }

  fifo := fifoNext
  io.dStarOut := dStar
  io.subframeValid := (state === 2.U)
  io.dataOut := completeSubframe
  io.stateOut := state
}

class ParityChecker (
  params: PacketizerParams
) extends Module {
  val io = IO(new Bundle{
    val subframeValid = Input(Bool())
    val dataIn = Input(Vec(params.subframeLength, UInt(params.wordLength.W)))
    val dStarIn = Input(UInt(2.W))
  })

  val subframe = Reg(Vec(params.subframeLength, UInt(params.wordLength.W)))
  val dStar = Reg(UInt(2.W))
  val parityBits = Wire(Vec(params.subframeLength, UInt(params.parityLength.W)))
  val wordValid = Wire(Vec(params.subframeLength), Bool())
  val done = Reg(Bool())

  done := io.subframeValid

  when (io.subframeValid) {
    subframe := io.dataIn
    dStar := io.dStarIn
  }

  for (w <- 0 until 10) {
    parityBits(w)(0) := 0.U // TODO
    parityBits(w)(1) := 0.U // TODO
    parityBits(w)(2) := 0.U // TODO
    parityBits(w)(3) := 0.U // TODO
    parityBits(w)(4) := 0.U // TODO
    parityBits(w)(5) := 0.U // TODO
    wordValid(w) := (parityBits(w) === subframe(w)(params.wordLength - 1, params.wordLength - params.parityLength))
  }

  when (done) {
    // send everything to regmap
  }
}
