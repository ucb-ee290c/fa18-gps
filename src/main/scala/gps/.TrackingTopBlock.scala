package gps

import chisel3._
import chisel3.util._
import dspblocks._
import dsptools.numbers._
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
abstract class TrackingReadQueue
(
  val depth: Int = 8,
  val streamParameters: AXI4StreamSlaveParameters = AXI4StreamSlaveParameters()
)(implicit p: Parameters) extends LazyModule with HasCSR {
  val streamNode = AXI4StreamSlaveNode(streamParameters)

  lazy val module = new LazyModuleImp(this) {
    require(streamNode.in.length == 1)

    // get the input bundle associated with the AXI4Stream node
    val in = streamNode.in(0)._1
    // width (in bits) of the input interface
    val width = in.params.n * 8
    // instantiate a queue
    val queue = Module(new Queue(UInt(in.params.dataBits.W), depth))
    // connect queue output to streaming output
    queue.io.enq.valid := in.valid
    queue.io.enq.bits := in.bits.data
    // don't use last
    in.ready := queue.io.enq.ready

    regmap(
      // each read adds an entry to the queue
      0x0 -> Seq(RegField.r(width, queue.io.deq)),
      // read the number of entries in the queue
      (width+7)/8 -> Seq(RegField.r(width, queue.io.count)),
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
class TLTrackingReadQueue
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

/**
  * Make DspBlock wrapper for tracking
  * @param trackingParams parameters for tracking
  * @param ev$1
  * @param ev$2
  * @param ev$3
  * @param p
  * @tparam D
  * @tparam U
  * @tparam EO
  * @tparam EI
  * @tparam B
  * @tparam T Type parameter for tracking, i.e. FixedPoint or DspReal
  */
abstract class TrackingBlock[D, U, EO, EI, B <: Data]
(
  val trackingParams: TrackingTopParams
)(implicit p: Parameters) extends DspBlock[D, U, EO, EI, B] {
  val streamNode = AXI4StreamIdentityNode()
  val mem = None

  lazy val module = new LazyModuleImp(this) {
    require(streamNode.in.length == 1)
    require(streamNode.out.length == 1)

    val in = streamNode.in.head._1
    val out = streamNode.out.head._1

    val descriptorWidth: Int = TrackingBundle(trackingParams).getWidth
    require(descriptorWidth <= in.params.n * 8, "Streaming interface too small")

    val tracking = Module(new TrackingTop(trackingParams))

    tracking.io.in.bits := in.bits.data.asTypeOf(new TrackingBundle(trackingParams))
    out.bits.data := tracking.io.out.bits;

    tracking.io.out.ready := out.ready
    out.valid := tracking.io.out.valid
  }
}

/**
  * TLDspBlock specialization of TrackingBlock
  * @param trackingParams parameters for tracking
  * @param ev$1
  * @param ev$2
  * @param ev$3
  * @param p
  * @tparam T Type parameter for tracking data type
  */
class TLTrackingBlock(
  trackingParams: TrackingTopParams
)(implicit p: Parameters) extends
  TrackingBlock[TLClientPortParameters, TLManagerPortParameters, TLEdgeOut, TLEdgeIn, TLBundle](trackingParams)
  with TLDspBlock

/**
  * TLChain is the "right way" to do this, but the dspblocks library seems to be broken.
  * In the interim, this should work.
  * @param trackingParams parameters for tracking
  * @param depth depth of queues
  * @param ev$1
  * @param ev$2
  * @param ev$3
  * @param p
  * @tparam T Type parameter for tracking, i.e. FixedPoint or DspReal
  */
class TrackingThing
(
  val trackingParams: TrackingTopParams,
  val depth: Int = 8,
)(implicit p: Parameters) extends LazyModule {
  // instantiate lazy modules
  val tracking = LazyModule(new TLTrackingBlock(trackingParams))
  val readQueue = LazyModule(new TLReadQueue(depth))

  // connect streamNodes of queues and tracking
  readQueue.streamNode := tracking.streamNode 

  lazy val module = new LazyModuleImp(this)
}
