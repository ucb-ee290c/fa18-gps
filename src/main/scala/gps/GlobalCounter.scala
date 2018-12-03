package gps

import chisel3._
import chisel3.experimental.FixedPoint
import chisel3.util._
import dsptools.numbers._


/** Parameters class for global counter
 *
 *  @param clkPeriod the period in seconds of the clock
 *  @param counterWidth how many bits the counter uses
 *  @param secondsWidth how many bits the whole-number part of the computed elapsed time in seconds is represented with
 *  @param secondsBP how many bits the decimal point of the computed elapsed time in seconds is represented with
 */
case class GlobalCounterParams (
  val clkPeriod: Double,
  val counterWidth: Int,
  val secondsWidth: Int,
  val secondsBP: Int
)

/** A module that counts and computes elapsed time in seconds
* IO:
* currCycle: Output(UInt), counter of elapsed number of cycles since startup/reset
* currTimeSeconds: Output(FixedPoint), based on the clock period, the elapsed time in seconds since startup/reset
* @param params An instance of the GlobalCounterParams 
*
*/
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
