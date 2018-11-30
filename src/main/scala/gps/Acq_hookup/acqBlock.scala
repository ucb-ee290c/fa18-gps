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

class acqBlock[T <: Data : Real](val config: acqConfig[T])(implicit p: Parameters) extends TLDspBlock with TLHasCSR {
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




    regmap(
        // regmap...
    )
  }
}
