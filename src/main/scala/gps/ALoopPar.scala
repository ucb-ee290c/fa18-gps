package gps

import chisel3._
//import chisel3.util.Decoupled
import chisel3.util._
import scala.math._
import dsptools.numbers._
import dsptools.numbers.implicits._
import dsptools.numbers.DspComplex
import chisel3.experimental.FixedPoint
import freechips.rocketchip.amba.axi4stream._
import freechips.rocketchip.config.Parameters
import freechips.rocketchip.diplomacy._
import freechips.rocketchip.regmapper._
import freechips.rocketchip.tilelink._


trait ALoopParParams[T1 <: Data, T2 <: Data] {
  val wADC: Int
  val wCA: Int
  val wNCOTct: Int
  val wNCORes: Int
  val wSumIQ: Int
  val wCorr: Int
  val wSumCorr: Int
  val wIFreq: Int
  val wFreq: Int
  val wLoop: Int
  val wCP: Int
  val nSample: Int
  val nLoop: Int
  val nFreq: Int
  val nCPSample: Int
  val CPMin: Int
  val CPStep: Int
  val freqMin: Int
  val freqStep: Int
  val fsample: Int
  val fchip: Int
  val NCOParams_ADC: NcoParams[T1]
  val NCOParams_CA: NcoParams[T1]
  val CA_Params: CAParams
  val pADC: T1
  val pCA: T1
  val pSumIQ: T1
  val pSumCorr: T1
  val pCorr: T1
  val pNCO: T1
  val pSate: UInt
  val pFreq: UInt
  val pIFreq: UInt
  val pCP: UInt
}

case class EgALoopParParams(
                            val wADC: Int,
                            val wCA: Int,
                            val wNCOTct: Int,
                            val wNCORes: Int,
                            val nSample: Int,
                            val nLoop: Int,
                            val nFreq: Int,
                            val nCPSample: Int,
                            val CPMin: Int,
                            val CPStep: Int,
                            val freqMin: Int,
                            val freqStep: Int,
                            val fsample: Int,
                            val fchip: Int
                          ) extends ALoopParParams[SInt, FixedPoint] {

  require(CPMin + (nCPSample - 1) * CPStep < nSample, s"The max CP can not exceed the nSample - 1, " +
                                                      s"CPMin = $CPMin, nCPSample = $nCPSample, " +
                                                      s"CPStep = $CPStep, nSample = $nSample")
  println(s"The max CP can not exceed the nSample - 1, " +
          s"CPMin = $CPMin, nCPSample = $nCPSample, " +
          s"CPStep = $CPStep, nSample = $nSample")
  println(s"freqMin = $freqMin")
//  val nCPSample = nSample

  val wIFreq = log2Ceil(nFreq) + 1
  val wFreq = log2Ceil(freqMin) + 2
  val wLoop = log2Ceil(nLoop) + 1
  val wCP = log2Ceil(nSample) + 1

  val wSumIQ = wCP + wLoop + wADC + wCA + wNCORes + 2
  val wCorr = 2 * wSumIQ + 1
  val wSumCorr = wCorr + wIFreq

  val pADC = SInt(wADC.W)
  val pCA = SInt(wCA.W)
  val pSumIQ = SInt(wSumIQ.W)
  val pCorr = SInt(wCorr.W)
  val pSumCorr = SInt(wSumCorr.W)
  val pNCO = SInt(wNCOTct.W)

  val pSate = UInt(5.W)
  val pIFreq = UInt(wIFreq.W)
  val pFreq = UInt(wFreq.W)
  val pCP = UInt(wCP.W)


  val NCOParams_ADC = SIntNcoParams (
    resolutionWidth = wNCORes,
    truncateWidth = wNCOTct,
    sinOut = true,
    highRes = true,
  )
  val NCOParams_CA = SIntNcoParams (
    resolutionWidth = wNCORes,
    truncateWidth = wNCOTct,
    sinOut = true,
    highRes = true,
  )

  val CA_Params = CAParams(
    fcoWidth = wNCOTct,
    codeWidth = wCA
  )

}





// input interface within the acquisition loop
class ALoopParInputBundle[T1 <: Data, T2 <: Data](params: ALoopParParams[T1, T2]) extends Bundle {

  val ADC: T1 = Input(params.pADC)
  val idx_sate: UInt = Input(params.pSate)
  val ready = Output(Bool())
  val valid = Input(Bool())
  val debugCA = Input(Bool())
  val debugNCO = Input(Bool())
  val CA: T1 = Input(params.pCA)
  val cos: T1 = Input(params.pNCO)
  val sin: T1 = Input(params.pNCO)

  override def cloneType: this.type = ALoopParInputBundle(params).asInstanceOf[this.type]
}
object ALoopParInputBundle {
  def apply[T1 <: Data, T2 <: Data](params: ALoopParParams[T1, T2]): ALoopParInputBundle[T1, T2] = new ALoopParInputBundle(params)
}


// output interface within the acquisition loop
class ALoopParOutputBundle[T1 <: Data, T2 <: Data](params: ALoopParParams[T1, T2]) extends Bundle {

  val iFreqOpt: UInt = Output(params.pIFreq.cloneType)
  val freqOpt: UInt = Output(params.pFreq.cloneType)
  val CPOpt: UInt = Output(params.pCP.cloneType)
  val max: SInt = Output(SInt(params.wCorr.W))
  val sum: SInt = Output(SInt(params.wSumCorr.W))
  val sateFound = Output(Bool())
  val ready = Input(Bool())
  val valid = Output(Bool())

  override def cloneType: this.type = ALoopParOutputBundle(params).asInstanceOf[this.type]
}
object ALoopParOutputBundle {
  def apply[T1 <: Data, T2 <: Data](params: ALoopParParams[T1, T2]): ALoopParOutputBundle[T1, T2] = new ALoopParOutputBundle(params)
}


//class ALoopParDebugBundle[T1 <: Data, T2 <: Data](params: ALoopParams[T1, T2]) extends Bundle {
//
//  val sineWaveTest = Input(Bool())
//  val selfCATest = Input(Bool())
//
//  val FreqNow: UInt = Output(params.ACtrlParams.pFreq.cloneType)
//  val iFreqNow: UInt = Output(params.ACtrlParams.pIdxFreq.cloneType)
//  val iFreqNext: UInt = Output(params.ACtrlParams.pIdxFreq.cloneType)
//  val iLoopNow: UInt = Output(params.ACtrlParams.pLoop.cloneType)
//  val iLoopNext: UInt = Output(params.ACtrlParams.pLoop.cloneType)
//  val iCPNow: UInt = Output(params.ACtrlParams.pCodePhase.cloneType)
//  val iCPNext: UInt = Output(params.ACtrlParams.pCodePhase.cloneType)
//  val max: T2 = Output(params.ACtrlParams.pMax.cloneType)
//  val reg_max: T2 = Output(params.ACtrlParams.pMax.cloneType)
//  val reg_tag_CP = Output(Bool())
//  val reg_tag_Loop = Output(Bool())
//  val reg_tag_Freq = Output(Bool())
//
//  val Correlation = Output(Vec(params.nLane, params.ACtrlParams.pCorrelation))
//  val iFreqOptItm: UInt = Output(params.ACtrlParams.pIdxFreq.cloneType)
//  val iFreqOptOut: UInt = Output(params.ACtrlParams.pIdxFreq.cloneType)
//  val CPOptItm: UInt = Output(params.ACtrlParams.pCodePhase.cloneType)
//  val CPOptOut: UInt = Output(params.ACtrlParams.pCodePhase.cloneType)
//  val vec = Output(Vec(params.nSample, params.ACtrlParams.pCorrelation.cloneType))
//  val state: UInt = Output(UInt(2.W))
//
//
//  override def cloneType: this.type = ALoopDebugBundle(params).asInstanceOf[this.type]
//}
//object ALoopDebugBundle {
//  def apply[T1 <: Data, T2 <: Data](params: ALoopParams[T1, T2]): ALoopDebugBundle[T1, T2] = new ALoopDebugBundle(params)
//}



class ALoopParIO[T1 <: Data, T2 <: Data](params: ALoopParParams[T1, T2]) extends Bundle {

  val in = ALoopParInputBundle(params)
  val out = ALoopParOutputBundle(params)
//  val debug = ALoopParDebugBundle(params)

  override def cloneType: this.type = ALoopParIO(params).asInstanceOf[this.type]
}
object ALoopParIO {
  def apply[T1 <: Data, T2 <: Data](params: ALoopParParams[T1, T2]): ALoopParIO[T1, T2] =
    new ALoopParIO(params)
}


//object TreeReduce {
//  def apply[V](in: Seq[V], func: (V, V) => V): V = {
//    if (in.length == 1) {
//      return in(0)
//    }
//    if (in.length == 2) {
//      return func(in(0), in(1))
//    }
//    if (in.length % 2 == 0) {
//      val withIdxs = in.zipWithIndex
//      val evens = withIdxs.filter{case (_, idx) => idx % 2 == 0}.map(_._1)
//      val odds  = withIdxs.filter{case (_, idx) => idx % 2 != 0}.map(_._1)
//      val evenOddPairs: Seq[(V, V)] = evens zip odds
//      return TreeReduce(evenOddPairs.map(x => func(x._1, x._2)), func)
//    } else {
//      return TreeReduce(Seq(in(0), TreeReduce(in.drop(1), func)), func)
//    }
//  }
//}


class ALoopPar[T1 <: Data:Ring:Real:BinaryRepresentation, T2 <: Data:Ring:Real:BinaryRepresentation]
  (val params: ALoopParParams[SInt, FixedPoint]) extends Module {

  val io = IO(ALoopParIO(params))


  val ca = Module(new CA(params.CA_Params))
  val nco_ADC = Module(new NCO[SInt](params.NCOParams_ADC))
  val nco_CA1x = Module(new NCO[SInt](params.NCOParams_CA))
  val nco_CA2x = Module(new NCO[SInt](params.NCOParams_CA))






  val idle = WireInit(UInt(2.W), 0.U)
  val acqing = WireInit(UInt(2.W), 1.U)
  val acqed = WireInit(UInt(2.W), 2.U)
  val preparing = WireInit(UInt(2.W), 3.U)
  val iFreqMax = WireInit(params.pIFreq, (params.nFreq-1).U)

  // 1 cycle for all the loop of 1 freqquency

  val cnt_loop_max = WireInit(UInt((params.wLoop+params.wCP+1).W), (params.nLoop*params.nSample-1).U)
  val cnt_begin_int = WireInit(UInt((params.wLoop+params.wCP+1).W), (params.nSample).U)


  val reg_cnt_loop = RegInit(UInt((params.wLoop+params.wCP+1).W), 0.U)
  val reg_state = RegInit(UInt(2.W), idle)
  val reg_ca_ready = RegInit(Bool(), false.B)

  val reg_iFreqNow = RegInit(params.pIFreq, 0.U)
  reg_iFreqNow := Mux(reg_state =/= acqing,
                      0.U,
                      Mux(reg_cnt_loop === cnt_loop_max,
                          Mux(reg_iFreqNow === iFreqMax, 0.U, reg_iFreqNow+1.U),
                          reg_iFreqNow))



  reg_cnt_loop := Mux((reg_state === idle || reg_state === acqed),
                      0.U,
                      Mux(reg_state === preparing,
                          Mux(reg_cnt_loop === (params.nSample-1).U, 0.U, reg_cnt_loop+1.U),
                          Mux(reg_cnt_loop === cnt_loop_max, 0.U, reg_cnt_loop+1.U)
                          )
                      )


  when (reg_state === idle) {
    reg_state := Mux(io.in.valid, preparing, idle)
  } .elsewhen (reg_state === preparing) {
    reg_state := Mux(reg_cnt_loop === (params.nSample-1).U, acqing, preparing)
  } .elsewhen (reg_state === acqing) {
    reg_state := Mux(reg_cnt_loop === cnt_loop_max && reg_iFreqNow === iFreqMax, acqed, acqing)
  } .otherwise {
    Mux(io.out.ready, idle, acqed)
  }





//  val reg_shift_CA = Reg(Vec(params.nSample, params.pCA))
  val reg_shift_CA = Reg(Vec(params.nSample, SInt(params.wCA.W)))
  for (i <- 0 until params.nSample-1) {
    reg_shift_CA(i) := reg_shift_CA(i+1)
  }
  reg_shift_CA(params.nSample-1) := Mux(io.in.debugCA,
                                        io.in.CA,
                                        Mux(reg_state === preparing,
                                            ca.io.punctual.asTypeOf(params.pCA),
                                            reg_shift_CA(0)
                                            )
                                        )



  val fsample = params.fsample
  val fchip = params.fchip
  val extrashift = 16
  val stepSizeCoeff = pow(2, params.NCOParams_ADC.resolutionWidth+extrashift) / fsample

//  val NCO_reset = reg_cnt_loop === 0.U
  val NCO_reset = false.B
  val freqNow = reg_iFreqNow * params.freqStep.U + params.freqMin.U


  val stepSizeNCO_ADC = (ConvertableTo[UInt].fromDouble(stepSizeCoeff) * freqNow) >> extrashift
  val stepSizeNCO_CA1x = (ConvertableTo[UInt].fromDouble(stepSizeCoeff) * ConvertableTo[UInt].fromDouble(fchip)) >> extrashift
  val stepSizeNCO_CA2x = stepSizeNCO_CA1x * ConvertableTo[UInt].fromInt(2)

  ca.io.satellite := io.in.idx_sate
  ca.io.fco := nco_CA1x.io.sin
  ca.io.fco2x := nco_CA2x.io.sin

  nco_ADC.io.stepSize := stepSizeNCO_ADC
  nco_CA1x.io.stepSize := stepSizeNCO_CA1x
  nco_CA2x.io.stepSize := stepSizeNCO_CA2x

  nco_ADC.io.softRst := NCO_reset
  nco_CA1x.io.softRst := NCO_reset
  nco_CA2x.io.softRst := NCO_reset


  val reg_sum_i = Reg(Vec(params.nCPSample, params.pSumIQ))
  val reg_sum_q = Reg(Vec(params.nCPSample, params.pSumIQ))

  val cos = Mux(io.in.debugNCO, io.in.cos, nco_ADC.io.cos)
  val sin = Mux(io.in.debugNCO, io.in.sin, nco_ADC.io.sin)

  when (reg_state === idle || reg_state === preparing) {
    for (i <- 0 until params.nCPSample) {
      reg_sum_i(i) := ConvertableTo[T1].fromInt(0)
      reg_sum_q(i) := ConvertableTo[T1].fromInt(0)
    }
  } .elsewhen(reg_cnt_loop === 0.U) {
    for (i <- 0 until params.nCPSample) {
      reg_sum_i(i) := io.in.ADC * reg_shift_CA(i * params.CPStep + params.CPMin) * cos
      reg_sum_q(i) := io.in.ADC * reg_shift_CA(i * params.CPStep + params.CPMin) * sin
//      reg_sum_i(i) := ConvertableTo[T1].fromInt(0)
//      reg_sum_q(i) := ConvertableTo[T1].fromInt(0)
    }
  } .elsewhen(reg_state === acqing) {
    for (i <- 0 until params.nCPSample) {
      reg_sum_i(i) := io.in.ADC * reg_shift_CA(i * params.CPStep + params.CPMin) * cos + reg_sum_i(i)
      reg_sum_q(i) := io.in.ADC * reg_shift_CA(i * params.CPStep + params.CPMin) * sin + reg_sum_q(i)
    }
  }

  val corrArr = Wire(Vec(params.nCPSample, params.pCorr))
//  val reg_corrArr = Reg(Vec(params.nCPSample, SInt(params.wCorr.W)))
  for (i <- 0 until params.nCPSample) {
    corrArr(i) := reg_sum_i(i) * reg_sum_i(i) + reg_sum_q(i) * reg_sum_q(i)
  }

  val sum = TreeReduce(corrArr, (x:SInt, y:SInt) => x +& y)
  val max = TreeReduce(corrArr, (x:SInt, y:SInt) => x.max(y))
  val optICP =  WireInit(params.pCP, 0.U)
  for (i <- 0 until params.nCPSample) {
    when (max === corrArr(i)) {
      optICP := i.U
    }
  }

  val reg_max = RegInit(SInt(params.wCorr.W), ConvertableTo[SInt].fromInt(0))
  val reg_sum = RegInit(SInt(params.wSumCorr.W), ConvertableTo[SInt].fromInt(0))
  val reg_optIFreq = RegInit(UInt(params.wFreq.W), params.freqMin.U)
  val reg_optICP = RegInit(UInt(params.wCP.W), 0.U)

  val reg_acqed = RegNext(reg_state === acqed, idle)

  when (reg_state === idle || reg_state === preparing) {
    reg_max := 0.S
    reg_sum := 0.S
  }. elsewhen (reg_cnt_loop === 0.U) {
    when (reg_acqed === false.B) {
      reg_sum := reg_sum + sum
    }
    when (max > reg_max) {
      reg_max := max
      reg_optICP := optICP
      when (reg_iFreqNow === 0.U) {
        reg_optIFreq := (params.nFreq-1).U
      } .otherwise {
        reg_optIFreq := reg_iFreqNow - 1.U
      }
    }
  }


  io.out.valid := reg_acqed
  io.out.iFreqOpt := reg_optIFreq
  io.out.CPOpt := reg_optICP * params.CPStep.U + params.CPMin.U
  io.out.freqOpt := reg_optIFreq * params.freqStep.U + params.freqMin.U
  io.out.max := reg_max
  io.out.sum := reg_sum
  io.out.sateFound := reg_max * ConvertableTo[SInt].fromDouble(params.nFreq*params.nCPSample/6) > reg_sum
  io.in.ready := reg_state === idle




}





