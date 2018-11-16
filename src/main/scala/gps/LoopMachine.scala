package gps

import chisel3._
import chisel3.experimental.FixedPoint
import chisel3.util._

import dsptools.numbers._

trait LoopParams[T <: Data] {
  val codeNcoWidth: Int
  val carrierNcoWidth: Int
  val inputWidth: Int
  val lfParamsCostas: LoopFilterParams[T]
  val lfParamsDLL: LoopFilterParams[T]
}

class LoopBundle[T <: Data](params: LoopParams[T]) extends Bundle {
  val codeNco = Output(UInt(params.codeNcoWidth.W))
  val code2xNco = Output(UInt(params.codeNcoWidth.W))
  val carrierNco = Output(UInt(params.carrierNcoWidth.W)) 

  val I_int = Input(UInt(params.inputWidth.W))
  val Q_int = Input(UInt(params.inputWidth.W))

}

object LoopBundle {
  def apply[T <: Data](params:LoopParams[T]): LoopBundle[T] = new LoopBundle(params)
}

class LoopMachine[T <: Data : Real : BinaryRepresentation](val loopParams: LoopParams[T], val DiscParams: AllDiscParams[T]) extends Module {
  val io = IO(new LoopBundle(loopParams))
   
    

  //FIXME: Fix inputs to the loop filter
  val lfCostas = Module(new LoopFilter(loopParams.lfParamsCostas))
  val lfDLL = Module(new LoopFilter(loopParams.lfParamsDLL))

  // Discriminator Setup  
  val freqDisc = Module(new FreqDiscriminator(DiscParams.freqDisc))
  val phaseDisc = Module(new PhaseDiscriminator(DiscParams.phaseDisc))
  val dllDisc = Module(new DllDiscriminator(DiscParams.dllDisc)) 
    
} 

trait AllDiscParams [T <: Data] {
  val phaseDisc : DiscParams[T]
  val freqDisc : DiscParams[T]
  val dllDisc : DiscParams[T]
}

// Discriminator code 
trait DiscParams [T <: Data]{
  val inWidth: Int
  val outWidth: Int
  val cordicParams: CordicParams[T]
}

class CostasDiscBundle[T <: Data](params: DiscParams[T]) {
  val ips = Input(SInt(params.inWidth.W))
  val qps = Input(SInt(params.inWidth.W))

  // FIXME: Incorrect output width. What should it be?
  val out = Output(UInt(params.outWidth.W))  
  val outValid = Output(Bool())
} 

object CostasDiscBundle {
  def apply[T <: Data](params:DiscParams[T]): CostasDiscBundle[T] = new CostasDiscBundle(params)
}

class PhaseDiscriminator[T <: Data : Real](val params: DiscParams[T]) extends Module {
  val io = IO(new CostasDiscBundle(params))

  val cordicCostas = Module(new Cordic1Cycle(params.cordicParams))
  .when(io.ips >= 0) {
    cordicCostas.io.in.x := io.ips
    cordicCostas.io.in.y := io.qps
  } .otherwise {
    cordicCostas.io.in.x := -1.S(params.inWidth.W) * io.ips
    cordicCostas.io.in.y := -1.S(params.inWidth.W) * io.qps
  }
  cordicCostas.io.in.valid := true.B
  
  io.out := cordicCostas.io.out.z    
  io.outValid := cordicCostas.io.out.fire

}

class FreqDiscriminator[T <: Data : Real](val params: DiscParams[T]) extends Module {
  val io = IO(new CostasDiscBundle(params))

  val ipsPrev = RegInit(SInt(params.inWidth.W), 0)
  val qpsPrev = RegInit(SInt(params.inWidth.W), 0)

  // FIXME: Later use ONE cordic for both phase and freq Discriminator
  val cordicCostas = Module(new Cordic1Cycle(params.cordicParams))
  
  val dot = io.ips * qps_prev - ips_prev * io.qps
  val cross = io.ips * ips_prev + io.qps * qps_prev

  cordicCostas.io.in.x := dot
  cordicCostas.io.in.y := cross
  cordicCostas.io.in.valid := true.B
  // TODO: Compensate for the 1/timeStep later
  io.out := cordicCostas.io.out.z 
  io.outValid := cordicCostas.io.out.valid

}

class DllDiscBundle[T <: Data : Real](params: DiscParams[T]) {
  val ips_e = Input(SInt(params.inWidth.W))
  val qps_e = Input(SInt(params.inWidth.W))

  val ips_l = Input(SInt(params.inWidth.W))
  val qps_l = Input(SInt(params.inWidth.W))

  // FIXME: Incorrect output width. What should it be?
  val out = Output(UInt(params.outWidth.W))  
  val outValid = Output(Bool())
} 

object DllDiscBundle {
  def apply[T <: Data](params:DiscParams[T]): DllDiscBundle[T] = new DllDiscBundle(params)
}

class DllDiscriminator[T <: Data : Real](val params: DiscParams[T]) extends Module {
  val io = IO(new DllDiscBundle(params))

  val e = io.ips_e*io.ips_e + io.qps_e*io.qps_e
  val l = io.ips_l*io.ips_l + io.qps_l*io.qps_l

  val cordicDLL = Module(new Cordic1Cycle(params.cordicParams))
    
  cordicDLL.io.in.x := (e - l)
  cordicDLL.io.in.y := (e + l).asFixed() 
  cordicDLL.io.in.z := 0.S
  cordicDLL.io.in.valid := true.B

  .when (e === 0.S || l === 0.S) {
    io.out := 0
  } .otherwise {
    io.out := cordicDLL.io.out.z * (-1).S  
  }
  io.outValid = cordicDLL.io.out.valid
}
