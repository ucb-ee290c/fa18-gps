package gps

import chisel3._

/**
  * Make an unapply function for the argument parser.
  * It allows us to match on parameters that are integers
  */
object Int {
  def unapply(v: String): Option[Int] = {
    try {
      Some(v.toInt)
    } catch {
      case _: NumberFormatException => None
    }
  }
}


/**
 * Define entry point for CORDIC generator
 */
object DesApp extends App {
  val usage = s"""Des arguments:
  |--wADC <Int>\t\tWidth of ADC
  |--nSample <Int>\t\tnumber of samples
  |""".stripMargin
  /**
   * Parse arguments
   *
   * Some arguments are used by the cordic generator and are used to construct a FixedCordicParams object.
   * The rest get returned as a List[String] to pass to the Chisel driver
   *
   */
  def argParse(args: List[String], params: SIntDesParams): (List[String], SIntDesParams) = {
    args match {
      case "--help" :: tail =>
        println(usage)
        val (newArgs, newParams) = argParse(tail, params)
        ("--help" +: newArgs, newParams)
      case "--wADC" :: Int(wADC) :: tail => argParse(tail, params.copy(wADC = wADC))
      case "--nSample" :: Int(nSample) :: tail => argParse(tail, params.copy(nSample = nSample))
      case chiselOpt :: tail => {
        val (newArgs, newParams) = argParse(tail, params)
        (chiselOpt +: newArgs, newParams)
      }
      case Nil => (args, params)
    }
  }
  val defaultParams = SIntDesParams(
    wADC = 3,
    nSample = 16,
  )
  val (chiselArgs, params) = argParse(args.toList, defaultParams)
  // Run the Chisel driver to generate a cordic
  Driver.execute(chiselArgs.toArray, () => new Des(params))
}
