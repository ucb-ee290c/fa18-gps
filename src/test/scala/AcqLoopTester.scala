package gps

import dsptools.DspTester
import dsptools.numbers._
import org.scalatest.{FlatSpec, Matchers}
import chisel3._
import chisel3.experimental.FixedPoint
import java.nio.ByteBuffer
import java.nio.file.{Files, Paths}
//import scala.math.BigInt


class ALoopSpec extends FlatSpec with Matchers {
  behavior of "ALoop"

  val params = EgALoopParams(
    wADC = 5,
    wCA = 3,
    wNCOTct = 5,
    wNCORes = 32,
    nSample = 256,
    nLoop = 2,
    nFreq = 5,
    nLane = 256,
    nStgFFT = 0,
    nStgIFFT = 0,
    nStgFFTMul = 4,
    freqMin = 1000,
    freqStep = 9,
  )
  it should "ALoop" in {
    val baseTrial = ALoopTestVec(idx_sate=0)
    val idx_sate = Seq(0)
    val trials = idx_sate.map { idx_sate => baseTrial.copy(idx_sate = idx_sate) }
    ALoopTester(params, trials) should be (true)
  }


}



/**
 * Case class holding information needed to run an individual test
 */
case class ALoopTestVec(
  idx_sate: Int,
  optFreq: Option[Int] = None,
  optCP: Option[Int] = None,
  sateFound: Option[Boolean] = None,
)

/**
 * DspTester for acquisition loop
 *
 * Run each trial in @trials
 */
class ALoopTester[T1 <: chisel3.Data, T2 <: chisel3.Data](c: ALoop[T1,T2], trials: Seq[ALoopTestVec], tolLSBs: Int = 1)
  extends DspTester(c) {




  val byteArray = Files.readAllBytes(Paths.get("python/data/acqctrl_test_vec.bin"))

  for (trial <- trials) {


    poke(c.io.in.valid, 0)
    poke(c.io.in.idx_sate, trial.idx_sate)
    poke(c.io.out.ready, 0)

    // wait until input is accepted
    var cycles = 0
    var fire = 0
    var key = 0
    var key1 = 0
    var key2 = 0
    var corr = 0.0
    var ifft_data = 0.0
    var data_ADC = 0.0

    print("trial")
    while (cycles < 3000) {

      if (cycles == 0) {poke(c.io.in.valid, 1)}
      else {poke(c.io.in.valid, 1)}

      data_ADC = byteArray(cycles)
      poke(c.io.in.ADC, data_ADC)

      cycles += 1

//      peek(c.io.Ain.ready)
//      peek(c.io.Aout.valid)
//      peek(c.io.Tin.ready)
//      peek(c.io.Tout.valid)
//      peek(c.io.Ain.Correlation)
//      peek(c.io.Aout.freqNow)
//      peek(c.io.Aout.freqNext)
//      peek(c.io.Aout.cpNow)
//      peek(c.io.Aout.cpNext)
//      peek(c.io.Tout.freqOpt)
//      peek(c.io.Tout.CPOpt)



      step(1)


    }


  }
}



/**
  * Convenience function for running tests
  */
object ALoopTester {
  def apply(params: ALoopParams[SInt, FixedPoint], trials: Seq[ALoopTestVec]): Boolean = {
    chisel3.iotesters.Driver.execute(Array("-tbn", "verilator", "-fiwv", "-fimed", "1000000000000"), ()
    => new ALoop[SInt, FixedPoint](params)) {
//    dsptools.Driver.execute(() => new ACtrl(params), TestSetup.dspTesterOptions) {
      c => new ALoopTester(c, trials)
    }
  }
}




