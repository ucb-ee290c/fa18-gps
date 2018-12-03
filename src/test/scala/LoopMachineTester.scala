package gps

import chisel3._
import dsptools.DspTester
import dsptools.numbers._

import scala.collection.mutable.ListBuffer
import org.scalatest.{FlatSpec, Matchers}

class LoopMachineTester[T <: chisel3.Data](c: LoopMachine[T], ie: Seq[Double], ip: Seq[Double], il: Seq[Double], qe: Seq[Double], qp: Seq[Double], ql: Seq[Double], output: Seq[(Double, Double)]) extends DspTester(c){
  poke(c.io.out.ready, 1)
  poke(c.io.in.valid, 1)

  var i = 0
  for (i <- 0 until ie.size) {
    poke(c.io.in.bits.epl.ie, ie(i))
    poke(c.io.in.bits.epl.ip, ip(i))
    poke(c.io.in.bits.epl.il, il(i))
    poke(c.io.in.bits.epl.qe, qe(i))
    poke(c.io.in.bits.epl.qp, qp(i))
    poke(c.io.in.bits.epl.ql, ql(i))

    while (!peek(c.io.in.ready)) {
      step(1)
    }

    while (!peek(c.io.out.valid)) {
      peek(c.io.out.bits.phaseErrOut)
      peek(c.io.out.bits.freqErrOut)
      peek(c.io.out.bits.dllErrOut)
      step(1)
    }

    fixTolLSBs.withValue(1) {
      expect(c.io.out.bits.codeNco, output(i)._1)
      expect(c.io.out.bits.carrierNco, output(i)._2)
    }
  }   

} 

object FixedLoopMachineTester {
  def apply(loopParams: ExampleLoopParams, ie: Seq[Double], ip: Seq[Double], il: Seq[Double], qe: Seq[Double], qp: Seq[Double], ql: Seq[Double], output: Seq[(Double, Double)]): Boolean = {
    chisel3.iotesters.Driver.execute(Array("-tbn", "firrtl", "-fiwv"), 
      () => new LoopMachine(loopParams)) {
      c => new LoopMachineTester(c, ie, ip, il, qe, qp, ql, output)
    }
  } 
}

object RealLoopMachineTester {
  def apply(loopParams: LoopParams[dsptools.numbers.DspReal], ie: Seq[Double], ip: Seq[Double], il: Seq[Double], qe: Seq[Double], qp: Seq[Double], ql: Seq[Double], output: Seq[(Double, Double)]): Boolean = {
    chisel3.iotesters.Driver.execute(Array("-tbn", "verilator", "-fiwv"), 
      () => new LoopMachine(loopParams)) {
      c => new LoopMachineTester(c, ie, ip, il, qe, qp, ql, output)
    }
  } 
}

class LoopMachineSpec extends FlatSpec with Matchers {
  behavior of "DspReal Loop Machine"
  val realCordicParams = RealCordicParams()

  val realDiscParams = RealDiscParams(cordicParams = realCordicParams)

  val realLfParamsCostas = new LoopFilter3rdParams[DspReal] {
    val proto = DspReal()
    val fBandwidth = 3.0
    val pBandwidth = 17.0
    val fDCGain = 1.0
    val pDCGain = 1.0 
  }

  val realLfParamsDLL = new LoopFilterParams[DspReal] {
    val proto = DspReal()
    val dcGain = 6000.0
    val bandwidth = 3.0
    val sampleRate = 1e3
  }
  val realLoopParams = new LoopParams[DspReal] {
    val protoIn = DspReal()
    val protoOut = DspReal()
    val inputWidth = 32
    val intTime = 0.001
    val lfParamsCostas = realLfParamsCostas
    val lfParamsDLL = realLfParamsDLL
    val phaseDisc = realDiscParams
    val freqDisc = realDiscParams.copy(cordicParams=realCordicParams.copy(calAtan2 = true))
    val dllDisc = realDiscParams.copy(cordicParams=realCordicParams.copy(dividing = true))
  }
  
  it should "converge all loops" in {
    val ie = Seq(1, 2, 3, 4, -5.0, 6.0, -7.0, 8.0, -9.0, 10.0)
    val ip = Seq(442, 5, 43, 2, 523.0, 65, 23, 12, 90, 22)
    val il = Seq(3.0, 7, 12, 1001, 1023, 1005, 1024, 672, 666, 777)
    val qe = Seq(2, 4, 601, 4, 4, 4, 4, 6, 9, -20.0)
    val qp = Seq(7, 12, 13, 239, -777, -80, 3, -120, 0, 0.0)
    val ql = Seq(-72, 12, -590, -7, 60, 3, -100, -120, 0.0, -1000)

    val dll = new DLLModel(6000, 3, 1e3, 2)
    val costas = new CostasModel(0.001, 17.0, 3.0, 0, 2) 

    val iInt = ie.zip(ip.zip(il))
    val qInt = qe.zip(qp.zip(ql))
    def f2[A,B,C](t: (A,(B,C))) = (t._1, t._2._1, t._2._2)
    val iIntFlat = iInt.map(f2(_))
    val qIntFlat = qInt.map(f2(_))

    val dllOut = iIntFlat.zip(qIntFlat).map((a: ((Double, Double, Double), (Double, Double, Double))) => {dll.updateDouble(a._1, a._2, 0)})
    val costasOut = ip.zip(qp).map((a: (Double, Double)) => {costas.updateDouble(a._1, a._2, 0)})

    val out = dllOut.zip(costasOut)
    RealLoopMachineTester(realLoopParams, ie, ip, il, qe, qp, ql, out) should be (true)
    
  } 
  
  behavior of "Fixed Loop Machine"

  val fixedLoopParams = ExampleLoopParams(inWidth=256,inBP=192, ncoWidth=256, 192)

    val ie = Seq(2500.0, 2400, 2600, 2330)
    val ip = Seq(5000.0, 4575, 3452, 6453)
    val il = Seq(2200.0, 2050, 2666, 2777)
    val qe = Seq(2100.0, 2600, 1990, 2314)
    val qp = Seq(4500.0, 5100, 4200, 3999)
    val ql = Seq(3100.0, 2120, 2700, 2000)

    val dll = new DLLModel(6000, 3, 1e3, 2)
    val costas = new CostasModel(0.001, 17.0, 3.0, 0, 2) 

    val iInt = ie.zip(ip.zip(il))
    val qInt = qe.zip(qp.zip(ql))
    def f2[A,B,C](t: (A,(B,C))) = (t._1, t._2._1, t._2._2)
    val iIntFlat = iInt.map(f2(_))
    val qIntFlat = qInt.map(f2(_))

    val dllOut = iIntFlat.zip(qIntFlat).map((a: ((Double, Double, Double), (Double, Double, Double))) => {dll.updateDouble(a._1, a._2, 0)})
    val costasOut = ip.zip(qp).map((a: (Double, Double)) => {costas.updateDouble(a._1, a._2, 0)})

    val out = dllOut.zip(costasOut)
  it should "converge all loops" in {
    FixedLoopMachineTester(fixedLoopParams, ie, ip, il, qe, qp, ql, out) should be (true)
  }  
  
}
