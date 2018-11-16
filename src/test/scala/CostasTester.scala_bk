package gps

import dsptools.numbers._
import dsptools.DspTester 
import org.scalatest.{FlatSpec, Matchers}

case class ABC(
  // input x, y and z
  ip: Int,
  qp: Int,
  lfcoeff0: Int,
  lfcoeff1: Int,
  lfcoeff2: Int,
  lfcoeff3: Int,
  lfcoeff4: Int,
  fbias: Int,
)

/* 
 * DspSpec for Costas
 */
class CostasSpec extends FlatSpec with Matchers {

  behavior of "Costas"
  val params_costas = SampledCostasParams(
    dataWidth = 10,
    ncoWidth = 20,
    cordicXYWidth=22,
    cordicZWidth=22,
    cordicNStages = 12,
    cordicCorrectGain = true,
    cordicCalAtan2 = false,
    cordicDividing = false,
    fllRightShift = 0,  // keep 0 right shift
  )

  it should "Run CostasTest" in {
    val input = ABC(ip=(-128), qp=(128), lfcoeff0=10000, lfcoeff1=0, lfcoeff2=0, lfcoeff3=0, lfcoeff4=0,
      fbias=0)
    SampledCostasTester(params_costas, input) should be (true)
  }

  behavior of "Div"
  val params_div = SampledCostasParams(
    dataWidth = 10,
    ncoWidth = 20,
    cordicXYWidth = 20,
    cordicZWidth = 20,
    cordicNStages = 20,
    cordicCorrectGain = false,
    cordicCalAtan2 = false,
    cordicDividing = true,
    fllRightShift = 0,  // keep 0 right shift
  )

  it should "Run DivTest" in {
    val input = ABC(ip=(128), qp=(-10), lfcoeff0=10000, lfcoeff1=0, lfcoeff2=0, lfcoeff3=0, lfcoeff4=0,
      fbias=0)
    SampledCostasTester(params_div, input) should be (true)
  }
}

/*
 * Tester for Costas
 */
class CostasTester[T <: chisel3.Data](dut: CostasLoop, input: ABC) extends DspTester(dut) {
  poke(dut.io.Ip, input.ip)
  poke(dut.io.Qp, input.qp)
  poke(dut.io.lfCoeff.phaseCoeff0, input.lfcoeff0)
  poke(dut.io.lfCoeff.phaseCoeff1, input.lfcoeff1)
  poke(dut.io.lfCoeff.phaseCoeff2, input.lfcoeff2)
  poke(dut.io.lfCoeff.freqCoeff0, input.lfcoeff3)
  poke(dut.io.lfCoeff.freqCoeff1, input.lfcoeff4)
  poke(dut.io.freqBias, input.fbias)

  peek(dut.io.freqCtrl)
  peek(dut.io.phaseCtrl)

  // debug
  peek(dut.io.xin)
  peek(dut.io.yin)
  peek(dut.io.zin)
  peek(dut.io.xout)
  peek(dut.io.yout)
  peek(dut.io.zout)

  expect(true, "always true")
  step(1)
}

object SampledCostasTester {
  def apply(params: SampledCostasParams, input: ABC): Boolean = {
    chisel3.iotesters.Driver.execute(Array("-tbn", "firrtl", "-fiwv"), () => new CostasLoop(params)) {
      c => new CostasTester(c, input)
    }
  }
}

