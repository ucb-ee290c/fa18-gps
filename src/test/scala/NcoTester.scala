package gps

import dsptools.DspTester 
import breeze.numerics.{sin, cos, pow}

class NcoTester[T <: chisel3.Data](c: NCO[T], input: Seq[Int], output: Seq[Double]) extends DspTester(c) {

    for (i <- 0 until input.length) {
        poke(c.io.stepSize, input(i))
        step(1)
        peek(c.io.regOut)
        expect(c.io.cos, output(i))
    }

}

object RealNcoTester {
    def apply(params: NcoParams[dsptools.numbers.DspReal], input: Seq[Int], output: Seq[Double]): Boolean = { 
        chisel3.iotesters.Driver.execute(Array("-tbn", "verilator", "-fiwv"), () => new NCO(params)) {
        c => new NcoTester(c, input, output)
        }   
    }
}
object FixedNcoTester {
    def apply(params: FixedNcoParams, input: Seq[Int], output: Seq[Double]): Boolean = { 
        chisel3.iotesters.Driver.execute(Array("-tbn", "firrtl", "-fiwv"), () => new NCO(params)) {
        c => new NcoTester(c, input, output)
        }   
    }
}

