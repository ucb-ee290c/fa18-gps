package gps

import chisel3._
import chisel3.util._
import scala.math._
import dsptools.numbers._
import dsptools.numbers.implicits._
import dsptools.numbers.DspComplex
import chisel3.experimental.FixedPoint
import freechips.rocketchip.amba.axi4stream._
import freechips.rocketchip.config.Parameters
import freechips.rocketchip.diplomacy._
import freechips.rocketchip.regmapper._
import freechips.rocketchip.tilelink._


/**
  * Make an unapply function for the argument parser.
  * It allows us to match on parameters that are integers
  */
//object Int {
//  def unapply(v: String): Option[Int] = {
//    try {
//      Some(v.toInt)
//    } catch {
//      case _: NumberFormatException => None
//    }
//  }
//}


/**
 * Define entry point for CORDIC generator
 */
object ALoopApp extends App {
  val usage = s"""ALoop arguments:
  |--wADC <Int>\t\tWidth of ADC
  |--wCA <Int>\t\tWidth of CA
  |--wNCOTct <Int>\t\tWidth of Truncated counter in NCO
  |--wNCORes <Int>\t\tWidth of Accurate counter in NCO
  |--wFFT <Int>\t\Total Width of FFT
  |--wFraction <Int>\t\tWidth of Fractional part of FFT
  |--nSample <Int>\t\tNumber of samples
  |--nLoop <Int>\t\tNumber of loops
  |--nFreq <Int>\t\tNumber of frequency to sweep
  |--nLane <Int>\t\tNumber of IFFT output lanes
  |--nStgFFT <Int>\t\tFFT's number of stages
  |--nStgIFFT <Int>\t\tIFFT's number of stages
  |--nStgFFTMul <Int>\t\tFFT Multiplier's number of stages
  |--freqMin <Int>\t\tMinimum frequency
  |--freqStep <Int>\t\tStep of frequency
  |""".stripMargin
  /**
   * Parse arguments
   *
   * Some arguments are used by the cordic generator and are used to construct a FixedCordicParams object.
   * The rest get returned as a List[String] to pass to the Chisel driver
   *
   */
  def argParse(args: List[String], params: EgALoopParams): (List[String], EgALoopParams) = {
    args match {
      case "--help" :: tail =>
        println(usage)
        val (newArgs, newParams) = argParse(tail, params)
        ("--help" +: newArgs, newParams)
      case "--wADC" :: Int(wADC) :: tail => argParse(tail, params.copy(wADC = wADC))
      case "--wCA" :: Int(wCA) :: tail => argParse(tail, params.copy(wCA = wCA))
      case "--wNCOTct" :: Int(wNCOTct) :: tail => argParse(tail, params.copy(wNCOTct = wNCOTct))
      case "--wNCORes" :: Int(wNCORes) :: tail => argParse(tail, params.copy(wNCORes = wNCORes))
      case "--wFFT" :: Int(wFFT) :: tail => argParse(tail, params.copy(wFFT = wFFT))
      case "--wFractionFFT" :: Int(wFractionFFT) :: tail => argParse(tail, params.copy(wFractionFFT = wFractionFFT))
      case "--nSample" :: Int(nSample) :: tail => argParse(tail, params.copy(nSample = nSample))
      case "--nLoop" :: Int(nLoop) :: tail => argParse(tail, params.copy(nLoop = nLoop))
      case "--nFreq" :: Int(nFreq) :: tail => argParse(tail, params.copy(nFreq = nFreq))
      case "--nLane" :: Int(nLane) :: tail => argParse(tail, params.copy(nLane = nLane))
      case "--nStgFFT" :: Int(nStgFFT) :: tail => argParse(tail, params.copy(nStgFFT = nStgFFT))
      case "--nStgIFFT" :: Int(nStgIFFT) :: tail => argParse(tail, params.copy(nStgIFFT = nStgIFFT))
      case "--nStgFFTMul" :: Int(nStgFFTMul) :: tail => argParse(tail, params.copy(nStgFFTMul = nStgFFTMul))
      case "--freqMin" :: Int(freqMin) :: tail => argParse(tail, params.copy(freqMin = freqMin))
      case "--freqStep" :: Int(freqStep) :: tail => argParse(tail, params.copy(freqStep = freqStep))
      case chiselOpt :: tail => {
        val (newArgs, newParams) = argParse(tail, params)
        (chiselOpt +: newArgs, newParams)
      }
      case Nil => (args, params)
    }
  }
  val defaultParams = EgALoopParams(
    wADC = 5,
    wCA = 3,
    wNCOTct = 5,
    wNCORes = 32,
    wFFT = 32,
    wFractionFFT = 16,
    nSample = 10,
    nLoop = 2,
    nFreq = 10,
    nLane = 1,
    nStgFFT = 4,
    nStgIFFT = 4,
    nStgFFTMul = 4,
    freqMin = 1000,
    freqStep = 9,
  )
  val (chiselArgs, params) = argParse(args.toList, defaultParams)
  // Run the Chisel driver to generate a cordic
  Driver.execute(chiselArgs.toArray, () => new ALoop[SInt, FixedPoint](params))
}
