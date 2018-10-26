package gps

import breeze.numerics.{sin, cos, pow}
import breeze.numerics.constants._

/**
 * Object for computing useful constants
 */
object NCOConstants {
  /** 
   * Get sequences of length n that get sin(i)...
   */
  def sine(n: Int) = for (i <- 0 until pow(2, n)) yield sin(i*2.0*Pi/pow(2, n)) 
  /** 
   * Get sequences of length n that get cosine(i)...
   */
  def cosine(n: Int) = for (i <- 0 until pow(2, n)) yield cos(i*2.0*Pi/pow(2, n)) 
}

