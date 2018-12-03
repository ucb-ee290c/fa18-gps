// See LICENSE.SiFive for license details.

package gps

import chisel3._
import freechips.rocketchip.config.Parameters
import freechips.rocketchip.devices.debug.Debug
import freechips.rocketchip.diplomacy.LazyModule
import freechips.rocketchip.util.GeneratorApp

class TestHarness()(implicit p: Parameters) extends Module {
  val io = IO(new Bundle {
    val success = Output(Bool())
  })

/*
  val dut = p(BuildTop)(clock, reset.toBool, p)
  dut.debug := DontCare
  dut.connectSimAXIMem()
  dut.dontTouchPorts()
  dut.tieOffInterrupts()
  io.success := dut.connectSimSerial()
*/

  val dut = Module(LazyModule(new ExampleTopWithTracking).module)
  dut.reset := reset.toBool() | dut.debug.ndreset

  dut.dontTouchPorts()
  dut.tieOffInterrupts()
  dut.connectSimAXIMem()
  Debug.connectDebug(dut.debug, clock, reset.toBool(), io.success) 
}

object Generator extends GeneratorApp {
  val longName = names.configProject + "." + names.configs
  generateFirrtl
  generateAnno
  generateTestSuiteMakefrags
  generateROMs
  generateArtefacts
}
