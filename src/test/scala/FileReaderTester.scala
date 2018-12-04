package gps

import chisel3._
import dsptools.DspTester
import org.scalatest.{FlatSpec, Matchers}

class FileReaderSpec extends FlatSpec with Matchers {
  behavior of "FileReader"

  val params = new FileReaderParams {
    val ReadBitWidth = 8
    val FileName = "src/test/resources/adc_sample_data.bin"
  }

  it should "read binary data from a file" in {
    FileReaderTester(params) should be (true)
  }
}

class FileReaderTester (c: FileReader) extends DspTester(c) {
  poke(c.io.run, false.B)
  step(10)
  poke(c.io.run, true.B)

  for(i <- 0 until 10) {
    step(1)
    peek(c.io.out)
  }

}

object FileReaderTester {
  def apply(params: FileReaderParams): Boolean = {
    chisel3.iotesters.Driver.execute(Array("-tbn", "verilator", "-fiwv"), () => new FileReader(params)) {
      c => new FileReaderTester(c)
    }
  }
}
