package gps

import dsptools.numbers._
import org.scalatest.{FlatSpec, Matchers}

class DesSpec extends FlatSpec with Matchers {
  behavior of "Des"

  val params = SIntDesParams(
    wADC = 4,
    nSample = 5,
  )
  it should "des" in {
    val baseTrial = XYZ(offset=0)
    val offset = Seq(2)
    val trials = offset.map { offset => baseTrial.copy(offset = offset) }
    DesTester(params, trials) should be (true)
  }


}

