package gps

import chisel3._
import dsptools.DspTester
import dsptools.numbers._

import scala.collection.mutable.ListBuffer
import org.scalatest.{FlatSpec, Matchers}

/*
 * Phase Discriminator Tester
 */ 
class PhaseDiscTester[T <: chisel3.Data](c: PhaseDiscriminator[T], input1: Seq[Double], input2: Seq[Double], output: Seq[Double], tolLSBs: Int = 10) extends DspTester(c) {
  poke(c.io.out.ready, 1)
  poke(c.io.in.valid, 1)

  var i = 0
  for (i <- 0 until input1.size) {
    poke(c.io.in.bits.ips, input1(i))
    poke(c.io.in.bits.qps, input2(i))

    while (!peek(c.io.in.ready)) {
      step(1)
    }

    while (!peek(c.io.out.valid)) {
      step(1)
    }

    fixTolLSBs.withValue(10) {
      expect(c.io.out.bits.output, output(i))
    }
  }   
}

object PhaseDiscTester {
  def apply(params: FixedDiscParams, input1: Seq[Double], input2: Seq[Double], output: Seq[Double]): Boolean = {
    chisel3.iotesters.Driver.execute(Array("-tbn", "firrtl", "-fiwv"), 
      () => new PhaseDiscriminator(params)) {
      c => new PhaseDiscTester(c, input1, input2, output)
    }
  } 
}

object RealPhaseDiscTester {
  def apply(params: DiscParams[dsptools.numbers.DspReal], input1: Seq[Double], input2: Seq[Double], output: Seq[Double]): Boolean = {
    chisel3.iotesters.Driver.execute(Array("-tbn", "verilator", "-fiwv"), 
      () => new PhaseDiscriminator(params)) {
      c => new PhaseDiscTester(c, input1, input2, output)
    }
  } 
}

class PhaseDiscSpec extends FlatSpec with Matchers {
  behavior of "DspReal Phase Discriminator"
  val realCordicParams = new CordicParams[DspReal] {
    val protoXY = DspReal()
    val protoZ = DspReal()
    val nStages = 30
    val correctGain = true
    val stagesPerCycle = 1
    val calAtan2 = false
    val dividing = false 
  }  

  val realParams = new DiscParams[DspReal] {
    val inWidth = 32 
    val outWidth = 32 
    val cordicParams = realCordicParams 
    val protoIn = DspReal()
    val protoOut = DspReal() 
  } 
  it should "atan" in {
    val in1 = Seq(1, 2, 3, 4, 5.0, 6.0, 7.0, 8.0, 9.0, 10.0)
    val in2 = Seq(442, 5, 43, 2, 543.0, 65, 23, 12, 90, 22)
    val out = in1.zip(in2).map((a: (Double, Double)) => math.atan(a._2/a._1))
    RealPhaseDiscTester(realParams, in1, in2, out) should be (true)
  }
     
}



/*
 * Frequency Discriminator Tester
 */ 

class FreqDiscTester[T <: chisel3.Data](c: FreqDiscriminator[T], input1: Seq[Double], input2: Seq[Double], output: Seq[Double], tolLSBs: Int = 10) extends DspTester(c) {
  poke(c.io.out.ready, 1)
  poke(c.io.in.valid, 1)

  var i = 0
  for (i <- 0 until input1.size) {
    poke(c.io.in.bits.ips, input1(i))
    poke(c.io.in.bits.qps, input2(i))

    while (!peek(c.io.in.ready)) {
      step(1)
    }

    while (!peek(c.io.out.valid)) {
      peek(c.io.ipsCurrOut)
      peek(c.io.qpsCurrOut)
      peek(c.io.ipsPrevOut)
      peek(c.io.qpsPrevOut)
      peek(c.io.dotOut)
      peek(c.io.crossOut)
      step(1)
    }

    fixTolLSBs.withValue(tolLSBs) {
      expect(c.io.out.bits.output, output(i))
    }
  }   
}

object FreqDiscTester {
  def apply(params: FixedDiscParams, input1: Seq[Double], input2: Seq[Double], output: Seq[Double]): Boolean = {
    chisel3.iotesters.Driver.execute(Array("-tbn", "firrtl", "-fiwv"), 
      () => new FreqDiscriminator(params)) {
      c => new FreqDiscTester(c, input1, input2, output)
    }
  } 
}

object RealFreqDiscTester {
  def apply(params: DiscParams[dsptools.numbers.DspReal], input1: Seq[Double], input2: Seq[Double], output: Seq[Double]): Boolean = {
    chisel3.iotesters.Driver.execute(Array("-tbn", "verilator", "-fiwv"), 
      () => new FreqDiscriminator(params)) {
      c => new FreqDiscTester(c, input1, input2, output)
    }
  } 
}

class FreqDiscSpec extends FlatSpec with Matchers {
  behavior of "DspReal FLL Discriminator"
  val realCordicParams = new CordicParams[DspReal] {
    val protoXY = DspReal()
    val protoZ = DspReal()
    val nStages = 30
    val correctGain = true
    val stagesPerCycle = 1
    val calAtan2 = true
    val dividing = false 
  }  

  val realParams = new DiscParams[DspReal] {
    val inWidth = 32 
    val outWidth = 32 
    val cordicParams = realCordicParams 
    val protoIn = DspReal()
    val protoOut = DspReal() 
  } 
  it should "atan" in {
    val in1 = Seq(1, 2, 3, 4, 5.0, 6.0, 7.0, 8.0, 9.0, 10.0)
    val in2 = Seq(442, 5, 43, 2, 523.0, 65, 23, 12, 90, 22)
    val costas = new CostasModel(0.001, 17.0, 3.0, 0, 2) 
    val out = in1.zip(in2).map((a: (Double, Double)) => {costas.update(a._1, a._2, 0); costas.freqErr * 0.001})
    println(out)
    RealFreqDiscTester(realParams, in1, in2, out) should be (true)
  }
}

