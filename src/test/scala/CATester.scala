package gps

import dsptools.DspTester
import scala.io._

/**
 * DspTester for FixedIterativeCordic
 *
 */
class CAEarlyTester(c: CA, prnCodes: Array[Array[Int]], ncoInput: Array[Int]) extends DspTester(c) {
  /*
  this test takes in an NCO input of (-1)^n with length 1023*2. Every 2 clock cycles the output of
  the generator should update. This test steps through all 32 satellite sequences and verifies that
  the CA can properly output the early signal. This test does not determine if punctual/late are
  correct. This test also verifies that the done signal properly goes high when a cycle is complete.
   */
  poke(c.io.fco2x, 0)
  for(j <- 0 until 32) {
    //The CA has 1 indexed satellite feedback positions, so we need 1 - 32 not 0 - 31
    poke(c.io.satellite, j + 1)
    for(i <- 0 until ncoInput.length) {
      poke(c.io.fco, ncoInput(i))
      if (i == 0) {step(1)}
      expect(c.io.done, 0)
      step(1)
      //NCO test is -1, 1, -1, 1 so it takes 2 cycles to actually update. Hence the i % 2
      //We're updating 2x as fast as prnCodes changes so i/2
      //The PRN Codes list is 0 indexed, so j is fine
      if (i % 2 == 0) { expect(c.io.early, 2*prnCodes(j)(i/2) - 1) }
    }
    expect(c.io.done, 1)
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


class CANoOutputTester(c: CA, prnCodes: Array[Array[Int]], ncoInput: Array[Int]) extends DspTester(c) {
  /*
    This test checks that if there's never a 0 crossing (constant input) that the output will not
    never change. Done should also never go high since the sequence is never completed.
  */
  //All 3 outputs should be the first entry forever for each satellite. 
  poke(c.io.fco2x, 0)
  for(j <- 0 until 32) {
    poke(c.io.satellite, j + 1)
    for(i <- 0 until ncoInput.length) {
      poke(c.io.fco, ncoInput(i))
      expect(c.io.done, 0)
      step(1)
      //See above test for j vs satellite -> j+1
      expect(c.io.early, prnCodes(j)(0))
      expect(c.io.punctual, prnCodes(j)(0))
      expect(c.io.late, prnCodes(j)(0))
    }
    expect(c.io.done, 0)
  }
}
object CANoOutputTester {
  def apply(params: CAParams, prnCodes: Array[Array[Int]], ncoInput: Array[Int]): Boolean = {
    chisel3.iotesters.Driver.execute(Array("-tbn", "firrtl", "-fiwv"), () => new CA(params)) {
      c => new CANoOutputTester(c, prnCodes, ncoInput)
    }
  }
}

