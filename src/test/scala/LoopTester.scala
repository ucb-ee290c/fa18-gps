package gps

import chisel3._
import dsptools.DspTester

import scala.collection.mutable.ListBuffer
import org.scalatest.{FlatSpec, Matchers}

class LoopFilterTester[T <: chisel3.Data](c: LoopFilter[T], input: Seq[Double], output: Seq[Double]) extends DspTester(c) {
  for ((in, out) <- input zip output) {
    fixTolLSBs.withValue(4) {
      poke(c.io.in, in)
      poke(c.io.valid, 0)
      step(1)
      poke(c.io.valid, 1)
      expect(c.io.out, out)
      step(1)
    }
  }
}
object LoopFilterTester {
  def apply(params: FixedFilterParams, input: Seq[Double], output: Seq[Double]): Boolean = {
    chisel3.iotesters.Driver.execute(Array("-tbn", "firrtl", "-fiwv"), 
      () => new LoopFilter(params)) {
      c => new LoopFilterTester(c, input, output)
    }
  }
}

object calcFilterOutput {
  def apply(input: Seq[Double], a: Double, b: Double): Seq[Double] = {
    var xPrev : Double = 0.0
    var yPrev : Double = 0.0
    var output = new ListBuffer[Double]()
    for (in <- input) {
      val out = a * (in + xPrev) - b * yPrev
      xPrev = in
      yPrev = out
      output += out
    }
    output.toList
  }
}

class LoopFilterSpec extends FlatSpec with Matchers {
  behavior of "LoopFilter"

  it should "Filter" in {
    val w = 20
    val dcGain = 12.0
    val bandwidth = 10.0
    val sampleRate = 1000.0
    val params = new FixedFilterParams(32, 12, dcGain, bandwidth, sampleRate)
    val (a, b) = GetLoopFilterCoeffs(params)
    println(a)
    println(b)
    val impulse = 1.0 :: List.fill(9)(0.0) 
    val output = calcFilterOutput(impulse, a, b)
    LoopFilterTester(params, impulse, output) should be (true)
  }
}
