package gps

import chisel3._

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
object ACtrlApp extends App {
  val usage = s"""Des arguments:
  |--nLoop <Int>\t\tNumber of loops
  |--nFreq <Int>\t\tNumber of frequency to sweep
  |--nSample <Int>\t\tNumber of samples
  |--nLane <Int>\t\tNumber of IFFT output lanes
  |--wCorrelation <Int>\t\tWidth of Correlation
  |--wLoop <Int>\t\tWidth of maximum Loop
  |--wIdxFreq <Int>\t\tWidth of Frequency Index
  |--wFreq <Int>\t\tWidth of Frequency
  |--wCodePhase <Int>\t\tWidth of CodePhase
  |--wLane <Int>\t\tWidth of Lane
  |--wADC <Int>\t\tWidth of ADC
  |--wSate <Int>\t\tWidth of Satellite
  |--freqMin <Int>\t\tMinimum frequency
  |--freqStep <Int>\t\tFrequency step
  |""".stripMargin
  /**
   * Parse arguments
   *
   * Some arguments are used by the cordic generator and are used to construct a FixedCordicParams object.
   * The rest get returned as a List[String] to pass to the Chisel driver
   *
   */
  def argParse(args: List[String], params: IntACtrlParams): (List[String], IntACtrlParams) = {
    args match {
      case "--help" :: tail =>
        println(usage)
        val (newArgs, newParams) = argParse(tail, params)
        ("--help" +: newArgs, newParams)
      case "--nLoop" :: Int(nLoop) :: tail => argParse(tail, params.copy(nLoop = nLoop))
      case "--nFreq" :: Int(nFreq) :: tail => argParse(tail, params.copy(nFreq = nFreq))
      case "--nSample" :: Int(nSample) :: tail => argParse(tail, params.copy(nSample = nSample))
      case "--nLane" :: Int(nLane) :: tail => argParse(tail, params.copy(nLane = nLane))
      case "--wCorrelation" :: Int(wCorrelation) :: tail => argParse(tail, params.copy(wCorrelation = wCorrelation))
      case "--wLoop" :: Int(wLoop) :: tail => argParse(tail, params.copy(wLoop = wLoop))
      case "--wIdxFreq" :: Int(wIdxFreq) :: tail => argParse(tail, params.copy(wIdxFreq = wIdxFreq))
      case "--wFreq" :: Int(wFreq) :: tail => argParse(tail, params.copy(wFreq = wFreq))
      case "--wCodePhase" :: Int(wCodePhase) :: tail => argParse(tail, params.copy(wCodePhase = wCodePhase))
      case "--wLane" :: Int(wLane) :: tail => argParse(tail, params.copy(wLane = wLane))
      case "--wADC" :: Int(wADC) :: tail => argParse(tail, params.copy(wADC = wADC))
      case "--wSate" :: Int(wSate) :: tail => argParse(tail, params.copy(wSate = wSate))
      case "--freqMin" :: Int(freqMin) :: tail => argParse(tail, params.copy(freqMin = freqMin))
      case "--freqStep" :: Int(freqStep) :: tail => argParse(tail, params.copy(freqStep = freqStep))
      case chiselOpt :: tail => {
        val (newArgs, newParams) = argParse(tail, params)
        (chiselOpt +: newArgs, newParams)
      }
      case Nil => (args, params)
    }
  }
  val defaultParams = IntACtrlParams(
    nLoop = 3,
    nFreq = 3,
    nSample = 3,
    nLane = 1,
    wCorrelation = 10,
    wLoop = 5,
    wIdxFreq = 5,
    wFreq = 20,
    wCodePhase = 5,
    wLane = 64,
    wADC = 10,
    wSate = 5,
    freqMin = 1000,
    freqStep = 9,
  )
  val (chiselArgs, params) = argParse(args.toList, defaultParams)
  // Run the Chisel driver to generate a cordic
  Driver.execute(chiselArgs.toArray, () => new ACtrl(params))
}
