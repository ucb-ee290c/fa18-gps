package gps

import dsptools.numbers._
import dsptools.DspTester 
import org.scalatest.{FlatSpec, Matchers}

case class cordicXYZ(
  // input x, y and z
  x: Double,
  y: Double,
  z: Double,
  vectoring: Boolean
)

/* 
 * DspSpec for Costas
 */
class CordicSpec extends FlatSpec with Matchers {

  behavior of "Cordic"
  val params_cordic = FixedCordicParams(
    xyWidth=22,
    xyBPWidth=12,
    zWidth=22,
    zBPWidth=12,
    nStages = 12,
    correctGain = true,
    calAtan2 = false,
    dividing = false,
  )

  it should "Run CordicTest" in {
    val input = cordicXYZ(x=(-128.0), y=(128.0), z=0.0, vectoring=true)
    CordicTester(params_cordic, input) should be (true)
  }

  behavior of "Div"
  val params_div = FixedCordicParams(
    xyWidth = 28,
    xyBPWidth = 18,
    zWidth = 28,
    zBPWidth = 18,
    nStages = 24,
    correctGain = false,
    calAtan2 = false,
    dividing = true,
  )

  it should "Run DivTest" in {
    val input = cordicXYZ(x=(128), y=(-1), z=0, vectoring=true)
    CordicTester(params_div, input) should be (true)
  }
}

/*
 * Tester for Cordic
 */
class CordicTester[T <: chisel3.Data](dut: Cordic1Cycle[T], input: cordicXYZ) extends DspTester(dut) {
  poke(dut.io.in.x, input.x)
  poke(dut.io.in.y, input.y)
  poke(dut.io.in.z, input.z)
  poke(dut.io.vectoring, input.vectoring)

  // debug
  peek(dut.io.out.x)
  peek(dut.io.out.y)
  peek(dut.io.out.z)

  expect(true, "always true")
  step(1)
}

object CordicTester {
  def apply(params: FixedCordicParams, input: cordicXYZ): Boolean = {
    chisel3.iotesters.Driver.execute(Array("-tbn", "firrtl", "-fiwv"), () => new Cordic1Cycle(params)) {
      c => new CordicTester(c, input)
    }
  }
}

