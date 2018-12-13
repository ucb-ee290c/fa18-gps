package gps

import dsptools.DspTester
import org.scalatest.{FlatSpec, Matchers}

case class ABC(
                xin: Double,
                yin: Double,
                zin: Double,
                vectoring: Boolean,
                xout: Option[Double] = None,
                yout: Option[Double] = None,
                zout: Option[Double] = None
              )

class FixedCordicTester[T <: chisel3.Data](c: FixedIterativeCordic[T], trials: Seq[ABC]) extends DspTester(c) {
  poke(c.io.out.ready, 1)
  poke(c.io.in.valid, 1)

  for (trial <- trials) {
    poke(c.io.in.bits.x, trial.xin)
    poke(c.io.in.bits.y, trial.yin)
    poke(c.io.in.bits.z, trial.zin)
    poke(c.io.vectoring, trial.vectoring)

    while (!peek(c.io.in.ready)) {
      step(1)
    }
    while (!peek(c.io.out.valid)) {
      peek(c.io.out.bits.x)
      peek(c.io.out.bits.y)
      peek(c.io.out.bits.z)
      step(1)
    }
    fixTolLSBs.withValue(4) {
      trial.xout.foreach { x => expect(c.io.out.bits.x, x) }
      trial.yout.foreach { y => expect(c.io.out.bits.y, y) }
      trial.zout.foreach { z => expect(c.io.out.bits.z, z) }
    }
  }
}

object FixedCordicTester {
  def apply(params: FixedCordicParams, trials: Seq[ABC]): Boolean = {
    chisel3.iotesters.Driver.execute(Array("-fiwv", "-tbn", "firrtl"), () => new FixedIterativeCordic(params)) {
      c => new FixedCordicTester(c, trials)
    }
  }
}

class FixedCordicSpec extends FlatSpec with Matchers {
  behavior of "FixedIterativeCordic"

  val params = FixedCordicParams(
    xyWidth = 32,
    xyBPWidth = 16,
    zWidth = 32,
    zBPWidth = 20,
    correctGain = true,
    nStages = 32,
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

  val baseTrial = ABC(xin = 0.0, yin = 0.0, zin = 0.0, vectoring = true)
  var yin = Seq(1, 2, 4, 8, 16, 31, 57)
  var angles = Seq(-3.0, -2.0, -1.0, -0.0, -1.0, 2.0, 3.0)

  val dividingTrials =
    // check dividing
    yin.map { y =>
      baseTrial.copy(xin = -29, yin = y, zout = Some(y / -29.0))
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

  it should "atan2 with stagePerCycle=1" in {
    FixedCordicTester(params.copy(calAtan2=true, dividing=false), atan2Trials) should be (true)
  }

  it should "atan with stagePerCycle=1" in {
    FixedCordicTester(params.copy(calAtan2=false, dividing=false), atanTrials) should be (true)
  }
}
