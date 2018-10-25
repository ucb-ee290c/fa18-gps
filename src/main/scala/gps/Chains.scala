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

class FFTThing
(
  val depth: Int = 8,
)(implicit p: Parameters) extends LazyModule {
  // instantiate lazy modules
  val writeQueue = LazyModule(new TLWriteQueue(depth))
  val fftConfig = FFTConfig(
        genIn = DspComplex(FixedPoint(12.W, 7.BP), FixedPoint(12.W, 7.BP)),
        genOut = DspComplex(FixedPoint(18.W, 6.BP), FixedPoint(18.W, 6.BP)),
        n = 4,
        lanes = 4,
        pipelineDepth = 0,
        quadrature = true,
	)
  val fft = LazyModule(new FFTBlock(fftConfig))
  val readQueue = LazyModule(new TLReadQueue(depth))

  // connect streamNodes of queues and cordic
  readQueue.streamNode := fft.streamNode := writeQueue.streamNode

  lazy val module = new LazyModuleImp(this)
}
