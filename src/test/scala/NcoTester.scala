package gps

import dsptools.numbers._
import dsptools.DspTester 
import org.scalatest.{FlatSpec, Matchers}
import breeze.numerics.{cos, sin, pow}
import breeze.numerics.constants._

/* 
 * DspSpec for NCO
 */
class NcoSpec extends FlatSpec with Matchers {
  behavior of "SIntNCO"

  val params = SIntNcoParams( 
    truncateWidth = 3,
    resolutionWidth = 5,
    sinOut = true
  )

  val paramsNoSin = SIntNcoParams( 
    truncateWidth = 3,
    resolutionWidth = 5,
    sinOut = false
  )

  it should "produce SInt random cosines and sines" in {
    val input = Seq(1, 2, 3, 4, 5, 6, 7, 7, 1, 2, 3, 4, 5, 4, 5, 6, 7, 0, 2)
    val outputCos = (input.scanLeft(0)(_ + _)).map(x => cos(2.0*Pi*x/pow(2, params.truncateWidth)))
    val outputSin = (input.scanLeft(0)(_ + _)).map(x => sin(2.0*Pi*x/pow(2, params.truncateWidth)))
    SIntNcoTester(params, input, outputCos, outputSin) should be (true)
  }

    val inputPer = Seq.fill(320){1}
    val outputPer = Seq.fill(320){1.0}
    val outputPerCos = (inputPer.scanLeft(0)(_ + _)).map(x => cos(2.0*Pi*x/pow(2, params.truncateWidth)))
    val outputPerSin = (inputPer.scanLeft(0)(_ + _)).map(x => sin(2.0*Pi*x/pow(2, params.truncateWidth)))
/*  it should "produce SInt periodic cosines and sines" in {
    SIntNcoTester(params, inputPer, outputPerCos, outputPerSin) should be (true)
  }*/
  it should "produce SInt periodic cosines only" in {
    SIntNcoTester(paramsNoSin, inputPer, outputPer, outputPerSin) should be (true)
  }


  behavior of "DspRealNCO"

  val realParams = new NcoParams[DspReal]{ 
    val proto = DspReal()
    val truncateWidth = 3
    val resolutionWidth = 32
    val sinOut = true
  }

/*  it should "produce DspReal random cosines and sines" in {
    val realInput = Seq(1, 2, 3, 4, 5, 6, 7, 7, 1, 2, 3, 4, 5, 4, 5, 6, 7, 0, 2)
    val realOutputCos = (realInput.scanLeft(0)(_ + _)).map(x => cos(2.0*Pi*x/pow(2, params.truncateWidth)))
    val realOutputSin = (realInput.scanLeft(0)(_ + _)).map(x => sin(2.0*Pi*x/pow(2, params.truncateWidth)))
    RealNcoTester(realParams, realInput, realOutputCos, realOutputSin) should be (true)
  }*/

}

/*
 * DspTester for NCO
 */
class NcoTester[T <: chisel3.Data](c: NCO[T], input: Seq[Int], outputCos: Seq[Double], outputSin: Seq[Double], sinOut: Boolean) extends DspTester(c) {

    for (i <- 0 until input.length) {
        poke(c.io.stepSize, input(i))
        peek(c.io.truncateRegOut)
        expect(c.io.cos, outputCos(i))
        if (sinOut) {
            expect(c.io.sin, outputSin(i))
        } else {
            expect(c.io.sin, 0)
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
object SIntNcoTester {
    def apply(params: SIntNcoParams, input: Seq[Int], outputCos: Seq[Double], outputSin: Seq[Double]): Boolean = { 
        chisel3.iotesters.Driver.execute(Array("-tbn", "firrtl", "-fiwv"), () => new NCO(params)) {
        c => new NcoTester(c, input, outputCos, outputSin, params.sinOut)
        }   
    }
}

