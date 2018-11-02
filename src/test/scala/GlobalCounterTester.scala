package gps

import dsptools.DspTester
import dsptools.numbers._
import org.scalatest.{FlatSpec, Matchers}

class GlobalCounterSpec extends FlatSpec with Matchers {
  behavior of "GlobalCounter"
  val clk = 1/(16 * scala.math.pow(10, 6))
  val params = GlobalCounterParams(
    clkPeriod = clk,
    counterWidth = 24,
    secondsWidth = 32,
    secondsBP = 32
  )
  it should "count" in {
    GlobalCounterFunctionalTester(params) should be (true)
  }
  it should "timestamp correctly" in {
    GlobalCounterSecondsTester(params) should be (true)
  }
}

class GlobalCounterFunctionalTester(c: GlobalCounter, params: GlobalCounterParams) extends DspTester(c) {
  val maxVal = (scala.math.pow(2, params.counterWidth) - 1).toInt
  for(i <- 0 until maxVal) {
    expect(c.io.currCycle, i)
    step(1)
  }
}

object GlobalCounterFunctionalTester {
  def apply(params: GlobalCounterParams) = {
    chisel3.iotesters.Driver.execute(Array("-tbn", "firrtl", "-fiwv"), () => new GlobalCounter(params)) {
      c => new GlobalCounterFunctionalTester(c, params)
    }
  }
}

class GlobalCounterSecondsTester(c: GlobalCounter, params: GlobalCounterParams) extends DspTester(c) {
  val maxVal = (scala.math.pow(2, params.counterWidth) - 1).toInt
  val tolLSBs = 20
  for(i <- 0 until maxVal) {
    fixTolLSBs.withValue(tolLSBs) {
      expect(c.io.currTimeSeconds, i*params.clkPeriod)
      step(1)
    }
  }
}

object GlobalCounterSecondsTester {
  def apply(params: GlobalCounterParams) = {
    chisel3.iotesters.Driver.execute(Array("-tbn", "firrtl", "-fiwv"), () => new GlobalCounter(params)) {
      c => new GlobalCounterSecondsTester(c, params)
    }
  }
}
