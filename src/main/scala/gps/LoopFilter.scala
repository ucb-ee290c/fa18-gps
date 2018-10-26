package gps

import chisel3._
import chisel3.experimental.FixedPoint
import chisel3.util._

import dsptools.numbers._

import scala.math._

object GetLoopFilterCoeffs {
  def apply(
    dcGain: Double, 
    bandwidth: Double, 
    sampleRate: Double
  ): (Int, Int) = {
    val tau = 1/(2*Pi*bandwidth)
    val T = 1/sampleRate
    ((dcGain/(1+2*tau/T)).toInt, ((1-2*tau/T)/(1+2*tau/T)).toInt)
  }
}
  
class LoopFilter(w: Int, dcGain: Double, bandwidth: Double, sampleRate: Double) extends Module {
  val io = IO(new Bundle {
    val in = Input(SInt(w.W))
    val out = Output(SInt(w.W))
  })
  val (a, b) = GetLoopFilterCoeffs(dcGain, bandwidth, sampleRate) 
  val x = RegInit(0.S(w.W))
  val y = RegInit(0.S(w.W))

  io.out := a.S*(io.in + x) - b.S*y

  x := io.in
  y := io.out
}
