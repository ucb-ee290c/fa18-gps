package gps

import dsptools.numbers._
import org.scalatest.{FlatSpec, Matchers}
import breeze.numerics.{cos, sin, pow, round}
import breeze.numerics.constants._

class NcoSpec extends FlatSpec with Matchers {
  behavior of "FixedNCO"

  val params = FixedNcoParams( 
    width = 3,
    sinOut = true
  )

  it should "produce SInt cosines and sines" in {
    val input = Seq(1, 2, 3, 4, 5, 6, 7, 7, 1, 2, 3, 4, 5, 4, 5, 6, 7, 0, 2)
    val outputCos = (input.scanLeft(0)(_ + _)).map(x => cos(2.0*Pi*x/pow(2, params.width)))
    val outputSin = (input.scanLeft(0)(_ + _)).map(x => sin(2.0*Pi*x/pow(2, params.width)))
    FixedNcoTester(params, input, outputCos, outputSin) should be (true)
  }
  behavior of "DspRealNCO"

  val realParams = new NcoParams[DspReal]{ 
    val proto = DspReal()
    val width = 3
    val sinOut = true
  }

  it should "produce DspReal cosines and sines" in {
    val realInput = Seq(1, 2, 3, 4, 5, 6, 7, 7, 1, 2, 3, 4, 5, 4, 5, 6, 7, 0, 2)
    val realOutputCos = (realInput.scanLeft(0)(_ + _)).map(x => cos(2.0*Pi*x/pow(2, params.width)))
    val realOutputSin = (realInput.scanLeft(0)(_ + _)).map(x => sin(2.0*Pi*x/pow(2, params.width)))
    RealNcoTester(realParams, realInput, realOutputCos, realOutputSin) should be (true)
  }

}
