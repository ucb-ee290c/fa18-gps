package gps

import chisel3._
import dsptools.DspTester
import dsptools.numbers._
import scala.collection.mutable.ListBuffer
import org.scalatest.{FlatSpec, Matchers, Ignore}

class LoopFilter3rdTester[T <: chisel3.Data](
  c: LoopFilter3rd[T], 
  freqErr: Seq[Double], 
  phaseErr: Seq[Double],
  intTime: Double, 
  output: Seq[Double]
) extends DspTester(c) {
  for ((ferr, (perr, out)) <- freqErr.zip(phaseErr.zip(output))) {
    fixTolLSBs.withValue(5) {
      poke(c.io.freqErr, ferr)
      poke(c.io.phaseErr, perr)
      poke(c.io.intTime, intTime)
      poke(c.io.valid, 1)
      peek(c.io.betaWire1)
      peek(c.io.betaWire2)
      expect(c.io.out, out)
      step(1)
    }
  }
}

object RealLoopFilter3rdTester {
  def apply(
    params: LoopFilter3rdParams[dsptools.numbers.DspReal], 
    freqErr: Seq[Double], 
    phaseErr: Seq[Double], 
    intTime: Double,
    output: Seq[Double]
  ): Boolean = {
    chisel3.iotesters.Driver.execute(Array("-tbn", "verilator", "-fiwv"),
      () => new LoopFilter3rd(params)) {
      c => new LoopFilter3rdTester(c, freqErr, phaseErr, intTime, output)
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
  def apply(
    freqErr: Seq[Double], 
    phaseErr: Seq[Double], 
    intTime: Double, 
    w0f: Double, 
    w0p: Double,
    fDcGain: Double=1,
    pDcGain: Double=1
  ): Seq[Double] = {
    var alpha : Double = 0.0
    var beta : Double = 0.0
    val a2: Double = 1.414
    val a3: Double = 1.1
    val b3: Double = 2.4
    var output = new ListBuffer[Double]()
    for ((ferr, perr) <- freqErr.zip(phaseErr)) {
      val betaWire = (w0f*w0f*ferr*fDcGain + w0p*w0p*w0p*perr*pDcGain) * intTime + beta
      val alphaWire = (a2*w0f*ferr*fDcGain + a3*w0p*w0p*perr*pDcGain + 
        (betaWire + beta)*0.5) * intTime + alpha
      val out = b3*w0p*perr*pDcGain + (alphaWire + alpha) * 0.5
      print(w0f*w0f*fDcGain*intTime*ferr)
      println()
      print(w0p*w0p*w0p*pDcGain*intTime*perr)
      println()
      beta = betaWire
      alpha = alphaWire
      output += out
    }
    println(output)
    output.toList
  }
}

class LoopFilter3rdSpec extends FlatSpec with Matchers {
  behavior of "FixedLoopFilter3rd"

  val fBandwidth = 3.0
  val pBandwidth = 17.0
  val width = 32
  val bPWidth = 20
  val params = new FixedFilter3rdParams(
    fBandwidth=fBandwidth, 
    pBandwidth=pBandwidth, 
    width=width, 
    bPWidth=bPWidth,
    fDCGain=1000)
  val (w0f, w0p) = GetLoopFilter3rdW0s(params)
  val freqErr = -3.14 :: 3.14 :: List.fill(9)(0.0)
  val phaseErr = 1.57 :: -1.57 :: List.fill(9)(0.0)
  val intTime = 0.001
  val output = calcFilter3rdOutput(freqErr, phaseErr, intTime, w0f, w0p, 1000)

  it should "Filter" in {
    LoopFilter3rdTester(params, freqErr, phaseErr, intTime, output) should be (true)
  }

  behavior of "RealLoopFilter3rd"

  it should "Filter" in {
    val realLfParams = new LoopFilter3rdParams[DspReal] {
      val proto = DspReal()
      val protoInt = DspReal()
      val fBandwidth = 3.0
      val pBandwidth = 17.0
      val fDCGain: Double = 1000
      val pDCGain: Double = 1
    }
    RealLoopFilter3rdTester(realLfParams, freqErr, phaseErr, intTime, output) should be (true)
  }  
}
