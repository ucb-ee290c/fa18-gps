package gps

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

class AcqThing
(
  // TODO: This parameter define the depth of Read/Write Queue, modify it if you need queue and want to change depth
  // val depth: Int = 8,
)(implicit p: Parameters) extends LazyModule {
  // instantiate lazy modules

  // TODO: need queues?
  // val writeQueue = LazyModule(new TLWriteQueue(depth))

  val acqConfig = AcqConfig(
      // TODO: your configurations
	)

  val acq = LazyModule(new AcqBlock(acqConfig))
  // val readQueue = LazyModule(new TLReadQueue(depth))

  // connect streamNodes of queues and acqCtrl

  // TODO: make connections you need, example:
  // readQueue.streamNode := acq.streamNode := writeQueue.streamNode


  lazy val module = new LazyModuleImp(this)
}
