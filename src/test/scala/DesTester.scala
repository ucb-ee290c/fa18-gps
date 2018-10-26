package gps

import dsptools.DspTester
import dsptools.numbers._
import org.scalatest.{FlatSpec, Matchers}



class DesSpec extends FlatSpec with Matchers {
  behavior of "Des"

  val params = SIntDesParams(
    wADC = 4,
    nSample = 5,
  )
  it should "des" in {
    val baseTrial = DES(offset=0)
    val offset = Seq(2)
    val trials = offset.map { offset => baseTrial.copy(offset = offset) }
    DesTester(params, trials) should be (true)
  }


}



/**
 * Case class holding information needed to run an individual test
 */
case class DES(
  // input x, y and z
//  in: Int,
  // mode
  offset: Int,
  // optional outputs
  // if None, then don't check the result
  // if Some(...), check that the result matches
  out: Option[Int] = None,
)

/**
 * DspTester for FixedIterativeCordic
 *
 * Run each trial in @trials
 */
class DesTester[T <: chisel3.Data](c: Des[T], trials: Seq[DES], tolLSBs: Int = 1) extends DspTester(c) {


  for (trial <- trials) {

    poke(c.io.offset, trial.offset)

    // wait until input is accepted
    var cycles = 0

    print("trial")
    while (cycles < 30) {
      poke(c.io.ready, true)
      cycles += 1
      poke(c.io.in, (cycles%6)-3)
      peek(c.io.valid)
      if (cycles%5 == 0) {peek(c.io.out)}

      step(1)
    }
    // wait until output is valid
//    cycles = 0


  }
}

/**
 * Convenience function for running tests
 */
object DesTester {
  def apply(params: SIntDesParams, trials: Seq[DES]): Boolean = {
    chisel3.iotesters.Driver.execute(Array("-tbn", "verilator", "-fiwv"), () => new Des(params)) {
      c => new DesTester(c, trials)
    }
  }
}

