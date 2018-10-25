package fft

import chisel3._
import chisel3.experimental._
import chisel3.util._
import dspblocks._
import dsptools._
import dsptools.numbers._
import dspjunctions._
import freechips.rocketchip.amba.axi4stream._
import freechips.rocketchip.config.Parameters
import freechips.rocketchip.diplomacy._
import freechips.rocketchip.regmapper._
import freechips.rocketchip.tilelink._

/**
  * The streaming interface adds elements into the queue.
  * The memory interface can read elements out of the queue.
  * @param depth number of entries in the queue
  * @param streamParameters parameters for the stream node
  * @param p
  */
abstract class ReadQueue
(
  val depth: Int = 8,
  val streamParameters: AXI4StreamSlaveParameters = AXI4StreamSlaveParameters()
)(implicit p: Parameters) extends LazyModule with HasCSR {
  val streamNode = AXI4StreamSlaveNode(streamParameters)

  lazy val module = new LazyModuleImp(this) {
    require(streamNode.in.length == 1)

    // get the input bundle associated with the AXI4Stream node
    val in = streamNode.in(0)._1
    val in_bits = in.bits.data.asTypeOf(Vec(2, DspComplex(FixedPoint(18.W, 6.BP), FixedPoint(18.W, 6.BP))))
    // width (in bits) of the input interface
    val width = in.params.n * 8
    // instantiate a queue
    val queue0 = Module(new Queue(DspComplex(FixedPoint(18.W, 6.BP), FixedPoint(18.W, 6.BP)), depth))
    val queue1 = Module(new Queue(DspComplex(FixedPoint(18.W, 6.BP), FixedPoint(18.W, 6.BP)), depth))
    // connect streaming output to queue output
    queue0.io.enq.valid := in.valid
    queue0.io.enq.bits := in_bits(0)
    queue1.io.enq.valid := in.valid
    queue1.io.enq.bits := in_bits(1)
    // don't use last. don't think we need it for slave
    in.ready := (queue0.io.enq.ready && queue1.io.enq.ready)

    val deq0 = Wire(Decoupled(UInt(24.W)))
    deq0.valid := queue0.io.deq.valid
    deq0.bits := queue0.io.deq.bits.asUInt()
    queue0.io.deq.ready := deq0.ready

    val deq1 = Wire(Decoupled(UInt(24.W)))
    deq1.valid := queue1.io.deq.valid
    deq1.bits := queue1.io.deq.bits.asUInt()
    queue1.io.deq.ready := deq1.ready


    regmap(
      // each write adds an entry to the queue
      0x0 -> Seq(RegField.r(width, deq0)),
      // read the number of entries in the queue
      (width+7)/8 -> Seq(RegField.r(width, deq1)),
    )
  }
}

/**
  * TLDspBlock specialization of ReadQueue
  * @param depth number of entries in the queue
  * @param csrAddress address range
  * @param beatBytes beatBytes of TL interface
  * @param p
  */
class TLReadQueue
(
  depth: Int = 8,
  csrAddress: AddressSet = AddressSet(0x2100, 0xff),
  beatBytes: Int = 8
)(implicit p: Parameters) extends ReadQueue(depth) with TLHasCSR {
  val devname = "tlQueueOut"
  val devcompat = Seq("ucb-art", "dsptools")
  val device = new SimpleDevice(devname, devcompat) {
    override def describe(resources: ResourceBindings): Description = {
      val Description(name, mapping) = super.describe(resources)
      Description(name, mapping)
    }
  }
  // make diplomatic TL node for regmap
  override val mem = Some(TLRegisterNode(address = Seq(csrAddress), device = device, beatBytes = beatBytes))

}
