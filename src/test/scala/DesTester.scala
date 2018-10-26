//package gps
//
//import dsptools.DspTester
//
///**
// * Case class holding information needed to run an individual test
// */
//case class XYZ(
//  // input x, y and z
//  in: Int,
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
//class DesTester[T <: chisel3.Data](c: Des[T], trials: Seq[XYZ], tolLSBs: Int = 1) extends DspTester(c) {
//
//
//  for (trial <- trials) {
//
//    poke(c.io.offset, trial.offset)
//
//    // wait until input is accepted
//    var cycles = 0
//    while (!peek(c.io.in.ready) && cycles < 100) {
//      cycles += 1
//      poke(c.io.in, (cycles)%17)
//      peek(c.io.out)
//
//      step(1)
//    }
//    // wait until output is valid
//    cycles = 0
//
//
//  }
//}
//
///**
// * Convenience function for running tests
// */
//object DesTester {
//  def apply(params: DesParams, trials: Seq[XYZ]): Boolean = {
//    chisel3.iotesters.Driver.execute(Array("-tbn", "verilator", "-fiwv"), () => new Des(params)) {
//      c => new DesTester(c, trials)
//    }
//  }
//}
//
