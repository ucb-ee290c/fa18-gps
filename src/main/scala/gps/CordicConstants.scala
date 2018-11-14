package gps

import breeze.numerics.{atan, pow, sqrt}

/**
 * Object for computing useful constants
 */
object CordicConstants {
  /**
   * Get sequences of length n that go 1.0, 0.5, 0.25, ...
   */
  def linear(a: Int, b: Int) = for (i <- a until b) yield pow(2.0, -i)
  /**
   * Get gain for n-stage CORDIC
   */
  def gain(n: Int) = linear(0, n).map(x => sqrt(1 + x * x)).reduce(_ * _)
  /**
   * Get sequences of length n that go atan(1), atan(0.5), atan(0.25), ...
   */
  def arctan(n: Int) = linear(0, n).map(atan(_))
}
