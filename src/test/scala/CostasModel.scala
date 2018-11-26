package gps 

import scala.math._


class CostasModel (intTime: Double, pllBW: Double, fllBw: Double, mode: Int, freqMode: Int) {
  var costasMode: Int = mode
  var fllMode: Int = freqMode
  var timeStep: Double = intTime

  var lf: Double = 0
  var ips2: Double = 0
  var qps2: Double = 0
  var phaseErr: Double = 0
  var freqErr: Double = 0
  var freqUpdate: Boolean = false
  var alpha: Double = 0
  var beta: Double = 0
  

  val w0f = fllBw/0.53
  val w0p = pllBW/0.7845


  def costasDetector(ips: Double, qps: Double): Double = {
    costasMode match {
      case 0 => if (ips >= 0) {
          atan2(qps, ips)
        } else {
          atan2(-1*qps, -1*ips) 
        }
      case 1 => math.atan2(qps, ips)
      case 2 => qps * signum(ips)
      case 3 => qps / ips
      case _ => qps * ips
    }
  }

  def freqDetector(ips1: Double, qps1: Double): Double = {
    val cross = ips1 * qps2 - ips2 * qps1
    val dot = ips1 * ips2 + qps1 * qps2

    freqMode match {
      case 0 => cross
      case 1 => cross * signum(dot) 
      case 2 => atan2(cross, dot)/timeStep
    }
  }

  def loopFilter(): Double = {
    // Filter Constants
    val a2 = 1.414
    val a3 = 1.1
    val b3 = 2.4

    var b = pow(w0f, 2)*timeStep*freqErr+pow(w0p, 3)*timeStep*phaseErr+beta
    var a = timeStep*(a2*w0f*freqErr+a3*pow(w0p,2)*phaseErr+0.5*(b + beta))+alpha
    lf = b3*w0p*phaseErr+0.5*(a+alpha)
    beta = b
    alpha = a
    lf
  }

  def update(ips: Double, qps: Double, freqBias: Int): Int = {
    phaseErr = -1*costasDetector(ips, qps)
    if (freqUpdate) {
      freqErr = freqDetector(ips, qps)
      freqUpdate = false
    } else {
      freqUpdate = true
    }
    loopFilter()
    var delta_freq = lf/(2*Pi)
    var code = delta_freq / (16*1023*1e3) * (pow(2, 30) - 1) 
    ips2 = ips
    qps2 = qps
    code.toInt + freqBias
  }
}
