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
   val depth: Int = 32,
)(implicit p: Parameters) extends LazyModule {
  // instantiate lazy modules

  // TODO: need queues?
  val writeQueue = LazyModule(new TLWriteQueue(depth))

  val nHalfFreq = 20
  val freqStep = 500
  val fsample = 16367600
  val fcarrier = 4130400
  val fchip = 1023000
  val nSample = 16368
  val CPStep = 8
  val CPMin = 0
  val nCPSample = ((nSample - CPMin - 1) / CPStep).toInt + 1
//  val nCPSample = 40


  val acqConfig = EgALoopParParams(
    // TODO: your configurations
    wADC = 4,
    wCA = 4,
    wNCOTct = 4,
    wNCORes = 32,
    nSample = nSample,
    nLoop = 1,
    nFreq = 2 * nHalfFreq + 1,
    nCPSample = nCPSample,
    CPMin = CPMin,
    CPStep = CPStep,
    freqMin = fcarrier - nHalfFreq * freqStep,
    freqStep = freqStep,
    fsample = fsample,
    fchip = fchip,

	)

  val acq = LazyModule(new acqBlock[SInt](acqConfig))
  val readQueue = LazyModule(new TLReadQueue(depth))

  // connect streamNodes of queues and acqCtrl

  // TODO: make connections you need, example:
  readQueue.streamNode := acq.streamNode := writeQueue.streamNode


  lazy val module = new LazyModuleImp(this)
}
