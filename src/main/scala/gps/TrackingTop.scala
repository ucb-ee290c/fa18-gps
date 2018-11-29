package gps

import chisel3._
import chisel3.experimental.FixedPoint
import chisel3.util._
import dsptools.numbers._
import scala.math.pow

case class TrackingTopParams(
  adcWidth: Int,       // Width of the ADC Input
  sampleRate: Double,  // Sample rate of the input data
  intBP: Int,
  ncoBP: Int
) extends TrackingChannelParams[SInt] with LoopParams[FixedPoint] {
  // Width of the Integrators
  // 0.02 is the maximum integration time, sizing things to prevent overflow,
  // +1 for signed
  val intWidth = log2Ceil(pow(2, adcWidth - 1).toInt*(sampleRate * 0.02).toInt) + 1
  // TODO Can we come up with some logical reasoning for this? 
  val ncoWidth = 20
 
  // Loop Filters and discriminator parameters
  // FIXME this should take a list of integration times and generate LUTs for
  // loop parameter values
  val intTime = 0.001
  // FIXME: widths may not be correct for costas loop filter 
  val protoIn = FixedPoint((intWidth + intBP).W, intBP.BP)
  val protoOut = FixedPoint((ncoWidth + ncoBP).W, ncoBP.BP)
  val lfParamsCostas = FixedFilter3rdParams(width = 20, bPWidth = 16)   
  // TODO Is there a way we can generate this? 
  val lfParamsDLL = FixedFilterParams(6000, 5, 1) 
  val phaseDisc = FixedDiscParams(
    inWidth = (intWidth + intBP), 
    inBP = intBP, 
    outWidth = (ncoWidth + ncoBP), 
    outBP = ncoBP)
  val freqDisc = phaseDisc.copy(calAtan2=true)
  val dllDisc = phaseDisc.copy(dividing=true)
  
  // Tracking Channel stuff
  // Carrier NCO is ncoWidth wide count, adcWidth out and has a sin output
  val carrierNcoParams = SIntNcoParams(ncoWidth, adcWidth, true)
  // Ca NCO is ncoWidth bits wide and has no sin output, 1 bit output
  val caNcoParams = SIntNcoParams(ncoWidth, 1, false)
  // Ca NCO 2x is ncoWidth - 1 bits wide to create double the frequency
  val ca2xNcoParams = SIntNcoParams(ncoWidth  - 1, 1, false)
  // Ca Params, 1 bit input from caNCO and 2 bits out to represent 1 and -1
  val caParams = CAParams(1, 2) 
  // Multipliers are 5 bit in and 5 bit out
  val mulParams = SampledMulParams(adcWidth)
  // Int Params 
  val intParams = SampledIntDumpParams(adcWidth, (sampleRate * 0.02).toInt)
  // Phase Lock Detector Params Limit set to +-30deg
  val phaseLockParams = LockDetectParams(FixedPoint(20.W, 12.BP), -0.54, 0.54,100)
}

class TrackingTop(params: TrackingTopParams) extends Module {
  val io = IO(new Bundle{
    val adcIn = Input(SInt(params.adcWidth.W))
    val epl = Output(EPLBundle(SInt(params.intWidth.W)))
    val dllErr = Output(params.protoOut)
    val freqErr = Output(params.protoOut)
    val phaseErr = Output(params.protoOut)
    val svNumber = Input(UInt(6.W))
    val carrierNcoBias = Input(UInt(params.ncoWidth))
    val codeNcoBias = Input(UInt(params.ncoWidth))
    val dump = Output(Bool())
  })
  val trackingChannel = Module(new TrackingChannel(params))
  trackingChannel.io.adcSample := io.adcIn
  trackingChannel.io.svNumber := io.svNumber
  
  val eplReg = Reg(EPLBundle(SInt(params.intWidth.W)))
  io.epl := eplReg
  val loopFilters = Module(new LoopMachine(params))
  loopFilters.io.in.bits.epl.ie := eplReg.ie.asFixed
  loopFilters.io.in.bits.epl.ip := eplReg.ip.asFixed
  loopFilters.io.in.bits.epl.il := eplReg.il.asFixed
  loopFilters.io.in.bits.epl.qe := eplReg.qe.asFixed
  loopFilters.io.in.bits.epl.qp := eplReg.qp.asFixed
  loopFilters.io.in.bits.epl.ql := eplReg.ql.asFixed
 
  val loopValues = Reg(LoopOutputBundle(params)) 
  val stagedValues = Reg(LoopOutputBundle(params)) 
  trackingChannel.io.dllIn := loopValues.codeNco.asUInt + io.carrierNcoBias.asUInt
  trackingChannel.io.costasIn := loopValues.carrierNco.asUInt + io.codeNcoBias
  trackingChannel.io.dump := false.B
  io.dllErr := loopValues.dllErrOut
  io.freqErr := loopValues.freqErrOut
  io.phaseErr := loopValues.phaseErrOut

  val state = Reg(UInt(2.W))
  val sIdle :: sUpdate :: sWait :: Nil = Enum(3)

  when (trackingChannel.io.caIndex === 1023.U) {
    loopValues := stagedValues
    eplReg := trackingChannel.io.toLoop
    io.dump := true.B
    trackingChannel.io.dump := false.B
  }.otherwise {
    loopValues := loopValues
    eplReg := eplReg
    io.dump := false.B
  }

  state := state
  loopFilters.io.in.valid := false.B
  loopFilters.io.out.ready := false.B
  stagedValues := stagedValues
  when (state === sIdle) {
    when (trackingChannel.io.caIndex === 1023.U) {
      state := sUpdate
    }
  }.elsewhen (state === sUpdate) {
    loopFilters.io.in.valid := true.B
    when (loopFilters.io.in.fire()) {
      state := sWait
    }
  }.elsewhen (state === sWait) {
    loopFilters.io.out.ready := true.B
    when (loopFilters.io.out.fire()) {
      stagedValues := loopFilters.io.out.bits
      state := sIdle
    }
  }
}
