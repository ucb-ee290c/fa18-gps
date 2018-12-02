//package gps
//
//import dsptools.DspTester
//import dsptools.numbers._
//import org.scalatest.{FlatSpec, Matchers}
//
//
//
//class DesSpec extends FlatSpec with Matchers {
//  behavior of "Des"
//
//  val params = SIntDesParams(
//    width = 8,
//    nSample = 16,
//    nLane = 4,
//  )
//  it should "des" in {
//    val baseTrial = DES(offset=0)
//    val offset = Seq(0)
//    val trials = offset.map { offset => baseTrial.copy(offset = offset) }
//    DesTester(params, trials) should be (true)
//  }
//
//
//}
//
//
//
///**
// * Case class holding information needed to run an individual test
// */
//case class DES(
//  // input x, y and z
////  in: Int,
//  // mode
//  offset: Int,
//  // optional outputs
//  // if None, then don't check the result
//  // if Some(...), check that the result matches
//  out: Option[Int] = None,
//)
//
///**
// * DspTester for FixedIterativeCordic
// *
// * Run each trial in @trials
// */
//class DesTester[T <: chisel3.Data](c: Des[T], trials: Seq[DES], tolLSBs: Int = 1) extends DspTester(c) {
//
//
//  for (trial <- trials) {
//
//    poke(c.io.offset, trial.offset)
//    poke(c.io.ready, 1)
//    poke(c.io.newreq, 0)
//
//    // wait until input is accepted
//    var cycles = 0
//
//    print("trial")
//    while (cycles < 60) {
////      poke(c.io.ready, true)
//      cycles += 1
//      if (cycles >= 20 && cycles < 30) {poke(c.io.ready, 0)}
//      if (cycles == 20) {poke(c.io.newreq, 1)} else {poke(c.io.newreq, 0)}
//      if (cycles >= 30) {poke(c.io.ready, 1)}
//      poke(c.io.in, (cycles%17))
//      peek(c.io.valid)
//
//      peek(c.io.out)
//      peek(c.io.valid)
//      peek(c.io.state)
//      peek(c.io.cnt_buffer)
////      if (cycles%5 == 0) {peek(c.io.out)}
//
//      step(1)
//    }
//    // wait until output is valid
////    cycles = 0
//
//
//  }
//}
//
///**
// * Convenience function for running tests
// */
//object DesTester {
//  def apply(params: SIntDesParams, trials: Seq[DES]): Boolean = {
//    chisel3.iotesters.Driver.execute(Array("-tbn", "verilator", "-fiwv", "-fimed", "10000000"), () => new Des(params)) {
////    dsptools.Driver.execute(() => new Des(params), TestSetup.dspTesterOptionsVerilog) {
//      c => new DesTester(c, trials)
//    }
//  }
//}
//
//
