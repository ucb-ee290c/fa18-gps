
package gps

import chisel3._
import chisel3.util._
import dsptools.numbers._

trait IntDumpParams[T <: Data] {
  val protoIn: T
  val protoInteg: T
}

/** A set of type parameters for the IntDump module with an SInt type.
 *  This will determine the type of the accumulation register and the input.
 *  @param inWidth input width from output of code wipe-off
 *  @param codeLength CA code length to determine necessary width of accumulator (L1 C/A length: 1023)
 */
case class SampledIntDumpParams(inWidth: Int, codeLength: Int) extends IntDumpParams[SInt] {
  val protoIn = SInt(inWidth.W)
  val protoInteg = SInt(log2Ceil(codeLength * (Math.pow(2,inWidth-1)-1).toInt).W)
}

class IntDumpIO[T <: Data](params: IntDumpParams[T]) extends Bundle {
  val in = Input(params.protoIn)
  val dump = Input(Bool())
  val integ = Output(params.protoInteg)

  override def cloneType: this.type = IntDumpIO(params).asInstanceOf[this.type]
}

object IntDumpIO {
  def apply[T <: Data](params: IntDumpParams[T]): IntDumpIO[T] =
    new IntDumpIO(params)
}

/** Type generic Integration and dump filter used to accumulate results of carrier and code wipe-off 
 *  for early, prompt, and late code multiplies to be used by the code and carrier tracking loops
 *
 *  IO:
 *  in: Input(SInt), input value to accumulate
 *  dump: Input(Bool), signal that is pulsed to dump the integration
 *  integ: Output(SInt), output of integrate and dump accumulator
 *
 *  Test:
 *  Peek poke tester: run with sbt test:testOnly gps.IntDumpSpec
 */
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
