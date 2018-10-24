package fft

import freechips.rocketchip.diplomacy.{LazyModule, LazyModuleImp}
import freechips.rocketchip.config.Parameters
import freechips.rocketchip.devices.tilelink._
import freechips.rocketchip.subsystem._
import freechips.rocketchip.util.DontTouch

trait HasPeripheryFFT extends BaseSubsystem {
  // instantiate FFT chain
  val FFTChain = LazyModule(new FFTThing())
  // connect memory interfaces to pbus
  pbus.toVariableWidthSlave(Some("QueueWrite")) { FFTChain.writeQueue.mem.get }
  pbus.toVariableWidthSlave(Some("FFTControl")) { FFTChain.fft.mem.get }
  pbus.toVariableWidthSlave(Some("QueueRead")) { FFTChain.readQueue.mem.get }
}

class ExampleTop(implicit p: Parameters) extends RocketSubsystem
    with CanHaveMasterAXI4MemPort
    with HasPeripheryBootROM
    with HasSyncExtInterrupts {
  override lazy val module = new ExampleTopModule(this)
}

class ExampleTopModule[+L <: ExampleTop](l: L) extends RocketSubsystemModuleImp(l)
    with HasRTCModuleImp
    with CanHaveMasterAXI4MemPortModuleImp
    with HasPeripheryBootROMModuleImp
    with HasExtInterruptsModuleImp
    with DontTouch

class ExampleTopWithFFT(implicit p: Parameters) extends ExampleTop
    // mix in FFT
    with HasPeripheryFFT {
  override lazy val module = new ExampleTopModule(this)
}
