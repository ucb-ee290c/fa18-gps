package gps

import chisel3._
import chisel3.util.{HasBlackBoxResource, HasBlackBoxInline}
import chisel3.core.{IntParam, StringParam}

trait FileReaderParams {
  val ReadBitWidth: Int
  val FileName: String
}

class FileReaderBundle(params: FileReaderParams) extends Bundle {
  val run = Input(Bool())
  val valid = Output(Bool())
  val out = Output(SInt(params.ReadBitWidth.W))
  
  override def cloneType: this.type = FileReaderBundle(params).asInstanceOf[this.type]
}

object FileReaderBundle {
  def apply(params: FileReaderParams): FileReaderBundle = new FileReaderBundle(params)
}

/** Black Box verilog module used to read test data from a file for Rocketchip integration tests
 *  Black Box verilog module is located at src/main/resources/BBFileReader.sv
 *
 *  @param: ReadBitWidth the bitwidth of the data to read
 *  @param: Filename name of the test data file to read from
 *
 *  IO:
 *  run: Input(Bool) send high when you want to read from the file
 *  valid: Output(Bool) high when data is valid out (ie. not at end of file yet)
 *  out: Output(SInt) test data read from the file
 */
class FileReader(val params: FileReaderParams) extends Module {
  val io = IO(new FileReaderBundle(params))
  
  val BBFR = Module(new BBFileReader(params))

  BBFR.io.clk := clock
  BBFR.io.data.run := io.run
  io.out := BBFR.io.data.out
  io.valid := BBFR.io.data.valid
}


class BBFileReader(val myParams: FileReaderParams) extends BlackBox(
  Map("IO_READWIDTH" -> IntParam(myParams.ReadBitWidth),
      "FILE_NAME" -> StringParam(myParams.FileName))
  ) with HasBlackBoxResource {
  val io = IO(new Bundle() {
    val clk = Input(Clock())
    val data = new FileReaderBundle(myParams)
  })

  setResource("/BBFileReader.sv")
}
