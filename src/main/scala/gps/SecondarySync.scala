package gps

import chisel3._

trait SecondaryLockParams[T <: Data] {
}

case class SecondarySyncParams(codeChunks: Int) extends SecondaryLockParams[SInt] {
}

class SecondarySyncIO[T <: Data](params: SecondaryLockParams[T]) extends Bundle {
  val ipIntDump = Input(SInt(32.W)) //bitwidth should be same as in phase prompt intdump
  val lockAchieved = Input(Bool())
  val dump = Input(Bool())
  val secondarySyncAchieved = Output(Bool())

  override def cloneType: this.type = SecondarySyncIO(params).asInstanceOf[this.type]
}

object SecondarySyncIO {
  def apply[T <: Data](params: SecondaryLockParams[T]): SecondarySyncIO[T] =
    new SecondarySyncIO(params)
}


/** Secondary Sync module that is used to determine the 50 Hz bit phase of the GPS signal using
 *  1 ms integrations.  This module is generic to integration threshold (dependent on the integration
 *  time) and to the bitwidth of the integration and dump module output.  Documented in doc/TimeKeeper.md
 *
 *  @param: intThreshold the integration threshold value that is used to determine the polarity of the 
 *  currently integrated bit
 *  @param: intDumpWidth the width of the integration and dump accumulator
 *
 *  IO:
 *  ipIntDump: Input(SInt), the output of the integration and dump accumulator
 *  lockAchieved: Input(Bool), true when the tracking loop has a lock on the signal which means the
 *  secondary sync module can now proceed to determine the 50 Hz bit phase.
 *  dump: Input(Bool), the same signal that is sent to the integration and dump modules; it is used
 *  here to determine when to sample the output of the integration since it will be maximum
 *  secondarySyncAchieved: Output(Bool), module outputs this as true when the 50 Hz bit phase has
 *  been recovered.
 *
 *  Testing:
 *  run: sbt test:testOnly gps.SecondarySyncSpec
 */
class SecondarySync[T <: Data](params: SecondaryLockParams[T]) extends Module {
  val io = IO(SecondarySyncIO(params))
  
  val stall = 0.U(2.W)
  val firstCheck = 1.U(2.W)
  val integrating = 2.U(2.W)
  val secondaryLock = 3.U(2.W)
  val state = RegInit(stall)

  when (!io.lockAchieved) {
    state := stall
  } .elsewhen (state === stall && io.lockAchieved) {
    state := firstCheck
  } .elsewhen (state === firstCheck && io.dump) {
    state := integrating
  } .elsewhen (state === integrating && io.secondarySyncAchieved) {
    state := secondaryLock
  } .elsewhen (state === secondaryLock && !io.lockAchieved) {
    state := stall
  }
  
  val syncReg = Reg(Bool())
  val syncRegDelay = Reg(Bool())

  when (state === stall) {
    syncReg := false.B
  } .elsewhen (state === firstCheck || state === integrating) {
    when (io.dump && io.ipIntDump > params.intThreshold.S) { //TODO make general to input size
      syncReg := true.B
    } .elsewhen (io.dump && io.ipIntDump < (-1*params.intThreshold).S) {
      syncReg := false.B
    }
  } //TODO add logic for when state is secondaryLock?

  //use state firstCheck to initially set syncReg and syncRegDelay to the same value
  when (state === stall) {
    syncRegDelay := false.B
  } .elsewhen (state === firstCheck) {
      when (io.dump && io.ipIntDump > params.intThreshold.S) { //TODO make general to input size
        syncRegDelay := true.B
      } .elsewhen (io.dump && io.ipIntDump < (-1*params.intThreshold).S) {
        syncRegDelay := false.B
      } 
  } .elsewhen (state === integrating && io.dump) {
    syncRegDelay := syncReg
  }


  //secondary sync is achieved when there is a mismatch between the current integration and the last one
  when ((state === integrating && syncReg =/= syncRegDelay) || state === secondaryLock) {
    io.secondarySyncAchieved := true.B
  } .otherwise {
    io.secondarySyncAchieved := false.B
  }

}
