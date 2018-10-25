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
  params: PacketizerParams
) extends Module {
  val io = IO(new Bundle{
    val I_in = Input(Bool())
    val validOut = Output(Bool())
    val validBits = Output(Vec(params.subframeLength, Bool()))
    val subframe = Output(Vec(params.subframeLength, Vec(params.wordLength, Bool())))
  })

  val parser = Module(new Parser(params))
  val parityChecker = Module(new ParityChecker(params))

  parityChecker.io.subframeValid := parser.io.subframeValid
  parityChecker.io.dataIn := parser.io.dataOut
  parityChecker.io.dStarIn := parser.io.dStarOut
}

class Parser (
  params: PacketizerParams
) extends Module {
  val io = IO(new Bundle{
    val iIn = Input(UInt(1.W))
    val validIn = Input(Bool())
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
  io.dStarOut := dStar
  io.subframeValid := (state === 2.U)
  io.dataOut := completeSubframe
  io.stateOut := state

  when (io.validIn) {
    fifo := fifoNext
  }

  switch (state) {
    is(sIdle) {
      when (io.validIn) {
        when (params.preamble === fifoNext) {
          state := sRecording
          subframe(0) := Cat(0.U((params.wordLength - params.preambleLength).W), fifoNext)
          currentWord := 0.U
          currentBit := (params.preambleLength).U
        }
      }
    }
    is (sRecording) {
      when (io.validIn) {
        subframe(currentWord) := (subframe(currentWord) << 1) + io.iIn
        when (currentBit === (params.wordLength - 1).U) {
          when (currentWord === (params.subframeLength - 1).U) {
            state := sDone
            dStar := (completeSubframe(params.subframeLength - 1))(1, 0)
            for (w <- 0 until params.subframeLength - 1) {
              completeSubframe(w) := subframe(w)
            }
            completeSubframe(params.subframeLength - 1) := (subframe(params.subframeLength - 1) << 1) + io.iIn
          } .otherwise {
            currentBit := 0.U
            currentWord := currentWord + 1.U
          }
        } .otherwise {
          currentBit := currentBit + 1
        }
      }
    }
    is (sDone) {
      for (word <- 0 until params.subframeLength) {
        subframe(word) := 0.U
      }
      state := sIdle
    }
  }
}

class ParityChecker (
  params: PacketizerParams
) extends Module {
  val io = IO(new Bundle{
    val subframeValid = Input(Bool())
    val dataIn = Input(Vec(params.subframeLength, Vec(params.wordLength, Bool())))
    val dStarIn = Input(UInt(2.W))
    val validBits = Output(Vec(params.subframeLength, Bool()))
    val validOut = Output(Bool())
    val parityOut = Output(Vec(params.subframeLength, Vec(params.parityLength, Bool())))
  })
  val subframe = Reg(Vec(params.subframeLength, Vec(params.wordLength, Bool())))
  val dStar = Reg(UInt(2.W))
  val parityBits = Wire(Vec(params.subframeLength, Vec(params.parityLength, Bool())))
  val done = Reg(Bool())
  val temp = Wire(UInt(6.W))
  val temp2 = Wire(UInt(30.W))

  done := io.subframeValid
  io.validOut := done

  when (io.subframeValid) {
    subframe := io.dataIn
    dStar := io.dStarIn
  }

  for (w <- 0 until params.subframeLength) {
    if (w == 0) {
      parityBits(w)(0) := dStar(0) ^ subframe(w)(0) ^ subframe(w)(1) ^ subframe(w)(2) ^ subframe(w)(4) ^ subframe(w)(5) ^ subframe(w)(9) ^ subframe(w)(10) ^ subframe(w)(11) ^ subframe(w)(12) ^ subframe(w)(13) ^ subframe(w)(16) ^ subframe(w)(17) ^ subframe(w)(19) ^ subframe(w)(22)
      parityBits(w)(1) := dStar(1) ^ subframe(w)(1) ^ subframe(w)(2) ^ subframe(w)(3) ^ subframe(w)(5) ^ subframe(w)(6) ^ subframe(w)(10) ^ subframe(w)(11) ^ subframe(w)(12) ^ subframe(w)(13) ^ subframe(w)(14) ^ subframe(w)(17) ^ subframe(w)(18) ^ subframe(w)(20) ^ subframe(w)(23)
      parityBits(w)(2) := dStar(0) ^ subframe(w)(0) ^ subframe(w)(2) ^ subframe(w)(3) ^ subframe(w)(4) ^ subframe(w)(6) ^ subframe(w)(7) ^ subframe(w)(11) ^ subframe(w)(12) ^ subframe(w)(13) ^ subframe(w)(14) ^ subframe(w)(15) ^ subframe(w)(18) ^ subframe(w)(19) ^ subframe(w)(21)
      parityBits(w)(3) := dStar(1) ^ subframe(w)(1) ^ subframe(w)(3) ^ subframe(w)(4) ^ subframe(w)(5) ^ subframe(w)(7) ^ subframe(w)(8) ^ subframe(w)(12) ^ subframe(w)(13) ^ subframe(w)(14) ^ subframe(w)(15) ^ subframe(w)(16) ^ subframe(w)(19) ^ subframe(w)(20) ^ subframe(w)(22)
      parityBits(w)(4) := dStar(1) ^ subframe(w)(0) ^ subframe(w)(2) ^ subframe(w)(4) ^ subframe(w)(5) ^ subframe(w)(6) ^ subframe(w)(8) ^ subframe(w)(9) ^ subframe(w)(13) ^ subframe(w)(14) ^ subframe(w)(15) ^ subframe(w)(16) ^ subframe(w)(17) ^ subframe(w)(20) ^ subframe(w)(21) ^ subframe(w)(23)
      parityBits(w)(5) := dStar(0) ^ subframe(w)(2) ^ subframe(w)(4) ^ subframe(w)(5) ^ subframe(w)(7) ^ subframe(w)(8) ^ subframe(w)(9) ^ subframe(w)(10) ^ subframe(w)(12) ^ subframe(w)(14) ^ subframe(w)(18) ^ subframe(w)(21) ^ subframe(w)(22) ^ subframe(w)(23)
    } else {
      parityBits(w)(0) := subframe(w-1)(params.wordLength - 2) ^ subframe(w)(0) ^ subframe(w)(1) ^ subframe(w)(2) ^ subframe(w)(4) ^ subframe(w)(5) ^ subframe(w)(9) ^ subframe(w)(10) ^ subframe(w)(11) ^ subframe(w)(12) ^ subframe(w)(13) ^ subframe(w)(16) ^ subframe(w)(17) ^ subframe(w)(19) ^ subframe(w)(22)
      parityBits(w)(1) := subframe(w-1)(params.wordLength - 1) ^ subframe(w)(1) ^ subframe(w)(2) ^ subframe(w)(3) ^ subframe(w)(5) ^ subframe(w)(6) ^ subframe(w)(10) ^ subframe(w)(11) ^ subframe(w)(12) ^ subframe(w)(13) ^ subframe(w)(14) ^ subframe(w)(17) ^ subframe(w)(18) ^ subframe(w)(20) ^ subframe(w)(23)
      parityBits(w)(2) := subframe(w-1)(params.wordLength - 2) ^ subframe(w)(0) ^ subframe(w)(2) ^ subframe(w)(3) ^ subframe(w)(4) ^ subframe(w)(6) ^ subframe(w)(7) ^ subframe(w)(11) ^ subframe(w)(12) ^ subframe(w)(13) ^ subframe(w)(14) ^ subframe(w)(15) ^ subframe(w)(18) ^ subframe(w)(19) ^ subframe(w)(21)
      parityBits(w)(3) := subframe(w-1)(params.wordLength - 1) ^ subframe(w)(1) ^ subframe(w)(3) ^ subframe(w)(4) ^ subframe(w)(5) ^ subframe(w)(7) ^ subframe(w)(8) ^ subframe(w)(12) ^ subframe(w)(13) ^ subframe(w)(14) ^ subframe(w)(15) ^ subframe(w)(16) ^ subframe(w)(19) ^ subframe(w)(20) ^ subframe(w)(22)
      parityBits(w)(4) := subframe(w-1)(params.wordLength - 1) ^ subframe(w)(0) ^ subframe(w)(2) ^ subframe(w)(4) ^ subframe(w)(5) ^ subframe(w)(6) ^ subframe(w)(8) ^ subframe(w)(9) ^ subframe(w)(13) ^ subframe(w)(14) ^ subframe(w)(15) ^ subframe(w)(16) ^ subframe(w)(17) ^ subframe(w)(20) ^ subframe(w)(21) ^ subframe(w)(23)
      parityBits(w)(5) := subframe(w-1)(params.wordLength - 2) ^ subframe(w)(2) ^ subframe(w)(4) ^ subframe(w)(5) ^ subframe(w)(7) ^ subframe(w)(8) ^ subframe(w)(9) ^ subframe(w)(10) ^ subframe(w)(12) ^ subframe(w)(14) ^ subframe(w)(18) ^ subframe(w)(21) ^ subframe(w)(22) ^ subframe(w)(23)
    }
    io.validBits(w) := (parityBits(w).asUInt === ((subframe(w).asUInt >> (params.wordLength - params.parityLength)) & ((1 << params.parityLength) - 1).U))
    io.parityOut(w) := parityBits(w)
  }
  temp := parityBits(0).asUInt
  temp2 := ((subframe(0).asUInt >> 24) & 63.U)
  printf(p"one: $temp")
  printf(p"two: $temp2")
}
