package gps
import scala.math._ 

class DLLModel(dcGain:Double, bandwidth:Double, sampleRate:Double, discriminatorNum:Int) {
  var gain: Double = dcGain
  var bw: Double = bandwidth
  var sr: Double = sampleRate
  var disNum: Int = discriminatorNum

  var tau: Double = 1/(2*Pi*bandwidth)
  var t: Double = 1/sampleRate
  var a: Double = 1 + 2*tau/t
  var b: Double = 1 - 2*tau/t
  var prevX: Double = 0
  var prevY: Double = 0
  var disOut: Double = 0

  def discriminator1(ie: Double, il: Double, qe: Double, ql: Double) : Double = {
    var e = sqrt(pow(ie, 2) + pow(qe, 2))
    var l = sqrt(pow(il, 2) + pow(ql, 2))
    
    if (e == 0 || l == 0) {
      0
    } else {
      1/2*(e-l)/(e+l)
    }
  }
    
  def discriminator2(ie: Double, il: Double, qe: Double, ql: Double) : Double = {
    var e = pow(ie, 2) + pow(qe, 2)
    var l = pow(il, 2) + pow(ql, 2)
    
    if (e == 0 || l == 0) {
      0
    } else {
      1/2*(e-l)/(e+l)
    }
  } 

  def loopFilter(x: Double) : Double = {
    var y = gain/a * (x + prevX) - b / a * prevY

    if (y < -1000) {
      y = -1000
    }   
    prevX = x
    prevY = y
    y
  } 

  def update(I_sample: Seq[Double], Q_sample: Seq[Double], freqBias: Double, carrierAssist: Double) {
    if (disNum == 1) {
      disOut = discriminator1(I_sample(0), I_sample(2), Q_sample(0), Q_sample(2))
    } else if (disNum == 2) {
      disOut = discriminator2(I_sample(0), I_sample(2), Q_sample(0), Q_sample(2))
    }   
    var lfOut = loopFilter(disOut)
    
    lfOut         
  }
}
