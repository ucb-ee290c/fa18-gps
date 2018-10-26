
package gps

import dsptools.DspTester
import chisel3._
import dsptools.numbers.DspReal
import org.scalatest.{FlatSpec, Matchers}

/*
 * DspSpec for IntDump
 */
class IntDumpSpec extends FlatSpec with Matchers {
  behavior of "IntDump"

  val params = new SampledIntDumpParams(inWidth = 3, codeLength = 1023) {}

  it should "integrate SInt inputs" in {
    IntDumpTester(params) should be (true)
  }

}

/*
 * DspTester for IntDump
 */
class IntDumpTester (c: IntDump[SInt]) extends DspTester(c) {
  val prnDataRaw = io.Source.fromFile("./src/test/scala/PRNCode.csv").getLines.toList.map(_.split(","))
  val prnData = prnDataRaw(0).map(_.toInt)

  var integTest = 0

  poke(c.io.dump, false.B)

  for (i <- 0 until prnData.length) {
    integTest = integTest + (prnData(i) * prnData(i))
    poke(c.io.in, (prnData(i) * prnData(i)).S)
    step(1)
    expect(c.io.integ, integTest.S)
  }

  poke(c.io.dump, true.B)
  poke(c.io.in, 1.S)
  step(1)
  expect(c.io.integ, 1.S)
}


object IntDumpTester {
  def apply(params: IntDumpParams[SInt]): Boolean = {
    chisel3.iotesters.Driver.execute(Array("-tbn", "firrtl", "-fiwv"), () => new IntDump[SInt](params)) {
      c => new IntDumpTester(c)
    }
  }
}
