package gps

import dsptools.DspTester
import scala.io._

/**
 * DspTester for FixedIterativeCordic
 *
 * Run each trial in @trials
 */
class CAEarlyTester(c: CA, prnCodes: Array[Array[Int]], ncoInput: Array[Int]) extends DspTester(c) {
  //val outputs = new Array[Int](1023)
  val maxCyclesToWait = 1024
  poke(c.io.fco2x, 0)
  for(j <- 0 until 1) {
    poke(c.io.satellite, j + 1)
    for(i <- 0 until ncoInput.length - 1000) {
      poke(c.io.fco, ncoInput(i))
      peek(c.io.fco)
      peek(c.io.testVec)
      step(1)
      expect(c.io.early, 2*prnCodes(j)(i) - 1) 
      //outputs(i) = c.io.early
    }
  }
  /*
  for(i <- 0 until 20) {
    printf("%d", outputs(i))
  }
  */
}
/**
 * Convenience function for running tests
 */
object CAEarlyTester {
  def apply(params: CAParams, prnCodes: Array[Array[Int]], ncoInput: Array[Int]): Boolean = {
    chisel3.iotesters.Driver.execute(Array("-tbn", "firrtl", "-fiwv"), () => new CA(params)) {
      c => new CAEarlyTester(c, prnCodes, ncoInput)
    }
  }
}


