package gps

import breeze.math.Complex
import chisel3._
import dspblocks.ShiftRegisterWithReset
import dspjunctions.ValidWithSync
import dsptools.numbers._
import org.scalatest.{FlatSpec, Matchers}
import chisel3.core.UInt

import scala.collection.mutable
import dsptools.DspTester

import scala.collection.mutable
import scala.math.abs

case class TEST(
                // input x, y and z
                dataIn: Seq[Double],
                caIn: Seq[Double],
                // optional outputs
//                out: Option[Double] = None
              )

class FFTMulSpec extends FlatSpec with Matchers {
  behavior of "FFT Mul"

  val laneCount = 16
  val params = FixedFFTMulParams(
    width = 24,
    bp = 20,
    laneCount = laneCount,
    pipeStageCount = 1,
  )

  it should "convert from stream to parallel" in {

    val testNum = 4
    val trials = new scala.collection.mutable.Queue[TEST]()
    (0 until testNum).foreach {x =>
      val dataInTmp = (0 until laneCount).map(i => scala.util.Random.nextDouble())
      val caInTmp = (0 until laneCount).map(i => scala.util.Random.nextDouble())
      trials += TEST(dataIn=dataInTmp, caIn=dataInTmp)
    }
      FixedFFTMulTester(params, trials) should be(true)
  }
}



class FFTMulTester[T <: chisel3.Data](c: FFTMul[T], trials: Seq[TEST], lane: Int) extends DspTester(c) {

  def compareOutputComplex(chisel: Seq[Double], ref: Seq[Double], epsilon: Double = 1e-4): Unit = {
    chisel.zip(ref).zipWithIndex.foreach { case ((c, r), index) =>
      if (c != r) {
        val err = abs(c - r) / (abs(r) + epsilon)
        assert(err < epsilon || abs(r) < epsilon, s"Error: mismatch in real value on output $index of ${err * 100}%\n\tReference: ${r}\n\tChisel:    ${c}")
      }
    }
  }

  for (trial <- trials) {
    val idealRes:Array[Double] = Array.fill(lane)(0)

    idealRes.zipWithIndex.zip(trial.dataIn.zip(trial.caIn)).foreach{case((_,r),(d,c)) => idealRes.update(r,d * c)}

    val retval = new scala.collection.mutable.Queue[Double]()
    while (!peek(c.io.out.sync)){
      trial.dataIn.zip(c.io.dataIn.bits).foreach { case (sig, port) => poke(port, sig) }
      trial.caIn.zip(c.io.caIn.bits).foreach { case (sig, port) => poke(port, sig) }
      poke(c.io.dataIn.valid, true)
      poke(c.io.caIn.valid, true)
      c.io.out.bits.foreach(x => retval += peek(x))
      step(1)
    }
    compareOutputComplex(retval, idealRes, epsilon = 1e-2)

  }
}
object FixedFFTMulTester {
  def apply(params: FixedFFTMulParams, trials: Seq[TEST]): Boolean = {
    chisel3.iotesters.Driver.execute(Array("-tbn", "firrtl", "-fiwv"), () => new FFTMul(params)) {
      c => new FFTMulTester(c, trials, params.laneCount)
    }
  }
}
