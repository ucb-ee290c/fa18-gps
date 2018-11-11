package gps

import dsptools.DspTester
import dsptools.numbers._
import org.scalatest.{FlatSpec, Matchers}
import chisel3._
import chisel3.experimental.FixedPoint
import java.nio.ByteBuffer
import java.nio.file.{Files, Paths}
//import scala.math.BigInt


class ACtrlSpec extends FlatSpec with Matchers {
  behavior of "ACtrl"

  val params = IntACtrlParams(
    nLoop = 3,
    nFreq = 3,
    nSample = 3,
    nLane = 1,
    wCorrelation = 32,
    wLoop = 5,
    wIdxFreq = 6,
    wFreq = 32,
    wCodePhase = 16,
    wLane = 5,
    wADC = 10,
    wSate = 5,
    freqMin = 1000, //4130400 - 10000,
    freqStep = 9, //500,
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
class ACtrlTester[T <: chisel3.Data](c: ACtrl[T], trials: Seq[XYZ], tolLSBs: Int = 1)
  extends DspTester(c) {


  poke(c.io.Aout.ready, 1)
  poke(c.io.Tin.valid, 0)
  poke(c.io.Tout.ready, 0)

  val byteArray = Files.readAllBytes(Paths.get("python/data/acqctrl_test_vec.bin"))

  for (trial <- trials) {


    poke(c.io.Ain.ADC, trial.ADC)
    poke(c.io.Ain.CodePhase, trial.CodePhase)
    poke(c.io.Tin.idx_sate, trial.idx_sate)



    // wait until input is accepted
    var cycles = 0
    var fire = 0
    var key = 0
    var key1 = 0
    var key2 = 0
    var corr = 0.0
    var ifft_data = 0.0

    print("trial")
    while (cycles < 40) {

      poke(c.io.Tin.valid, cycles == 0)

      cycles += 1
      if (cycles == 2) {poke(c.io.Tin.valid, 0)}
      if (cycles > 2) {

        key1 = (fire / 9).toInt
        key2 = fire % 3
        key = key1 * 3 + key2

        print(key)

        if (key == 0) corr = 001.0;
        else if (key == 1) corr = 002.0;
        else if (key == 2) corr = 003.0;
        else if (key == 3) corr = 004.0;
        else if (key == 4) corr = 005.0;
        else if (key == 5) corr = 1006.0;
        else if (key == 6) corr = 007.0;
        else if (key == 7) corr = 008.0;
        else corr = 009.0


        var ifft_data_7, ifft_data_6, ifft_data_5, ifft_data_4,
            ifft_data_3, ifft_data_2, ifft_data_1, ifft_data_0: Byte = 0
        ifft_data_7 = byteArray(fire*8+7)
        ifft_data_6 = byteArray(fire*8+6)
        ifft_data_5 = byteArray(fire*8+5)
        ifft_data_4 = byteArray(fire*8+4)
        ifft_data_3 = byteArray(fire*8+3)
        ifft_data_2 = byteArray(fire*8+2)
        ifft_data_1 = byteArray(fire*8+1)
        ifft_data_0 = byteArray(fire*8+0)
        var ifft_data_final = Array(ifft_data_7, ifft_data_6, ifft_data_5, ifft_data_4, ifft_data_3, ifft_data_2, ifft_data_1, ifft_data_0)
        ifft_data = ByteBuffer.wrap(ifft_data_final).getDouble / 100000
//        poke(c.io.Ain.Correlation, ifft_data)

        poke(c.io.Ain.Correlation(0), corr)
        poke(c.io.Ain.valid, 1)

        peek(c.io.Ain.Correlation)
//        peek(c.io.Aout.freqNow)
//        peek(c.io.Aout.cpNow)



        fire = fire + 1
      }
      else {

        poke(c.io.Ain.valid, 0)
      }



      peek(c.io.Ain.ready)
      peek(c.io.Aout.valid)
      peek(c.io.Tin.ready)
      peek(c.io.Tout.valid)
      peek(c.io.Ain.Correlation)
      peek(c.io.Aout.freqNow)
      peek(c.io.Aout.freqNext)
      peek(c.io.Aout.cpNow)
      peek(c.io.Aout.cpNext)
      peek(c.io.Tout.freqOpt)
      peek(c.io.Tout.CPOpt)

//      peek(c.io.Tout.iFreqOptItm)
//      peek(c.io.Tout.iFreqOptOut)
//      peek(c.io.Tout.CPOptItm)
//      peek(c.io.Tout.max)
//      peek(c.io.Tout.vec)
//      peek(c.io.Tout.state)
//      peek(c.io.Tout.CPOptOut)
//      peek(c.io.Tout.sateFound)
//      peek(c.io.Reg.max)
//      peek(c.io.Reg.sum)

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
  def apply(params: ACtrlParams[FixedPoint], trials: Seq[XYZ]): Boolean = {
    chisel3.iotesters.Driver.execute(Array("-tbn", "verilator", "-fiwv", "-fimed", "1000000000000"), () => new ACtrl(params)) {
//    dsptools.Driver.execute(() => new ACtrl(params), TestSetup.dspTesterOptions) {
      c => new ACtrlTester(c, trials)
    }
  }
}




class ACtrlTester2[T <: chisel3.Data](c: ACtrl[T], trials: Seq[XYZ], tolLSBs: Int = 1)
  extends DspTester(c) {


  poke(c.io.Aout.ready, 1)
  poke(c.io.Tin.valid, 0)
  poke(c.io.Tout.ready, 0)

  val byteArray = Files.readAllBytes(Paths.get("python/data/acqctrl_test_vec.bin"))

  for (trial <- trials) {


    poke(c.io.Ain.ADC, trial.ADC)
    poke(c.io.Ain.CodePhase, trial.CodePhase)
    poke(c.io.Tin.idx_sate, trial.idx_sate)

    // wait until input is accepted
    var cycles = 0
    var fire = 0
    var key = 0
    var key1 = 0
    var key2 = 0
    var corr = 0.0
    var ifft_data = 0.0

    print("trial")
    while (cycles < 15) {

      poke(c.io.Tin.valid, cycles == 0)

      cycles += 1
      if (cycles == 2) {poke(c.io.Tin.valid, 0)}
      if (cycles > 2) {



        val key_freq = 2
        val key_CP = 2

        var coeff = 0.1
        if ((fire / 3).toInt == key_freq) coeff = 1.0
        else coeff = 0.1



        if (key == 0) corr = 1001.0;
        else if (key == 1) corr = 002.0;
        else corr = 009.0


        var corr0, corr1, corr2 = 0.0
        if (key_CP == 0) {
          corr0 = corr * coeff
          corr1 = 1.0 * coeff
          corr2 = 2.0 * coeff
        }
        else if (key_CP == 1) {
          corr0 = 2.0 * coeff
          corr1 = corr * coeff
          corr2 = 1.0 * coeff
        }
        else {
          corr0 = 1.0 * coeff
          corr1 = 2.0 * coeff
          corr2 = corr * coeff
        }

        poke(c.io.Ain.Correlation(0), corr0)
        poke(c.io.Ain.Correlation(1), corr1)
        poke(c.io.Ain.Correlation(2), corr2)
        poke(c.io.Ain.valid, 1)

        peek(c.io.Ain.Correlation)
        //        peek(c.io.Aout.freqNow)
        //        peek(c.io.Aout.cpNow)



        fire = fire + 1
      }
      else {

        poke(c.io.Ain.valid, 0)
      }



      peek(c.io.Ain.ready)
      peek(c.io.Aout.valid)
      peek(c.io.Tin.ready)
      peek(c.io.Tout.valid)
      peek(c.io.Ain.Correlation)
      peek(c.io.Aout.freqNow)
      peek(c.io.Aout.freqNext)
      peek(c.io.Aout.cpNow)
      peek(c.io.Aout.cpNext)
      peek(c.io.Tout.freqOpt)
      peek(c.io.Tout.CPOpt)

//      peek(c.io.Debug.iFreqNow)
//      peek(c.io.Debug.iLoopNow)
//      peek(c.io.Debug.iCPNow)
//      peek(c.io.Debug.max)
//      peek(c.io.Debug.reg_max)
//      peek(c.io.Debug.reg_tag_CP)

//      io.Debug.iFreqNow := reg_iFreqNow
//      io.Debug.iLoopNow := reg_iLoopNow
//      io.Debug.max := max_itm
//      io.Debug.reg_max := reg_max
//      io.Debug.reg_tag_CP := reg_tag_CP
//      peek(c.io.Tout.iFreqOptItm)
//      peek(c.io.Tout.iFreqOptOut)
//      peek(c.io.Tout.CPOptItm)
//      peek(c.io.Tout.CPOptOut)
//      peek(c.io.Tout.max)
//      peek(c.io.Tout.CPOpt_itm)
//      peek(c.io.Tout.vec)
      //      peek(c.io.Tout.state)
      //      peek(c.io.Tout.sateFound)
      //      peek(c.io.Reg.max)
      //      peek(c.io.Reg.sum)

      step(1)


    }
    // wait until output is valid
    //    cycles = 0


  }
}


/**
  * Convenience function for running tests
  */
object ACtrlTester2 {
  def apply(params: ACtrlParams[FixedPoint], trials: Seq[XYZ]): Boolean = {
    chisel3.iotesters.Driver.execute(Array("-tbn", "verilator", "-fiwv", "-fimed", "1000000000000"), () => new ACtrl(params)) {
      c => new ACtrlTester2(c, trials)
    }
  }
}

