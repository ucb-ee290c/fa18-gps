package gps

import dsptools.DspTester
import dsptools.numbers._
import org.scalatest.{FlatSpec, Matchers}
import chisel3._
import chisel3.experimental.FixedPoint
import java.nio.file.{Files, Paths}
import scala.math._
import breeze.plot._
import breeze.linalg._

class AcqParallelSpec extends FlatSpec with Matchers {
  behavior of "Acquisition with parallel search"

  val nHalfFreq = 1
  val freqStep = 500
  val fsample = 16367600
  val fcarrier = 4127190
  val fchip = 1023000
  val nChipSample = 16368
  val CPStep = 8
  val CPMin = 0
  val nChipCycle = ((nChipSample - CPMin - 1) / CPStep).toInt + 1
//  val nCPSample = 40

  val params = EgAcqParallelParams(
    widthADC = 4,
    widthCA = 2,  // 4
    widthNCOTrunct = 2, // 4
    widthNCORes = 32,
    nChipSample = nChipSample,
    nLoop = 1,
    nFreq = 2 * nHalfFreq + 1,
    nChipCycle = nChipCycle,
    chipMin = CPMin,
    chipStep = CPStep,
    freqMin = fcarrier - nHalfFreq * freqStep,
    freqStep = freqStep,
    freqSample = fsample,
    freqChip = fchip,
  )
  it should "AcqParallel" in {
    val baseTrial = AcqParallelTestVec(idx_sate=0)
    val idx_sate = Seq(3)
    val trials = idx_sate.map { idx_sate => baseTrial.copy(idx_sate = idx_sate) }
    AcqParallelTester(params, trials) should be (true)
  }
}



/**
 * Case class holding information needed to run an individual test
 */
case class AcqParallelTestVec(
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
class AcqParallelTester[T1 <: chisel3.Data, T2 <: chisel3.Data](c: AcqParallel[T1,T2], trials: Seq[AcqParallelTestVec], tolLSBs: Int = 1)
  extends DspTester(c) {

  val byteArray = Files.readAllBytes(Paths.get("python/data/gioveAandB_short.bin"))
  val corrMatrix = Array.ofDim[Double](c.params.nFreq, c.params.nChipCycle)
  var idxFreq = 0

  for (trial <- trials) {

    poke(c.io.in.valid, 0)
    poke(c.io.in.idxSV, trial.idx_sate)
    poke(c.io.in.debugCA, 0)
    poke(c.io.in.debugNCO, 0)
    poke(c.io.out.ready, 0)

    // wait until input is accepted
    var cycles = 0

    print("trial")
    updatableDspVerbose.withValue(false) {
      while (cycles < 300) {    // 35000
        if (cycles == 1) {
          poke(c.io.in.valid, 1)
        }
        else {
          poke(c.io.in.valid, 0)
        }

        val data_ADC = math.cos((2 * Pi) * (cycles) / 32) * 4
        val data_CA_pre = math.cos((2 * Pi) * (cycles - 10) / 32) * 4
        var data_CA = 0.0
        if (data_CA_pre > 0.0) {
          data_CA = 1.0
        }
        else {
          data_CA = -1.0
        }
        val data_cos = 1.0
        val data_sin = 0.0
        val data_ADC_real = byteArray(cycles).toInt

        poke(c.io.in.ADC, data_ADC_real)
        poke(c.io.in.CA, data_CA)
        poke(c.io.in.cos, data_cos)
        poke(c.io.in.sin, data_sin)

        if (peek(c.io.out.valid)) {
          peek(c.io.out.freqOpt)
          peek(c.io.out.chipOpt)
          peek(c.io.out.chipOptChk)
          peek(c.io.out.svFound)
          peek(c.io.out.max)
          peek(c.io.out.sum)
        }

        if (peek(c.io.out.acqed)) {
          if (idxFreq < c.params.nFreq) {
            val corrArr = peek(c.io.out.corrArr).toArray
            corrMatrix(idxFreq) = corrArr.map(x => x.toDouble)
          }
          idxFreq += 1
        }
        cycles += 1
        step(1)
      }
    }
    peek(c.io.out.freqOpt)
    peek(c.io.out.chipOpt)
    peek(c.io.out.chipOptChk)
    peek(c.io.out.svFound)
    peek(c.io.out.max)
    peek(c.io.out.sum)

//    val f = Figure()
//    val p = f.subplot(0)
//    val x = linspace(0, c.params.nChipCycle-1, c.params.nChipCycle)
//    p += plot(x, corrMatrix(0))
//    p.xlabel = "x axis"
//    p.ylabel = "y axis"
//    f.saveas("lines.png") // save current figure as a .png, eps and pdf also supported
    import java.io._
    val pw = new PrintWriter(new File("matlab/corrArr.txt"))
    for (i <- 0 to c.params.nFreq-1) {
      corrMatrix(i).foreach(x => pw.write(x.toString + "\n"))
    }
    pw.close()
  }
}

/**
  * Convenience function for running tests
  */
object AcqParallelTester {
  def apply(params: AcqParallelParams[SInt, FixedPoint], trials: Seq[AcqParallelTestVec]): Boolean = {
    chisel3.iotesters.Driver.execute(Array("-tbn", "treadle", "-fiwv", "-fimed", "10000"), ()
    => new AcqParallel[SInt, FixedPoint](params)) {
//    dsptools.Driver.execute(() => new ACtrl(params), TestSetup.dspTesterOptions) {
      c => new AcqParallelTester(c, trials)
    }
  }
}




