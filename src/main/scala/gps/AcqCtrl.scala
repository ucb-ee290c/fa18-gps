package gps

import chisel3._
import chisel3.experimental.FixedPoint
import chisel3.util._
import dsptools.numbers._

case class ACtrlParams (
  val nLoop: Int,
  val nFreq: Int,
  val FreqMin: Int,
  val nSample: Int,
  val wCorrelation: Int,
  val wLoop: Int,
  val wFreq: Int,
  val wCodePhase: Int,
  val wADC: Int,
  val wSate: Int,
)


// input interface within the acquisition loop
class ACtrlAInputBundle(params: ACtrlParams) extends Bundle {
  val ADC: Vec(params.nSample, UInt(params.wADC.W))
  val CodePhase: UInt(params.wCodePhase.W)
  val Correlation: SInt(params.wCorrelation.W)

  override def cloneType: this.type = ACtrlAInputBundle(params).asInstanceOf[this.type]
}
object ACtrlAInputBundle {
  def apply(params: ACtrlParams): ACtrlAInputBundle = new ACtrlAInputBundle(params)
}


// output interface within the acquisition loop
class ACtrlAOutputBundle(params: ACtrlParams) extends Bundle {
  val ADC: Vec(params.nSample, UInt(params.wADC.W))
  val Freq: UInt(params.wFreq.W)
  val OptFreq: UInt(params.wFreq.W)
  val OptCodePhase: UInt(params.wCodePhase.W)
  val SateFound: Bool()

  override def cloneType: this.type = ACtrlAOutputBundle(params).asInstanceOf[this.type]
}
object ACtrlAOutputBundle {
  def apply(params: ACtrlParams): ACtrlAOutputBundle = new ACtrlAOutputBundle(params)
}

// input interface to the tracking loop
class ACtrlTInputBundle(params: ACtrlParams) extends Bundle {
  val idx_sate: UInt(params.wSate.W)


  override def cloneType: this.type = ACtrlTInputBundle(params).asInstanceOf[this.type]
}
object ACtrlTInputBundle {
  def apply(params: ACtrlParams): ACtrlTInputBundle = new ACtrlTInputBundle(params)
}

// output interface to the tracking loop
class ACtrlTOutputBundle(params: ACtrlParams) extends Bundle {
  val ADC: Vec(params.nSample, UInt(params.wADC.W))
  val Freq: UInt(params.wFreq.W)
  val OptFreq: UInt(params.wFreq.W)
  val OptCodePhase: UInt(params.wCodePhase.W)
  val SateFound: Bool()

  override def cloneType: this.type = ACtrlTOutputBundle(params).asInstanceOf[this.type]
}
object ACtrlTOutputBundle {
  def apply(params: ACtrlParams): ACtrlTOutputBundle = new ACtrlTOutputBundle(params)
}


class ACtrlIO(params: ACtrlParams) extends Bundle {
  val Ain = Flipped(Decoupled(ACtrlAInputBundle(params)))
  val Aout = Decoupled(ACtrlAOutputBundle(params))
  val Tin = Flipped(Decoupled(ACtrlTInputBundle(params)))
  val Tout = Decoupled(ACtrlTOutputBundle(params))


  override def cloneType: this.type = ACtrlIO(params).asInstanceOf[this.type]
}


class ACtrl(params: ACtrlParams) extends Module {

  val io = IO(ACtrlIO(params))

  val reg_cnt = RegInit(UInt(params.wCodePhase.W), 0.U)
  reg_cnt := Mux(reg_cnt === params.nSample-1.U, 0.U, reg_cnt+1.U)
  val reg_shift = Reg(Vec(params.nSample, SInt(params.wADC.W)))

  val reg_idxCP = RegInit(UInt(params.wCodePhase.W), 0.U)
  val reg_idxLoop = RegInit(UInt(params.wLoop.W), 0.U)
  val reg_idxFreq = RegInit(UInt(params.wFreq.W), 0.U)

  // the index of CodePhase, Loop and Frequency of the next cycle, if there is no output from the FFT
  // block, i.e. no io.Ain.fire(), none of them will be different from the current state
  val switchCP= io.Ain.fire()
  val switchLoop = switchCP && (reg_idxCP === (params.nSample-1).U)
  val switchFreq = switchLoop && (reg_idxLoop === (params.nLoop-1).U)
  val switchSate = switchFreq && (reg_idxFreq === (params.nFreq-1).U)

  val idxCPNext = Mux(switchCP, Mux(switchLoop, 0.U, reg_idxCP+1.U), reg_idxCP)
  val idxLoopNext = Mux(switchLoop, Mux(switchFreq, 0.U, reg_idxLoop+1.U), reg_idxLoop)
  val idxFreqNext = Mux(switchFreq, Mux(switchSate, 0.U, reg_idxFreq+1.U), reg_idxFreq)

  reg_idxCP := idxCPNext
  reg_idxLoop := idxLoopNext
  reg_idxFreq := idxFreqNext

  reg state = RegInit(UInt(1.W), 0.U)
  val idle = Wire(UInt(1.W), 0.U)
  val acq = Wire(UInt(1.W), 1.U)
  state := Mux(state === idle, Mux(io.Tin.valid, acq, idle), Mux(io.Tout.fire(), idle, acq))



  // TODO: is io.Ain.ready always true?
  io.Ain.ready := (state === acq)
  io.Aout.valid := (state === acq) && switchCP
  io.Tin.ready := state === idle
  io.Tout.valid := (state === acq) && iswitchSate



  // val nbit_sum =
  // val nbit_max =

  // input and output signals



}



