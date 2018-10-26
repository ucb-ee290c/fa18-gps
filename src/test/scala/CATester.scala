package gps

import dsptools.DspTester
import dsptools.numbers._
import org.scalatest.{FlatSpec, Matchers}
import scala.io._

/**
 * DspSpec for CACode Gen
 */
class CASpec extends FlatSpec with Matchers {
  behavior of "CA"
  val params = CAParams(
    fcoWidth = 10,
    codeWidth = 2,
  )
  val prnCodeRaw = io.Source.fromFile("./src/test/scala/PRNCode.csv").getLines.toList.map(_.split(","))
  //Creates an array of 32 arrays each of which has a 1023 length PRN sequence
  val prnCodes = Array.ofDim[Int](prnCodeRaw.length, prnCodeRaw(0).length)
  for(i <- 0 until prnCodeRaw.length) {
    prnCodes(i) = prnCodeRaw(i).map(_.toInt)
  }
  val ncoInput = new Array[Int](1023*2)
  for(i <- 0 until ncoInput.length) {
    if (i % 2 == 0) { ncoInput(i) = -1 }
    else { ncoInput(i) = 1 }
  }
  it should "give the correct early PRN" in {
  //Tests if the early signal is what's expected.
    CAEarlyTester(params, prnCodes, ncoInput) should be (true)
  }
  //Tests if there's never a zero crossing on the NCO that the output is always the same
  it should "never change" in {
    val ncoInput2 = Array.fill[Int](1023*2)(0) 
    CANoOutputTester(params, prnCodes, ncoInput2) should be (true)
  }
  /*
  //Tests something
  it should "output punctual/late correctly" in {
    val ncoInputTemp = new Array[Int](1023*4)
    val ncoInput2xTemp = new Array[Int](1023*4)
    for(i <- 0 until 1023*4) {
      if(i % 2 == 0){ ncoInput2xTemp(i) = -1 }
      else { ncoInput2xTemp(i) = 1 }

      if(i % 4 == 0 || i % 4 == 1) { ncoInputTemp(i) = -1 }
      else if (i % 4 == 1 || i % 4 == 2) { ncoInputTemp(i) = 1 }
    }
    CAPunctualTester(params, prnCodes, ncoInputTemp, ncoInput2xTemp) should be (true)
  }
  */
}


/**
 * DspTester for CACode Gen
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

which translates to:
early:     1 1 1 -1 -1 -1 -1 1 1 X
punc:  X   1 1 1 -1 -1 -1 -1 1 1
late:  X X 1 1 1 -1 -1 -1 -1 1 1
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
    chisel3.iotesters.Driver.execute(Array("-tbn", "verilator", "-fiwv"), () => new CA(params)) {
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
    chisel3.iotesters.Driver.execute(Array("-tbn", "verilator", "-fiwv"), () => new CA(params)) {
      c => new CASwitchSatelliteTester(c, prnCodes, ncoInput)
    }
  }
}
