package gps

import chisel3._

//TODO add params

case class TimeKeeperParams (
  val fcoWidth: Int,
  val resolutionWidth: Int
)


class TimeKeeperIO(params: TimeKeeperParams) extends Bundle {
  val record = Input(Bool())
  val preambleDetected = Input(Bool())
  val ncoInput = Input(UInt(params.resolutionWidth.W)) //TODO input of nco for highest resolution
  val caInput = Input(SInt(params.fcoWidth.W)) //TODO input of CA code gen for ccounting chips
  val timeOutput = Output(UInt(32.W)) //TODO output time in ns

  //override def clonetype: this.type = TimeKeeperIO(params).asInstanceOf[this.type]
}

object TimeKeeperIO {
  def apply(params: TimeKeeperParams): TimeKeeperIO = new TimeKeeperIO(params)
}

class TimeKeeper(val params: TimeKeeperParams) extends Module {
  val io = IO(new TimeKeeperIO(params))

  val sIdle = 0.U(1.W)
  val sCounting = 1.U(1.W)
  val state = RegInit(0.U(2.W))

  when (state === sIdle) {
    when (io.record) {
      state := sCounting
    }
  } .otherwise {
    when (!io.record) {
      state := sIdle
    }
  }


  
  val ncoDial = RegInit(0.U(params.resolutionWidth.W)) //TODO fix reg width
  val chipDial = RegInit(0.U(10.W)) //TODO parameterize from code length
  val msDial = RegInit(0.U(32.W))

  when (io.record) {
    ncoDial := ncoDial + io.ncoInput
  } .otherwise {
    ncoDial := 0.U
  }
  
  //store the previous input to detect zero crossing for chip change
  val caInputPrev = RegInit(0.S(params.fcoWidth.W)) //TODO fix reg width
  caInputPrev := io.caInput

  when (io.record) {
    when(caInputPrev < 0.S && io.caInput >= 0.S) {
      chipDial := chipDial + 1.U
    }
  } .otherwise {
    chipDial := 0.U
  }

  val chipDialPrev = Reg(UInt(10.W))
  chipDialPrev := chipDial

  when (io.record) {
    when(chipDialPrev > 0.U && chipDial === 0.U) {
      msDial := msDial + 1.U
    }
  } .otherwise {
    msDial := 0.U
  }

}
