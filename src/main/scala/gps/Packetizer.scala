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

object ExtractedParamsBundle {
  def apply(): ExtractedParamsBundle = new ExtractedParamsBundle
}

class ExtractedParamsBundle extends Bundle {
  val subframe_id = UInt(3.W)

  // from subframe 1
  val week_number = UInt(10.W)
  val sv_accuracy = UInt(4.W)
  val sv_health = UInt(6.W)
  val iodc = UInt(10.W)
  val t_gd = SInt(8.W)
  val a_f2 = SInt(8.W)
  val a_f1 = SInt(16.W)
  val a_f0 = SInt(22.W)

  // from subframe 2
  val iode = UInt(8.W)
  val c_rs = SInt(16.W)
  val delta_n = SInt(16.W)
  val m_0 = SInt(32.W)
  val c_uc = SInt(16.W)
  val e = UInt(32.W)
  val c_us = SInt(16.W)
  val sqrt_a = UInt(32.W)
  val t_oe = UInt(16.W)

  // from subframe 3
  val c_ic = SInt(16.W)
  val omega_0 = SInt(32.W)
  val c_is = SInt(16.W)
  val i_0 = SInt(32.W)
  val c_rc = SInt(16.W)
  val omega = SInt(32.W)
  val dot_omega = SInt(24.W)
  val idot = SInt(14.W)
}

class Packetizer (
  params: PacketizerParams
) extends Module {
  val io = IO(new Bundle{
    val iIn = Input(Bool())
    val validIn = Input(Bool())
    val validOut = Output(Bool())
    val validBits = Output(Vec(params.subframeLength, Bool()))
    val subframe = Output(Vec(params.subframeLength, Vec(params.wordLength, Bool())))
    val extractedValues = Output(new ExtractedParamsBundle)
  })

  val parser = Module(new Parser(params))
  val parityChecker = Module(new ParityChecker(params))
  val paramExtractor = Module(new ParamExtractor(params))

  parser.io.iIn := io.iIn
  parser.io.validIn := io.validIn

  parityChecker.io.subframeValid := parser.io.subframeValid
  for (w <- 0 until params.subframeLength) {
    parityChecker.io.dataIn(w) := Reverse(parser.io.dataOut(w)).toBools
  }
  parityChecker.io.dStarIn := parser.io.dStarOut

  paramExtractor.io.subframe := parser.io.dataOut
  io.extractedValues := paramExtractor.io.extractedValues

  io.validOut := parityChecker.io.validOut
  io.validBits := parityChecker.io.validBits
  io.subframe := parityChecker.io.dataIn
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
  val inverted = RegInit(false.B)
  val correctedBit = Wire(UInt(1.W))
  val sIdle :: sRecording :: sDone :: Nil = Enum(3)

  when(inverted) {
    correctedBit := !io.iIn
  } .otherwise {
    correctedBit := io.iIn
  }
  correctedBit := io.iIn

  fifoNext := (fifo << 1) + correctedBit.asUInt()
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
        when (params.preamble === fifoNext || ~params.preamble === ~fifoNext) {
          state := sRecording
          subframe(0) := Cat(0.U((params.wordLength - params.preambleLength).W), fifoNext)
          currentWord := 0.U
          currentBit := (params.preambleLength).U

          inverted := false.B
          when (!params.preamble === !fifoNext) {
            inverted := true.B
          }
        }
      }
    }
    is (sRecording) {
      when (io.validIn) {
        subframe(currentWord) := (subframe(currentWord) << 1) + correctedBit
        when (currentBit === (params.wordLength - 1).U) {
          when (currentWord === (params.subframeLength - 1).U) {
            state := sDone
            dStar := (completeSubframe(params.subframeLength - 1))(1, 0)
            for (w <- 0 until params.subframeLength - 1) {
              completeSubframe(w) := subframe(w)
            }
            completeSubframe(params.subframeLength - 1) := (subframe(params.subframeLength - 1) << 1) + correctedBit
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
}

class ParamExtractor (
  params: PacketizerParams
) extends Module {
  val io = IO(new Bundle{
    val subframe = Input(Vec(params.subframeLength, UInt(params.wordLength.W)))
    val extractedValues = Output(new ExtractedParamsBundle)
  })
  io.extractedValues.subframe_id := io.subframe(1)(10, 8)

  // From subframe 1
  io.extractedValues.week_number := io.subframe(2)(29, 20)
  io.extractedValues.sv_accuracy := io.subframe(2)(17, 14)
  io.extractedValues.sv_health := io.subframe(2)(13, 8)
  io.extractedValues.iodc := Cat(io.subframe(2)(7, 6), io.subframe(7)(29, 22))
  io.extractedValues.t_gd := io.subframe(6)(13, 6).asSInt
  io.extractedValues.a_f2 := io.subframe(8)(29, 22).asSInt
  io.extractedValues.a_f1 := io.subframe(8)(21, 6).asSInt
  io.extractedValues.a_f0 := io.subframe(9)(29, 8).asSInt

  // From subframe 2
  io.extractedValues.iode := io.subframe(2)(29, 22)
  io.extractedValues.c_rs := io.subframe(2)(21, 6).asSInt
  io.extractedValues.delta_n := io.subframe(3)(29, 14).asSInt
  io.extractedValues.m_0 := Cat(io.subframe(3)(13, 6), io.subframe(4)(29, 6)).asSInt
  io.extractedValues.c_uc := io.subframe(5)(29, 14).asSInt
  io.extractedValues.e := Cat(io.subframe(5)(13, 6), io.subframe(6)(29, 6))
  io.extractedValues.c_us := io.subframe(7)(29, 14).asSInt
  io.extractedValues.sqrt_a := Cat(io.subframe(7)(13, 6), io.subframe(8)(29, 6))
  io.extractedValues.t_oe := io.subframe(9)(29, 14)

  // From subframe 3
  io.extractedValues.c_ic := io.subframe(2)(29, 14).asSInt
  io.extractedValues.omega_0 := Cat(io.subframe(2)(13, 6), io.subframe(3)(29, 6)).asSInt
  io.extractedValues.c_is := io.subframe(4)(29, 14).asSInt
  io.extractedValues.i_0 := Cat(io.subframe(4)(13, 6), io.subframe(5)(29, 6)).asSInt
  io.extractedValues.c_rc := io.subframe(6)(29, 14).asSInt
  io.extractedValues.omega := Cat(io.subframe(6)(13, 6), io.subframe(7)(29, 6)).asSInt
  io.extractedValues.dot_omega := io.subframe(8)(29, 6).asSInt
  io.extractedValues.idot := io.subframe(9)(21, 8).asSInt
}
