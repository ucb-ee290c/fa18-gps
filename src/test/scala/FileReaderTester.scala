package gps

import dsptools.DspTester
import chisel3.core._
import org.scalatest.{FlatSpec, Matchers}

class FileReaderTester (
  c: FileReader,
  ) extends DspTester(c) {

  step(1)
  poke(c.io.run, true.B)
  for (i <- 0 until 5) {
    step(1)
    peek(c.io.out)
  }
  step(1)
  expect(c.io.valid, false.B)

}


object FileReaderTester {
  def apply(params: FileReaderParams): Boolean = {
    chisel3.iotesters.Driver.execute(Array("-tbn", "verilator", "-fiwv"), () => new FileReader(params)) {
      c => new FileReaderTester(c)
    }
  }
}

class FileReaderSpec extends FlatSpec with Matchers {
  behavior of "FileReader"

  val params = new FileReaderParams {
    val ReadBitWidth = 16
    val FileName = "src/test/resources/BBFileReaderData.txt"
  }

  it should "Read data from test file" in {
    FileReaderTester(params) should be (true)
  }
}
