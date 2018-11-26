package gps

import chisel3._
import chisel3.experimental.FixedPoint
import chisel3.util._

import dsptools.numbers._

trait LoopParams[T <: Data] {
  val codeNcoWidth: Int
  val carrierNcoWidth: Int
  val inputWidth: Int
  val lfParamsCostas: LoopFilter3rdParams[T]
  val lfParamsDLL: LoopFilterParams[T]
}

case class ExampleLoopParams(
) extends LoopParams[FixedPoint] {
  val codeNcoWidth = 32
  val carrierNcoWidth = 32
  val inputWidth = 32

  //FIXME: widths may not be correct for costas loop filter 
  val lfParamsCostas = FixedFilter3rdParams(width = 20, BPWidth = 16)   
  val lfParamsDLL = FixedFilterParams(6000, 5, 1) 
} 

class LoopBundle[T <: Data](params: LoopParams[T], discParams: AllDiscParams[T]) extends Bundle {
  val ie = Input(SInt(params.inputWidth.W))
  val ip = Input(SInt(params.inputWidth.W))
  val il = Input(SInt(params.inputWidth.W))
  val qe = Input(SInt(params.inputWidth.W))
  val qp = Input(SInt(params.inputWidth.W))
  val ql = Input(SInt(params.inputWidth.W))
  val inValid = Input(Bool())
  val intTime = Input(params.lfParamsCostas.proto.cloneType)
  // FIXME: type for freqBias may not be correct
  val costasFreqBias = Input(params.lfParamsCostas.proto.cloneType)
  val dllFreqBias = Input(params.lfParamsDLL.proto.cloneType)

  val codeNco = Output(UInt(params.codeNcoWidth.W))
  val code2xNco = Output(UInt(params.codeNcoWidth.W))
  val carrierNco = Output(UInt(params.carrierNcoWidth.W)) 
  val outValid = Output(Bool())
  val dllErrRegOut = Output(SInt(discParams.dllDisc.outWidth.W))
  val phaseErrRegOut = Output(SInt(discParams.phaseDisc.outWidth.W))
  val freqErrRegOut = Output(SInt(discParams.freqDisc.outWidth.W))

  override def cloneType: this.type = LoopBundle(params, discParams).asInstanceOf[this.type]
}

object LoopBundle {
  def apply[T <: Data](params:LoopParams[T], discParams:AllDiscParams[T]): LoopBundle[T] = new LoopBundle(params, discParams)
}

class LoopMachine[T <: Data : Real : BinaryRepresentation](val loopParams: LoopParams[T], val discParams: AllDiscParams[T]) extends Module {
  val io = IO(LoopBundle(loopParams, discParams))
   
  //FIXME: Fix inputs to the loop filter
  val lfCostas = Module(new LoopFilter3rd(loopParams.lfParamsCostas))
  val lfDLL = Module(new LoopFilter(loopParams.lfParamsDLL))

  // Discriminator Setup  
  val freqDisc = Module(new FreqDiscriminator(discParams.freqDisc))
  val phaseDisc = Module(new PhaseDiscriminator(discParams.phaseDisc))
  val dllDisc = Module(new DllDiscriminator(discParams.dllDisc)) 

  val phaseErrReg = Reg(SInt(discParams.phaseDisc.outWidth.W))
  val freqErrReg = Reg(SInt(discParams.freqDisc.outWidth.W))

  var freqUpdate = false
  
  // Costas Loop  
  phaseDisc.io.in.bits.ips := io.ip 
  phaseDisc.io.in.bits.qps := io.qp
  freqDisc.io.in.bits.ips := io.ip
  freqDisc.io.in.bits.qps := io.qp
  dllDisc.io.ipsE := io.ie
  dllDisc.io.qpsE := io.qe
  dllDisc.io.ipsL := io.il
  dllDisc.io.qpsL := io.ql

  val phaseErr = phaseDisc.io.out   
  val freqErr = freqDisc.io.out

  when (phaseDisc.io.out.valid) {
    phaseErrReg := phaseErr
  }

  when (freqDisc.io.out.valid) {
    if (freqUpdate) {
      freqErrReg := freqErr
      freqUpdate = false
    } else {
      freqUpdate = true
    }
  }

  lfCostas.io.freqErr := freqErrReg
  lfCostas.io.phaseErr := phaseErrReg
  lfCostas.io.valid := freqDisc.io.out.valid && phaseDisc.io.out.valid
  lfCostas.io.intTime := io.intTime

  val codeCoeff = ConvertableTo[T].fromDouble(1/((2*math.Pi) * (16*1023*1e3) * (math.pow(2, 30) - 1)))   
  
  io.phaseErrRegOut := phaseErrReg
  io.freqErrRegOut := freqErrReg

  io.codeNco := lfCostas.io.out * codeCoeff + io.costasFreqBias 
  io.code2xNco := 2*io.codeNco


  // DLL
  val dllErrReg = Reg(SInt(discParams.dllDisc.outWidth.W))
  val dllErr = dllDisc.io.out
  
  when (dllDisc.io.outValid) {
    dllErrReg := dllErr 
  }

  lfDLL.io.in := dllErrReg
  lfDLL.io.valid := dllDisc.io.outValid

  io.dllErrRegOut := dllErrReg
   
  io.carrierNco := lfDLL.io.out + io.dllFreqBias
    
    
} 

