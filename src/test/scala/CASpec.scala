package gps

import dsptools.numbers._
import org.scalatest.{FlatSpec, Matchers}
import scala.io._
class CASpec extends FlatSpec with Matchers {
  behavior of "CA"
  val params = CAParams(
    fcoWidth = 10,
    codeWidth = 2,
  )
  val prnCodeRaw = io.Source.fromFile("/home/nwerblun/ee290c/fa18-gps/src/test/scala/PRNCode.csv").getLines.toList.map(_.split(","))
  //Creates an array of 32 arrays each of which has a 1023 length PRN sequence
  val prnCodes = Array.ofDim[Int](prnCodeRaw.length, prnCodeRaw(0).length)
  for(i <- 0 until prnCodeRaw.length) {
    prnCodes(i) = prnCodeRaw(i).map(_.toInt)
  }
  val ncoInput = new Array[Int](1023*2)
  for(i <- 0 until ncoInput.length) {
    if (i % 2 == 0) { ncoInput(i) = -1 }
    else { ncoInput(i) = 1 }
  }
  it should "give the correct early PRN" in {

  //Tests if the early signal is what's expected.
    CAEarlyTester(params, prnCodes, ncoInput) should be (true)
  }
  //Tests if there's never a zero crossing on the NCO that the output is always the same
  it should "never change" in {
    val ncoInput2 = Array.fill[Int](1023*2)(0) 
    CANoOutputTester(params, prnCodes, ncoInput2) should be (true)
  }

  /*
  //tests what happens if you switch the satellite midway through 
  it should "correctly reset" in {
   val trials_3 = 
   CASatelliteSwitchTester(params, trials_3) should be (true)
   }*/
}

