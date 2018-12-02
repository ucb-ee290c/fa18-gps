// See LICENSE for license details.

package gps

import chisel3._
import dsptools._
import dsptools.numbers._
import dspjunctions._
import dspblocks._
import freechips.rocketchip.amba.axi4stream._
import freechips.rocketchip.config.Parameters
import freechips.rocketchip.diplomacy._
import freechips.rocketchip.regmapper._
import freechips.rocketchip.tilelink._

class acqBlock[T <: Data : Real](val config: ALoopParParams[SInt])(implicit p: Parameters) extends TLDspBlock with TLHasCSR {
  val streamNode = AXI4StreamIdentityNode()
  def csrAddress = AddressSet(0x2200, 0xff)
  def beatBytes = 8
  def devname = "tlacq"
  def devcompat = Seq("ucb-art", "acq")
  val device = new SimpleDevice(devname, devcompat) {
    override def describe(resources: ResourceBindings): Description = {
      val Description(name, mapping) = super.describe(resources)
      Description(name, mapping)
    }
  }
  override val mem = Some(TLRegisterNode(address = Seq(csrAddress), device = device, beatBytes = beatBytes))

  lazy val module = new LazyModuleImp(this) {
    val (in, inP) = streamNode.in.head
    val (out, outP) = streamNode.out.head

    // TODO: initialize the acq block and make connection, refer to cordic lab or FFT/FFTblock.scala
    // val module = Module(new acq[T](config))


    val module = Module(new ALoopPar[T](config))

//    val dataSetEndClear = RegInit(0.U(64.W))
//    module.io.data_set_end_clear := dataSetEndClear

    in.ready := module.io.in.ready
    module.io.in.valid := in.valid
    out.valid := module.io.out.valid
    module.io.out.ready := out.ready

    // TODO:
    module.io.in.bits := in.bits
    out.bits := module.io.out.bits





    // TODO:
    assert(out.ready)

//    out.bits.data := module.io.out.bits.asUInt






    regmap(
        // regmap...
    )
  }
}
