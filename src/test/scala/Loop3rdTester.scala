package gps

import chisel3._
import dsptools.DspTester

import scala.collection.mutable.ListBuffer
import org.scalatest.{FlatSpec, Matchers}

class LoopFilter3rdTester[T <: chisel3.Data](c: LoopFilter3rd[T], freqErr: Seq[Double], phaseErr: Seq[Double],
                                             intTime: Double, output: Seq[Double]) extends DspTester(c) {
  for ((ferr, (perr, out)) <- freqErr.zip(phaseErr.zip(output))) {
    fixTolLSBs.withValue(4) {
      poke(c.io.freqErr, ferr)
      poke(c.io.phaseErr, perr)
      poke(c.io.intTime, intTime)
      poke(c.io.valid, 0)
      step(1)
      poke(c.io.valid, 1)
      expect(c.io.out, out)
      step(1)
    }
  }
}
object LoopFilter3rdTester {
  def apply(params: FixedFilter3rdParams, freqErr: Seq[Double], phaseErr: Seq[Double], intTime: Double,
            output: Seq[Double]): Boolean = {
    chisel3.iotesters.Driver.execute(Array("-tbn", "firrtl", "-fiwv"),
      () => new LoopFilter3rd(params)) {
      c => new LoopFilter3rdTester(c, freqErr, phaseErr, intTime, output)
    }
  }
}

object calcFilter3rdOutput {
  def apply(freqErr: Seq[Double], phaseErr: Seq[Double], intTime: Double, w0f: Double, w0p: Double): Seq[Double] = {
    var alpha : Double = 0.0
    var beta : Double = 0.0
    val a2: Double = 1.414
    val a3: Double = 1.1
    val b3: Double = 2.4
    var output = new ListBuffer[Double]()
    for ((ferr, perr) <- freqErr.zip(phaseErr)) {
      val betaWire = (w0f*w0f*ferr + w0p*w0p*w0p*perr) * intTime + beta
      val alphaWire = (a2*w0f*ferr + a3*w0p*w0p*perr + (betaWire + beta)*0.5) * intTime + alpha
      val out = b3*w0p*perr + (alphaWire + alpha) * 0.5
      beta = betaWire
      alpha = alphaWire
      output += out
    }
    output.toList
  }
}

class LoopFilter3rdSpec extends FlatSpec with Matchers {
  behavior of "LoopFilter3rd"

  it should "Filter" in {
    val fBandwidth = 3.0
    val pBandwidth = 17.0
    val width = 24
    val BPWidth = 16
    val params = new FixedFilter3rdParams(fBandwidth=fBandwidth, pBandwidth=pBandwidth, width=width, BPWidth=BPWidth)
    val (w0f, w0p) = GetLoopFilter3rdW0s(params)
    println(w0f)
    println(w0p)
    val freqErr = 1.0 :: List.fill(9)(0.0)
    val phaseErr = 1.0 :: List.fill(9)(0.0)
    val intTime = 0.001
    val output = calcFilter3rdOutput(freqErr, phaseErr, intTime, w0f, w0p)
    LoopFilter3rdTester(params, freqErr, phaseErr, intTime, output) should be (true)
  }
}
