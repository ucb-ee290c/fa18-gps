
package gps

import dsptools.DspTester
import chisel3._
import dsptools.numbers.DspReal

class MulTester(c: Mul[SInt]) extends DspTester(c) {
  poke(c.io.in1, 3.S)
  poke(c.io.in2, 2.S)
  expect(c.io.out, 6.S)

  poke(c.io.in2, (-1).S)
  expect(c.io.out, (-3).S)
}

object MulTester {
  def apply(params: SampledMulParams): Boolean = {
    chisel3.iotesters.Driver.execute(Array("-tbn", "firrtl", "-fiwv"), () => new Mul[SInt](params)) {
      c => new MulTester(c)
    }
  }
}