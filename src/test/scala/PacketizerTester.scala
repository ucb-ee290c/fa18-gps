package gps

import dsptools.DspTester

import chisel3._

class ParserTester(c: Parser) extends DspTester(c) {
  val preamble = List(1, 0, 0, 0, 1, 0, 1, 1)
  val subframe = List(List(1, 0, 0, 0, 1, 0, 1, 1, 0, 0, 0, 0, 0, 1, 0, 1, 1, 1, 0, 0, 0, 1, 1, 0, 0, 1, 1, 0, 0, 0),
                      List(1, 0, 0, 1, 1, 0, 1, 0, 0, 0, 1, 0, 1, 1, 0, 0, 1, 0, 1, 1, 0, 0, 1, 1, 0, 1, 0, 1, 0, 0),
                      List(1, 1, 0, 0, 0, 0, 1, 1, 0, 1, 0, 1, 1, 1, 0, 0, 0, 0, 0, 1, 0, 0, 0, 1, 1, 0, 1, 1, 1, 0),
                      List(0, 0, 1, 1, 1, 0, 1, 0, 1, 1, 0, 0, 0, 0, 1, 1, 0, 0, 1, 1, 1, 0, 1, 1, 0, 1, 1, 0, 1, 1),
                      List(0, 1, 1, 0, 1, 1, 1, 1, 0, 0, 0, 1, 1, 0, 1, 1, 0, 1, 1, 1, 0, 1, 0, 1, 1, 1, 0, 1, 0, 0),
                      List(1, 0, 0, 0, 0, 1, 0, 0, 1, 1, 0, 1, 0, 0, 1, 0, 1, 1, 1, 0, 1, 0, 1, 0, 1, 0, 0, 0, 0, 0),
                      List(0, 1, 1, 0, 0, 1, 0, 1, 0, 0, 0, 0, 0, 1, 0, 0, 1, 1, 0, 0, 0, 1, 1, 1, 1, 0, 0, 0, 1, 0),
                      List(1, 0, 1, 1, 0, 1, 0, 0, 1, 0, 1, 1, 0, 1, 0, 1, 0, 1, 0, 1, 1, 0, 1, 1, 0, 1, 0, 0, 1, 1),
                      List(0, 1, 0, 1, 0, 0, 1, 0, 0, 0, 0, 0, 1, 1, 0, 1, 1, 0, 1, 0, 1, 1, 1, 0, 0, 0, 0, 1, 1, 1),
                      List(1, 1, 1, 0, 0, 1, 1, 1, 0, 0, 1, 0, 0, 1, 1, 0, 0, 1, 1, 0, 0, 0, 1, 1, 1, 1, 0, 1, 1, 1))
  val subframeAsInts = List(Integer.parseInt("100010110000010111000110011000", 2),
                            Integer.parseInt("100110100010110010110011010100", 2),
                            Integer.parseInt("110000110101110000010001101110", 2),
                            Integer.parseInt("001110101100001100111011011011", 2),
                            Integer.parseInt("011011110001101101110101110100", 2),
                            Integer.parseInt("100001001101001011101010100000", 2),
                            Integer.parseInt("011001010000010011000111100010", 2),
                            Integer.parseInt("101101001011010101011011010011", 2),
                            Integer.parseInt("010100100000110110101110000111", 2),
                            Integer.parseInt("111001110010011001100011110111", 2))
  for (w <- 0 until 10) {
    for (b <- 0 until 30) {
      val curr_bit = (w * 30) + b
      if (curr_bit < 8) {
        expect(c.io.stateOut, 0)
      } else {
        expect(c.io.stateOut, 1)
      }

      expect(c.io.subframeValid, 0)
      poke(c.io.iIn, subframe(w)(b))
      poke(c.io.validIn, 1)
      step(1)
      poke(c.io.validIn, 0)
      if (curr_bit < 299) {
        step(19)
      }
    }
  }

  expect(c.io.subframeValid, 1)
  expect(c.io.stateOut, 2)
  for (w <- 0 until 10) {
    expect(c.io.dataOut(w), subframeAsInts(w))
  }
  step(1)
  expect(c.io.subframeValid, 0)
  expect(c.io.stateOut, 0)
}

object ParserTester {
  def apply(): Boolean = {
    val preamble = "b10001011".U(8.W)
    val params = PacketizerParams(10, 30, 8, preamble, 6)
    chisel3.iotesters.Driver.execute(Array("-tbn", "firrtl", "-fiwv"), () => new Parser(params)) {
      c => new ParserTester(c)
    }
  }
}

class ParityCheckerTester(c: ParityChecker) extends DspTester(c) {
  poke(c.io.dataIn(0), Integer.parseInt("100010110000000000000000000000", 2))
  step(1)
}

object ParityCheckerTester {
  def apply(): Boolean = {
    val preamble = "b10001011".U(8.W)
    val params = PacketizerParams(10, 30, 8, preamble, 6)
    chisel3.iotesters.Driver.execute(Array("-tbn", "firrtl", "-fiwv"), () => new ParityChecker(params)) {
      c => new ParityCheckerTester(c)
    }
  }
}
