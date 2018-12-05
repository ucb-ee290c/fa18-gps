package gps

import breeze.linalg._
import breeze.plot._
import chisel3._
import dsptools.DspTester 
import dsptools.numbers._
import java.io._
import math.pow
import org.scalatest.{FlatSpec, Matchers}
import scala.collection.mutable.ListBuffer


/**
 * Data Set Params hold info about the test vector being used
 * as well as info about the channel setup
 */
case class TrackingDataSetParam(
  sampleFreq: Double, // Data Sample Rate
  svFreq: Double, // SV frequency
  startInd: Long, // Index of the file for the test to start on
  stopInd: Long,
  svNumber: Int,
  adcWidth: Int,
  filename: String,
) {
  val carrierNcoCodeNom: Int = ((svFreq / sampleFreq) * pow(2, 30)).round.toInt
  val caNcoCodeNom: Int = ((1.023e6 / sampleFreq) * pow(2, 30)).round.toInt
  val topParams = TrackingTopParams(adcWidth, sampleFreq, 12, 30)
}

/**
 * Example Data from adc_sample_data.bin
 */
object ExampleTopData extends TrackingDataSetParam(
  16367600,
  4.128460*1e6, 
  31408,
  16000000,
  22,
  5,
  "python/adc_sample_data.bin"
)

/* 
 * DspSpec for a Tracking Channel
 */
class TrackingTopSpec extends FlatSpec with Matchers {
  behavior of "TrackingTop"
  it should "track" in {
    val params = ExampleTopData
    TrackingTopTester(params)
  }  
}

/*
 * DspTester for Tracking Channel
 */
class TrackingTopTester(
  c: TrackingTop,
  params: TrackingDataSetParam
) extends DspTester(c) {
  var inFile = None: Option[FileInputStream]
  val display = true
  try { 
    inFile = Some(new FileInputStream(params.filename))
    var in: Int = 0
    var ind: Int = 0

    val hits = new ListBuffer[Int]()
    val ieArr = new ListBuffer[Int]()
    val ipArr = new ListBuffer[Int]()
    val ilArr = new ListBuffer[Int]()
    val qeArr = new ListBuffer[Int]()
    val qpArr = new ListBuffer[Int]()
    val qlArr = new ListBuffer[Int]()
    val costasError = new ListBuffer[Double]()
    val freqError = new ListBuffer[Double]()
    val dllError = new ListBuffer[Double]()
    
    while ({in = inFile.get.read; (in != -1) && (ind < params.startInd)}) {
      ind += 1
    }
    
    poke(c.io.svNumber, params.svNumber)
    poke(c.io.carrierNcoBias, params.carrierNcoCodeNom)
    poke(c.io.codeNcoBias, params.caNcoCodeNom)
    updatableDspVerbose.withValue(false) {
      while ({in = inFile.get.read; (in != -1) && (ind < params.stopInd)}) {
        poke(c.io.adcIn, in.byteValue)
        step(1)
        if (peek(c.io.dump)) {
          hits += ind
          val ie = peek(c.io.epl.ie)
          val ip = peek(c.io.epl.ip)
          val il = peek(c.io.epl.il)
          val qe = peek(c.io.epl.qe)
          val qp = peek(c.io.epl.qp)
          val ql = peek(c.io.epl.ql)

          ieArr += ie
          ipArr += ip
          ilArr += il
          qeArr += qe
          qpArr += qp
          qlArr += ql
          
          costasError += peek(c.io.phaseErr)
          freqError += peek(c.io.freqErr)
          dllError += peek(c.io.dllErr)
        }
        
        if (ind.toDouble / params.stopInd * 10 % 1 == 0) {
          println(s"Percentage = ${(ind.toDouble / params.stopInd.toDouble *
            100.0).toInt}")
        }
        ind += 1
      }
    }
    print(ipArr)
    println()
    print(qpArr)
    println()
    print(costasError)
    println()

    if (display) {
      val dllFig = new Figure("DLL Response", 2,1)
      val iqPltD = dllFig.subplot(0)
      iqPltD += plot(DenseVector.range(0, ipArr.length, 1), ieArr,
        colorcode="blue")
      iqPltD += plot(DenseVector.range(0, qpArr.length, 1), qpArr,
        colorcode="red")
      val errPltD = dllFig.subplot(1)
      errPltD += plot(DenseVector.rangeD(0.0, dllError.length.toDouble, 1.0), dllError)
      dllFig.refresh()
      
      val costasFig = new Figure("Costas Response", 3,1)
      val iqPltC = costasFig.subplot(0)
      iqPltC += plot(DenseVector.range(0, ipArr.length, 1), ieArr)
      iqPltC += plot(DenseVector.range(0, qpArr.length, 1), qpArr)
      val freqErrPlt = costasFig.subplot(1)
      freqErrPlt += plot(DenseVector.rangeD(0.0, dllError.length.toDouble, 1.0),
        freqError)
      val costasErrPlt = costasFig.subplot(2)
      costasErrPlt += plot(DenseVector.rangeD(0.0, dllError.length.toDouble, 1.0),
        costasError)
      costasFig.refresh()
    }
  } catch {
    case e: IOException => e.printStackTrace
  } finally {
    if (inFile.isDefined) inFile.get.close
  }
}
object TrackingTopTester {
  def apply(params: TrackingDataSetParam): Boolean = { 
    chisel3.iotesters.Driver.execute(Array("-tbn", "verilator"), 
      () => new TrackingTop(params.topParams)) {
      c => new TrackingTopTester(c, params)
    }   
  }
}
