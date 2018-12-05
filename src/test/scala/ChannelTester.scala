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
case class ChannelDataSetParam[T <: Data](
  sampleFreq: Double, // Data Sample Rate
  svFreq: Double, // SV frequency
  startInd: Long, // Index of the file for the test to start on
  stopInd: Long,
  svNumber: Int,
  filename: String,
  channelParams: TrackingChannelParams[T]
) {
  val carrierNcoCodeNom: Int = ((svFreq / sampleFreq) * 
    pow(2, channelParams.carrierNcoParams.resolutionWidth)).round.toInt
  val caNcoCodeNom: Int = ((1.023e6 / sampleFreq) * 
    pow(2, channelParams.caNcoParams.resolutionWidth)).round.toInt
}

/**
 * Example Data from adc_sample_data.bin
 */
object ExampleChannelData extends ChannelDataSetParam(
  16367600,
  4.128460*1e6, 
  31408,
  16000000,
  22,
  "python/adc_sample_data.bin",
  ExampleTrackingChannelParams()
)

/* 
 * DspSpec for a Tracking Channel
 */
class ChannelSpec extends FlatSpec with Matchers {
  behavior of "TrackingChannel"
  it should "track" in {
    val params = ExampleChannelData
    ChannelTester(params)
  }  
}

/*
 * DspTester for Tracking Channel
 */
class ChannelTester[T <: Data](
  c: TrackingChannel[T], 
  params: ChannelDataSetParam[T]
) extends DspTester(c) {
  var inFile = None: Option[FileInputStream]
  val dll = new DLLModel(6000, 3, 1e3, 1)
  val costas = new CostasModel(0.001, 17.0, 3.0, 0, 2) 
  val display = false
  try { 
    inFile = Some(new FileInputStream(params.filename))
    var in: Int = 0
    var ind: Int = 0
    var caCode: Int = params.caNcoCodeNom
    var carrierCode: Int = params.carrierNcoCodeNom
    var integrationTime: Int = 0

    val hits = new ListBuffer[Int]()
    val ieArr = new ListBuffer[Int]()
    val ipArr = new ListBuffer[Int]()
    val ilArr = new ListBuffer[Int]()
    val qeArr = new ListBuffer[Int]()
    val qpArr = new ListBuffer[Int]()
    val qlArr = new ListBuffer[Int]()
    val lockArr = new ListBuffer[Int]()
    val costasError = new ListBuffer[Double]()
    val freqError = new ListBuffer[Double]()
    val dllError = new ListBuffer[Double]()
    
    while ({in = inFile.get.read; (in != -1) && (ind < params.startInd)}) {
      ind += 1
    }
    
    poke(c.io.svNumber, params.svNumber)
    poke(c.io.dump, false)
    updatableDspVerbose.withValue(false) {
      while ({in = inFile.get.read; (in != -1) && (ind < params.stopInd)}) {
        poke(c.io.dllIn, caCode)
        poke(c.io.costasIn, carrierCode)
        poke(c.io.adcSample, in.byteValue)
        step(1)
        val count = peek(c.io.caIndex)
        if (count == 1023) {
          integrationTime += 1
        }
        if (integrationTime == 1) {
          hits += ind
          val ie = peek(c.io.toLoop.ie)
          val ip = peek(c.io.toLoop.ip)
          val il = peek(c.io.toLoop.il)
          val qe = peek(c.io.toLoop.qe)
          val qp = peek(c.io.toLoop.qp)
          val ql = peek(c.io.toLoop.ql)

          ieArr += ie
          ipArr += ip
          ilArr += il
          qeArr += qe
          qpArr += qp
          qlArr += ql

          caCode = dll.update((ie.toDouble, ip.toDouble, il.toDouble), 
            (qe.toDouble, qp.toDouble, ql.toDouble), params.caNcoCodeNom)
          carrierCode = costas.update(ip.toDouble, qp.toDouble, params.carrierNcoCodeNom)
          
          costasError += costas.phaseErr
          freqError += costas.freqErr
          dllError += dll.disOut
          integrationTime = 0

          poke(c.io.phaseErr.bits, costas.phaseErr)
          poke(c.io.phaseErr.valid, true)
          
          poke(c.io.dump, true)
          val lock = peek(c.io.lock)
          if (peek(c.io.lock)) {
            lockArr += 1
          } else {
            lockArr += 0
          }
        } else {
          poke(c.io.dump, false)
          poke(c.io.phaseErr.valid, false)
        }
        ind += 1
        if (ind.toDouble / params.stopInd * 10 % 1 == 0) {
          println(s"Percentage = ${(ind.toDouble / params.stopInd.toDouble *
            100.0).toInt}")
        }
      }
    }

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
      costasErrPlt += plot(DenseVector.range(0, lockArr.length, 1), lockArr)
      costasFig.refresh()
    }
  } catch {
    case e: IOException => e.printStackTrace
  } finally {
    if (inFile.isDefined) inFile.get.close
  }
}
object ChannelTester {

  def apply[T <: Data : Real](params: ChannelDataSetParam[T]): Boolean = { 
    chisel3.iotesters.Driver.execute(Array("-tbn", "verilator", "-tgvo", "off"), 
      () => new TrackingChannel(params.channelParams)) {
      c => new ChannelTester(c, params)
    }   
  }
}
