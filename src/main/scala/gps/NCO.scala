package gps

import chisel3._
import chisel3.util._

import dsptools.numbers._

/** Base class for NCO parameters
 *
 *  These are type generic
 *  @tparam T NCO output type
 */
trait NcoParams[T <: Data] {
  /** Output prototype */
  val proto: T
  /** NCO full register bit width */
  val resolutionWidth: Int
  /** NCO output bit width */ 
  val truncateWidth: Int
  /** Boolean that determines if the NCO outputs a sine wave */
  val sinOut: Boolean
}

/** NCO parameters object for an NCO with an SInt output
 *
 *  @param resolutionWidth NCO register bit width (full resolution)
 *  @param truncadeWidth NCO output bit width (after truncating)
 *  @param sinOut If true, NCO also has a sine output
 */
case class SIntNcoParams(
  resolutionWidth: Int,
  truncateWidth: Int,
  sinOut: Boolean
) extends NcoParams[SInt] {
  /** Output prototype: SInt of width truncateWidth */
  val proto = SInt(truncateWidth.W)
}

/** Bundle type that describes an NCO that outputs both sine and cosine
 *  
 */
class NcoSinOutBundle[T <: Data](params: NcoParams[T]) extends Bundle {
  /** NCO accumulator input step size */
  val stepSize = Input(UInt(params.resolutionWidth.W))

  /** The NCO register output */ 
  val truncateRegOut = Output(UInt(params.truncateWidth.W))
  /** Output sine signal (if sinOut == true) */
  val sin: T = Output(params.proto.cloneType)
  /** Output cosinde signal */ 
  val cos: T = Output(params.proto.cloneType)

  override def cloneType: this.type = NcoSinOutBundle(params).asInstanceOf[this.type]
}

/** Factory for [[gps.NcoSinOutBundle]] instances. */
object NcoSinOutBundle {
  /** Creates an NcoSinOutBundle with given set of params.
   *
   *  @param params The NCO parameters 
   */
  def apply[T <: Data](params: NcoParams[T]): NcoSinOutBundle[T] = new NcoSinOutBundle(params)
}

/** Bundle type that describes an NCO that only outputs cosines
 *  
 */
class NcoBundle[T <: Data](params: NcoParams[T]) extends Bundle {
  /** NCO accumulator input step size */
  val stepSize = Input(UInt(params.resolutionWidth.W))
  /** Output cosinde signal */ 
  val cos: T = Output(params.proto.cloneType)
  /** The NCO register output */ 
  val truncateRegOut = Output(UInt(params.truncateWidth.W))

  override def cloneType: this.type = NcoBundle(params).asInstanceOf[this.type]
}

/** Factory for [[gps.NcoBundle]] instances. */
object NcoBundle {
  /** Creates an NcoBundle with given set of params.
   *
   *  @param params The NCO parameters 
   */
  def apply[T <: Data](params: NcoParams[T]): NcoBundle[T] = new NcoBundle(params)
}

/** NCO module 
 *  
 *  Calculates both sine and cosine outputs
 *
 *  @param params NCO parameters
 */
class NCO[T <: Data : Real](val params: NcoParams[T]) extends Module {
  /** NcoSineOutBundle IO */
  val io = IO(new NcoSinOutBundle(params)) 
    
  /** NCO base module instance */
  val cosNCO = Module(new NCOBase(params))  
    
  cosNCO.io.stepSize := io.stepSize
  io.cos := cosNCO.io.cos

  io.truncateRegOut := cosNCO.io.truncateRegOut
  io.sin := ConvertableTo[T].fromDouble(0.0)

  if (params.sinOut) {
    /** LUT that contains sine values */
    val sineLUT = VecInit(NCOConstants.sine(params.truncateWidth).map(ConvertableTo[T].fromDouble(_)))
    io.sin := sineLUT(cosNCO.io.truncateRegOut)
  }
}

/** NCO base module 
 * 
 *  Only calculates cosine outputs 
 *
 *  @param params NCO parameters
 */
class NCOBase[T <: Data : Real](val params: NcoParams[T]) extends Module {
  /** NcoBundle IO */
  val io = IO(new NcoBundle(params))
   
  /** Register that accumulates NCO value */
  val reg = RegInit(UInt(params.resolutionWidth.W), 0.U)
    
  /** LUT that contains cosine values */
  val cosineLUT = VecInit(NCOConstants.cosine(params.truncateWidth).map(ConvertableTo[T].fromDouble(_)))

  reg := reg + io.stepSize
  io.truncateRegOut := reg >> (params.resolutionWidth - params.truncateWidth)
  io.cos := cosineLUT(io.truncateRegOut)
}
