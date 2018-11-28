package gps

import dsptools.DspTester
import chisel3._
import org.scalatest.{FlatSpec, Matchers}

/*
 * DspSpec for SecondarySync
 */
class SecondarySyncSpec extends FlatSpec with Matchers {
  behavior of "SecondarySync"

  val params = new SecondarySyncParams(intThreshold = 1000, intDumpWidth = 32) {}

  it should "synchronize with the baseband data bits" in {
    SecondarySyncTester(params) should be (true)
  }
}

/*
 * DspTester for SecondarySync
 */
class SecondarySyncTester(c: SecondarySync[SInt]) extends DspTester(c) {
/* ipIntDump = SInt(32.W)
 * lockAchieved = Bool
 * dump = Bool
 * secondarySyncAchieved = Bool
 */

  //test that it will lock going from high to low
  poke(c.io.ipIntDump, 5000.S)
  poke(c.io.lockAchieved, false.B)
  poke(c.io.dump, false.B)
  step(10)
  poke(c.io.lockAchieved, true.B)
  step(10)
  expect(c.io.secondarySyncAchieved, false.B)

  for (i <- 0 until 20) {
    if (i % 5 == 0) {
      poke(c.io.dump, true.B)
      step(1)
      poke(c.io.dump, false.B)
    }
    step(1)
  }

  expect(c.io.secondarySyncAchieved, false.B)

  poke(c.io.ipIntDump, (-5000).S)
  step(1) 
  poke(c.io.dump, true.B)
  step(1)
  poke(c.io.dump, false.B)
  step(5)

  expect(c.io.secondarySyncAchieved, true.B)
  
  //check that after losing primary lock, secondary lock is also lost
  poke(c.io.lockAchieved, false.B)
  step(3)
  expect(c.io.secondarySyncAchieved, false.B)

  //test that it will lock going from low to high
  poke(c.io.ipIntDump, (-5000).S)
  poke(c.io.lockAchieved, true.B)
  step(5)

  for (i <- 0 until 20) {
    if (i % 5 == 0) {
      poke(c.io.dump, true.B)
      step(1)
      poke(c.io.dump, false.B)
    }
    step(1)
  }

  expect(c.io.secondarySyncAchieved, false.B)

  poke(c.io.ipIntDump, 5000.S)
  step(1) 
  poke(c.io.dump, true.B)
  step(1)
  poke(c.io.dump, false.B)
  step(5)

  expect(c.io.secondarySyncAchieved, true.B)
}

object SecondarySyncTester {
  def apply(params: SecondaryLockParams[SInt]): Boolean = {
    dsptools.Driver.execute(() => new SecondarySync[SInt](params), TestSetup.dspTesterOptions) {
      c => new SecondarySyncTester(c)
    }
  }
}
