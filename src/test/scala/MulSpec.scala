
package gps

import chisel3._
import org.scalatest.{FlatSpec, Matchers}
import dsptools.numbers.DspReal

class MulSpec extends FlatSpec with Matchers {
  behavior of "Mul"

  val params = new SampledMulParams(3) {
  }

  val realParams = new MulParams[DspReal] {
    val protoIn = DspReal()
    val protoOut = DspReal()
  }

  it should "multiply two SInt inputs" in {
    MulTester(params) should be (true)
  }
}