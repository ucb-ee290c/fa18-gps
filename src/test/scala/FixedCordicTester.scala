package gps

import dsptools.DspTester

case class XYZ(
                xin: Double,
                yin: Double,
                zin: Double,
                vectoring: Boolean,
                xout: Option[Double] = None,
                yout: Option[Double] = None,
                zout: Option[Double] = None
              )

class FixedCordicTester(c: FixedIterativeCordic, trials: Seq[XYZ]) extends DspTester(c) {
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
  def apply(params: FixedCordicParams, trials: Seq[XYZ]): Boolean = {
    chisel3.iotesters.Driver.execute(Array("-fiwv", "-tbn", "firrtl"), () => new FixedIterativeCordic(params)) {
      c => new FixedCordicTester(c, trials)
    }
  }
}