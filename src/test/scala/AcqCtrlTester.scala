package gps

import dsptools.DspTester
import dsptools.numbers._
import org.scalatest.{FlatSpec, Matchers}
import chisel3._


class ACtrlSpec extends FlatSpec with Matchers {
  behavior of "ACtrl"

  val params = IntACtrlParams(
    nLoop = 1,
    nFreq = 3,
    nSample = 3,
    wCorrelation = 20,
    wLoop = 5,
    wIdxFreq = 5,
    wFreq = 20,
    wCodePhase = 5,
    wADC = 10,
    wSate = 5,
    lane = 1,
    freqMin = 1000,
    freqStep = 9,
  )
  it should "ACtrl" in {
    val baseTrial = XYZ(ADC=0, CodePhase=0, idx_sate=0)
    val idx_sate = Seq(0)
    val trials = idx_sate.map { idx_sate => baseTrial.copy(idx_sate = idx_sate) }
    ACtrlTester(params, trials) should be (true)
  }


}



/**
 * Case class holding information needed to run an individual test
 */
case class XYZ(
  // input x, y and z
//  in: Int,
  // mode
  ADC: Int,
  CodePhase: Int,
  idx_sate: Int,
  // optional outputs
  // if None, then don't check the result
  // if Some(...), check that the result matches
  freq: Option[Int] = None,
  optFreq: Option[Int] = None,
  optCP: Option[Int] = None,
  sateFound: Option[Boolean] = None,
)

/**
 * DspTester for FixedIterativeCordic
 *
 * Run each trial in @trials
 */
class ACtrlTester[T1 <: chisel3.Data,T2 <: chisel3.Data,T3 <: chisel3.Data](c: ACtrl[T1,T2,T3], trials: Seq[XYZ], tolLSBs: Int = 1)
  extends DspTester(c) {

  poke(c.io.Ain.valid, 1)
  poke(c.io.Aout.ready, 1)
  poke(c.io.Tin.valid, 1)
  poke(c.io.Tout.ready, 0)

  for (trial <- trials) {


    poke(c.io.Ain.ADC, trial.ADC)
    poke(c.io.Ain.CodePhase, trial.CodePhase)
    poke(c.io.Tin.idx_sate, trial.idx_sate)



    // wait until input is accepted
    var cycles = 0
    var key = 0
    var corr = 0.0

    print("trial")
    while (cycles < 15) {

      key = cycles % 9

      if (cycles == 5) {poke(c.io.Tin.valid, 0)}


      if (key == 0) corr = 000.0;
      else if (key == 1) corr = 0000.0;
      else if (key == 2) corr = 0000.0;
      else if (key == 3) corr = 0000.0;
      else if (key == 4) corr = 0000.0;
      else if (key == 5) corr = 0000.0;
      else if (key == 6) corr = 0000.0;
      else if (key == 7) corr = 10000.0;
      else corr = 000.0

      poke(c.io.Ain.Correlation, corr)

      peek(c.io.Ain.ready)
      peek(c.io.Aout.valid)
      peek(c.io.Tout.valid)
      peek(c.io.Aout.freqNow)
      peek(c.io.Aout.freqNext)
      peek(c.io.Aout.cpNow)
      peek(c.io.Aout.cpNext)
      peek(c.io.Tout.optFreq)
      peek(c.io.Tout.optCP)
      peek(c.io.Tout.optIdxFreqItm)
      peek(c.io.Tout.optIdxFreqOut)
      peek(c.io.Tout.optCPItm)
      peek(c.io.Tout.optCPOut)
      peek(c.io.Tout.sateFound)
//      peek(c.io.Reg.max)
//      peek(c.io.Reg.sum)


      cycles += 1
      step(1)
    }
    // wait until output is valid
//    cycles = 0


  }
}

/**
 * Convenience function for running tests
 */
object ACtrlTester {
  def apply(params: ACtrlParams[UInt,SInt,DspReal], trials: Seq[XYZ]): Boolean = {
    chisel3.iotesters.Driver.execute(Array("-tbn", "verilator", "-fiwv"), () => new ACtrl(params)) {
      c => new ACtrlTester(c, trials)
    }
  }
}

