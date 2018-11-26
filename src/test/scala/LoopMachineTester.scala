package gps

import chisel3._
import dsptools.DspTester
import dsptools.numbers._

import scala.collection.mutable.ListBuffer
import org.scalatest.{FlatSpec, Matchers}

class LoopMachineTester[T <: chisel3.Data](c: LoopMachine[T], ie: Seq[Double], ip: Seq[Double], il: Seq[Double], qe: Seq[Double], qp: Seq[Double], ql: Seq[Double], output: Seq[(Int, Int)]) extends DspTester(c){
  poke(c.io.out.ready, 1)
  poke(c.io.in.valid, 1)

  var i = 0
  for (i <- 0 until ie.size) {
    poke(c.io.in.bits.ie, ie(i))
    poke(c.io.in.bits.ip, ip(i))
    poke(c.io.in.bits.il, il(i))
    poke(c.io.in.bits.qe, qe(i))
    poke(c.io.in.bits.qp, qp(i))
    poke(c.io.in.bits.ql, ql(i))

    while (!peek(c.io.in.ready)) {
      step(1)
    }

    while (!peek(c.io.out.valid)) {
      step(1)
    }

    fixTolLSBs.withValue(10) {
      expect(c.io.out.bits.codeNco, output(i)._1)
      expect(c.io.out.bits.carrierNco, output(i)._2)
    }
  }   

} 

object FixedLoopMachineTester {
  def apply(loopParams: ExampleLoopParams, discParams: ExampleAllDiscParams, ie: Seq[Double], ip: Seq[Double], il: Seq[Double], qe: Seq[Double], qp: Seq[Double], ql: Seq[Double], output: Seq[(Int, Int)]): Boolean = {
    chisel3.iotesters.Driver.execute(Array("-tbn", "firrtl", "-fiwv"), 
      () => new LoopMachine(loopParams, discParams)) {
      c => new LoopMachineTester(c, ie, ip, il, qe, qp, ql, output)
    }
  } 
}

object RealLoopMachineTester {
  def apply(loopParams: LoopParams[dsptools.numbers.DspReal], discParams: AllDiscParams[dsptools.numbers.DspReal], ie: Seq[Double], ip: Seq[Double], il: Seq[Double], qe: Seq[Double], qp: Seq[Double], ql: Seq[Double], output: Seq[(Int, Int)]): Boolean = {
    chisel3.iotesters.Driver.execute(Array("-tbn", "verilator", "-fiwv"), 
      () => new LoopMachine(loopParams, discParams)) {
      c => new LoopMachineTester(c, ie, ip, il, qe, qp, ql, output)
    }
  } 
}

class LoopMachineSpec extends FlatSpec with Matchers {
  behavior of "DspReal Loop Machine"
  val realCordicParams = RealCordicParams()

  val realDiscParams = RealDiscParams(cordicParams = realCordicParams)

  val realAllDiscParams = new AllDiscParams[DspReal] {
    val phaseDisc = realDiscParams
    val freqDisc = realDiscParams.copy(cordicParams=realCordicParams.copy(calAtan2 = true))
    val dllDisc = realDiscParams.copy(cordicParams=realCordicParams.copy(dividing = true))
  } 
  val realLfParamsCostas = new LoopFilter3rdParams[DspReal] {
    val proto = DspReal()
    val fBandwidth = 3.0
    val pBandwidth = 17.0
    val a2 = 1.414
    val a3 = 1.1
    val b3 = 2.4
    val fDCGain = 1.0
    val pDCGain = 1.0 
  }

  val realLfParamsDLL = new LoopFilterParams[DspReal] {
    val proto = DspReal()
    val dcGain = 6000.0
    val bandwidth = 5.0
    val sampleRate = 1.0
  }
  val realLoopParams = new LoopParams[DspReal] {
    val protoIn = DspReal()
    val protoOut = DspReal()
    val inputWidth = 32
    val intTime = 0.001
    val lfParamsCostas = realLfParamsCostas
    val lfParamsDLL = realLfParamsDLL
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

    val dllOut = iIntFlat.zip(qIntFlat).map((a: ((Double, Double, Double), (Double, Double, Double))) => {dll.update(a._1, a._2, 0)})
    val costasOut = ip.zip(qp).map((a: (Double, Double)) => {costas.update(a._1, a._2, 0)})

    val out = dllOut.zip(costasOut)
    RealLoopMachineTester(realLoopParams, realAllDiscParams, ie, ip, il, qe, qp, ql, out) should be (true)
    
  } 
  
}
