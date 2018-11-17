package gps

import org.scalatest.{FlatSpec, Matchers}


class FixedCordicSpec extends FlatSpec with Matchers {
  behavior of "FixedIterativeCordic"

//  val params = FixedCordicParams(
//    xyWidth = 16,
//    xyBPWidth = 8,
//    zWidth = 16,
//    zBPWidth = 8,
//    correctGain = true,
//    stagesPerCycle = 1,
//    calAtan2 = false,
//    dividing = false,
//  )
  /**
    * ****NOTES for setting Input value:
    * for x and y, the max input <= 2**(N-2) - 1
    *   as the MSB is sign, and MSB-1 to avoid overflow
    * for z at rotation/vectoring mode:
    *   z is from -pi to pi
    * at dividing mode:
    *   as long as z have the same bits as x,y, fine

    * ****NOTES for setting up stages:
    * for rotation/vectoring mode:
    *   Suggest nStages <= xyWidth and ZWidth
    *   ZWidth-ZBPWidth = 3 is enough
    * for dividing mode:
    *   z should have the same bits as xy
    *   xyWidth-xyBPWidth <= xyBPWidth
    *   zWidth-zBPWidth <= zBPWidth
    *   suggest nStages = max(xyWidth, zWidth)
   */


  val params = FixedCordicParams(
    xyWidth = 20,
    xyBPWidth = 12,
    zWidth = 20,
    zBPWidth = 12,
    correctGain = true,
    nStages = 20,
    stagesPerCycle = 1,
    calAtan2 = false,
    dividing = true,
  )

  def angleMap(phi: Double): Double = {
    if (phi > math.Pi / 2)
      phi - math.Pi
    else
      if (phi < -math.Pi / 2)
        phi + math.Pi
      else
        phi
  }

  val baseTrial = XYZ(xin = 0.0, yin = 0.0, zin = 0.0, vectoring = true)
  var yin = Seq(1, 2, 4, 8, 16, 31)
  var angles = Seq(-3.0, -2.0, -1.0, -0.0, -1.0, 2.0, 3.0)

  val dividingTrials =
    // check dividing
    yin.map { y =>
      baseTrial.copy(xin = 31, yin = y, zout = Some(y / 31.0))
    }

  val atan2Trials =
    // check atan2
    angles.map { phi =>
      baseTrial.copy(xin = 15 * math.cos(phi), yin = 15 * math.sin(phi), zout = Some(phi))
    }
  val atanTrials =
    // check atan
    angles.map { phi =>
      baseTrial.copy(xin = 15 * math.cos(phi), yin = 15 * math.sin(phi), zout = Some(angleMap(phi)))
    }

  it should "dividing with stagesPerCycle=1" in {
    FixedCordicTester(params.copy(calAtan2=false, dividing=true), dividingTrials) should be (true)
  }

//  it should "atan2 with stagePerCycle=1" in {
//    FixedCordicTester(params.copy(calAtan2=true, dividing=false), atan2Trials) should be (true)
//  }
//
//  it should "atan with stagePerCycle=1" in {
//    FixedCordicTester(params.copy(calAtan2=false, dividing=false), atanTrials) should be (true)
//  }

//  it should "rotate with stagesPerCycle=4" in {
//    FixedCordicTester(params.copy(stagesPerCycle = 4), rotateTrials) should be (true)
//  }
//  it should "vector with stagesPerCycle=1" in {
//    FixedCordicTester(params, trials) should be (true)
//  }
//  it should "vector with stagesPerCycle=4" in {
//    FixedCordicTester(params.copy(stagesPerCycle = 4), vectorTrials) should be (true)
//  }

}
