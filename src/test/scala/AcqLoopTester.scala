package gps

import dsptools.DspTester
import dsptools.numbers._
import org.scalatest.{FlatSpec, Matchers}
import chisel3._
import chisel3.experimental.FixedPoint
import java.nio.ByteBuffer
import java.nio.file.{Files, Paths}
import scala.math._


class ALoopSpec extends FlatSpec with Matchers {
  behavior of "ALoop"

  val nHalfFreq = 4
  val freqStep = 100000

  val params = EgALoopParams(
    wADC = 8,
    wCA = 3,
    wNCOTct = 8,
    wNCORes = 32,
    wFFT = 32,
    wFractionFFT = 16,
    nSample = 32,
    nLoop = 2,
    nFreq = 2 * nHalfFreq + 1,
    nLane = 8,
    nStgFFT = 0,
    nStgIFFT = 0,
    nStgFFTMul = 4,
    freqStep = freqStep,
    freqMin = 2045950 - nHalfFreq * freqStep,
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




  val byteArray = Files.readAllBytes(Paths.get("python/data/gioveAandB_short.bin"))

  for (trial <- trials) {


    poke(c.io.in.valid, 0)
    poke(c.io.in.idx_sate, trial.idx_sate)
    poke(c.io.out.ready, 0)
    poke(c.io.debug.sineWaveTest, 1)
    poke(c.io.debug.selfCATest, 0)

    // wait until input is accepted
    var cycles = 0
    var fire = 0
    var key = 0
    var key1 = 0
    var key2 = 0
    var corr = 0.0
    var ifft_data = 0.0
    var data_ADC = 0.0
    var data_cos = 0.0
    var lt_p_0p5 = true
    var st_n_0p5 = true

    print("trial")
    while (cycles < 1500) {

      if (cycles == 0) {poke(c.io.in.valid, 1)}
      else {poke(c.io.in.valid, 1)}

//      data_ADC = byteArray(cycles)
      data_cos = math.cos((cycles-1) * (2 * 3.1415926535897932384626 / 8))
      data_ADC = (data_cos*32).toInt
//      lt_p_0p5 = data_cos > 0.5
//      st_n_0p5 = data_cos < -0.5
//      if (lt_p_0p5) {
//        data_ADC = 1
//      }
//      else if (st_n_0p5) {
//        data_ADC = -1
//      }
//      else {
//        data_ADC = 0
//      }

//      data_ADC = (math.cos(cycles * (2 * 3.1415927 / 8)) * 8).toInt
//      data_ADC = (math.cos(cycles * (2 * 3.1415927 / 8)) * 8).toInt

      poke(c.io.in.ADC, data_ADC)

      if (peek(c.io.out.valid)) {
        peek(c.io.out.iFreqOpt)
        peek(c.io.out.freqOpt)
        peek(c.io.out.CPOpt)
      }

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



      cycles += 1
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




