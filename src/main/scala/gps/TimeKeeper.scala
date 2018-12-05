package gps

import chisel3._

case class TimeKeeperParams (
  val fcoWidth: Int,
  val resolutionWidth: Int
)


class TimeKeeperIO(params: TimeKeeperParams) extends Bundle {
  val record = Input(Bool())
  val preambleDetected = Input(Bool())
  val ncoInput = Input(UInt(params.resolutionWidth.W)) //input of nco for highest resolution
  val caInput = Input(SInt(params.fcoWidth.W)) //input of CA code gen for ccounting chips
  val ncoDialOut = Output(UInt(params.resolutionWidth.W))
  val chipDialOut = Output(UInt(10.W))
  val msDialOut = Output(UInt(32.W))
  //override def clonetype: this.type = TimeKeeperIO(params).asInstanceOf[this.type]
}

object TimeKeeperIO {
  def apply(params: TimeKeeperParams): TimeKeeperIO = new TimeKeeperIO(params)
}

/** TimeKeeper is a module that is used to track the GPS signal transmit time from a known
 *  GPS second based on the navigation message.  This module tracks several levels of time resoultion
 *  for the positioning process. Documented in doc/TimeKeeper.md.
 *
 * @param fcoWidth the width of the output of the NCO used to track the code chips
 * @param resolutionWidth width of the accumulator in the NCO for measuring sub-chip level of time
 *
 * IO:
 *
 * '''record''': Input(Bool) starting recording time since the exact GPS time is known at that moment
 *
 * '''preambleDetected''': Input(Bool) indicates preamble is detected so that the TimeKeeper can start synchronized to a precise GPS second
 *
 * '''ncoInput''': Input(UInt) input also to the NCO used for sub-chip level time accuracy
 * 
 * '''caInput''': Input(SInt) output of NCO that is used to determine PRN chip transitions based on zero-crossings
 * 
 * '''ncoDialOut''': Output(UInt) smallest time resolution output for sub-chip level timing
 * 
 * '''chipDialOut''': Output(UInt) tracker for number of chips elapsed mod 1023 (1023 chips is 1ms)
 * 
 * '''msDialOut''': Output(UInt) tracker for number of milliseconds elapsed since the known GPS second measured from the GPS message 
 */
class TimeKeeper(val params: TimeKeeperParams) extends Module {
  val io = IO(new TimeKeeperIO(params))

  val sIdle = 0.U(1.W)
  val sCounting = 1.U(1.W)
  val state = RegInit(0.U(2.W))
  
  //state machine; begin operation when the control signals with io.record
  //which means that the navigation knows the GPS second at that moment
  when (state === sIdle) {
    when (io.record) {
      state := sCounting
    }
  } .otherwise {
    when (!io.record) {
      state := sIdle
    }
  }

  val ncoDial = RegInit(0.U(params.resolutionWidth.W))
  val chipDial = RegInit(0.U(10.W))
  val msDial = RegInit(0.U(32.W))
  io.ncoDialOut := ncoDial
  io.chipDialOut := chipDial
  io.msDialOut := msDial

  when (io.record) {
    ncoDial := ncoDial + io.ncoInput
  } .otherwise {
    ncoDial := 0.U
  }
  
  //store the previous input to detect zero crossing for chip change
  val caInputPrev = RegInit(0.S(params.fcoWidth.W))
  caInputPrev := io.caInput

  //detect zero crossings of the NCO output to determine chip transitions
  when (io.record) {
    when(caInputPrev < 0.S && io.caInput >= 0.S) {
      chipDial := chipDial + 1.U
    }
  } .otherwise {
    chipDial := 0.U
  }

  val chipDialPrev = Reg(UInt(10.W))
  chipDialPrev := chipDial
  
  //detect a millisecond when 1023 chips have elapsed
  when (io.record) {
    when(chipDialPrev > 0.U && chipDial === 0.U) {
      msDial := msDial + 1.U
    }
  } .otherwise {
    msDial := 0.U
  }

}
