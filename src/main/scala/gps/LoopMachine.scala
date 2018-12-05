package gps

import chisel3._
import chisel3.experimental.FixedPoint
import chisel3.util._

import dsptools.numbers._

/** Loop machine parameters
 *  
 *  This is type generic
 */
trait LoopParams[T <: Data] {
  /** Input prototype */
  val protoIn: T
  /** Output prototype */
  val protoOut: T
  /** Costas loop filter parameters */
  val lfParamsCostas: LoopFilter3rdParams[T]
  /** DLL loop filter parameters */
  val lfParamsDLL: LoopFilterParams[T]
  /** Integration time */
  val intTime: Double
}

/** Fixed point loop machine example parameters
 *
 *  @param inWidth Loop machine input fixed point overall bit width
 *  @param inBP Loop machine input fixed point binary point bit width
 *  @param ncoWidth Loop machine output fixed point overall bit width
 *  @param ncoBP Loop machine output fixed point binary point bit width
 */
case class ExampleLoopParams(
  inWidth: Int = 32,
  inBP: Int = 12,
  ncoWidth: Int = 32,
  ncoBP: Int = 0,
) extends LoopParams[FixedPoint] {
  /** Integration time */
  val intTime = 0.001
  /** Input fixed point prototype */
  val protoIn = FixedPoint(inWidth.W, inBP.BP)
  /** Output fixed point prototype */
  val protoOut = FixedPoint(ncoWidth.W, ncoBP.BP)
  /** Instance of the Costas 3rd order loop filter parameters */
  val lfParamsCostas = FixedFilter3rdParams(width = 20, BPWidth = 16)   
  /** Instance of the DLL loop filter parameters */
  val lfParamsDLL = FixedFilterParams(6000, 5, 1) 
} 

/** Input loop machine bundle
 *  
 *  @param params Loop machine parameters
 *  @param discParams  
 */
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
  val dllUpdate = Bool()

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

  // Debugging purposes
  io.out.bits.dllUpdate := dllRegUpdate
  
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

