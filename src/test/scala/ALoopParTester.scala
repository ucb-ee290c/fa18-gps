package gps

import dsptools.DspTester
import dsptools.numbers._
import org.scalatest.{FlatSpec, Matchers}
import chisel3._
import chisel3.experimental.FixedPoint
import java.nio.ByteBuffer
import java.nio.file.{Files, Paths}
import scala.math._


class ALoopParSpec extends FlatSpec with Matchers {
  behavior of "ALoopPar"

  val nHalfFreq = 4
  val freqStep = 100
  val fsample = 16367600

  val params = EgALoopParParams(
    wADC = 4,
    wCA = 4,
    wNCOTct = 4,
    wNCORes = 32,
    nSample = 16368,
    nLoop = 2,
    nFreq = 2 * nHalfFreq + 1,
    CPStep = 8,
    freqStep = freqStep,
    freqMin = 2045950 - nHalfFreq * freqStep,
  )
  it should "ALoop" in {
    val baseTrial = ALoopParTestVec(idx_sate=0)
    val idx_sate = Seq(0)
    val trials = idx_sate.map { idx_sate => baseTrial.copy(idx_sate = idx_sate) }
    ALoopParTester(params, trials) should be (true)
  }


}



/**
 * Case class holding information needed to run an individual test
 */
case class ALoopParTestVec(
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
class ALoopParTester[T1 <: chisel3.Data, T2 <: chisel3.Data](c: ALoopPar[T1,T2], trials: Seq[ALoopParTestVec], tolLSBs: Int = 1)
  extends DspTester(c) {




//  val byteArray = Files.readAllBytes(Paths.get("python/data/gioveAandB_short.bin"))

  for (trial <- trials) {


    poke(c.io.in.valid, 0)
    poke(c.io.in.idx_sate, trial.idx_sate)
    poke(c.io.out.ready, 0)
    poke(c.io.in.debugCA, 1)
    poke(c.io.in.debugNCO, 1)

    // wait until input is accepted
    var cycles = 0


    print("trial")
    while (cycles < 10000) {

      if (cycles == 1) {poke(c.io.in.valid, 1)}
      else {poke(c.io.in.valid, 0)}


      val data_ADC = math.cos((2 * Pi) * (cycles) / 32) * 4
      val data_CA_pre = math.cos((2 * Pi) * (cycles-10) / 32) * 4
      var data_CA = 0.0
      if (data_CA_pre > 0.0) {
        data_CA = 1.0
      }
      else {
        data_CA = -1.0
      }
//      val data_CA = 1.0
      val data_cos = 1.0
      val data_sin = 0.0


      poke(c.io.in.ADC, data_ADC)
      poke(c.io.in.CA, data_CA)
      poke(c.io.in.cos, data_cos)
      poke(c.io.in.sin, data_sin)

      if (peek(c.io.out.valid)) {
        peek(c.io.out.freqOpt)
        peek(c.io.out.CPOpt)
      }

      cycles += 1
      step(1)


    }


  }
}



/**
  * Convenience function for running tests
  */
object ALoopParTester {
  def apply(params: ALoopParParams[SInt, FixedPoint], trials: Seq[ALoopParTestVec]): Boolean = {
    chisel3.iotesters.Driver.execute(Array("-tbn", "verilator", "-fiwv", "-fimed", "1000000000000"), ()
    => new ALoopPar[SInt, FixedPoint](params)) {
//    dsptools.Driver.execute(() => new ACtrl(params), TestSetup.dspTesterOptions) {
      c => new ALoopParTester(c, trials)
    }
  }
}




