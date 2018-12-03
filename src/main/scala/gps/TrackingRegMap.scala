package gps

import chisel3._
import chisel3.util._
import freechips.rocketchip.subsystem.BaseSubsystem
import freechips.rocketchip.config.{Parameters, Field}
import freechips.rocketchip.diplomacy._
import freechips.rocketchip.regmapper.{HasRegMap, RegField}
import freechips.rocketchip.tilelink._
import freechips.rocketchip.util.UIntIsOneOf

case class TrackingParams(address: BigInt, beatBytes: Int)

trait TrackingTLModule extends HasRegMap {
  implicit val p: Parameters
  def params: TrackingParams

  val topParams = TrackingTopParams(5, 16367600, 12, 30)
 
  val adcIn = Reg(UInt(topParams.adcWidth.W))
  val svNumber = Reg(UInt(6.W))
  val carrierNcoBias = Reg(UInt(topParams.ncoWidth.W))
  val codeNcoBias = Reg(UInt(topParams.ncoWidth.W))

  val packetValidOut = Reg(Bool())
  val packetValidBits = Reg(UInt(topParams.packetParams.subframeLength.W))
  val packetExtractedValues = Reg(ExtractedParamsBundle())

  val top = Module(new TrackingTop(topParams))
  top.io.adcIn := adcIn
  top.io.svNumber := svNumber
  top.io.carrierNcoBias := carrierNcoBias
  top.io.codeNcoBias := codeNcoBias 

  packetValidOut := top.io.packetValidOut
  packetValidBits := top.io.packetValidBits 
  packetExtractedValues := top.io.packetExtractedValues

  regmap(
    0x00 -> Seq(
      RegField(topParams.adcWidth, adcIn)),
    0x04 -> Seq(
      RegField(6, svNumber)),
    0x08 -> Seq(
      RegField(topParams.ncoWidth, carrierNcoBias)),
    0x0c -> Seq(
      RegField(topParams.ncoWidth, codeNcoBias)), 
    0x10 -> Seq(
      RegField(1, packetValidOut)), 
    0x14 -> Seq(
      RegField(topParams.packetParams.subframeLength, packetValidBits)), 
    0x18 -> Seq(
      RegField(ExtractedParamsBundle().getWidth, packetExtractedValues.asUInt)), 
  )
}

class TrackingTL(c: TrackingParams)(implicit p: Parameters)
  extends TLRegisterRouter(
    c.address, "tracking", Seq("ucbbar, tracking"),
    beatBytes = c.beatBytes)(
      new TLRegBundle(c, _))(
      new TLRegModule(c, _, _) with TrackingTLModule)

trait HasPeripheryTracking { this: BaseSubsystem =>
  implicit val p: Parameters

  private val address = 0x2000
  private val portName = "tracking"

  val tracking = LazyModule(new TrackingTL(
    TrackingParams(address, pbus.beatBytes))(p))

  pbus.toVariableWidthSlave(Some(portName)) { tracking.node }
}

trait HasPeripheryTrackingModuleImp extends LazyModuleImp {
  implicit val p: Parameters
  val outer: HasPeripheryTracking

}

