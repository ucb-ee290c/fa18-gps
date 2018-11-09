package gps

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
case class DataSetParam[T <: Data](
  sampleFreq: Double, // Data Sample Rate
  svFreq: Double, // SV frequency
  startInd: Long,
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
object ExampleData extends DataSetParam(
  1.023*16*1e6,
  4.128460*1e6, 
  15039,
  100000,
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
    val params = ExampleData
    ChannelTester(params)
  }  
}

/*
 * DspTester for Tracking Channel
 */
class ChannelTester[T <: Data](c: TrackingChannel[T], params: DataSetParam[T]) extends DspTester(c) {
  var inFile = None: Option[FileInputStream]
  val dll = new DLLModel(12000, 10, params.sampleFreq, 0)
  val costas = new CostasModel(List(1000, 5, 0.5, 1e-6, 1e-7), 0, 1,
    params.carrierNcoCodeNom) 
  try { 
    inFile = Some(new FileInputStream(params.filename))
    var in: Int = 0
    var ind: Int = 0
    var caCode: Int = params.caNcoCodeNom
    var carrierCode: Int = params.carrierNcoCodeNom

    val hits = new ListBuffer[Int]()
    val ieArr = new ListBuffer[Int]()
    val ipArr = new ListBuffer[Int]()
    val ilArr = new ListBuffer[Int]()
    val qeArr = new ListBuffer[Int]()
    val qpArr = new ListBuffer[Int]()
    val qlArr = new ListBuffer[Int]()
    
    while ({in = inFile.get.read; (in != -1) && (ind < params.startInd)}) {
      ind += 1
    }
    
    poke(c.io.svNumber, params.svNumber)
    updatableDspVerbose.withValue(false) {
      while ({in = inFile.get.read; (in != -1) && (ind < params.stopInd)}) {
        poke(c.io.dump, false)
        poke(c.io.dllIn, caCode)
        poke(c.io.costasIn, carrierCode)
        poke(c.io.adcSample, in.byteValue)
        step(1)
        val count = peek(c.io.caIndex)
        if (count == 1023) {
          hits += ind
          val ie = peek(c.io.ie)
          val ip = peek(c.io.ip)
          val il = peek(c.io.il)
          val qe = peek(c.io.qe)
          val qp = peek(c.io.qp)
          val ql = peek(c.io.ql)

          ieArr += ie
          ipArr += ip
          ilArr += il
          qeArr += qe
          qpArr += qp
          qlArr += ql

          caCode = dll.update((ie.toDouble, ip.toDouble, il.toDouble), 
            (qe.toDouble, qp.toDouble, ql.toDouble), params.caNcoCodeNom)
          carrierCode = costas.update(ip.toDouble, qp.toDouble, params.carrierNcoCodeNom)
        }
        ind += 1
      }
    }
    
    print(hits)
    println()
    print(ieArr)
    println()
    print(ipArr)
    println()
    print(ilArr)
    println()
    print(qeArr)
    println()
    print(qpArr)
    println()
    print(qlArr)
    println()
  } catch {
    case e: IOException => e.printStackTrace
  } finally {
    if (inFile.isDefined) inFile.get.close
  }
}
object ChannelTester {
  def apply[T <: Data : Ring : Real](params: DataSetParam[T]): Boolean = { 
    chisel3.iotesters.Driver.execute(Array("-tbn", "verilator"), 
      () => new TrackingChannel(params.channelParams)) {
      c => new ChannelTester(c, params)
    }   
  }
}
