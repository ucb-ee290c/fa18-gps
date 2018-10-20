package gps

import dsptools.DspTester

import chisel3._

class ParserTester(c: Parser) extends DspTester(c) {
  poke(c.io.iIn, true)
  step(1)
  poke(c.io.iIn, false)
  step(1)
  poke(c.io.iIn, false)
  step(1)
  poke(c.io.iIn, false)
  step(1)
  poke(c.io.iIn, true)
  step(1)
  poke(c.io.iIn, false)
  step(1)
  poke(c.io.iIn, true)
  step(1)
  poke(c.io.iIn, true)
  step(1)

  expect(c.io.stateOut, 1)
}

object ParserTester {
  def apply(): Boolean = {
      val preamble = "b10001011".U(8.W)
      val params = PacketizerParams(10, 30, 8, preamble)
      chisel3.iotesters.Driver.execute(Array("-tbn", "firrtl", "-fiwv"), () => new Parser(params)) {
      c => new ParserTester(c)
      }
  }
}
