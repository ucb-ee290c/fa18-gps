package gps

import dsptools.DspTester
import dsptools.numbers.DspReal
import chisel3._
import org.scalatest.{FlatSpec, Matchers}

/* 
 * DspSpec for Mul
 */ 
class MulSpec extends FlatSpec with Matchers {
  behavior of "Mul"

  val params = new SampledMulParams(3) {
  }

  it should "multiply two SInt inputs" in {
    MulTester(params) should be (true)
  }
}

/*
 * DspTester for Mul
 */
class MulTester(c: Mul[SInt]) extends DspTester(c) {
  poke(c.io.in1, 3.S)
  poke(c.io.in2, 2.S)
  expect(c.io.out, 6.S)

  poke(c.io.in2, (-1).S)
  expect(c.io.out, (-3).S)
}

object MulTester {
  def apply(params: SampledMulParams): Boolean = {
    dsptools.Driver.execute(() => new Mul[SInt](params), TestSetup.dspTesterOptions) {
      c => new MulTester(c)
    }
  }
}
