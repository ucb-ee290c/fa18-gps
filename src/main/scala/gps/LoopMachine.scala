package gps

import chisel3._
import chisel3.experimental.FixedPoint
import chisel3.util._

import dsptools.numbers._

trait LoopParams[T <: Data] {
  val protoIn: T
  val protoOut: T
  val lfParamsCostas: LoopFilter3rdParams[T]
  val lfParamsDLL: LoopFilterParams[T]
  val intTime: Double
}

case class ExampleLoopParams(
  inWidth: Int = 32,
  inBP: Int = 12,
  ncoWidth: Int = 32,
  ncoBP: Int = 0,
) extends LoopParams[FixedPoint] {
  val intTime = 0.001
  //FIXME: widths may not be correct for costas loop filter 
  val protoIn = FixedPoint(inWidth.W, inBP.BP)
  val protoOut = FixedPoint(ncoWidth.W, ncoBP.BP)
  val lfParamsCostas = FixedFilter3rdParams(width = 20, BPWidth = 16)   
  val lfParamsDLL = FixedFilterParams(6000, 5, 1) 
} 

class LoopInputBundle[T <: Data](params: LoopParams[T], discParams: AllDiscParams[T]) extends Bundle {
  val ie: T = params.protoIn.cloneType
  val ip: T = params.protoIn.cloneType
  val il: T = params.protoIn.cloneType 
  val qe: T = params.protoIn.cloneType
  val qp: T = params.protoIn.cloneType
  val ql: T = params.protoIn.cloneType 
  // FIXME: type for freqBias may not be correct
  val costasFreqBias: T = params.lfParamsCostas.proto.cloneType
  val dllFreqBias: T = params.lfParamsDLL.proto.cloneType

  override def cloneType: this.type = LoopInputBundle(params, discParams).asInstanceOf[this.type]
}

object LoopInputBundle {
  def apply[T <: Data](params:LoopParams[T], discParams:AllDiscParams[T]): LoopInputBundle[T] = new LoopInputBundle(params, discParams)
}

class LoopOutputBundle[T <: Data](params: LoopParams[T], discParams: AllDiscParams[T]) extends Bundle {
  val codeNco = params.protoOut.cloneType
  val code2xNco = params.protoOut.cloneType
  val carrierNco = params.protoOut.cloneType
  val dllErrRegOut = params.protoOut.cloneType 
  val phaseErrRegOut = params.protoOut.cloneType
  val freqErrRegOut = params.protoOut.cloneType 

  override def cloneType: this.type = LoopOutputBundle(params, discParams).asInstanceOf[this.type]
}

object LoopOutputBundle {
  def apply[T <: Data](params:LoopParams[T], discParams:AllDiscParams[T]): LoopOutputBundle[T] = new LoopOutputBundle(params, discParams)
}

class LoopBundle[T <: Data](params: LoopParams[T], discParams: AllDiscParams[T]) extends Bundle {
  val in = Flipped(Decoupled(LoopInputBundle(params, discParams)))
  val out = Decoupled(LoopOutputBundle(params, discParams))

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

  val s_init :: s_alg :: s_done :: nil = Enum(3)
  val state = RegInit(s_init) 

  val phaseRegUpdate = RegInit(false.B)
  val freqRegUpdate = RegInit(false.B)
  val dllRegUpdate = RegInit(false.B)

  val phaseErrReg = Reg(loopParams.protoOut.cloneType)
  val freqErrReg = Reg(loopParams.protoOut.cloneType)
  val dllErrReg = Reg(loopParams.protoOut.cloneType)

  var freqUpdate = false
  
  // Costas Loop  
  lfCostas.io.intTime := ConvertableTo[T].fromDouble(loopParams.intTime)
  phaseDisc.io.in.bits.ips := io.in.bits.ip 
  phaseDisc.io.in.bits.qps := io.in.bits.qp
  freqDisc.io.in.bits.ips := io.in.bits.ip
  freqDisc.io.in.bits.qps := io.in.bits.qp
  dllDisc.io.in.bits.ipsE := io.in.bits.ie
  dllDisc.io.in.bits.qpsE := io.in.bits.qe
  dllDisc.io.in.bits.ipsL := io.in.bits.il
  dllDisc.io.in.bits.qpsL := io.in.bits.ql

  val phaseErr = phaseDisc.io.out.bits.output   
  val freqErr = freqDisc.io.out.bits.output
  val dllErr = dllDisc.io.out.bits.output

  phaseErrReg := phaseErrReg
  freqErrReg := freqErrReg
  dllErrReg := dllErrReg

  phaseDisc.io.in.valid := false.B
  freqDisc.io.in.valid := false.B
  dllDisc.io.in.valid := false.B
  phaseDisc.io.out.ready := false.B
  freqDisc.io.out.ready := false.B
  dllDisc.io.out.ready := false.B

  phaseRegUpdate := phaseRegUpdate
  freqRegUpdate := freqRegUpdate
  dllRegUpdate := dllRegUpdate


  when (state === s_init) {
    io.in.ready := true.B
    io.out.valid := false.B 
    state := s_init

    when (io.in.fire()) {
      state := s_alg
    }
  } .elsewhen (state === s_alg) {
    io.in.ready := false.B
    io.out.valid := false.B
    state := s_alg
    
    phaseDisc.io.in.valid := true.B
    freqDisc.io.in.valid := true.B
    dllDisc.io.in.valid := true.B

    phaseDisc.io.out.ready := true.B
    freqDisc.io.out.ready := true.B
    dllDisc.io.out.ready := true.B

    when (phaseRegUpdate && freqRegUpdate && dllRegUpdate) {
      state := s_done
    }
  } .otherwise {
    io.in.ready := false.B
    io.out.valid := true.B
    state := s_init

    phaseRegUpdate := false.B
    freqRegUpdate := false.B
    dllRegUpdate := false.B

  }

  when (phaseDisc.io.out.fire()) {
    phaseErrReg := phaseErr
    phaseRegUpdate := true.B
  }

  when (freqDisc.io.out.fire()) {
    freqErrReg := freqErr
    freqRegUpdate := true.B
  }

  lfCostas.io.freqErr := freqErrReg
  lfCostas.io.phaseErr := phaseErrReg
  lfCostas.io.valid := phaseRegUpdate && freqRegUpdate

  val codeCoeff = ConvertableTo[T].fromDouble(1/((2*math.Pi) * (16*1023*1e3) * (math.pow(2, 30) - 1) * loopParams.intTime)) 
  
  io.out.bits.phaseErrRegOut := phaseErrReg
  io.out.bits.freqErrRegOut := freqErrReg

  io.out.bits.codeNco := lfCostas.io.out * codeCoeff + io.in.bits.costasFreqBias 
  io.out.bits.code2xNco := 2*io.out.bits.codeNco


  // DLL
  
  when (dllDisc.io.out.fire()) {
    dllErrReg := dllErr 
    dllRegUpdate := true.B
  }

  lfDLL.io.in := dllErrReg
  lfDLL.io.valid := dllRegUpdate

  io.out.bits.dllErrRegOut := dllErrReg
   
  io.out.bits.carrierNco := lfDLL.io.out + io.in.bits.dllFreqBias
    
    
} 

