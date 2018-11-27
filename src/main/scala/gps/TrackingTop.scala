package gps

import chisel3._
import chisel3.experimental.FixedPoint
import chisel3.util._
import dsptools.numbers._
import scala.math.pow

case class TrackingTopParams(
  adcWidth: Int,       // Width of the ADC Input
  sampleRate: Double,  // Sample rate of the input data
  inBP: Int,
  ncoBP: Int
) extends TrackingChannelParams[SInt, FixedPoint] with LoopParams[FixedPoint] {
  // Width of the Integrators
  // 0.02 is the maximum integration time, sizing things to prevent overflow,
  // +1 for signed
  val integWidth = log2Ceil(pow(2, adcWidth - 1).toInt*(sampleRate * 0.02).toInt) + 1
  // TODO Can we come up with some logical reasoning for this? 
  val ncoWidth = 30
 
  // Loop Filters and discriminator parameters
  // FIXME this should take a list of integration times and generate LUTs for
  // loop parameter values
  val intTime = 0.001
  // FIXME: widths may not be correct for costas loop filter 
  val protoIn = FixedPoint((integWidth + inBP).W, inBP.BP)
  val protoOut = FixedPoint((ncoWidth + ncoBP).W, ncoBP.BP)
  val lfParamsCostas = FixedFilter3rdParams(width = 20, bPWidth = 16)   
  // TODO Is there a way we can generate this? 
  val lfParamsDLL = FixedFilterParams(6000, 5, 1) 
  
  // Tracking Channel stuff
  // Carrier NCO is ncoWidth wide count, adcWidth out and has a sin output
  val carrierNcoParams = SIntNcoParams(ncoWidth, adcWidth, true)
  // Ca NCO is ncoWidth bits wide and has no sin output, 1 bit output
  val caNcoParams = SIntNcoParams(ncoWidth, 1, false)
  // Ca NCO 2x is ncoWidth - 1 bits wide to create double the frequency
  val ca2xNcoParams = SIntNcoParams(ncoWidth  - 1, 1, false)
  // Ca Params, 1 bit input from caNCO and 2 bits out to represent 1 and -1
  val caParams = CAParams(1, 2) 
  // Multipliers are 5 bit in and 5 bit out
  val mulParams = SampledMulParams(adcWidth)
  // Int Params 
  val intParams = SampledIntDumpParams(adcWidth, (sampleRate * 0.02).toInt)
  // Phase Lock Detector Params Limit set to +-15deg
  val phaseLockParams = LockDetectParams(FixedPoint(20.W, 12.BP), -0.26, 0.26,100)
}

class TrackingTop(params: TracingTopParams) extends Module {

}
