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
  |--width <Int>\t\tWidth of Input
  |--nSample <Int>\t\tnumber of samples
  |--nLane <Int>\t\tnumber of lanes
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
      case "--width" :: Int(width) :: tail => argParse(tail, params.copy(width = width))
      case "--nSample" :: Int(nSample) :: tail => argParse(tail, params.copy(nSample = nSample))
      case "--nLane" :: Int(nLane) :: tail => argParse(tail, params.copy(nLane = nLane))
      case chiselOpt :: tail => {
        val (newArgs, newParams) = argParse(tail, params)
        (chiselOpt +: newArgs, newParams)
      }
      case Nil => (args, params)
    }
  }
  val defaultParams = SIntDesParams(
    width = 5,
    nSample = 16,
    nLane = 4,
  )
  val (chiselArgs, params) = argParse(args.toList, defaultParams)
  // Run the Chisel driver to generate a cordic
  Driver.execute(chiselArgs.toArray, () => new Des(params))
}
