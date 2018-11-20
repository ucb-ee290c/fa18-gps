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

  when (state === stall && io.lockAchieved) { //TODO add && io.dump so that the first change in the variable is not detected
    state := firstCheck
  } .elsewhen (state === firstCheck && io.dump) {
    state := integrating
  } .elsewhen (state === integrating && io.secondarySyncAchieved) {
    state := secondaryLock  //TODO make it change to stall when loses lock
  } .elsewhen (state === secondaryLock && !io.lockAchieved) {
    state := stall
  }
  
  //TODO use iter to output a pulse at the edges of the data bit after secondary sync has been achieved
//  val iter = Reg(8.W) //make parameterized width based on input size
//  
//  when (state === integrating) {
//    iter := iter + 1
//  } .else {
//    iter := 0
//  }
//
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
  







//  val syncCheck = RegInit(VecInit(Seq.fill(params.codeChunks)(0.S(64.W)))) //TODO fix this bitwidth 
//  val syncBools = RegInit(VecInit(Seq.fill(params.codeChunks)(Bool())))
//  
//  when (state === integrating) {
//    syncBools(iter) := true.B
//  } .elsewhen (state === stall) {
//    syncBools := Seq.fill(params.codeChunks)(Bool())  //TODO check if this will work
//  }
//
//  for (i <- 0 until params.codeChunks) {
//    when (state === integrating && syncBools(i) === true.B) {
//      syncCheck(i) := syncCheck(i) + io.ipIntDump
//    } .elsewhen (state === stall) {
//      syncCheck(i) := 0.S
//    }
//  }
//
  //add state machine for stalling until lock, integrating, then finding bin with best integration
  //could try both implementations (one being with the 20 integrations and the other being
  //with integrating until it goes in the other direction)
  
  


}
