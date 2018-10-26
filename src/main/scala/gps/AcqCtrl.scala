package gps

import chisel3._
//import chisel3.experimental.FixedPoint
//import chisel3.util.Decoupled
import chisel3.util._
import scala.math._
import dsptools.numbers._

trait ACtrlParams [T1 <: Data, T2 <: Data, T3 <: Data] {
  val nLoop: Int
  val nFreq: Int
  val nSample: Int
  val wCorrelation: Int
  val wLoop: Int
  val wIdxFreq: Int
  val wFreq: Int
  val wCodePhase: Int
  val wADC: Int
  val wSate: Int
  val lane: Int
  val freqMin: Int
  val freqStep: Int
//  val wMax: Int
//  val wSum: Int

  val pIdxFreq: T1
  val pFreq: T1
  val pLoop: T1
  val pCodePhase: T1
  val pSate: T1
  val pADC: T2
  val pCorrelation: T3
  val pMax: T3
  val pSum: T3

}

case class IntACtrlParams (
                         val nLoop: Int,
                         val nFreq: Int,
                         val nSample: Int,
                         val wCorrelation: Int,
                         val wLoop: Int,
                         val wIdxFreq: Int,
                         val wFreq: Int,
                         val wCodePhase: Int,
                         val wADC: Int,
                         val wSate: Int,
                         val lane: Int,
                         val freqMin: Int,
                         val freqStep: Int,


                       ) extends ACtrlParams[UInt, SInt, DspReal] {

  val wMax: Int = wCodePhase + wLoop + wADC
  val wSum: Int = wMax + wCodePhase + wIdxFreq


  val pIdxFreq = UInt(wIdxFreq.W)
  val pFreq = UInt(wFreq.W)
  val pLoop = UInt(wLoop.W)
  val pCodePhase = UInt(wCodePhase.W)
  val pSate = UInt(wSate.W)
  val pADC = SInt(wADC.W)
  val pCorrelation = DspReal()
  val pMax = DspReal()
  val pSum = DspReal()

//  val pCorrelation = SInt(wCorrelation.W)
//  val pMax = SInt(wMax.W)
//  val pSum = SInt(wSum.W)

}





// input interface within the acquisition loop
class ACtrlAInputBundle[T1 <: Data, T2 <: Data, T3 <: Data](params: ACtrlParams[T1, T2, T3]) extends Bundle {
//  val ADC: T2 = params.pADC
//  val CodePhase: T1 = params.pCodePhase
//  val Correlation: T3 = params.pCorrelation
  val ADC: T2 = Input(params.pADC)
  val CodePhase: T1 = Input(params.pCodePhase)
  val Correlation: T3 = Input(params.pCorrelation)
  val ready = Output(Bool())
  val valid = Input(Bool())

  override def cloneType: this.type = ACtrlAInputBundle(params).asInstanceOf[this.type]
}
object ACtrlAInputBundle {
  def apply[T1 <: Data, T2 <: Data, T3 <: Data](params: ACtrlParams[T1, T2, T3]): ACtrlAInputBundle[T1, T2, T3] = new ACtrlAInputBundle(params)
}


// output interface within the acquisition loop
class ACtrlAOutputBundle[T1 <: Data, T2 <: Data, T3 <: Data](params: ACtrlParams[T1, T2, T3]) extends Bundle {

  val freqNow: T1 = Output(params.pFreq.cloneType)
  val freqNext: T1 = Output(params.pFreq.cloneType)
  val cpNow: T1 = Output(params.pCodePhase.cloneType)
  val cpNext: T1 = Output(params.pCodePhase.cloneType)
  val ready = Input(Bool())
  val valid = Output(Bool())

  override def cloneType: this.type = ACtrlAOutputBundle(params).asInstanceOf[this.type]
}
object ACtrlAOutputBundle {
  def apply[T1 <: Data, T2 <: Data, T3 <: Data](params: ACtrlParams[T1, T2, T3]): ACtrlAOutputBundle[T1, T2, T3] = new ACtrlAOutputBundle(params)
}

// input interface to the tracking loop
class ACtrlTInputBundle[T1 <: Data, T2 <: Data, T3 <: Data](params: ACtrlParams[T1, T2, T3]) extends Bundle {
  val idx_sate: T1 = Input(params.pSate)
  val valid = Input(Bool())
  val ready = Output(Bool())

  override def cloneType: this.type = ACtrlTInputBundle(params).asInstanceOf[this.type]
}
object ACtrlTInputBundle {
  def apply[T1 <: Data, T2 <: Data, T3 <: Data](params: ACtrlParams[T1, T2, T3]): ACtrlTInputBundle[T1, T2, T3] = new ACtrlTInputBundle(params)
}

// output interface to the tracking loop
class ACtrlTOutputBundle[T1 <: Data, T2 <: Data, T3 <: Data](params: ACtrlParams[T1, T2, T3]) extends Bundle {

  val optFreq: T1 = Output(params.pFreq.cloneType)
  val optCP: T1 = Output(params.pCodePhase.cloneType)
  val sateFound = Output(Bool())
  val optIdxFreqItm: T1 = Output(params.pIdxFreq.cloneType)
  val optIdxFreqOut: T1 = Output(params.pIdxFreq.cloneType)
  val optCPItm: T1 = Output(params.pCodePhase.cloneType)
  val optCPOut: T1 = Output(params.pCodePhase.cloneType)
  val ready = Input(Bool())
  val valid = Output(Bool())

  override def cloneType: this.type = ACtrlTOutputBundle(params).asInstanceOf[this.type]
}
object ACtrlTOutputBundle {
  def apply[T1 <: Data, T2 <: Data, T3 <: Data](params: ACtrlParams[T1, T2, T3]): ACtrlTOutputBundle[T1, T2, T3] = new ACtrlTOutputBundle(params)
}

class DummyBundle[T1 <: Data, T2 <: Data, T3 <: Data](params: ACtrlParams[T1, T2, T3]) extends Bundle {

  val sum: T3 = Output(params.pSum.cloneType)
  val max: T3 = Output(params.pMax.cloneType)
//  val cArr0: T3 = Output(params.pCorrelation.cloneType)
//  val cArr1: T3 = Output(params.pCorrelation.cloneType)

  override def cloneType: this.type = DummyBundle(params).asInstanceOf[this.type]
}
object DummyBundle {
  def apply[T1 <: Data, T2 <: Data, T3 <: Data](params: ACtrlParams[T1, T2, T3]): DummyBundle[T1, T2, T3] = new DummyBundle(params)
}



class ACtrlIO[T1 <: Data, T2 <: Data, T3 <: Data](params: ACtrlParams[T1, T2, T3]) extends Bundle {
//  val Ain = Flipped(Decoupled(Vec(params.nSample, ACtrlAInputBundle(params))))
//  val Aout = Decoupled(ACtrlAOutputBundle(params))
//  val Tin = Flipped(Decoupled(ACtrlTInputBundle(params)))
//  val Tout = Decoupled(ACtrlTOutputBundle(params))
  val Ain = ACtrlAInputBundle(params)
  val Aout = ACtrlAOutputBundle(params)
  val Tin = ACtrlTInputBundle(params)
  val Tout = ACtrlTOutputBundle(params)
//  val Reg = DummyBundle(params)


  override def cloneType: this.type = ACtrlIO(params).asInstanceOf[this.type]
}
object ACtrlIO {
  def apply[T1 <: Data, T2 <: Data, T3 <: Data](params: ACtrlParams[T1, T2, T3]): ACtrlIO[T1, T2, T3] =
    new ACtrlIO(params)
}


class ACtrl[T1 <: Data, T2 <: Data, T3 <: Data:ConvertableTo:Ring:Real](params: ACtrlParams[T1,T2,T3]) extends Module {

  val io = IO(ACtrlIO(params))

  val Ain_fire = io.Ain.ready && io.Ain.valid
  val Aout_fire = io.Aout.ready && io.Aout.valid
  val Tin_fire = io.Tin.ready && io.Tin.valid
  val Tout_fire = io.Tout.ready && io.Tout.valid



  val reg_cnt = RegInit(UInt(params.wCodePhase.W), 0.U)
  reg_cnt := Mux(reg_cnt === (params.nSample-1).U, 0.U, reg_cnt+(1.U))
//  val reg_shift = Reg(Vec(params.nSample, params.pADC))

  val reg_idxCP = RegInit(UInt(params.wCodePhase.W), 0.U)
  val reg_idxLoop = RegInit(UInt(params.wLoop.W), 0.U)
  val reg_idxFreq = RegInit(UInt(params.wIdxFreq.W), 0.U)

  // the index of CodePhase, Loop and Frequency of the next cycle, if there is no output from the FFT
  // block, i.e. no io.Ain.fire(), none of them will be different from the current state
  val switchCP= Ain_fire
  val switchLoop = switchCP && (reg_idxCP === (params.nSample-1).U)
  val switchFreq = switchLoop && (reg_idxLoop === (params.nLoop-1).U)
  val reg_switchFreq = RegNext(switchFreq)
  val switchSate = switchFreq && (reg_idxFreq === (params.nFreq-1).U)
  val reg_switchSate = RegNext(switchSate)

  val idxCPNext = Mux(switchCP, Mux(switchLoop, 0.U, reg_idxCP+1.U), reg_idxCP)
  val idxLoopNext = Mux(switchLoop, Mux(switchFreq, 0.U, reg_idxLoop+1.U), reg_idxLoop)
  val idxFreqNext = Mux(switchFreq, Mux(switchSate, 0.U, reg_idxFreq+1.U), reg_idxFreq)

  reg_idxCP := idxCPNext
  reg_idxLoop := idxLoopNext
  reg_idxFreq := idxFreqNext

  io.Aout.freqNow := reg_idxFreq * params.freqStep.U + params.freqMin.U
  io.Aout.freqNext := idxFreqNext * params.freqStep.U + params.freqMin.U
  io.Aout.cpNow := reg_idxCP
  io.Aout.cpNext := idxCPNext

  val reg_state = RegInit(UInt(1.W), 0.U)
  val idle = Wire(UInt(1.W), 0.U)
  val acq = Wire(UInt(1.W), 1.U)
  reg_state := Mux(reg_state === idle, Mux(Tin_fire, acq, idle), Mux(io.Tout.valid, idle, acq))

  val reg_not_reset = RegInit(Bool(), false.B)
  reg_not_reset := Mux(switchSate, true.B, reg_not_reset)

  // TODO: is io.Ain.ready always true?
  io.Ain.ready := (reg_state === acq)
  io.Aout.valid := (reg_state === acq) && switchCP
  io.Tin.ready := reg_state === idle
  io.Tout.valid := ((reg_state === idle) && reg_not_reset) || reg_switchSate

//  io.Reg.max := reg_max
//  io.Reg.sum := reg_sum
//  io.Reg.cArr0 := reg_correlationArray(0)
//  io.Reg.cArr1 := reg_correlationArray(1)


  val reg_max = RegInit(params.pMax, ConvertableTo[T3].fromInt(0))
  val reg_correlationArray = Reg(Vec(params.nSample, params.pCorrelation))
  val reg_sum = RegInit(params.pSum, ConvertableTo[T3].fromInt(0))
  // use _itm signals as outputs now
  val reg_optIdxFreq_itm = Reg(UInt(params.wIdxFreq.W))
  val reg_optIdxFreq_out = Reg(UInt(params.wIdxFreq.W))
  val reg_optCP_itm = Reg(UInt(params.wCodePhase.W))
  val reg_optCP_out = Reg(UInt(params.wCodePhase.W))

  // TODO: hardcoded, should depend on k
  val threshold = 6

  val reg_sateFound_itm = RegInit(Bool(), false.B)
  val reg_sateFound_out = RegInit(Bool(), false.B)

  val optIdxFreq = Mux(switchSate, reg_optIdxFreq_itm, reg_optIdxFreq_out)
  io.Tout.optFreq := optIdxFreq * params.freqStep.U + params.freqMin.U
  io.Tout.optCP := Mux(switchSate, reg_optCP_itm, reg_optCP_out)
  io.Tout.optIdxFreqItm := reg_optIdxFreq_itm
  io.Tout.optIdxFreqOut := reg_optIdxFreq_out
  io.Tout.optCPItm := reg_optCP_itm
  io.Tout.optCPOut := reg_optCP_out
  io.Tout.sateFound := Mux(switchSate, reg_sateFound_itm, reg_sateFound_out)


  // should be fine to reset reg_max, reg_correlationArray and reg_sum in idle state since this will not
  // affect reg_optFreq and reg_optCP, if affected, try to
  // reset then if requested to start acquisition for a new satellite
  when(reg_state === idle) {

//    when(io.Tin.valid) {
//
//    }.otherwise
    reg_max := 0.U.asReal()
    for (i <- 0 until params.nSample) {
      reg_correlationArray(i) := 0.U.asReal()
    }
    reg_sum := 0.U.asReal()

  } .otherwise {
    // once we get data from the fft, update the reg_correlationArray and reg_sum
    when (switchCP) {
      reg_sum := reg_sum + io.Ain.Correlation
      for (i <- 0 until params.nSample) {
        when(reg_idxCP === i.U) {
          reg_correlationArray(i) := reg_correlationArray(i) + io.Ain.Correlation
        }
      }
    }

      // if the loop for a certain frequency is finished, finout the maximum correlation in the array
      // the correlation array needs to resset
    when (switchFreq) {
      for (i <- 0 until params.nSample) {
        if (i < params.nSample - 1) {
          when(reg_correlationArray(i) > reg_max) {
            reg_max := reg_correlationArray(i)
            reg_optCP_itm := reg_idxCP
            reg_optIdxFreq_itm := reg_idxFreq
          }
        }
        else {
          when(reg_correlationArray(i) + io.Ain.Correlation > reg_max) {
            reg_max := reg_correlationArray(i) + io.Ain.Correlation
            reg_optCP_itm := reg_idxCP
            reg_optIdxFreq_itm := reg_idxFreq
          }
        }
        reg_correlationArray(i) := 0.S.asReal()

      }
    }

    when (reg_switchSate) {
      reg_optIdxFreq_out := reg_optIdxFreq_itm
      reg_optCP_out := reg_optCP_itm
        // TODO: fix bugs here
      reg_sateFound_itm := true.B
//            reg_sateFound_itm := reg_max * ConvertableTo[T3].fromInt(params.nSample) * ConvertableTo[T3]fromInt(params.nFreq) >
//                               ConvertableTo[T3]fromInt(threshold) * reg_sum
      reg_sateFound_out := reg_sateFound_itm

    }



  }


}

  // reset reg_optFreq and

  // input and output signals







