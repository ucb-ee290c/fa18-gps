package gps

import freechips.rocketchip.diplomacy.{LazyModule, LazyModuleImp}
import freechips.rocketchip.config.Parameters
import freechips.rocketchip.devices.tilelink._
import freechips.rocketchip.subsystem._
import freechips.rocketchip.util.DontTouch

trait HasPeripheryAcq extends BaseSubsystem {
  // instantiate Acq top block
  val AcqChain = LazyModule(new AcqThing())
  // connect memory interfaces to pbus, modified it if you have mem to bus
   pbus.toVariableWidthSlave(Some("QueueWrite")) { AcqChain.writeQueue.mem.get }
   pbus.toVariableWidthSlave(Some("AcqControl")) { AcqChain.acq.mem.get }
   pbus.toVariableWidthSlave(Some("QueueRead")) { AcqChain.readQueue.mem.get }
}

//class ExampleTop(implicit p: Parameters) extends RocketSubsystem
//    with CanHaveMasterAXI4MemPort
//    with HasPeripheryBootROM
//    with HasSyncExtInterrupts {
//  override lazy val module = new ExampleTopModule(this)
//}
//
//class ExampleTopModule[+L <: ExampleTop](l: L) extends RocketSubsystemModuleImp(l)
//    with HasRTCModuleImp
//    with CanHaveMasterAXI4MemPortModuleImp
//    with HasPeripheryBootROMModuleImp
//    with HasExtInterruptsModuleImp
//    with DontTouch

class ExampleTopWithAcq(implicit p: Parameters) extends ExampleTop
    // mix in Acq top block
    with HasPeripheryAcq {
  override lazy val module = new ExampleTopModule(this)
}
