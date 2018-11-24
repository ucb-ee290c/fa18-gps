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

case class ExampleLoopParams(
) extends LoopParams[FixedPoint] {
  val codeNcoWidth = 32
  val carrierNcoWidth = 32
  val inputWidth = 32
  val lfParamsCostas = FixedFilterParams(1, 5, 1)   //FIXME: Not the correct values for costas loop filter 
  val lfParamsDLL = FixedFilterParams(6000, 5, 1) 
} 

class LoopBundle[T <: Data](params: LoopParams[T]) extends Bundle {
  val codeNco = Output(UInt(params.codeNcoWidth.W))
  val code2xNco = Output(UInt(params.codeNcoWidth.W))
  val carrierNco = Output(UInt(params.carrierNcoWidth.W)) 

  val I_int = Input(UInt(params.inputWidth.W))
  val Q_int = Input(UInt(params.inputWidth.W))

  override def cloneType: this.type = LoopBundle(params).asInstanceOf[this.type]
}

object LoopBundle {
  def apply[T <: Data](params:LoopParams[T]): LoopBundle[T] = new LoopBundle(params)
}

class LoopMachine[T <: Data : Real : BinaryRepresentation](val loopParams: LoopParams[T], val discParams: AllDiscParams[T]) extends Module {
  val io = IO(LoopBundle(loopParams))
   
  //FIXME: Fix inputs to the loop filter
  val lfCostas = Module(new LoopFilter(loopParams.lfParamsCostas))
  val lfDLL = Module(new LoopFilter(loopParams.lfParamsDLL))

  // Discriminator Setup  
  val freqDisc = Module(new FreqDiscriminator(discParams.freqDisc))
  val phaseDisc = Module(new PhaseDiscriminator(discParams.phaseDisc))
  val dllDisc = Module(new DllDiscriminator(discParams.dllDisc)) 
    
  io.codeNco := 0.U
  io.code2xNco := 2*io.codeNco
  io.carrierNco := 0.U
    
} 

