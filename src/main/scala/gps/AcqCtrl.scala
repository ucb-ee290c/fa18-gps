package gps

import chisel3._
//import chisel3.util.Decoupled
import chisel3.util._
import scala.math._
import dsptools.numbers._
import chisel3.experimental.FixedPoint

trait ACtrlParams [T <: Data] {
  val nLoop: Int
  val nFreq: Int
  val nSample: Int
  val nLane: Int
  val wCorrelation: Int
  val wLoop: Int
  val wIdxFreq: Int
  val wFreq: Int
  val wCodePhase: Int
  val wLane: Int
  val wADC: Int
  val wSate: Int
  val freqMin: Int
  val freqStep: Int
//  val wMax: Int
//  val wSum: Int



  val pIdxFreq: UInt
  val pFreq: UInt
  val pLoop: UInt
  val pCodePhase: UInt
  val pSate: UInt
  val pADC: T
  val pCorrelation: T
  val pMax: T
  val pSum: T

}

case class IntACtrlParams (
                         val nLoop: Int,
                         val nFreq: Int,
                         val nSample: Int,
                         val nLane: Int,
                         val wCorrelation: Int,
                         val wLoop: Int,
                         val wIdxFreq: Int,
                         val wFreq: Int,
                         val wCodePhase: Int,
                         val wLane: Int,
                         val wADC: Int,
                         val wSate: Int,
                         val freqMin: Int,
                         val freqStep: Int,


                       ) extends ACtrlParams[FixedPoint] {

  val bpADC: Int = 0

  val wMax: Int = wCodePhase + wLoop + wADC
  val wSum: Int = wMax + wCodePhase + wIdxFreq
  val wCyc: Int = wCodePhase - wLane + 1


  val pIdxFreq = UInt(wIdxFreq.W)
  val pFreq = UInt(wFreq.W)
  val pLoop = UInt(wLoop.W)
  val pCodePhase = UInt(wCodePhase.W)
  val pSate = UInt(wSate.W)
  val pADC = FixedPoint(wADC.W, bpADC.BP)
  val pCorrelation = FixedPoint((wCorrelation+1).W, 1.BP)
  val pMax = FixedPoint((wMax+1).W, 1.BP)
  val pSum = FixedPoint((wSum+1).W, 1.BP)

//  val pCorrelation = SInt(wCorrelation.W)
//  val pMax = SInt(wMax.W)
//  val pSum = SInt(wSum.W)

}





// input interface within the acquisition loop
class ACtrlAInputBundle[T <: Data](params: ACtrlParams[T]) extends Bundle {
//  val ADC: T2 = params.pADC
//  val CodePhase: T1 = params.pCodePhase
//  val Correlation: T3 = params.pCorrelation
  val ADC: T = Input(params.pADC)
  val CodePhase: UInt = Input(params.pCodePhase)
  val Correlation = Input(Vec(params.nLane, params.pCorrelation))
  val ready = Output(Bool())
  val valid = Input(Bool())

  override def cloneType: this.type = ACtrlAInputBundle(params).asInstanceOf[this.type]
}
object ACtrlAInputBundle {
  def apply[T <: Data](params: ACtrlParams[T]): ACtrlAInputBundle[T] = new ACtrlAInputBundle(params)
}


// output interface within the acquisition loop
class ACtrlAOutputBundle[T <: Data](params: ACtrlParams[T]) extends Bundle {

  val freqNow: UInt = Output(params.pFreq.cloneType)
  val freqNext: UInt = Output(params.pFreq.cloneType)
  val cpNow: UInt = Output(params.pCodePhase.cloneType)
  val cpNext: UInt = Output(params.pCodePhase.cloneType)
  val ready = Input(Bool())
  val valid = Output(Bool())

  override def cloneType: this.type = ACtrlAOutputBundle(params).asInstanceOf[this.type]
}
object ACtrlAOutputBundle {
  def apply[T <: Data](params: ACtrlParams[T]): ACtrlAOutputBundle[T] = new ACtrlAOutputBundle(params)
}

// input interface to the tracking loop
class ACtrlTInputBundle[T <: Data](params: ACtrlParams[T]) extends Bundle {
  val idx_sate: UInt = Input(params.pSate)
  val valid = Input(Bool())
  val ready = Output(Bool())

  override def cloneType: this.type = ACtrlTInputBundle(params).asInstanceOf[this.type]
}
object ACtrlTInputBundle {
  def apply[T <: Data](params: ACtrlParams[T]): ACtrlTInputBundle[T] = new ACtrlTInputBundle(params)
}

// output interface to the tracking loop
class ACtrlTOutputBundle[T <: Data](params: ACtrlParams[T]) extends Bundle {

  val freqOpt: UInt = Output(params.pFreq.cloneType)
  val CPOpt: UInt = Output(params.pCodePhase.cloneType)
  val sateFound = Output(Bool())
  val iFreqOptItm: UInt = Output(params.pIdxFreq.cloneType)
  val iFreqOptOut: UInt = Output(params.pIdxFreq.cloneType)
  val CPOptItm: UInt = Output(params.pCodePhase.cloneType)
  val CPOptOut: UInt = Output(params.pCodePhase.cloneType)
  val max: T = Output(params.pMax.cloneType)
  val CPOpt_itm: UInt = Output(params.pCodePhase.cloneType)
  val vec = Output(Vec(params.nSample, params.pCorrelation.cloneType))
  val state = Output(Bool())
  val ready = Input(Bool())
  val valid = Output(Bool())

  override def cloneType: this.type = ACtrlTOutputBundle(params).asInstanceOf[this.type]
}
object ACtrlTOutputBundle {
  def apply[T <: Data](params: ACtrlParams[T]): ACtrlTOutputBundle[T] = new ACtrlTOutputBundle(params)
}

class DummyBundle[T <: Data](params: ACtrlParams[T]) extends Bundle {

  val sum: T = Output(params.pSum.cloneType)
  val max: T = Output(params.pMax.cloneType)

  override def cloneType: this.type = DummyBundle(params).asInstanceOf[this.type]
}
object DummyBundle {
  def apply[T <: Data](params: ACtrlParams[T]): DummyBundle[T] = new DummyBundle(params)
}

class ACtrlDebugBundle[T <: Data](params: ACtrlParams[T]) extends Bundle {

  val iFreqNow: UInt = Output(params.pIdxFreq.cloneType)
  val iLoopNow: UInt = Output(params.pLoop.cloneType)
  val iCPNow: UInt = Output(params.pCodePhase.cloneType)
  val max: T = Output(params.pMax.cloneType)
  val reg_max: T = Output(params.pMax.cloneType)
  val reg_tag_CP = Output(Bool())
//  val freqOpt: T1 = Output(params.pFreq.cloneType)
//  val CPOpt: T1 = Output(params.pCodePhase.cloneType)
//  val sateFound = Output(Bool())
//  val iFreqOptItm: T1 = Output(params.pIdxFreq.cloneType)
//  val iFreqOptOut: T1 = Output(params.pIdxFreq.cloneType)
//  val CPOptItm: T1 = Output(params.pCodePhase.cloneType)
//  val CPOptOut: T1 = Output(params.pCodePhase.cloneType)
//  val max: T3 = Output(params.pMax.cloneType)
//  val CPOpt_itm: T1 = Output(params.pCodePhase.cloneType)
//  val vec = Output(Vec(params.nSample, params.pCorrelation.cloneType))
//  val state = Output(Bool())
//  val ready = Input(Bool())
//  val valid = Output(Bool())

  override def cloneType: this.type = ACtrlDebugBundle(params).asInstanceOf[this.type]
}
object ACtrlDebugBundle {
  def apply[T <: Data](params: ACtrlParams[T]): ACtrlDebugBundle[T] = new ACtrlDebugBundle(params)
}



class ACtrlIO[T <: Data](params: ACtrlParams[T]) extends Bundle {
//  val Ain = Flipped(Decoupled(Vec(params.nSample, ACtrlAInputBundle(params))))
//  val Aout = Decoupled(ACtrlAOutputBundle(params))
//  val Tin = Flipped(Decoupled(ACtrlTInputBundle(params)))
//  val Tout = Decoupled(ACtrlTOutputBundle(params))
  val Ain = ACtrlAInputBundle(params)
  val Aout = ACtrlAOutputBundle(params)
  val Tin = ACtrlTInputBundle(params)
  val Tout = ACtrlTOutputBundle(params)
  val Debug = ACtrlDebugBundle(params)
//  val Reg = DummyBundle(params)


  override def cloneType: this.type = ACtrlIO(params).asInstanceOf[this.type]
}
object ACtrlIO {
  def apply[T <: Data](params: ACtrlParams[T]): ACtrlIO[T] =
    new ACtrlIO(params)
}


object TreeReduce {
  def apply[V](in: Seq[V], func: (V, V) => V): V = {
    if (in.length == 1) {
      return in(0)
    }
    if (in.length == 2) {
      return func(in(0), in(1))
    }
    if (in.length % 2 == 0) {
      val withIdxs = in.zipWithIndex
      val evens = withIdxs.filter{case (_, idx) => idx % 2 == 0}.map(_._1)
      val odds  = withIdxs.filter{case (_, idx) => idx % 2 != 0}.map(_._1)
      val evenOddPairs: Seq[(V, V)] = evens zip odds
      return TreeReduce(evenOddPairs.map(x => func(x._1, x._2)), func)
    } else {
      return TreeReduce(Seq(in(0), TreeReduce(in.drop(1), func)), func)
    }
  }
}


class ACtrl[T <: Data:ConvertableTo:Ring:Real](params: ACtrlParams[T]) extends Module {

  val io = IO(ACtrlIO(params))


  val iCPMax = params.nSample-1
  val iLoopMax = params.nLoop-1
  val iFreqMax = params.nFreq-1

  val idle = WireInit(UInt(2.W), 0.U)
  val acqing = WireInit(UInt(2.W), 1.U)
  val acqed = WireInit(UInt(2.W), 2.U)


  val Ain_fire = io.Ain.ready && io.Ain.valid
  val Aout_fire = io.Aout.ready && io.Aout.valid
  val Tin_fire = io.Tin.ready && io.Tin.valid
  val Tout_fire = io.Tout.ready && io.Tout.valid


  val reg_state = RegInit(UInt(2.W), idle)
  val reg_acq_finished = RegInit(Bool(), false.B)
  val reg_sateFound_itm = RegInit(Bool(), false.B)
  val reg_tag_CP, reg_tag_Loop, reg_tag_Freq = RegInit(Bool(), false.B)

  val reg_iCPNow = RegInit(UInt(params.wCodePhase.W), 0.U)
  val reg_iLoopNow = RegInit(UInt(params.wLoop.W), 0.U)
  val reg_iFreqNow = RegInit(UInt(params.wIdxFreq.W), 0.U)


  val acq_finished = (reg_tag_CP && reg_iCPNow === 0.U && reg_iLoopNow === 0.U && reg_iFreqNow === 0.U)

  val iCPNext = Mux(reg_iCPNow + params.nLane.U - 1.U === iCPMax.U,
                    0.U, Mux(acq_finished, reg_iCPNow, reg_iCPNow+params.nLane.U))
  val iLoopNext = Mux(reg_iCPNow + params.nLane.U - 1.U === iCPMax.U,
                      Mux(reg_iLoopNow === iLoopMax.U, 0.U, reg_iLoopNow+1.U), reg_iLoopNow)
  val iFreqNext = Mux((reg_iCPNow + params.nLane.U - 1.U === iCPMax.U && reg_iLoopNow === iLoopMax.U),
                      Mux(reg_iFreqNow === iFreqMax.U, 0.U, reg_iFreqNow+1.U), reg_iFreqNow)


  reg_state := Mux(reg_state === idle, Mux(Tin_fire, acqing, idle),
                   Mux(reg_state === acqing, Mux(acq_finished, acqed, acqing), Mux(Tout_fire, idle, acqed)))

//  reg_tag_CP := Mux(reg_state === idle, false.B, Mux(reg_iCPNow > 0.U, true.B, reg_tag_CP))
//  reg_tag_Loop := Mux(reg_state === idle, false.B, Mux((reg_iLoopNow === 1.U), true.B, reg_tag_Loop))
  reg_tag_Loop := Mux(reg_state === idle, false.B, Mux(reg_tag_CP && reg_iCPNow===0.U, true.B, reg_tag_Loop))
  reg_tag_Freq := Mux(reg_state === idle, false.B, Mux(reg_iFreqNow === 1.U, true.B, reg_tag_Freq))
  reg_acq_finished := acq_finished

  io.Tout.state := reg_state



  // TODO: is io.Ain.ready always true?
  io.Ain.ready := reg_state === acqing
  io.Aout.valid := (reg_state === acqing) && io.Ain.valid
  io.Tin.ready := reg_state === idle
  io.Tout.valid := reg_acq_finished

  val update_max = WireInit(Bool(), false.B)

  val reg_max = RegInit(params.pMax, ConvertableTo[T].fromInt(0))
  val reg_correlationArray = Reg(Vec(params.nSample, params.pCorrelation))




  val reg_sum = RegInit(params.pSum, ConvertableTo[T].fromInt(0))

//  val correlationArray = WireInit(Vec(10, UInt(5.W)))
//  val max_itm = WireInit(ConvertableTo[T3].fromInt(0))
//  val max_itm = reg_correlationArray.reduce(_ max _)
  val max_itm = TreeReduce(reg_correlationArray, (x:T, y:T) => x.max(y))
  val CPOpt_itm = WireInit(UInt(params.wCodePhase.W), 0.U)
  for (i <- 0 until params.nSample) {
    when (reg_correlationArray(i.U) === max_itm) {
      CPOpt_itm := i.U
    }
  }

  io.Tout.max := max_itm
  io.Tout.CPOpt_itm := CPOpt_itm
  io.Tout.vec := reg_correlationArray


  io.Debug.iFreqNow := reg_iFreqNow
  io.Debug.iLoopNow := reg_iLoopNow
  io.Debug.iCPNow := reg_iCPNow
  io.Debug.max := max_itm
  io.Debug.reg_max := reg_max
  io.Debug.reg_tag_CP := reg_tag_CP

//  max_itm := reg_correlationArray.reduce(_ max _)

//
//
//  CPOpt_itm := reg_correlationArray.zipWithIndex.maxBy(_._1)._2.U

//  val cArr_idx = reg_correlationArray.zipWithIndex
////  val max_idx = (Wire(params.pMax), Wire(UInt(params.wCodePhase.W)))
//  val max_idx = cArr_idx.reduce((x:(T3,UInt), y:(T3,UInt)) => Mux(x._1 > y._1, x, y))
//  val max_idx = TreeReduce(cArr_idx, (x:(T3,Int), y:(T3,Int)) => Mux(x._1 > y._1, x, y))
//  max_itm := max_idx._1
//  CPOpt_itm := max_idx._2
  //  cArr_idx(1) := cArr_idx(1) + 1.U
//


//  max_itm := TreeReduce(reg_correlationArray, (x:T3, y:T3) => x.max(y))
//  max_itm := reg_correlationArray.reduce(_ max _)
//  CPOpt_itm := reg_correlationArray.indexOf(max_itm, 0).S

//  val iArray = Wire(Vec(params.nSample, UInt(params.wCodePhase.W)))
////  val iArray = new Array[Int](params.nSample)
//  for (i <- 0 until params.nSample) {
//    iArray(i) := i.U
//  }
//  CPOpt_itm := TreeReduce(iArray, (x:UInt, y:UInt) => Mux(reg_correlationArray(x) > reg_correlationArray(y), x, y))





//  val CPOpt_itm = reg_correlationArray.indexOf(max_itm).U

  // use _itm signals as outputs now
  val reg_iFreqOpt_itm = Reg(UInt(params.wIdxFreq.W))
  val reg_iFreqOpt_out = Reg(UInt(params.wIdxFreq.W))
  val reg_CPOpt_itm = Reg(UInt(params.wCodePhase.W))
  val reg_CPOpt_out = Reg(UInt(params.wCodePhase.W))

  // TODO: hardcoded, should depend on k
  val threshold = 6


  io.Tout.freqOpt := reg_iFreqOpt_itm * params.freqStep.U + params.freqMin.U
  io.Tout.CPOpt := reg_CPOpt_itm //Mux(switchSate, reg_optCP_itm, reg_optCP_out)
  io.Tout.iFreqOptItm := reg_iFreqOpt_itm
  io.Tout.iFreqOptOut := Mux(reg_acq_finished, reg_iFreqOpt_itm, reg_iFreqOpt_out)
  io.Tout.CPOptItm := reg_CPOpt_itm
  io.Tout.CPOptOut := Mux(reg_acq_finished, reg_CPOpt_itm, reg_CPOpt_out)
  io.Tout.sateFound := reg_sateFound_itm //Mux(switchSate, reg_sateFound_itm, reg_sateFound_out)

  io.Aout.freqNow := reg_iFreqNow * params.freqStep.U + params.freqMin.U
  io.Aout.freqNext := iFreqNext * params.freqStep.U + params.freqMin.U
  io.Aout.cpNow := reg_iCPNow
  io.Aout.cpNext := iCPNext


  // should be fine to reset reg_max, reg_correlationArray and reg_sum in idle state since this will not
  // affect reg_optFreq and reg_optCP, if affected, try to
  // reset then if requested to start acquisition for a new satellite
  when(reg_state === idle) {
    reg_tag_CP := false.B

    reg_max := ConvertableTo[T].fromInt(0)
    reg_sum := ConvertableTo[T].fromInt(0)
    for (i <- 0 until params.nSample) {
      reg_correlationArray(i) := ConvertableTo[T].fromInt(0)
    }

  }
  .elsewhen(reg_state === acqing) {

    // state machine
    when (Ain_fire) {
      reg_tag_CP := true.B

      reg_iCPNow := iCPNext
      reg_iLoopNow := iLoopNext
      reg_iFreqNow := iFreqNext

//      for (j <- 0 until params.nLane) {
//        reg_sum := reg_sum + io.Ain.Correlation(j)
//      }
      reg_sum := reg_sum + TreeReduce(io.Ain.Correlation, (x:T, y:T) => x+y)


      for (i <- 0 until params.nSample) {
        when(i.U === reg_iCPNow) {

          when(reg_iLoopNow === 0.U && reg_tag_Loop) {
            for (j <- 0 until params.nLane) {
              reg_correlationArray((i+j)%params.nSample) := io.Ain.Correlation(j)
            }
          }
          .otherwise {
            for (j <- 0 until params.nLane) {
              reg_correlationArray((i+j)%params.nSample) := reg_correlationArray((i+j)%params.nSample) + io.Ain.Correlation(j)
            }
          }

        }
      }



    }


    when (reg_iCPNow === 0.U && reg_iLoopNow === 0.U && reg_tag_CP) {
      when (max_itm > reg_max) {
        reg_max := max_itm
        reg_CPOpt_itm := CPOpt_itm
        reg_iFreqOpt_itm := Mux(reg_iFreqNow === 0.U, iFreqMax.U, reg_iFreqNow - 1.U)
      }
    }


    when (reg_acq_finished) {
      reg_iFreqOpt_out := reg_iFreqOpt_itm
      reg_CPOpt_out := reg_CPOpt_itm
      reg_sateFound_itm := true.B
    }
  }



}





