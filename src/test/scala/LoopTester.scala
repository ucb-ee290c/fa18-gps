package gps

import chisel3._
import dsptools.DspTester

import scala.collection.mutable.ListBuffer
import org.scalatest.{FlatSpec, Matchers}

class LoopFilterTester(c: LoopFilter, input: Seq[SInt], output: Seq[SInt]) extends DspTester(c) {
  for ((in, out) <- input zip output) {
    poke(c.io.in, in)
    step(1)
    expect(c.io.out, out)
  }
}
object LoopFilterTester {
  def apply(w: Int, dcGain: Double, bandwidth: Double, sampleRate: Double, input:
  Seq[SInt], output: Seq[SInt]): Boolean = {
    chisel3.iotesters.Driver.execute(Array("-tbn", "firrtl", "-fiwv"), 
      () => new LoopFilter(w, dcGain, bandwidth, sampleRate)) {
      c => new LoopFilterTester(c, input, output)
    }
  }
}

object calcFilterOutput {
  def apply(input: Seq[Int], a: Int, b: Int): Seq[Int] = {
    var xPrev : Int = 0
    var yPrev : Int = 0
    var output = new ListBuffer[Int]()
    for (in <- input) {
      var out = a * (in + xPrev) - b * yPrev
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
    val w = 10
    val dcGain = 12.0
    val bandwidth = 10.0
    val sampleRate = 1000.0
    val (a, b) = GetLoopFilterCoeffs(dcGain, bandwidth, sampleRate)
    val impulse = 1 :: List.fill(9)(0) 
    val input = impulse.map(x => x.S)
    val output = calcFilterOutput(impulse, a, b).map(x => x.S)
    LoopFilterTester(w, dcGain, bandwidth, sampleRate, input, output) should be (true)
  }
}
