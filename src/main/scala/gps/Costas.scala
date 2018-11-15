package gps

import chisel3._
import chisel3.experimental.FixedPoint
import dsptools.numbers._

trait CostasParams[T <: Data] {
  val protoData: T
  val protoFreq: T
  val protoPhase: T
  val protoCoeff: FixedPoint
  val protoCordic: FixedCordicParams
}

case class SampledCostasParams(
  // width of input data
  dataWidth: Int,
  // width of freq ctrl
  ncoWidth: Int,
  //
  cordicXYWidth: Int,
  cordicZWidth: Int,
  // nStages
  cordicNStages: Int,
  // correct gain
  cordicCorrectGain: Boolean = true,
  cordicCalAtan2: Boolean = false,
  cordicDividing: Boolean = false,
  // fll right shift bits
  fllRightShift: Int,
  // filter coefficient bits
  // lfCoeffWidth: Int
) extends CostasParams[SInt] {
  val protoData = SInt(dataWidth.W)
  val protoFreq = SInt(ncoWidth.W)
  val protoPhase = SInt(ncoWidth.W)
  val protoCoeff = FixedPoint((ncoWidth+cordicZWidth-dataWidth).W, (cordicZWidth-dataWidth).BP)
  val protoCordic = FixedCordicParams(
    xyWidth = cordicXYWidth,
    xyBPWidth = cordicXYWidth-dataWidth,
    zWidth = cordicZWidth,
    zBPWidth = cordicZWidth-dataWidth,
    nStages = cordicNStages,
    correctGain = cordicCorrectGain,
    calAtan2 = cordicCalAtan2,
    dividing = cordicDividing,
  )
}

class lfCoeffIO[T <: Data](params: CostasParams[T]) extends Bundle {
  val phaseCoeff0 = Input(params.protoCoeff)
  val phaseCoeff1 = Input(params.protoCoeff)
  val phaseCoeff2 = Input(params.protoCoeff)
  val freqCoeff0 = Input(params.protoCoeff)
  val freqCoeff1 = Input(params.protoCoeff)
}

class CostasIO[T <: Data](params: SampledCostasParams) extends Bundle {
  val Ip = Input(params.protoData)
  val Qp = Input(params.protoData)
  val lfCoeff = new lfCoeffIO(params)
  val freqBias = Input(params.protoFreq)
  val freqCtrl = Output(params.protoFreq)
  val phaseCtrl = Output(params.protoPhase)

  // debug
  val xin = Output(params.protoCordic.protoXY)
  val yin = Output(params.protoCordic.protoXY)
  val zin = Output(params.protoCordic.protoZ)
  val xout = Output(params.protoCordic.protoXY)
  val yout = Output(params.protoCordic.protoXY)
  val zout = Output(params.protoCordic.protoZ)
  val xMid = Output(Vec(params.cordicNStages+1, params.protoCordic.protoXY))
  val yMid = Output(Vec(params.cordicNStages+1, params.protoCordic.protoXY))
  val zMid = Output(Vec(params.cordicNStages+1, params.protoCordic.protoZ))
}

object CostasIO {
  def apply[T <: Data](params: SampledCostasParams): CostasIO[T] =
    new CostasIO(params)
}

class costasDis(val params: SampledCostasParams) extends Module {
  val io = IO( new Bundle {
    val Ip = Input(params.protoData)
    val Qp = Input(params.protoData)
    val costasDisOut = Output(params.protoCoeff)

    // debug
    val xin = Output(params.protoCordic.protoXY)
    val yin = Output(params.protoCordic.protoXY)
    val zin = Output(params.protoCordic.protoZ)
    val xout = Output(params.protoCordic.protoXY)
    val yout = Output(params.protoCordic.protoXY)
    val zout = Output(params.protoCordic.protoZ)
    val xMid = Output(Vec(params.cordicNStages+1, params.protoCordic.protoXY))
    val yMid = Output(Vec(params.cordicNStages+1, params.protoCordic.protoXY))
    val zMid = Output(Vec(params.cordicNStages+1, params.protoCordic.protoZ))
  })
  // get cordic
  val cordic = Module(new Cordic1Cycle(params.protoCordic))

  // coridic input/output
  val xOutCordic = Wire(params.protoCordic.protoXY.cloneType)
  val yOutCordic = Wire(params.protoCordic.protoXY.cloneType)
  val zOutCordic = Wire(params.protoCordic.protoZ.cloneType)

  // format to cordic Input
  cordic.io.in.x := io.Ip.asTypeOf(params.protoCordic.protoXY.cloneType) << (params.cordicXYWidth-params.dataWidth)
  cordic.io.in.y := io.Qp.asTypeOf(params.protoCordic.protoXY.cloneType) << (params.cordicXYWidth-params.dataWidth)
  cordic.io.in.z := 0.S.asTypeOf(params.protoCordic.protoZ)
  cordic.io.vectoring := true.B
  // cordic.io.dividing := false.B
  // cordic output
  xOutCordic := cordic.io.out.x
  yOutCordic := cordic.io.out.y
  zOutCordic := cordic.io.out.z

  // costas discreminator
  io.costasDisOut := zOutCordic.asTypeOf(params.protoCoeff.cloneType)

  io.xin := cordic.io.in.x
  io.yin :=  cordic.io.in.y
  io.zin := cordic.io.in.z
  io.xout := xOutCordic
  io.yout := yOutCordic
  io.zout := zOutCordic

  io.xMid := cordic.io.xMid
  io.yMid := cordic.io.yMid
  io.zMid := cordic.io.zMid

}


class fllDis(val params: SampledCostasParams) extends Module {
  val io = IO( new Bundle {
    val Ip = Input(params.protoData)
    val Qp = Input(params.protoData)
    val fllDisOut = Output(SInt((2 * params.dataWidth).W))  // multiply
  })
  // delayed version
  val IpDel = Reg(params.protoData.cloneType)
  val QpDel = Reg(params.protoData.cloneType)

  // before shift
  val fllDisMid = Wire(SInt((2*params.dataWidth).W))

  // delay
  IpDel := io.Ip
  QpDel := io.Qp
  // fll discreminator
  fllDisMid := IpDel * io.Qp - QpDel * io.Ip
  // use right shift replace division
  io.fllDisOut := fllDisMid >> params.fllRightShift
}


class loopFilter(val params: SampledCostasParams) extends Module {
  val io = IO( new Bundle {
    val costasDis = Input(params.protoCoeff)
    val fllDis = Input(SInt((2 * params.dataWidth).W))
    val lfCoeff = new lfCoeffIO(params)
    val freqOut = Output(params.protoFreq)
  })

  val freqOutMid = Wire(params.protoCoeff)
  val fllDisMid = Wire(params.protoCoeff)

  val lfSumSum = RegInit(params.protoCoeff.cloneType, 0.S.asTypeOf(params.protoCoeff))
  val lfSum = RegInit(params.protoCoeff.cloneType, 0.S.asTypeOf(params.protoCoeff))

  fllDisMid := io.fllDis.asTypeOf(params.protoCoeff.cloneType) << (params.cordicZWidth-params.dataWidth)

  lfSumSum := io.lfCoeff.phaseCoeff2 * io.costasDis + io.lfCoeff.freqCoeff1 * fllDisMid
  lfSum := io.lfCoeff.phaseCoeff1 * io.costasDis + io.lfCoeff.freqCoeff0 * fllDisMid + lfSumSum
  freqOutMid := io.lfCoeff.phaseCoeff0 * io.costasDis + lfSum

  io.freqOut := (freqOutMid >> params.cordicNStages).asTypeOf(params.protoFreq.cloneType)
}


class CostasLoop(val params: SampledCostasParams) extends Module {
  val io = IO(CostasIO(params))

  // costas and fll
  val costas = Module(new costasDis(params))
  val fll = Module(new fllDis(params))
  val lf = Module(new loopFilter(params))

  // costas output
  val costasDisOut = Wire(params.protoCoeff.cloneType)
  // fll output
  val fllDisOut = Wire(SInt((2*params.dataWidth).W))
  //  freq_mid
  val freqMid = Wire(params.protoFreq.cloneType)

  // connect costas
  costas.io.Ip := io.Ip
  costas.io.Qp := io.Qp
  costasDisOut := costas.io.costasDisOut

  // connect fll
  fll.io.Ip := io.Ip
  fll.io.Qp := io.Qp
  fllDisOut := fll.io.fllDisOut

  // connect loop filter
  lf.io.costasDis := costasDisOut
  lf.io.fllDis := fllDisOut
  lf.io.lfCoeff := io.lfCoeff
  freqMid := lf.io.freqOut

  // use fake value for now
  io.freqCtrl := io.freqBias + freqMid
  io.phaseCtrl := 0.S

  // debug
  io.xin := costas.io.xin
  io.yin := costas.io.yin
  io.zin := costas.io.zin
  io.xout := costas.io.xout
  io.yout := costas.io.yout
  io.zout := costas.io.zout

  io.xMid := costas.io.xMid
  io.yMid := costas.io.yMid
  io.zMid := costas.io.zMid
}