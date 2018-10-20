package gps

import dsptools.DspTester

import chisel3._

class ParserTester(c: Parser) extends DspTester(c) {
  // val preamble = (1, 0, 1, 1, 1, 0, 0, 0)
  // for (b <- 0 until 8) {
  //   poke(c.io.iIn, preamble[b].toBool())
  // }

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
  step(2)

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
