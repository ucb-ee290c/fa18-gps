package gps

import org.scalatest.{FlatSpec, Matchers}

class FixedCordicSpec extends FlatSpec with Matchers {
  behavior of "FixedIterativeCordic"

  val params = FixedCordicParams(
    xyWidth = 16,
    zWidth = 16,
    correctGain = true,
    stagesPerCycle = 1,
    calAtan2 = true,
  )
  val angles = (BigDecimal(-math.Pi) to math.Pi by 0.1)
  val rotateBaseTrial = XYZ(xin=1.0, yin=0.0, zin=0.0, vectoring=false)
  val rotateTrials = angles.map { phi => rotateBaseTrial.copy(zin = phi.toDouble, xout = Some(math.cos(phi.toDouble))) }
  val vectorBaseTrial = XYZ(xin=0.0, yin=0.0, zin=0.0, vectoring=true)
  val vectorTrials = angles.tail.map { phi => vectorBaseTrial.copy(
    xin = math.cos(phi.toDouble),
    yin = math.sin(phi.toDouble),
    zout = Some(phi.toDouble)
  )}


  it should "rotate with stagesPerCycle=1" in {
    FixedCordicTester(params, rotateTrials) should be (true)
  }
  it should "rotate with stagesPerCycle=4" in {
    FixedCordicTester(params.copy(stagesPerCycle = 4), rotateTrials) should be (true)
  }

  it should "vector with stagesPerCycle=1" in {
    FixedCordicTester(params, vectorTrials) should be (true)
  }
  it should "vector with stagesPerCycle=4" in {
    FixedCordicTester(params.copy(stagesPerCycle = 4), vectorTrials) should be (true)
  }
}
