package gps

import dsptools.DspTester 
import breeze.numerics.{sin, cos, pow}

class NcoTester[T <: chisel3.Data](c: NCO[T], input: Seq[Int], outputCos: Seq[Double], outputSin: Seq[Double], sinOut: Boolean) extends DspTester(c) {

    for (i <- 0 until input.length) {
        poke(c.io.stepSize, input(i))
        peek(c.io.regOut)
        expect(c.io.cos, outputCos(i))
        if (sinOut) {
            expect(c.io.sin, outputSin(i))
        }
        step(1)
    }

}

object RealNcoTester {
    def apply(params: NcoParams[dsptools.numbers.DspReal], input: Seq[Int], outputCos: Seq[Double], outputSin: Seq[Double]): Boolean = { 
        chisel3.iotesters.Driver.execute(Array("-tbn", "verilator", "-fiwv"), () => new NCO(params)) {
        c => new NcoTester(c, input, outputCos, outputSin, params.sinOut)
        }   
    }
}
object FixedNcoTester {
    def apply(params: FixedNcoParams, input: Seq[Int], outputCos: Seq[Double], outputSin: Seq[Double]): Boolean = { 
        chisel3.iotesters.Driver.execute(Array("-tbn", "firrtl", "-fiwv"), () => new NCO(params)) {
        c => new NcoTester(c, input, outputCos, outputSin, params.sinOut)
        }   
    }
}

