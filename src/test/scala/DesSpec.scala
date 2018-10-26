package gps

import dsptools.numbers._
import org.scalatest.{FlatSpec, Matchers}

class DesSpec extends FlatSpec with Matchers {
  behavior of "Des"

  val params = SIntDesParams(
    wADC = 3,
    nSample = 16,
  )
  it should "des" in {
    val baseTrial = XYZ(offset=0, ready=true)
    val offset = Seq(0, 1, 2, 3, 4)
    val trials = offset.map { offset => baseTrial.copy(offset = offset) }
//    FixedCordicTester(params, trials) should be (true)
  }


}

