package gps 

import scala.math._


class CostasModel (intTime: Double, pllBW: Double, fllBw: Double) {
  var costasMode: Int = costasMode
  var fllMode: Int = freqMode

  var ips2: Double = 0
  var qps2: Double = 0
  var phaseErr: Double = 0
  var freqErr: Double = 0
  var alpha: Double = 0
  var beta: Double = 0

  val w0f = fllBw*1.89

  def costasDetector(ips: Double, qps: Double): Double = {
    costasMode match {
      case 0 => if (I_ps >= 0) {
          atan2(Q_ps, I_ps)
        } else {
          atan2(-1*Q_ps, -1*I_ps) 
        }
      case 1 => math.atan2(Q_ps, I_ps)
      case 2 => Q_ps * signum(I_ps)
      case 3 => Q_ps / I_ps
      case _ => Q_ps * I_ps
    }
  }

  def freqDetector(ips1: Double, qps1: Double): Double = {
    val cross = ips1 * qps2 - ips2 * qps1
    val dot = ips1 * ips2 + qps1 * qps2

    freqMode match {
      case 0 => cross
      case 1 => cross * signum(dot) 
      case 2 => atan2(cross, dot)
    }
  }

  def loopFilter(): Double = {
    val b = pow(w0f, 2)*timeStep*freqErr+pow(w0p, 3)*timeStep*phaseErr+beta
    val a = timeStep*(a2*w0f*freqErr+a3*pow(w0p,2)*phaseErr+0.5*(b + beta))+alpha
    lf = b3*w0p*phaseErr+0.5*(a+alpha)
    beta = b
    alpha = a
    lf
  }

  def update(ips: Double, qps: Double, freqBias: Int): Int = {
    phaseErr = -1*costasDetector(ips, qps)
    freqError = freqDetector(ips, qps)
    loopFilter()
    ips2 = ips
    qps2 = qps
    lf.toInt + freqBias
  }
}
