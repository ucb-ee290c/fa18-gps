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
  15040,
  32000,
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
  val dll = new DLLModel(10000, 10, params.sampleFreq, 0)
  val costas = new CostasModel(List(1000, 5, 0.5, 1e-6, 1e-7), 0, 1,
    params.carrierNcoCodeNom) 
  try { 
    inFile = Some(new FileInputStream(params.filename))
    var in: Int = 0
    var ind: Int = 0
    var caCode: Int = params.caNcoCodeNom
    var carrierCode: Int = params.carrierNcoCodeNom

    var hits = new ListBuffer[Int]()
    var ieArr = new ListBuffer[Int]()
    var ipArr = new ListBuffer[Int]()
    var ilArr = new ListBuffer[Int]()
    var qeArr = new ListBuffer[Int]()
    var qpArr = new ListBuffer[Int]()
    var qlArr = new ListBuffer[Int]()
    
    while ({in = inFile.get.read; (in != -1) && (ind < params.startInd)}) {
      ind += 1
    }
    
    poke(c.io.svNumber, params.svNumber-1)
    while ({in = inFile.get.read; (in != -1) && (ind < params.stopInd)}) {
      poke(c.io.dump, false)
      poke(c.io.dllIn, caCode)
      poke(c.io.costasIn, carrierCode)
      poke(c.io.adcSample, in.byteValue)
      step(1)
      val count = peek(c.io.caIndex)
      if (count == 1023) {
        hits += ind
        ieArr += peek(c.io.ie)
        ipArr += peek(c.io.ip)
        ilArr += peek(c.io.il)
        qeArr += peek(c.io.qe)
        qpArr += peek(c.io.qp)
        qlArr += peek(c.io.ql)
      }
      ind += 1
    }
    
    print(hits)
    println()
    print(ipArr)
    println()
    print(ieArr)
    println()
    print(ilArr)
    println()
    print(qpArr)
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
