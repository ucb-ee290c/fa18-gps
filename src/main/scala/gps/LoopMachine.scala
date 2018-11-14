package gps

import chisel3._
import chisel3.experimental.FixedPoint
import chisel3.util._

import dsptools.numbers._

trait LoopParams[T <: Data] {
  val codeNcoWidth: Int
  val carrierNcoWidth: Int
  val inputWidth: Int
  val cordicParamsCostas: CordicParams[T]
  val cordicParamsDLL: CordicParams[T]
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

class LoopMachine[T <: Data : Real : BinaryRepresentation](val params: LoopParams[T]) extends Module {
  val io = IO(new LoopBundle(params))
   
    

  //FIXME: Fix inputs to the loop filter
  val loopFilterCostas = Module(new LoopFilter(params.lfParamsCostas))
  val loopFilterDLL = Module(new LoopFilter(params.lfParamsDLL))

  val cordicCostas = Module(new Cordic1Cycle(params.cordicParamsCostas))
  val cordicDLL = Module(new Cordic1Cycle(params.cordicParamsDLL))

  
  
    

} 

