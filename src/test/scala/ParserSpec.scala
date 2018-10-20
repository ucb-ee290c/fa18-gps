package gps

import dsptools.numbers._
import org.scalatest.{FlatSpec, Matchers}

class ParserSpec extends FlatSpec with Matchers {
  behavior of "Packetizer: parser submodule"

  it should "identify preamble" in {
    ParserTester() should be (true)
  }
}
