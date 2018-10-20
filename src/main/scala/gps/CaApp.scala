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
object CaApp extends App {
  val usage = s"""CA:
  |--fcoW <Int>\t\tWidth of input from NCO
  |--codeW <Int>\t\tWidth of code output (should probably be 1)
  |""".stripMargin
  /**
   * Parse arguments
   *
   * Some arguments are used by the cordic generator and are used to construct a FixedCordicParams object.
   * The rest get returned as a List[String] to pass to the Chisel driver
   *
   */
  def argParse(args: List[String], params: CAParams): (List[String], CAParams) = {
    args match {
      case "--help" :: tail =>
        println(usage)
        val (newArgs, newParams) = argParse(tail, params)
        ("--help" +: newArgs, newParams)
      case "--fcoW" :: Int(fcoW) :: tail => argParse(tail, params.copy(fcoWidth = fcoW))
      case "--codeW" :: Int(codeW) :: tail => argParse(tail, params.copy(codeWidth = codeW))
      case chiselOpt :: tail => {
        val (newArgs, newParams) = argParse(tail, params)
        (chiselOpt +: newArgs, newParams)
      }
      case Nil => (args, params)
    }
  }
  val defaultParams = CAParams(
    fcoWidth = 10,
    codeWidth = 1
  )
  val (chiselArgs, params) = argParse(args.toList, defaultParams)
  // Run the Chisel driver to generate a cordic
  Driver.execute(chiselArgs.toArray, () => new CA(params))
}
