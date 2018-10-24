
package gps

import chisel3._
import chisel3.util._
import dsptools.numbers._

trait IntDumpParams[T <: Data] {
  val protoIn: T
  val protoInteg: T
}

case class SampledIntDumpParams(inWidth: Int, codeLength: Int) extends IntDumpParams[SInt] {
  val protoIn = SInt(inWidth.W)
  val protoInteg = SInt(log2Ceil(codeLength * (Math.pow(2,inWidth-1)-1).toInt).W)
}

class IntDumpIO[T <: Data](params: IntDumpParams[T]) extends Bundle {
  val in = Input(params.protoIn)
  val dump = Input(Bool())
  val integ = Output(params.protoInteg)
}

object IntDumpIO {
  def apply[T <: Data](params: IntDumpParams[T]): IntDumpIO[T] =
    new IntDumpIO(params)
}

class IntDump[T <: Data : Ring] (val params: IntDumpParams[T]) extends Module {
  val io = IO(IntDumpIO(params))

  val integReg = RegInit(params.protoInteg, Ring[T].zero)//(params.protoInteg.cloneType))

  when (io.dump) {
    integReg := io.in
  } .otherwise {
    integReg := integReg + io.in
  }

  io.integ := integReg
}