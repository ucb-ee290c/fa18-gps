package gps 
import scala.math._

class CostasModel (coeffs: Seq[Double], costasMode: Int, freqMode: Int, freqBias: Int) {
  var lfCoeff: Seq[Double] = coeffs
  var cm: Int = costasMode
  var fm: Int = freqMode
  var fb: Int = freqBias

  var avgMag: = 0
  var count: Int = 0
  var Ips_d: = 0
  var Qps_d: = 0
  var err: Double = 0
  var dLfOut: = 0
  var LfOut = 0
  var lf = 0
  var lfSum = 0
  var lfSumSum = 0

  def costasDetector(I_ps: Double, Q_ps: Double, mode: Int) : Double = {
    if (mode == 0) {
      if (I_ps >= 0) {
        atan2(Q_ps, I_ps)
      } else {
        atan2(-1*Q_ps, -1*I_ps) 
      }
    } else if (mode == 1) {
      math.atan2(Q_ps, I_ps)
    } else if (mode == 2) {
      Q_ps * sign(I_ps)
    } else if (mode == 3) {
      Q_ps / I_ps
    } else {
      Q_ps * I_ps
    }
  }

  def freqDetector(I_ps:Double, Q_ps: Double, mode: Int) : Double = {
    var cross = I_ps * Qps_d - Ips_d * Q_ps
    var dot = I_ps * Ips_d - Q_ps * Qps_d

    if (mode == 1) {
      cross
    } else if (mode == 2) {
      cross * sign(dot)
    } else {
      atan2(dot, cross)
    }
  }

  def loopFilter(phaseErr: Double, freqErr: Double, coeffs: Seq[Double]) : Double = {
    lfSumSum = lfSumSum +  coeffs(2) * phaseErr + coeffs(4) * freqErr
    lfSum = lfSum + coeffs(1) * phaseErr + coeffs(3) * freqErr + lfSumSum
    lf = coeffs(0) * phaseErr + lfSum

    lf
  }

  def update(I_int: Double, Q_int: Double, freqBias: Int) : Double = {
    err = costasDetector(I_int, Q_int, cm)
    freqErr = freDetector(I_int, Q_int, fm)
    dLfOut = loopFilter(-1*err, freqErr, lfCoeef)
    Ips_d = I_int
    Qps_d = Q_int

    lfOut   
  }
}
