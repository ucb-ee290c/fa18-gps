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
  val freqDisc: DiscParams[T]
  val phaseDisc: DiscParams[T]
  val dllDisc: DiscParams[T]
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
  val lfParamsCostas = FixedFilter3rdParams(width = 20, bPWidth = 16)   
  val lfParamsDLL = FixedFilterParams(6000, 5, 1) 
  val freqDisc = FixedDiscParams(inWidth, inBP, ncoWidth, ncoBP, calAtan2=true)
  val phaseDisc = FixedDiscParams(inWidth, inBP, ncoWidth, ncoBP)
  val dllDisc =  FixedDiscParams(inWidth, inBP, ncoWidth, ncoBP, dividing=true)
} 

class LoopInputBundle[T <: Data](protoIn: T, protoOut: T) extends Bundle {
  val epl = EPLBundle(protoIn)
//  val ie: T = protoIn.cloneType
//  val ip: T = protoIn.cloneType
//  val il: T = protoIn.cloneType 
//  val qe: T = protoIn.cloneType
//  val qp: T = protoIn.cloneType
//  val ql: T = protoIn.cloneType 
  val costasFreqBias: T = protoOut.cloneType
  val dllFreqBias: T = protoOut.cloneType

  override def cloneType: this.type = LoopInputBundle(protoIn, protoOut).asInstanceOf[this.type]
}
object LoopInputBundle {
  def apply[T <: Data](protoIn: T, protoOut: T): LoopInputBundle[T] = 
    new LoopInputBundle(protoIn, protoOut)
}

class LoopOutputBundle[T <: Data](params: LoopParams[T]) extends Bundle {
  val codeNco = params.protoOut.cloneType
  val code2xNco = params.protoOut.cloneType
  val carrierNco = params.protoOut.cloneType
  val dllErrRegOut = params.protoOut.cloneType 
  val phaseErrRegOut = params.protoOut.cloneType
  val freqErrRegOut = params.protoOut.cloneType 

  override def cloneType: this.type = LoopOutputBundle(params).asInstanceOf[this.type]
}

object LoopOutputBundle {
  def apply[T <: Data](params: LoopParams[T]): LoopOutputBundle[T] = 
    new LoopOutputBundle(params)
}

class LoopBundle[T <: Data](params: LoopParams[T]) extends Bundle {
  val in = Flipped(Decoupled(LoopInputBundle(params.protoIn, params.protoOut)))
  val out = Decoupled(LoopOutputBundle(params))

  override def cloneType: this.type = LoopBundle(params).asInstanceOf[this.type]
}
object LoopBundle {
  def apply[T <: Data](params:LoopParams[T]): LoopBundle[T] = new LoopBundle(params)
}

class LoopMachine[T <: Data : Real : BinaryRepresentation](
  val loopParams: LoopParams[T], 
) extends Module {
  val io = IO(LoopBundle(loopParams))
   
  //FIXME: Fix inputs to the loop filter
  val lfCostas = Module(new LoopFilter3rd(loopParams.lfParamsCostas))
  val lfDLL = Module(new LoopFilter(loopParams.lfParamsDLL))

  // Discriminator Setup  
  val freqDisc = Module(new FreqDiscriminator(loopParams.freqDisc))
  val phaseDisc = Module(new PhaseDiscriminator(loopParams.phaseDisc))
  val dllDisc = Module(new DllDiscriminator(loopParams.dllDisc)) 

  val s_init :: s_cordic :: s_lf :: s_done :: nil = Enum(4)
  val state = RegInit(s_init) 

  val phaseRegUpdate = RegInit(false.B)
  val freqRegUpdate = RegInit(false.B)
  val dllRegUpdate = RegInit(false.B)

  val phaseErrReg = Reg(loopParams.protoOut.cloneType)
  val freqErrReg = Reg(loopParams.protoOut.cloneType)
  val dllErrReg = Reg(loopParams.protoOut.cloneType)

  val lfDllOut = Reg(loopParams.protoOut.cloneType)
  val lfCostasOut = Reg(loopParams.protoOut.cloneType)

  // Costas Loop  
  lfCostas.io.intTime := ConvertableTo[T].fromDouble(loopParams.intTime)
  phaseDisc.io.in.bits.ips := io.in.bits.epl.ip 
  phaseDisc.io.in.bits.qps := io.in.bits.epl.qp
  freqDisc.io.in.bits.ips := io.in.bits.epl.ip
  freqDisc.io.in.bits.qps := io.in.bits.epl.qp
  dllDisc.io.in.bits.ipsE := io.in.bits.epl.ie
  dllDisc.io.in.bits.qpsE := io.in.bits.epl.qe
  dllDisc.io.in.bits.ipsL := io.in.bits.epl.il
  dllDisc.io.in.bits.qpsL := io.in.bits.epl.ql

  val phaseErr = -phaseDisc.io.out.bits.output   
  val freqErr = ConvertableTo[T].fromDouble(1/loopParams.intTime) * freqDisc.io.out.bits.output
  val dllErr = dllDisc.io.out.bits.output

  phaseErrReg := phaseErrReg
  freqErrReg := freqErrReg
  dllErrReg := dllErrReg

  val dllDiscInValid = RegInit(false.B)
  val phaseDiscInValid = RegInit(false.B)
  val freqDiscInValid = RegInit(false.B)

  phaseDisc.io.in.valid := phaseDiscInValid
  freqDisc.io.in.valid := freqDiscInValid
  dllDisc.io.in.valid := dllDiscInValid

  phaseDiscInValid := phaseDiscInValid
  freqDiscInValid := freqDiscInValid
  dllDiscInValid := dllDiscInValid 

  phaseDisc.io.out.ready := false.B
  freqDisc.io.out.ready := false.B
  dllDisc.io.out.ready := false.B

  phaseRegUpdate := phaseRegUpdate
  freqRegUpdate := freqRegUpdate
  dllRegUpdate := dllRegUpdate

  lfCostas.io.valid := false.B
  lfDLL.io.valid := false.B
  lfDllOut := lfDllOut
  lfCostasOut := lfCostasOut

  when (state === s_init) {
    io.in.ready := true.B
    io.out.valid := false.B 
    state := s_init

    when (io.in.fire()) {
      state := s_cordic

      phaseDiscInValid := true.B
      freqDiscInValid := true.B
      dllDiscInValid := true.B
    }
  } .elsewhen (state === s_cordic) {
    io.in.ready := false.B
    io.out.valid := false.B
    state := s_cordic
    

    phaseDisc.io.out.ready := true.B
    freqDisc.io.out.ready := true.B
    dllDisc.io.out.ready := true.B

    when (phaseRegUpdate && freqRegUpdate && dllRegUpdate) {
      state := s_lf
    }
  } .elsewhen (state === s_lf) {
    io.in.ready := false.B
    io.out.valid := false.B
    state := s_done

    lfCostas.io.valid := true.B
    lfDLL.io.valid := true.B
    
    lfDllOut := lfDLL.io.out
    lfCostasOut := lfCostas.io.out

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
    phaseDiscInValid := false.B
  }

  when (freqDisc.io.out.fire()) {
    freqErrReg := freqErr
    freqRegUpdate := true.B
    freqDiscInValid := false.B
  }

  lfCostas.io.freqErr := freqErrReg
  lfCostas.io.phaseErr := phaseErrReg

  val codeCoeff = ConvertableTo[T].fromDouble(1/((2*math.Pi) * (16*1023*1e3)) * (math.pow(2, 30) - 1)) 
  
  io.out.bits.phaseErrRegOut := phaseErrReg
  io.out.bits.freqErrRegOut := freqErrReg

  io.out.bits.carrierNco := lfCostasOut * codeCoeff + io.in.bits.costasFreqBias 


  // DLL
  when (dllDisc.io.out.fire()) {
    dllErrReg := dllErr 
    dllRegUpdate := true.B
    dllDiscInValid := false.B
  }

  lfDLL.io.in := dllErrReg

  io.out.bits.dllErrRegOut := dllErrReg
   
  io.out.bits.codeNco := lfDllOut + io.in.bits.dllFreqBias
  io.out.bits.code2xNco := 2*io.out.bits.codeNco
} 

