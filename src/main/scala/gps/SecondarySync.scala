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
    when (io.dump && io.ipIntDump > 1000.S) { //TODO make general to input size
      syncReg := true.B
    } .elsewhen (io.dump && io.ipIntDump < (-1000).S) {
      syncReg := false.B
    }
  } //TODO add logic for when state is secondaryLock?

  //use state firstCheck to initially set syncReg and syncRegDelay to the same value
  when (state === stall) {
    syncRegDelay := false.B
  } .elsewhen (state === firstCheck) {
      when (io.dump && io.ipIntDump > 1000.S) { //TODO make general to input size
        syncRegDelay := true.B
      } .elsewhen (io.dump && io.ipIntDump < (-1000).S) {
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
