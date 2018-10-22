package gps

import dsptools.DspTester

import chisel3._

class ParserTester(c: Parser) extends DspTester(c) {
  val preamble = List(true, false, false, false, true, false, true, true)
  for (b <- 0 until 8) {
    expect(c.io.stateOut, 0)
    expect(c.io.subframeValid, 0)
    poke(c.io.iIn, preamble(b))
    step(1)
  }
  poke(c.io.iIn, false)
  for (b <- 0 until 292) {
    expect(c.io.stateOut, 1)
    expect(c.io.subframeValid, 0)
    step(1)
  }

  expect(c.io.stateOut, 2)
  expect(c.io.subframeValid, 1)
  expect(c.io.dataOut(0), (139 << 22))
  for (w <- 1 until 10) {
    expect(c.io.dataOut(w), 0)
  }
  step(1)
  expect(c.io.stateOut, 0)
  expect(c.io.subframeValid, 0)
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
