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
  val freqDisc: DiscParams[T]
  val phaseDisc: DiscParams[T]
  val dllDisc: DiscParams[T]
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
  val lfParamsCostas = FixedFilter3rdParams(width = 20, bPWidth = 16)   
  /** Instance of the DLL loop filter parameters */
  val lfParamsDLL = FixedFilterParams(6000, 5, 1) 
  val freqDisc = FixedDiscParams(inWidth, inBP, ncoWidth, (ncoWidth-3), calAtan2=true)
  val phaseDisc = FixedDiscParams(inWidth, inBP, ncoWidth, (ncoWidth-3))
  val dllDisc =  FixedDiscParams(inWidth, inBP, ncoWidth, (ncoWidth-3), dividing=true)
} 

// TODO Remove this
class LoopInputBundle[T <: Data](protoIn: T, protoOut: T) extends Bundle {
  val epl = EPLBundle(protoIn)

  override def cloneType: this.type = LoopInputBundle(protoIn, protoOut).asInstanceOf[this.type]
}
object LoopInputBundle {
  def apply[T <: Data](protoIn: T, protoOut: T): LoopInputBundle[T] = 
    new LoopInputBundle(protoIn, protoOut)
}

class LoopOutputBundle[T <: Data](params: LoopParams[T]) extends Bundle {
  val codeNco = params.protoOut.cloneType
  val carrierNco = params.protoOut.cloneType
  val dllErrOut = params.protoOut.cloneType 
  val phaseErrOut = params.protoOut.cloneType
  val freqErrOut = params.protoOut.cloneType 

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

  /** Costas loop input connections */ 
  lfCostas.io.intTime := ConvertableTo[T].fromDouble(loopParams.intTime)
  phaseDisc.io.in.bits.ips := io.in.bits.epl.ip 
  phaseDisc.io.in.bits.qps := io.in.bits.epl.qp
  freqDisc.io.in.bits.ips := io.in.bits.epl.ip
  freqDisc.io.in.bits.qps := io.in.bits.epl.qp
  dllDisc.io.in.bits.ipsE := io.in.bits.epl.ie
  dllDisc.io.in.bits.qpsE := io.in.bits.epl.qe
  dllDisc.io.in.bits.ipsL := io.in.bits.epl.il
  dllDisc.io.in.bits.qpsL := io.in.bits.epl.ql

  /** Phase error calculations */
  val phaseErr = -phaseDisc.io.out.bits.output   
  /** Frequency error calculations */ 
  val freqErr = freqDisc.io.out.bits.output
  /** DLL error calculations */
  val dllErr = dllDisc.io.out.bits.output

  phaseErrReg := phaseErrReg
  freqErrReg := freqErrReg
  dllErrReg := dllErrReg

  /** Boolean register that determines if the DLL discriminator input signal is valid */
  val dllDiscInValid = RegInit(false.B)
  /** Boolean register that determines if the Costas phase discriminator input signal is valid */
  val phaseDiscInValid = RegInit(false.B)
  /** Boolean register that determines if the Costas frequency discriminator input signal is valid */
  val freqDiscInValid = RegInit(false.B)

  /** Connection between the input valid signals of all discriminators to their respective registers */
  phaseDisc.io.in.valid := phaseDiscInValid
  freqDisc.io.in.valid := freqDiscInValid
  dllDisc.io.in.valid := dllDiscInValid

  /** By default the input valid registers set to themselves unless otherwise changed below */
  phaseDiscInValid := phaseDiscInValid
  freqDiscInValid := freqDiscInValid
  dllDiscInValid := dllDiscInValid 

  /** By default the output ready signals for all three discriminators get set to false */
  phaseDisc.io.out.ready := false.B
  freqDisc.io.out.ready := false.B
  dllDisc.io.out.ready := false.B

  /** By default the discriminator output valid register flags get set to themselves */
  phaseRegUpdate := phaseRegUpdate
  freqRegUpdate := freqRegUpdate
  dllRegUpdate := dllRegUpdate

  /** Initialization of loop filter output connections */
  lfCostas.io.valid := false.B
  lfDLL.io.valid := false.B
  lfDllOut := lfDllOut
  lfCostasOut := lfCostasOut

  when (state === s_init) {
    /** Initial State */
    io.in.ready := true.B
    io.out.valid := false.B 
    state := s_init

    /** When the input values are ready and valid, move to the discriminator calculation state */
    when (io.in.fire()) {
      state := s_cordic

      phaseDiscInValid := true.B
      freqDiscInValid := true.B
      dllDiscInValid := true.B
    }
  } .elsewhen (state === s_cordic) {
    /** Cordic and discriminator calculation state */
    io.in.ready := false.B
    io.out.valid := false.B
    state := s_cordic
    

    phaseDisc.io.out.ready := true.B
    freqDisc.io.out.ready := true.B
    dllDisc.io.out.ready := true.B
    
    /** When all discriminator output values have been updated, move to the loop filter calculation state */
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

  val codeCoeff = loopParams.protoOut.fromDouble(1/((2*math.Pi) * (16367600)) * (math.pow(2, 30) - 1)) 
  
  io.out.bits.phaseErrOut := phaseErrReg
  io.out.bits.freqErrOut := freqErrReg

  io.out.bits.carrierNco := lfCostasOut * codeCoeff


  // DLL
  when (dllDisc.io.out.fire()) {
    dllErrReg := dllErr 
    dllRegUpdate := true.B
    dllDiscInValid := false.B
  }

  lfDLL.io.in := dllErrReg

  io.out.bits.dllErrOut := dllErrReg
   
  io.out.bits.codeNco := lfDllOut
} 

