package gps

import dsptools.DspTester
import scala.io._

/**
 * DspTester for FixedIterativeCordic
 *
 * Run each trial in @trials
 */
class CAEarlyTester(c: CA, trials: Seq[Int]) extends DspTester(c) {
  val prnCodeRaw = io.Source.fromFile("PRNCode.csv").getLines.toList.map(_.split(","))
  val prnCode = prnCodeRaw(0).map(_.toInt) 
  for(i <- 0 until 10) {
    printf("%d", prnCode(i))
  }
}
/**
 * Convenience function for running tests
 */
object CAEarlyTester {
  def apply(params: CAParams, trials: Seq[Int]): Boolean = {
    chisel3.iotesters.Driver.execute(Array("-tbn", "firrtl", "-fiwv"), () => new CA(params)) {
      c => new CAEarlyTester(c, trials)
    }
  }
}


