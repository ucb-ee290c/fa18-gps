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
/*
Tests punctual functionality. This test is "correct" in that I manually printed the values out and 
checked by hand. A fully automated one is in progress.

For reference, the first 20 outputs:
early: 111111-1-1-1-1-1-1-1-11111-1-1
punc:  1111111-1-1-1-1-1-1-1-11111-1
late:  111111111-1-1-1-1-1-1-1-1111
*/
class CAPunctualTester(c: CA, prnCodes: Array[Array[Int]], ncoInput: Array[Int], ncoInput2x: Array[Int]) extends DspTester(c) {
  var early_outputs = List[Int]()
  var punc_outputs = List[Int]()
  var late_outputs = List[Int]()
  for(j <- 0 until 1) {
    poke(c.io.satellite, j + 1)
    for(i <- 0 until ncoInput2x.length) {
      poke(c.io.fco, ncoInput(i))
      poke(c.io.fco2x, ncoInput2x(i))
      step(1)
      early_outputs = early_outputs :+ peek(c.io.early)
      punc_outputs = punc_outputs :+ peek(c.io.punctual)
      late_outputs = late_outputs :+ peek(c.io.late)
    }
    for(i <- 0 until 20) {
      printf("%d", early_outputs(i))
    }
    printf("\n")
    for(i <- 0 until 20) {
      printf("%d", punc_outputs(i))
    }
    printf("\n")
    for(i <- 0 until 20) {
      printf("%d", late_outputs(i))
    }
    printf("\n")
  }
}

object CAPunctualTester {
  def apply(params: CAParams, prnCodes: Array[Array[Int]], ncoInput: Array[Int], ncoInput2x: Array[Int]): Boolean = {
    chisel3.iotesters.Driver.execute(Array("-tbn", "firrtl", "-fiwv"), () => new CA(params)) {
      c => new CAPunctualTester(c, prnCodes, ncoInput, ncoInput2x)
    }
  }
}
/*
Tests that switching the satellite mid-test works properly. I can't think of a good way to test this
without introducing more outputs. For now I will trust that it works.
*/
class CASwitchSatelliteTester(c: CA, prnCodes: Array[Array[Int]], ncoInput: Array[Int]) extends DspTester(c) {
  poke(c.io.satellite, 1)
  for(i <- 0 until 30) {
    poke(c.io.fco, ncoInput(i))
    step(1)
  }
}

object CASwitchSatelliteTester {
  def apply(params: CAParams, prnCodes: Array[Array[Int]], ncoInput: Array[Int]): Boolean = {
    chisel3.iotesters.Driver.execute(Array("-tbn", "firrtl", "-fiwv"), () => new CA(params)) {
      c => new CASwitchSatelliteTester(c, prnCodes, ncoInput)
    }
  }
}
