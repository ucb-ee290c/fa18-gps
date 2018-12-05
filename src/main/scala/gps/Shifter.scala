
package gps

import chisel3._
import chisel3.experimental.FixedPoint
import chisel3.util.Decoupled
import chisel3.util._
import scala.math._
import dsptools._
import dsptools.numbers._
import dsptools.numbers.implicits._
//import freechips.rocketchip.diplomacy.LazyModule
//import freechips.rocketchip.subsystem.BaseSubsystem

/**
  * Base class for CORDIC parameters
  * These are type generic
  */
trait ShifterParams[T <: Data] {
  val proto: T
  val width: Int
  val nSample: Int
}


case class SIntShifterParams(
                          width: Int,
                          nSample: Int,
                        ) extends ShifterParams[SInt] {

  val proto = SInt(width.W)
}



class Shifter[T <: Data:Real](val params: ShifterParams[T]) extends Module {

  require(params.nSample > 0)
  require(params.width > 0)

  val io = IO(new Bundle{
    val in: T = Input(params.proto)
    val out: Vec[T] = Output(Vec(params.nSample, params.proto))
  })

  val reg_shift = Reg(Vec(params.nSample, params.proto))

  for (i <- 0 until params.nSample-1) {
    reg_shift(i) := reg_shift(i+1)
  }
  reg_shift(params.nSample-1) := io.in


  for (i <- 0 until params.nSample) {
    io.out(i) := reg_shift(i)
  }

}

