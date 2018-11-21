package gps

import dsptools.DspTester
import dsptools.numbers._
import org.scalatest.{FlatSpec, Matchers}
import chisel3._
import chisel3.experimental.FixedPoint
import java.nio.ByteBuffer
import java.nio.file.{Files, Paths}
import scala.math._


class FFTChainSpec extends FlatSpec with Matchers {
  behavior of "FFTChain"



  val params = FixedFFTChainParams(
    width = 64,
    bp = 32,
    nSample = 32,
    nLane = 8,
    nStgFFT = 0,
    nStgIFFT = 0,
    nStgFFTMul = 4,
  )
  it should "FFTChain" in {
    val baseTrial = FFTChainTestVec(idx_sate=0)
    val idx_sate = Seq(0)
    val trials = idx_sate.map { idx_sate => baseTrial.copy(idx_sate = idx_sate) }
    FFTChainTester(params, trials) should be (true)
  }


}



/**
 * Case class holding information needed to run an individual test
 */
case class FFTChainTestVec(
  idx_sate: Int,
)

/**
 * DspTester for acquisition loop
 *
 * Run each trial in @trials
 */
class FFTChainTester[T <: chisel3.Data](c: FFTChain[T], trials: Seq[FFTChainTestVec], tolLSBs: Int = 1)
  extends DspTester(c) {

//  val byteArray = Files.readAllBytes(Paths.get("python/data/gioveAandB_short.bin"))

  for (trial <- trials) {


    poke(c.io.in.valid, 0)
    poke(c.io.in.sync, 0)
    for (i <- 0 until 8) {

      poke(c.io.in.CA(i), 1)
      poke(c.io.in.ADC(i), 1)
      poke(c.io.in.cos(i), 1)
      poke(c.io.in.sin(i), 0)
    }

    // wait until input is accepted
    var cycles = 0
//    var fire = 0
//    var key = 0
//    var key1 = 0
//    var key2 = 0
//    var corr = 0.0
//    var ifft_data = 0.0
//    var data_ADC = 0.0
//    var data_cos = 0.0
//    var lt_p_0p5 = true
//    var st_n_0p5 = true

    print("trial")
    while (cycles < 1500) {


      cycles += 1
      step(1)


    }


  }
}



/**
  * Convenience function for running tests
  */
object FFTChainTester {
  def apply(params: FFTChainParams[FixedPoint], trials: Seq[FFTChainTestVec]): Boolean = {
    chisel3.iotesters.Driver.execute(Array("-tbn", "verilator", "-fiwv", "-fimed", "1000000000000"), ()
    => new FFTChain[FixedPoint](params)) {
//    dsptools.Driver.execute(() => new ACtrl(params), TestSetup.dspTesterOptions) {
      c => new FFTChainTester(c, trials)
    }
  }
}




