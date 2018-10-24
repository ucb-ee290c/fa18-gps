
package gps

import chisel3._
import org.scalatest.{FlatSpec, Matchers}
import dsptools.numbers.DspReal

class IntDumpSpec extends FlatSpec with Matchers {
  behavior of "IntDump"

  val params = new SampledIntDumpParams(adcWidth = 3, codeLength = 1023) {}

  it should "integrate SInt inputs" in {
    IntDumpTester(params) should be (true)
  }

}