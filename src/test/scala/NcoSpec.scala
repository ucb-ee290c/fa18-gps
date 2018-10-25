package gps

import dsptools.numbers._
import org.scalatest.{FlatSpec, Matchers}

class NcoSpec extends FlatSpec with Matchers {
  behavior of "NCO"

  it should "produce carrier osc" in {
    NcoTester() should be (true)
  }

}