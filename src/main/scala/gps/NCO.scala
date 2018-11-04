package gps

import chisel3._
import chisel3.experimental.FixedPoint
import chisel3.util._

import dsptools.numbers._

/**
 * Base class for NCO parameters
 *
 * These are type generic
 */
trait NcoParams[T <: Data] {
  val proto: T
  val resolutionWidth: Int
  val truncateWidth: Int
  val sinOut: Boolean
}

/**
 * NCO parameters object for an NCO with a fixed-point output
 */
case class SIntNcoParams(
  resolutionWidth: Int,
  sinOut: Boolean,
  truncateWidth: Int
) extends NcoParams[SInt] {
  // binary point is (Width-3) to represent Pi/2 exactly
  val proto = SInt(truncateWidth.W)
}

/**
 * Bundle type that describes an NCO that outputs both sine and cosine
 */
class NcoSinOutBundle[T <: Data](params: NcoParams[T]) extends Bundle {
  val stepSize = Input(UInt(params.resolutionWidth.W))

  val truncateRegOut = Output(UInt(params.truncateWidth.W))
  val sin: T = Output(params.proto.cloneType)
  val cos: T = Output(params.proto.cloneType)

  override def cloneType: this.type = NcoSinOutBundle(params).asInstanceOf[this.type]
}
object NcoSinOutBundle {
  def apply[T <: Data](params: NcoParams[T]): NcoSinOutBundle[T] = new NcoSinOutBundle(params)
}

/**
 * Bundle type that describes an NCO that only outputs cosines
 */
class NcoBundle[T <: Data](params: NcoParams[T]) extends Bundle {
  val stepSize = Input(UInt(params.resolutionWidth.W))
  val cos: T = Output(params.proto.cloneType)
  val truncateRegOut = Output(UInt(params.truncateWidth.W))

  override def cloneType: this.type = NcoBundle(params).asInstanceOf[this.type]
}
object NcoBundle {
  def apply[T <: Data](params: NcoParams[T]): NcoBundle[T] = new NcoBundle(params)
}

class NCO[T <: Data : Real](val params: NcoParams[T]) extends Module {
  val io = IO(new NcoSinOutBundle(params)) 
    
  val cosNCO = Module(new NCOBase(params))  
    
  cosNCO.io.stepSize := io.stepSize
  io.cos := cosNCO.io.cos

  io.truncateRegOut := cosNCO.io.truncateRegOut
  io.sin := ConvertableTo[T].fromDouble(0.0)

  if (params.sinOut) {
    val sineLUT = VecInit(NCOConstants.sine(params.truncateWidth).map(ConvertableTo[T].fromDouble(_)))
    io.sin := sineLUT(cosNCO.io.truncateRegOut)
  }
}

class NCOBase[T <: Data : Real](val params: NcoParams[T]) extends Module {
  val io = IO(new NcoBundle(params))
   
  val reg = RegInit(UInt(params.resolutionWidth.W), 0.U)
    
  val cosineLUT = VecInit(NCOConstants.cosine(params.truncateWidth).map(ConvertableTo[T].fromDouble(_)))

  reg := reg + io.stepSize
  val truncateReg = reg >> (params.resolutionWidth - params.truncateWidth)
  io.cos := cosineLUT(truncateReg)
  io.truncateRegOut := truncateReg 
}
