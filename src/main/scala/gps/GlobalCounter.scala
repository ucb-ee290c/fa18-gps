package gps

import chisel3._
import chisel3.experimental.FixedPoint
import chisel3.util._
import dsptools.numbers._

case class GlobalCounterParams (
  val clkPeriod: Double,
  val counterWidth: Int,
  val secondsWidth: Int,
  val secondsBP: Int
)

class GlobalCounter(params: GlobalCounterParams) extends Module {
  val io = IO(new Bundle {
    val currCycle = Output(UInt(params.counterWidth.W))
    val currTimeSeconds = Output(FixedPoint(params.secondsWidth.W, params.secondsBP.BP))
  })
  //If you want to pick the width yourself
  val counter = RegInit(0.U(params.counterWidth.W))
  counter := counter + 1.U
  //If you'd rather just specify the max value
  //val counter = RegInit(0.U(log2Ceil(params.counterMax+1).W))
  io.currTimeSeconds := counter.asFixed(FixedPoint(params.secondsWidth.W, params.secondsBP.BP))*params.clkPeriod.F(params.secondsWidth.W, params.secondsBP.BP)
  io.currCycle := counter
}
