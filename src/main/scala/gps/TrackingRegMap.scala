package gps

import chisel3._
import freechips.rocketchip.subsystem.BaseSubsystem
import freechips.rocketchip.config.Parameters
import freechips.rocketchip.diplomacy._
import freechips.rocketchip.regmapper.{HasRegMap, RegField}
import freechips.rocketchip.tilelink._

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
  val packetExtractedValues = Reg(ExtractedParamsUIntBundle())

  val top = Module(new TrackingTop(topParams))
  top.io.adcIn := adcIn.asSInt
  top.io.svNumber := svNumber
  top.io.carrierNcoBias := carrierNcoBias
  top.io.codeNcoBias := codeNcoBias 

  packetValidOut := top.io.packetValidOut
  packetValidBits := top.io.packetValidBits.asUInt 

  packetExtractedValues.subframe_id := top.io.packetExtractedValues.subframe_id
  packetExtractedValues.week_number := top.io.packetExtractedValues.week_number
  packetExtractedValues.sv_accuracy := top.io.packetExtractedValues.sv_accuracy
  packetExtractedValues.sv_health := top.io.packetExtractedValues.sv_health
  packetExtractedValues.iodc := top.io.packetExtractedValues.iodc
  packetExtractedValues.t_gd := top.io.packetExtractedValues.t_gd.asUInt
  packetExtractedValues.a_f2 := top.io.packetExtractedValues.a_f2.asUInt
  packetExtractedValues.a_f1 := top.io.packetExtractedValues.a_f1.asUInt
  packetExtractedValues.a_f0 := top.io.packetExtractedValues.a_f0.asUInt
  packetExtractedValues.iode := top.io.packetExtractedValues.iode
  packetExtractedValues.c_rs := top.io.packetExtractedValues.c_rs.asUInt
  packetExtractedValues.delta_n := top.io.packetExtractedValues.delta_n.asUInt
  packetExtractedValues.m_0 := top.io.packetExtractedValues.m_0.asUInt
  packetExtractedValues.c_uc := top.io.packetExtractedValues.c_uc.asUInt
  packetExtractedValues.e := top.io.packetExtractedValues.e
  packetExtractedValues.c_us := top.io.packetExtractedValues.c_us.asUInt
  packetExtractedValues.sqrt_a := top.io.packetExtractedValues.sqrt_a
  packetExtractedValues.t_oe := top.io.packetExtractedValues.t_oe
  packetExtractedValues.c_ic := top.io.packetExtractedValues.c_ic.asUInt
  packetExtractedValues.omega_0 := top.io.packetExtractedValues.omega_0.asUInt
  packetExtractedValues.c_is := top.io.packetExtractedValues.c_is.asUInt
  packetExtractedValues.i_0 := top.io.packetExtractedValues.i_0.asUInt
  packetExtractedValues.c_rc := top.io.packetExtractedValues.c_rc.asUInt
  packetExtractedValues.omega := top.io.packetExtractedValues.omega.asUInt
  packetExtractedValues.dot_omega := top.io.packetExtractedValues.dot_omega.asUInt
  packetExtractedValues.idot := top.io.packetExtractedValues.idot.asUInt


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
      RegField(3, packetExtractedValues.subframe_id)), 
    0x1c -> Seq(
      RegField(10, packetExtractedValues.week_number)), 
    0x20 -> Seq(
      RegField(4, packetExtractedValues.sv_accuracy)), 
    0x24 -> Seq(
      RegField(6, packetExtractedValues.sv_health)), 
    0x28 -> Seq(
      RegField(10, packetExtractedValues.iodc)), 
    0x2c -> Seq(
      RegField(8, packetExtractedValues.t_gd)), 
    0x30 -> Seq(
      RegField(8, packetExtractedValues.a_f2)), 
    0x34 -> Seq(
      RegField(16, packetExtractedValues.a_f1)), 
    0x38 -> Seq(
      RegField(22, packetExtractedValues.a_f0)), 
    0x3c -> Seq(
      RegField(8, packetExtractedValues.iode)), 
    0x40 -> Seq(
      RegField(16, packetExtractedValues.c_rs)), 
    0x44 -> Seq(
      RegField(16, packetExtractedValues.delta_n)), 
    0x48 -> Seq(
      RegField(32, packetExtractedValues.m_0)), 
    0x4c -> Seq(
      RegField(16, packetExtractedValues.c_uc)), 
    0x50 -> Seq(
      RegField(32, packetExtractedValues.e)), 
    0x54 -> Seq(
      RegField(16, packetExtractedValues.c_us)), 
    0x58 -> Seq(
      RegField(32, packetExtractedValues.sqrt_a)), 
    0x5c -> Seq(
      RegField(16, packetExtractedValues.t_oe)), 
    0x60 -> Seq(
      RegField(16, packetExtractedValues.c_ic)), 
    0x64 -> Seq(
      RegField(32, packetExtractedValues.omega_0)), 
    0x68 -> Seq(
      RegField(16, packetExtractedValues.c_is)), 
    0x6c -> Seq(
      RegField(32, packetExtractedValues.i_0)), 
    0x70 -> Seq(
      RegField(16, packetExtractedValues.c_rc)), 
    0x74 -> Seq(
      RegField(32, packetExtractedValues.omega)), 
    0x78 -> Seq(
      RegField(24, packetExtractedValues.dot_omega)), 
    0x7c -> Seq(
      RegField(14, packetExtractedValues.idot)) 
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

