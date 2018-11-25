package gps

import chisel3._
import chisel3.experimental.FixedPoint
import chisel3.util._

import dsptools.numbers._

trait AllDiscParams [T <: Data] {
  val phaseDisc : DiscParams[T]
  val freqDisc : DiscParams[T]
  val dllDisc : DiscParams[T]
}

case class ExampleAllDiscParams(
) extends AllDiscParams[FixedPoint] {
  val phaseDisc = FixedDiscParams(32, 32)
  val freqDisc = FixedDiscParams(32, 32)
  val dllDisc = FixedDiscParams(32, 32)
}

// Discriminator code 
trait DiscParams [T <: Data]{
  val inWidth: Int
  val outWidth: Int
  val cordicParams: CordicParams[T]
}

case class FixedDiscParams(
  val inWidth: Int,
  val outWidth: Int,
) extends DiscParams[FixedPoint] {
  val cordicParams = FixedCordicParams(xyWidth = inWidth, xyBPWidth = 2, zWidth=outWidth, zBPWidth= 2, nStages=2) 
}

class CostasDiscBundle[T <: Data](params: DiscParams[T]) extends Bundle { 
  val ips = Input(SInt(params.inWidth.W))
  val qps = Input(SInt(params.inWidth.W))

  // FIXME: Incorrect output width. What should it be?
  val out = Output(UInt(params.outWidth.W))  
  val outValid = Output(Bool())
  override def cloneType: this.type = CostasDiscBundle(params).asInstanceOf[this.type]
} 

object CostasDiscBundle {
  def apply[T <: Data](params:DiscParams[T]): CostasDiscBundle[T] = new CostasDiscBundle(params)
}

class PhaseDiscriminator[T <: Data : Real : BinaryRepresentation](val params: DiscParams[T]) extends Module {
  val io = IO(CostasDiscBundle(params))

  val cordicCostas = Module(new FixedIterativeCordic(params.cordicParams))
  when(io.ips >= 0.S) {
    cordicCostas.io.in.bits.x := io.ips
    cordicCostas.io.in.bits.y := io.qps
  } .otherwise {
    cordicCostas.io.in.bits.x := -1.S(params.inWidth.W) * io.ips
    cordicCostas.io.in.bits.y := -1.S(params.inWidth.W) * io.qps
  }
  cordicCostas.io.in.valid := true.B
  
  io.out := cordicCostas.io.out.bits.z    
  io.outValid := cordicCostas.io.out.fire

}

class FreqDiscriminator[T <: Data : Real : BinaryRepresentation](val params: DiscParams[T]) extends Module {
  val io = IO(new CostasDiscBundle(params))

  val ipsPrev = RegInit(SInt(params.inWidth.W), 0.S)
  val qpsPrev = RegInit(SInt(params.inWidth.W), 0.S)

  // FIXME: Later use ONE cordic for both phase and freq Discriminator
  val cordicCostas = Module(new FixedIterativeCordic(params.cordicParams))
  
  val dot = io.ips * qpsPrev - ipsPrev * io.qps
  val cross = io.ips * ipsPrev + io.qps * qpsPrev

  cordicCostas.io.in.bits.x := dot
  cordicCostas.io.in.bits.y := cross
  cordicCostas.io.in.valid := true.B
  // TODO: Compensate for the 1/timeStep later
  io.out := cordicCostas.io.out.bits.z 
  io.outValid := cordicCostas.io.out.valid

}

class DllDiscBundle[T <: Data](params: DiscParams[T]) extends Bundle {
  val ipsE = Input(SInt(params.inWidth.W))
  val qpsE = Input(SInt(params.inWidth.W))

  val ipsL = Input(SInt(params.inWidth.W))
  val qpsL = Input(SInt(params.inWidth.W))

  // FIXME: Incorrect output width. What should it be?
  val out = Output(SInt(params.outWidth.W))  
  val outValid = Output(Bool())

  override def cloneType: this.type = DllDiscBundle(params).asInstanceOf[this.type]
} 

object DllDiscBundle {
  def apply[T <: Data](params:DiscParams[T]): DllDiscBundle[T] = new DllDiscBundle(params)
}

class DllDiscriminator[T <: Data : Real : BinaryRepresentation](val params: DiscParams[T]) extends Module {
  val io = IO(DllDiscBundle(params))

  val e = io.ipsE*io.ipsE + io.qpsE*io.qpsE
  val l = io.ipsL*io.ipsL + io.qpsL*io.qpsL

  val cordicDLL = Module(new FixedIterativeCordic(params.cordicParams))
    
  cordicDLL.io.in.bits.x := (e - l)
  cordicDLL.io.in.bits.y := (e + l) 
  cordicDLL.io.in.bits.z := ConvertableTo[T].fromDouble(0)
  cordicDLL.io.in.valid := true.B

  when (e === 0.S || l === 0.S) {
    io.out := 0.U
  } .otherwise {
    io.out := cordicDLL.io.out.bits.z * ConvertableTo[T].fromDouble(-1.0)  
  }
  io.outValid := cordicDLL.io.out.valid
}

