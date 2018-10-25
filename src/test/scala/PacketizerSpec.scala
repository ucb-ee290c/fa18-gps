package gps

import dsptools.numbers._
import org.scalatest.{FlatSpec, Matchers}

class ParserSpec extends FlatSpec with Matchers {
  behavior of "Packetizer: parser submodule"

  it should "output valid subframe" in {
    ParserTester() should be (true)
  }
}

class ParityCheckerSpec extends FlatSpec with Matchers {
  behavior of "Packetizer: parity checker submodule"

  it should "output correct parity bits" in {
    ParityCheckerTester() should be (true)
  }
}
