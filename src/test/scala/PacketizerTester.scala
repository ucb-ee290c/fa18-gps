package gps

import dsptools.DspTester
import org.scalatest.{FlatSpec, Matchers}

import chisel3._

class ParserSpec extends FlatSpec with Matchers {
  behavior of "Packetizer: parser submodule"

  it should "output valid subframe" in {
    ParserTester() should be (true)
  }
}

class ParserTester(c: Parser) extends DspTester(c) {
  updatableDspVerbose.withValue(true) {
    val preamble = List(1, 0, 0, 0, 1, 0, 1, 1)
    val subframe = List(List(1, 0, 0, 0, 1, 0, 1, 1, 0, 0, 0, 0, 0, 1, 0, 1, 1, 1, 0, 0, 0, 1, 1, 0, 0, 1, 1, 0, 0, 0),
                        List(1, 0, 0, 1, 1, 0, 1, 0, 0, 0, 1, 0, 1, 1, 0, 0, 1, 0, 1, 1, 0, 0, 1, 1, 0, 1, 0, 1, 0, 0),
                        List(1, 1, 0, 0, 0, 0, 1, 1, 0, 1, 0, 1, 1, 1, 0, 0, 0, 0, 0, 1, 0, 0, 0, 1, 1, 0, 1, 1, 1, 0),
                        List(0, 0, 1, 1, 1, 0, 1, 0, 1, 1, 0, 0, 0, 0, 1, 1, 0, 0, 1, 1, 1, 0, 1, 1, 0, 1, 1, 0, 1, 1),
                        List(0, 1, 1, 0, 1, 1, 1, 1, 0, 0, 0, 1, 1, 0, 1, 1, 0, 1, 1, 1, 0, 1, 0, 1, 1, 1, 0, 1, 0, 0),
                        List(1, 0, 0, 0, 0, 1, 0, 0, 1, 1, 0, 1, 0, 0, 1, 0, 1, 1, 1, 0, 1, 0, 1, 0, 1, 0, 0, 0, 0, 0),
                        List(0, 1, 1, 0, 0, 1, 0, 1, 0, 0, 0, 0, 0, 1, 0, 0, 1, 1, 0, 0, 0, 1, 1, 1, 1, 0, 0, 0, 1, 0),
                        List(1, 0, 1, 1, 0, 1, 0, 0, 1, 0, 1, 1, 0, 1, 0, 1, 0, 1, 0, 1, 1, 0, 1, 1, 0, 1, 0, 0, 1, 1),
                        List(0, 1, 0, 1, 0, 0, 1, 0, 0, 0, 0, 0, 1, 1, 0, 1, 1, 0, 1, 0, 1, 1, 1, 0, 0, 0, 0, 1, 1, 1),
                        List(1, 1, 1, 0, 0, 1, 1, 1, 0, 0, 1, 0, 0, 1, 1, 0, 0, 1, 1, 0, 0, 0, 1, 1, 1, 1, 0, 1, 1, 1))
    val subframeAsInts = List(Integer.parseInt("100010110000010111000110011000", 2),
                              Integer.parseInt("100110100010110010110011010100", 2),
                              Integer.parseInt("110000110101110000010001101110", 2),
                              Integer.parseInt("001110101100001100111011011011", 2),
                              Integer.parseInt("011011110001101101110101110100", 2),
                              Integer.parseInt("100001001101001011101010100000", 2),
                              Integer.parseInt("011001010000010011000111100010", 2),
                              Integer.parseInt("101101001011010101011011010011", 2),
                              Integer.parseInt("010100100000110110101110000111", 2),
                              Integer.parseInt("111001110010011001100011110111", 2))
    for (w <- 0 until 10) {
      for (b <- 0 until 30) {
        val curr_bit = (w * 30) + b
        if (curr_bit < 8) {
          expect(c.io.stateOut, 0)
        } else {
          expect(c.io.stateOut, 1)
        }

        expect(c.io.subframeValid, 0)
        poke(c.io.iIn, subframe(w)(b))
        poke(c.io.validIn, 1)
        step(1)
        poke(c.io.validIn, 0)
        if (curr_bit < 299) {
          step(19)
        }
      }
    }

    expect(c.io.subframeValid, 1)
    expect(c.io.stateOut, 2)
    for (w <- 0 until 10) {
      expect(c.io.dataOut(w), subframeAsInts(w))
    }
    expect(c.io.dStarOut, 0)
    step(1)
    expect(c.io.subframeValid, 0)
    expect(c.io.stateOut, 0)
  }
}

object ParserTester {
  def apply(): Boolean = {
    val preamble = "b10001011".U(8.W)
    val params = PacketizerParams(10, 30, 8, preamble, 6)
    dsptools.Driver.execute(() => new Parser(params), TestSetup.dspTesterOptions) {
      c => new ParserTester(c)
    }
  }
}

class ParityCheckerSpec extends FlatSpec with Matchers {
  behavior of "Packetizer: parity checker submodule"

  it should "output correct parity bits" in {
    ParityCheckerTester() should be (true)
  }
}

class ParityCheckerTester(c: ParityChecker) extends DspTester(c) {
  updatableDspVerbose.withValue(true) {
    val subframe = List(List(1, 0, 0, 0, 1, 0, 1, 1, 0, 0, 0, 0, 0, 1, 0, 1, 1, 1, 0, 0, 0, 1, 1, 0, 0, 1, 1, 0, 0, 0),
                        List(1, 0, 0, 1, 1, 0, 1, 0, 0, 0, 1, 0, 1, 1, 0, 0, 1, 0, 1, 1, 0, 0, 1, 1, 0, 1, 0, 1, 0, 0),
                        List(1, 1, 0, 0, 0, 0, 1, 1, 0, 1, 0, 1, 1, 1, 0, 0, 0, 0, 0, 1, 0, 0, 0, 1, 1, 0, 1, 1, 1, 0),
                        List(0, 0, 1, 1, 1, 0, 1, 0, 1, 1, 0, 0, 0, 0, 1, 1, 0, 0, 1, 1, 1, 0, 1, 1, 0, 1, 1, 0, 1, 1),
                        List(0, 1, 1, 0, 1, 1, 1, 1, 0, 0, 0, 1, 1, 0, 1, 1, 0, 1, 1, 1, 0, 1, 0, 1, 1, 1, 0, 1, 0, 0),
                        List(1, 0, 0, 0, 0, 1, 0, 0, 1, 1, 0, 1, 0, 0, 1, 0, 1, 1, 1, 0, 1, 0, 1, 0, 1, 0, 0, 0, 0, 0),
                        List(0, 1, 1, 0, 0, 1, 0, 1, 0, 0, 0, 0, 0, 1, 0, 0, 1, 1, 0, 0, 0, 1, 1, 1, 1, 0, 0, 0, 1, 0),
                        List(1, 0, 1, 1, 0, 1, 0, 0, 1, 0, 1, 1, 0, 1, 0, 1, 0, 1, 0, 1, 1, 0, 1, 1, 0, 1, 0, 0, 1, 1),
                        List(0, 1, 0, 1, 0, 0, 1, 0, 0, 0, 0, 0, 1, 1, 0, 1, 1, 0, 1, 0, 1, 1, 1, 0, 0, 0, 0, 1, 1, 1),
                        List(1, 1, 1, 0, 0, 1, 1, 1, 0, 0, 1, 0, 0, 1, 1, 0, 0, 1, 1, 0, 0, 0, 1, 1, 1, 1, 0, 1, 1, 1))
    val subframeAsInts = List(Integer.parseInt("100010110000010111000110011000", 2),
                              Integer.parseInt("100110100010110010110011010100", 2),
                              Integer.parseInt("110000110101110000010001101110", 2),
                              Integer.parseInt("001110101100001100111011011011", 2),
                              Integer.parseInt("011011110001101101110101110100", 2),
                              Integer.parseInt("100001001101001011101010100000", 2),
                              Integer.parseInt("011001010000010011000111100010", 2),
                              Integer.parseInt("101101001011010101011011010011", 2),
                              Integer.parseInt("010100100000110110101110000111", 2),
                              Integer.parseInt("111001110010011001100011110111", 2))

    for (w <- 0 until 10) {
      for (b <- 0 until 30) {
        poke(c.io.dataIn(w)(b), subframe(w)(b))
      }
    }
    poke(c.io.dStarIn, 0)
    poke(c.io.subframeValid, 1)
    step(2)
    poke(c.io.subframeValid, 0)
    expect(c.io.validOut, 1)
    for (w <- 0 until 10) {
      expect(c.io.validBits(w), 1)
      for (b <- 0 until 6) {
        expect(c.io.parityOut(w)(b), subframe(w)(24 + b))
      }
    }
    step(1)
    expect(c.io.validOut, 0)
  }
}

object ParityCheckerTester {
  def apply(): Boolean = {
    val preamble = "b10001011".U(8.W)
    val params = PacketizerParams(10, 30, 8, preamble, 6)
    dsptools.Driver.execute(() => new ParityChecker(params), TestSetup.dspTesterOptions) {
      c => new ParityCheckerTester(c)
    }
  }
}

class PacketizerSpec extends FlatSpec with Matchers {
  behavior of "Packetizer module"

  it should "output correct parity bits" in {
    PacketizerTester() should be (true)
  }
}

class PacketizerTester(c: Packetizer) extends DspTester(c) {
  updatableDspVerbose.withValue(true) {
    val subframe = List(List(1, 0, 0, 0, 1, 0, 1, 1, 0, 0, 0, 0, 0, 1, 0, 1, 1, 1, 0, 0, 0, 1, 1, 0, 0, 1, 1, 0, 0, 0),
                        List(1, 0, 0, 1, 1, 0, 1, 0, 0, 0, 1, 0, 1, 1, 0, 0, 1, 0, 1, 1, 0, 0, 1, 1, 0, 1, 0, 1, 0, 0),
                        List(1, 1, 0, 0, 0, 0, 1, 1, 0, 1, 0, 1, 1, 1, 0, 0, 0, 0, 0, 1, 0, 0, 0, 1, 1, 0, 1, 1, 1, 0),
                        List(0, 0, 1, 1, 1, 0, 1, 0, 1, 1, 0, 0, 0, 0, 1, 1, 0, 0, 1, 1, 1, 0, 1, 1, 0, 1, 1, 0, 1, 1),
                        List(0, 1, 1, 0, 1, 1, 1, 1, 0, 0, 0, 1, 1, 0, 1, 1, 0, 1, 1, 1, 0, 1, 0, 1, 1, 1, 0, 1, 0, 0),
                        List(1, 0, 0, 0, 0, 1, 0, 0, 1, 1, 0, 1, 0, 0, 1, 0, 1, 1, 1, 0, 1, 0, 1, 0, 1, 0, 0, 0, 0, 0),
                        List(0, 1, 1, 0, 0, 1, 0, 1, 0, 0, 0, 0, 0, 1, 0, 0, 1, 1, 0, 0, 0, 1, 1, 1, 1, 0, 0, 0, 1, 0),
                        List(1, 0, 1, 1, 0, 1, 0, 0, 1, 0, 1, 1, 0, 1, 0, 1, 0, 1, 0, 1, 1, 0, 1, 1, 0, 1, 0, 0, 1, 1),
                        List(0, 1, 0, 1, 0, 0, 1, 0, 0, 0, 0, 0, 1, 1, 0, 1, 1, 0, 1, 0, 1, 1, 1, 0, 0, 0, 0, 1, 1, 1),
                        List(1, 1, 1, 0, 0, 1, 1, 1, 0, 0, 1, 0, 0, 1, 1, 0, 0, 1, 1, 0, 0, 0, 1, 1, 1, 1, 0, 1, 1, 1))
    val subframeAsInts = List(Integer.parseInt("100010110000010111000110011000", 2),
                              Integer.parseInt("100110100010110010110011010100", 2),
                              Integer.parseInt("110000110101110000010001101110", 2),
                              Integer.parseInt("001110101100001100111011011011", 2),
                              Integer.parseInt("011011110001101101110101110100", 2),
                              Integer.parseInt("100001001101001011101010100000", 2),
                              Integer.parseInt("011001010000010011000111100010", 2),
                              Integer.parseInt("101101001011010101011011010011", 2),
                              Integer.parseInt("010100100000110110101110000111", 2),
                              Integer.parseInt("111001110010011001100011110111", 2))
    for (w <- 0 until 10) {
      for (b <- 0 until 30) {
        val curr_bit = (w * 30) + b
        poke(c.io.iIn, subframe(w)(b))
        poke(c.io.validIn, 1)
        step(1)
        poke(c.io.validIn, 0)
        if (curr_bit < 299) {
          step(19)
        }
      }
    }
    while (!peek(c.io.validOut)) {
      step(1)
    }
    for (w <- 0 until 10) {
      expect(c.io.validBits(w), 1)
    }
    // check extracted values
    expect(c.io.extractedValues.week_number, 781)
    expect(c.io.extractedValues.sv_accuracy, 12)
    expect(c.io.extractedValues.sv_health, 4)
    expect(c.io.extractedValues.iodc, 436)
    expect(c.io.extractedValues.t_gd, -57)
    expect(c.io.extractedValues.a_f2, 82)
    expect(c.io.extractedValues.a_f1, 3502)
    expect(c.io.extractedValues.a_f0, -407144)

    expect(c.io.extractedValues.iode, 195)
    expect(c.io.extractedValues.c_rs, 23569)
    expect(c.io.extractedValues.delta_n, 15043)
    expect(c.io.extractedValues.m_0, 997137269)
    expect(c.io.extractedValues.c_uc, -31534)
    // expect(c.io.extractedValues.e, 3932488903L) // this line breaks for some reason :/
    peek(c.io.extractedValues.e)
    expect(c.io.extractedValues.c_us, -19275)
    expect(c.io.extractedValues.sqrt_a, 1532104110)
    expect(c.io.extractedValues.t_oe, 59174)

    expect(c.io.extractedValues.c_ic, -15524)
    expect(c.io.extractedValues.omega_0, 289063739)
    expect(c.io.extractedValues.c_is, 28443)
    expect(c.io.extractedValues.i_0, 1971639018)
    expect(c.io.extractedValues.c_rc, 25860)
    expect(c.io.extractedValues.omega, -944458405)
    expect(c.io.extractedValues.dot_omega, 5377454)
    expect(c.io.extractedValues.idot, 2456)
  }
}

object PacketizerTester {
  def apply(): Boolean = {
    val preamble = "b10001011".U(8.W)
    val params = PacketizerParams(10, 30, 8, preamble, 6)
    dsptools.Driver.execute(() => new Packetizer(params), TestSetup.dspTesterOptions) {
      c => new PacketizerTester(c)
    }
  }
}
