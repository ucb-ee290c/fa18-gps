package gps

import org.scalatest.{FlatSpec, Matchers}


class FixedCordicSpec extends FlatSpec with Matchers {
  behavior of "FixedIterativeCordic"

  val params = FixedCordicParams(
    xyWidth = 16,
    xyBPWidth = 8,
    zWidth = 16,
    zBPWidth = 8,
    correctGain = true,
    stagesPerCycle = 1,
    calAtan2 = false,
    dividing = false,
  )
//  val params = FixedCordicParams(
//    xyWidth = 20,
//    xyBPWidth = 12,
//    zWidth = 20,
//    zBPWidth = 12,
//    correctGain = true,
//    // nStages = 20,
//    stagesPerCycle = 1,
//    calAtan2 = false,
//    dividing = true,
//  )
  def angleMap(phi: Double): Double = {
    if (phi > math.Pi / 2)
      phi - math.Pi
    else
      if (phi < -math.Pi / 2)
        phi + math.Pi
      else
        phi
  }

  val baseTrial = XYZ(xin=0.0, yin=0, zin=0.0, vectoring=true)
  var angles = Seq(-3.0, -2.0, -1.0, -0.0, -1.0, 2.0, 3.0)
  //    Seq(baseTrial.copy(xin = 127, yin = 1, zout = Some(0.0)))
  val trials =
  if (params.calAtan2) {
    angles.map { phi =>
      baseTrial.copy(xin = 15*math.cos(phi), yin = 15*math.sin(phi), zout = Some(phi))
    }
  }else{
    angles.map { phi =>
      baseTrial.copy(xin = 15*math.cos(phi), yin = 15*math.sin(phi), zout = Some(angleMap(phi)))
    }
  }
  FixedCordicTester(params, trials) should be (true)

//  it should "rotate with stagesPerCycle=1" in {
//    FixedCordicTester(params, rotateTrials) should be (true)
//  }
//  it should "rotate with stagesPerCycle=4" in {
//    FixedCordicTester(params.copy(stagesPerCycle = 4), rotateTrials) should be (true)
//  }

  it should "vector with stagesPerCycle=1" in {
    FixedCordicTester(params, trials) should be (true)
  }
//  it should "vector with stagesPerCycle=4" in {
//    FixedCordicTester(params.copy(stagesPerCycle = 4), vectorTrials) should be (true)
//  }
}
