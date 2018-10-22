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
  poke(c.io.fco2x, 0)
  for(j <- 0 until 32) {
    poke(c.io.satellite, j + 1)
    for(i <- 0 until ncoInput.length) {
      poke(c.io.fco, ncoInput(i))
      if (i == 0) {step(1)}
      step(1)
      //NCO test is -1, 1, -1, 1 so it takes 2 cycles to actually update. Hence the i % 2
      //We're updating 2x as fast as prnCodes changes so i/2
      if (i % 2 == 0) { expect(c.io.early, 2*prnCodes(j)(i/2) - 1) }
      //outputs(i) = c.io.early
    }
  }
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


