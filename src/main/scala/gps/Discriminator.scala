package gps

import chisel3._
import chisel3.experimental.FixedPoint
import chisel3.util._

import dsptools.numbers._

trait AllDiscParams[T <: Data] {
  val phaseDisc : DiscParams[T]
  val freqDisc : DiscParams[T]
  val dllDisc : DiscParams[T]
}

case class ExampleAllDiscParams(
) extends AllDiscParams[FixedPoint] {
  val phaseDisc = FixedDiscParams()
  val freqDisc = FixedDiscParams()
  val dllDisc = FixedDiscParams()
}

// Discriminator code 
trait DiscParams [T <: Data]{
  val cordicParams: CordicParams[T]
  val protoIn : T
  val protoOut: T
}

case class RealDiscParams(
  inWidth: Int = 32,
  outWidth: Int = 32,
  cordicParams: CordicParams[DspReal] = RealCordicParams(),
) extends DiscParams[DspReal] {
  val protoIn = DspReal()
  val protoOut = DspReal()
}

case class FixedDiscParams(
  val inWidth: Int = 32,
  val inBP: Int = 12, 
  val outWidth: Int = 32,
  val outBP: Int = 12, 
  val calAtan2: Boolean = false,
  val dividing: Boolean = false
) extends DiscParams[FixedPoint] {
  val cordicParams = FixedCordicParams(
    xyWidth = inWidth, 
    xyBPWidth = inBP, 
    zWidth = outWidth, 
    zBPWidth = outBP, 
    nStages = 50,
    calAtan2 = calAtan2,
    dividing = dividing) 
  val protoIn = FixedPoint(inWidth.W, inBP.BP)
  val protoOut = FixedPoint(outWidth.W, outBP.BP)
  print("Disc Params OutBP: ")
  print(outBP)
  println()
}
class CostasDiscInputBundle[T <: Data](params: DiscParams[T]) extends Bundle { 
  val ips: T = params.protoIn.cloneType
  val qps: T = params.protoIn.cloneType
  
  override def cloneType: this.type = CostasDiscInputBundle(params).asInstanceOf[this.type]
} 

object CostasDiscInputBundle {
  def apply[T <: Data](params: DiscParams[T]): CostasDiscInputBundle[T] = new CostasDiscInputBundle(params)
}

class DiscOutputBundle[T <: Data](params: DiscParams[T]) extends Bundle { 
  val output = params.protoOut.cloneType  

  override def cloneType: this.type = DiscOutputBundle(params).asInstanceOf[this.type]
} 

object DiscOutputBundle {
  def apply[T <: Data](params: DiscParams[T]): DiscOutputBundle[T] = new DiscOutputBundle(params)
}

class CostasDiscBundle[T <: Data](params: DiscParams[T]) extends Bundle { 
  val in = Flipped(Decoupled(CostasDiscInputBundle(params)))
  val out = Decoupled(DiscOutputBundle(params))
    
  override def cloneType: this.type = CostasDiscBundle(params).asInstanceOf[this.type]
} 

object CostasDiscBundle {
  def apply[T <: Data](params:DiscParams[T]): CostasDiscBundle[T] = new CostasDiscBundle(params)
}

class PhaseDiscriminator[T <: Data : Real : BinaryRepresentation](val params: DiscParams[T]) extends Module {
  val io = IO(CostasDiscBundle(params))

  val cordicCostas = Module(new FixedIterativeCordic(params.cordicParams))

  cordicCostas.io.in.bits.z := ConvertableTo[T].fromDouble(0.0)
  cordicCostas.io.vectoring := true.B

  cordicCostas.io.in.bits.x := io.in.bits.ips
  cordicCostas.io.in.bits.y := io.in.bits.qps

  cordicCostas.io.in.valid := io.in.valid
  io.in.ready := cordicCostas.io.in.ready
  
  io.out.bits.output := cordicCostas.io.out.bits.z    
  io.out.valid := cordicCostas.io.out.valid
  cordicCostas.io.out.ready := io.out.ready
}

class FreqDiscriminator[T <: Data : Real : BinaryRepresentation](val params: DiscParams[T]) extends Module {
  val io = IO(new CostasDiscBundle(params))

  val s_init :: s_alg :: s_done :: nil = Enum(3)
  val state = RegInit(s_init) 

  val ipsCurr = RegInit(params.protoIn.cloneType, Ring[T].zero)
  val qpsCurr = RegInit(params.protoIn.cloneType, Ring[T].zero)

  val ipsPrev = RegInit(params.protoIn.cloneType, Ring[T].zero)
  val qpsPrev = RegInit(params.protoIn.cloneType, Ring[T].zero)

  val outputReg = RegInit(params.protoOut.cloneType, Ring[T].zero)
  // FIXME: Later use ONE cordic for both phase and freq Discriminator
  val cordicCostas = Module(new FixedIterativeCordic(params.cordicParams))
 
  val freqUpdate = RegInit(UInt(1.W), 0.U)
 
  val cross = ipsCurr * qpsPrev - ipsPrev * qpsCurr
  val dot = ipsCurr * ipsPrev + qpsCurr * qpsPrev

  cordicCostas.io.vectoring := true.B
  cordicCostas.io.out.ready := true.B
  cordicCostas.io.in.valid := false.B

  freqUpdate := freqUpdate
  ipsCurr := ipsCurr
  qpsCurr := qpsCurr
  ipsPrev := ipsPrev
  qpsPrev := qpsPrev

  when (state === s_init) {
    io.in.ready := true.B
    io.out.valid := false.B

    when (io.in.fire()) {
      state := s_alg
    
      ipsCurr := io.in.bits.ips
      qpsCurr := io.in.bits.qps 
    } .otherwise {
      state := s_init
    }
  } .elsewhen (state === s_alg) {
    io.in.ready := false.B
    io.out.valid := false.B   

    cordicCostas.io.in.valid := true.B
    
    when (cordicCostas.io.out.fire()) {
      state := s_done
    }  
  } .otherwise {
    io.in.ready := false.B
    io.out.valid := true.B
    ipsPrev := ipsCurr
    qpsPrev := qpsCurr
    state := s_init
    freqUpdate := freqUpdate + 1
  }
  cordicCostas.io.in.bits.x := dot
  cordicCostas.io.in.bits.y := cross
  cordicCostas.io.in.bits.z := ConvertableTo[T].fromDouble(0.0)

  // TODO: Compensate for the 1/timeStep later
  outputReg := Mux(freqUpdate === 0.U, outputReg, cordicCostas.io.out.bits.z)
  io.out.bits.output := outputReg

}

class DllDiscInputBundle[T <: Data](params: DiscParams[T]) extends Bundle {
  val ipsE: T = Input(params.protoIn.cloneType)
  val qpsE: T = Input(params.protoIn.cloneType)

  val ipsL: T = Input(params.protoIn.cloneType)
  val qpsL: T = Input(params.protoIn.cloneType)

  // FIXME: Incorrect output width. What should it be?

  override def cloneType: this.type = DllDiscInputBundle(params).asInstanceOf[this.type]
} 

object DllDiscInputBundle {
  def apply[T <: Data](params:DiscParams[T]): DllDiscInputBundle[T] = new DllDiscInputBundle(params)
}

class DllDiscBundle[T <: Data](params: DiscParams[T]) extends Bundle {
  val in = Flipped(Decoupled(DllDiscInputBundle(params)))
  val out = Decoupled(DiscOutputBundle(params))

  override def cloneType: this.type = DllDiscBundle(params).asInstanceOf[this.type]
} 

object DllDiscBundle {
  def apply[T <: Data](params:DiscParams[T]): DllDiscBundle[T] = new DllDiscBundle(params)
}

class DllDiscriminator[T <: Data : Real : BinaryRepresentation](val params: DiscParams[T]) extends Module {
  val io = IO(DllDiscBundle(params))

  val e = io.in.bits.ipsE*io.in.bits.ipsE + io.in.bits.qpsE*io.in.bits.qpsE
  val l = io.in.bits.ipsL*io.in.bits.ipsL + io.in.bits.qpsL*io.in.bits.qpsL

  val cordicDLL = Module(new FixedIterativeCordic(params.cordicParams))
    
  cordicDLL.io.vectoring := true.B
  cordicDLL.io.in.bits.x := (e + l)
  cordicDLL.io.in.bits.y := (e - l) 
  cordicDLL.io.in.bits.z := ConvertableTo[T].fromDouble(0)
  cordicDLL.io.in.valid := io.in.valid
  io.in.ready := cordicDLL.io.in.ready

  when (e === Ring[T].zero || l === Ring[T].zero) {
    io.out.bits.output := Ring[T].zero
  } .otherwise {
    io.out.bits.output := ConvertableTo[T].fromDouble(0.5) * cordicDLL.io.out.bits.z  
  }
  cordicDLL.io.out.ready := io.out.ready
  io.out.valid := cordicDLL.io.out.valid
}

