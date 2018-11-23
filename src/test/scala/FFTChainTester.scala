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
    width = 32,
    bp = 16,
    nSample = 64,
    nLane = 64,
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
    for (i <- 0 until 16) {

      poke(c.io.in.CA(i), 1.0)
      poke(c.io.in.ADC(i), 1)
      poke(c.io.in.cos(i), 1)
      poke(c.io.in.sin(i), 0)
    }

    // wait until input is accepted
    var cycles = 0


    print("trial")
    while (cycles < 100) {

      if (cycles == 11) {
        val offset = (cycles - 11) * 16


        for (i <- 0 until 64) {

          poke(c.io.in.ADC(i), (math.cos((offset + i) * (2 * 3.1415927 / 32)) * 1).toDouble)
//          poke(c.io.in.ADC(i), 1.0)

          poke(c.io.in.cos(i), (math.cos((offset + i) * (2 * 3.1415927 / 32)) * 1).toDouble)
//          poke(c.io.in.sin(i), (math.sin((offset + i) * (2 * 3.1415927 / 32)) * 1).toDouble)
          poke(c.io.in.sin(i), 0.0)
          poke(c.io.in.CA(i), 1.0)

        }
        poke(c.io.in.valid, 1)

//        if (cycles == 14) {
//          poke(c.io.in.sync, 1)
//        }
//        else {
//          poke(c.io.in.sync, 0)
//        }
      }
      else if (cycles == 41) {

        val offset = (cycles - 41) * 16

        for (i <- 0 until 64) {

//          poke(c.io.in.ADC(i), (math.cos((offset + i) * (2 * 3.1415927 / 32)) * 8).toInt)
//          poke(c.io.in.cos(i), (math.cos((offset + i) * (2 * 3.1415927 / 32 * 1.1)) * 8).toDouble)
//          poke(c.io.in.sin(i), (math.sin((offset + i) * (2 * 3.1415927 / 32 * 1.1)) * 8).toDouble)
          poke(c.io.in.ADC(i), 1.0)
          poke(c.io.in.cos(i), 1.0)
          poke(c.io.in.sin(i), 0.0)
          poke(c.io.in.CA(i), 1.0)

        }
        poke(c.io.in.valid, 1)

//        if (cycles == 14) {
//          poke(c.io.in.sync, 1)
//        }
//        else {
//          poke(c.io.in.sync, 0)
//        }
      }
      else if (cycles == 71) {

        val offset = (cycles - 71) * 16

        for (i <- 0 until 16) {

          poke(c.io.in.ADC(i), (math.cos((offset + i) * (2 * 3.1415927 / 32)) * 8).toInt)
          poke(c.io.in.cos(i), (math.cos((offset + i) * (2 * 3.1415927 / 32 * 0.9)) * 8).toDouble)
          poke(c.io.in.sin(i), (math.sin((offset + i) * (2 * 3.1415927 / 32 * 0.9)) * 8).toDouble)
          poke(c.io.in.CA(i), 1.0)

        }
        poke(c.io.in.valid, 1)

        //        if (cycles == 14) {
        //          poke(c.io.in.sync, 1)
        //        }
        //        else {
        //          poke(c.io.in.sync, 0)
        //        }
      }
      else {

        poke(c.io.in.valid, 0)
        poke(c.io.in.sync, 0)
      }


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




