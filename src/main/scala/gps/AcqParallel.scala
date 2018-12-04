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


trait AcqParallelParams[T1 <: Data, T2 <: Data] {
  val widthADC: Int
  val widthCA: Int
  val widthNCOTrunct: Int
  val widthNCORes: Int
  val widthSumIQ: Int
  val widthCorr: Int
  val widthSumCorr: Int
  val widthIdxFreq: Int
  val widthFreq: Int
  val widthLoop: Int
  val widthChipPhase: Int
  val nChipSample: Int
  val nLoop: Int
  val nFreq: Int
  val nChipCycle: Int
  val chipMin: Int
  val chipStep: Int
  val freqMin: Int
  val freqStep: Int
  val freqSample: Int
  val freqChip: Int
  val NCOParamsADC: NcoParams[T1]
  val NCOParamsCA: NcoParams[T1]
  val CAParamsX: CAParams
  val protoADC: T1
  val protoCA: T1
  val protoSumIQ: T1
  val protoSumCorr: T1
  val protoCorr: T1
  val protoNCO: T1
  val protoSV: UInt
  val protoFreq: UInt
  val protoIdxFreq: UInt
  val protoChipPhase: UInt
}

/**
  * @constructor acquisiton top -level
  * @param widthADC ADC width
  * @param widthCA CA code width
  * @param widthNCOTrunct NCO output width, output is truncated
  * @param widthNCORes NCO input width
  * @param nChipSample number of chip samples, should be freqSample/freqChip
  * @param nLoop number of averaging loops to be run, 1 for not averaging at all
  * @param nFreq number of frequencies to be swept
  * @param nChipCycle number of correlation calculations, for calcualtion
  * @param chipMin minimum chip index
  * @param chipStep chip sweept step
  * @param freqMin minimum frequenecy to sweep
  * @param freqStep frequeny sweep step
  * @param freqSample ADC sample rate
  * @param freqChip CA chip sample rate
  */

case class EgAcqParallelParams(
   val widthADC: Int,
   val widthCA: Int,
   val widthNCOTrunct: Int,
   val widthNCORes: Int,
   val nChipSample: Int,
   val nLoop: Int,
   val nFreq: Int,
   val nChipCycle: Int,
   val chipMin: Int,
   val chipStep: Int,
   val freqMin: Int,
   val freqStep: Int,
   val freqSample: Int,
   val freqChip: Int
) extends AcqParallelParams[SInt, FixedPoint] {
  require(chipMin + (nChipCycle - 1) * chipStep < nChipSample, s"The max CP can not exceed the nChipSample - 1, " +
    s"chipMin = $chipMin, nChipCycle = $nChipCycle, " +
    s"chipStep = $chipStep, nChipSample = $nChipSample")
  println(s"The max CP can not exceed the nChipSample - 1, " +
    s"chipMin = $chipMin, nChipCycle = $nChipCycle, " +
    s"chipStep = $chipStep, nChipSample = $nChipSample")
  println(s"freqMin = $freqMin")

  val widthIdxFreq = log2Ceil(nFreq) + 1
  val widthFreq = log2Ceil(freqMin) + 2
  val widthLoop = log2Ceil(nLoop) + 1
  val widthChipPhase = log2Ceil(nChipSample) + 1

  val widthSumIQ = widthChipPhase + widthLoop + widthADC + widthCA + widthNCORes + 2
  val widthCorr = 2 * widthSumIQ + 1
  val widthSumCorr = widthCorr + widthIdxFreq

  val protoADC = SInt(widthADC.W)
  val protoCA = SInt(widthCA.W)
  val protoSumIQ = SInt(widthSumIQ.W)
  val protoCorr = SInt(widthCorr.W)
  val protoSumCorr = SInt(widthSumCorr.W)
  val protoNCO = SInt(widthNCOTrunct.W)

  val protoSV = UInt(5.W)
  val protoIdxFreq = UInt(widthIdxFreq.W)
  val protoFreq = UInt(widthFreq.W)
  val protoChipPhase = UInt(widthChipPhase.W)

  val NCOParamsADC = SIntNcoParams (
    resolutionWidth = widthNCORes,
    truncateWidth = widthNCOTrunct,
    sinOut = true,
    highRes = true,
  )
  val NCOParamsCA = SIntNcoParams (
    resolutionWidth = widthNCORes,
    truncateWidth = widthNCOTrunct,
    sinOut = true,
    highRes = true,
  )

  val CAParamsX = CAParams(
    fcoWidth = widthNCOTrunct,
    codeWidth = widthCA
  )
}

// input interface within the acquisition loop
class AcqParallelInputBundle[T1 <: Data, T2 <: Data](params: AcqParallelParams[T1, T2]) extends Bundle {

  val ADC: T1 = Input(params.protoADC)
  val idxSV: UInt = Input(params.protoSV)
  val ready = Output(Bool())
  val valid = Input(Bool())
  val debugCA = Input(Bool())
  val debugNCO = Input(Bool())
  val CA: T1 = Input(params.protoCA)
  val cos: T1 = Input(params.protoNCO)
  val sin: T1 = Input(params.protoNCO)

  override def cloneType: this.type = AcqParallelInputBundle(params).asInstanceOf[this.type]
}
object AcqParallelInputBundle {
  def apply[T1 <: Data, T2 <: Data](params: AcqParallelParams[T1, T2]): AcqParallelInputBundle[T1, T2] = new AcqParallelInputBundle(params)
}


// output interface within the acquisition loop
class AcqParallelOutputBundle[T1 <: Data, T2 <: Data](params: AcqParallelParams[T1, T2]) extends Bundle {

  val idxFreqOpt: UInt = Output(params.protoIdxFreq.cloneType)
  val freqOpt: UInt = Output(params.protoFreq.cloneType)
  val chipOpt: UInt = Output(params.protoChipPhase.cloneType)
  val max: SInt = Output(SInt(params.widthCorr.W))
  val sum: SInt = Output(SInt(params.widthSumCorr.W))
  val svFound = Output(Bool())
  val ready = Input(Bool())
  val valid = Output(Bool())

  // for debug and plot
  val chipOptChk: UInt = Output(params.protoChipPhase.cloneType)
  val acqed = Output(Bool())
  val corrArr = Output(Vec(params.nChipCycle, params.protoCorr))

  override def cloneType: this.type = AcqParallelOutputBundle(params).asInstanceOf[this.type]
}
object AcqParallelOutputBundle {
  def apply[T1 <: Data, T2 <: Data](params: AcqParallelParams[T1, T2]): AcqParallelOutputBundle[T1, T2] = new AcqParallelOutputBundle(params)
}


class AcqParallelIO[T1 <: Data, T2 <: Data](params: AcqParallelParams[T1, T2]) extends Bundle {

  val in = AcqParallelInputBundle(params)
  val out = AcqParallelOutputBundle(params)

  override def cloneType: this.type = AcqParallelIO(params).asInstanceOf[this.type]
}
object AcqParallelIO {
  def apply[T1 <: Data, T2 <: Data](params: AcqParallelParams[T1, T2]): AcqParallelIO[T1, T2] =
    new AcqParallelIO(params)
}


class AcqParallel[T1 <: Data:Ring:Real:BinaryRepresentation, T2 <: Data:Ring:Real:BinaryRepresentation]
  (val params: AcqParallelParams[SInt, FixedPoint]) extends Module {

  // instantiate IO
  val io = IO(AcqParallelIO(params))

  /**
    * Instantiate all NCOs and make them run at the right frequencies
    * Instantiate ca code generator to get CA code
    */
  // instantiate all blocks
  val ca = Module(new CA(params.CAParamsX))
  val nco_ADC = Module(new NCO[SInt](params.NCOParamsADC))
  val nco_CA1x = Module(new NCO[SInt](params.NCOParamsCA))
  val nco_CA2x = Module(new NCO[SInt](params.NCOParamsCA))

  // sample rate and chip frequency
  val freqSample = params.freqSample
  val freqChip = params.freqChip

  // Use extraShift to avoid accuracy loss
  val extraShift = 16
  val stepSizeCoeff = pow(2, params.widthNCORes+extraShift) / freqSample

  val ncoReset = RegInit(Bool(), false.B)  // keep ncoReset being false

  // get frequency index and frequency
  val regIdxFreq = RegInit(params.protoIdxFreq, 0.U)
  val freqIF = regIdxFreq * params.freqStep.U + params.freqMin.U

  // get step size for NCOs
  val stepSizeNCO_ADC = (ConvertableTo[UInt].fromDouble(stepSizeCoeff) * freqIF) >> extraShift
  val stepSizeNCO_CA1x = (ConvertableTo[UInt].fromDouble(stepSizeCoeff) * ConvertableTo[UInt].fromDouble(freqChip)) >> extraShift
  val stepSizeNCO_CA2x = stepSizeNCO_CA1x * ConvertableTo[UInt].fromInt(2)

  // connect CA, NCO_ADC, NCO_CA1x and NCO_CA2x
  ca.io.satellite := io.in.idxSV
  ca.io.fco := nco_CA1x.io.sin
  ca.io.fco2x := nco_CA2x.io.sin

  nco_ADC.io.stepSize := stepSizeNCO_ADC
  nco_CA1x.io.stepSize := stepSizeNCO_CA1x
  nco_CA2x.io.stepSize := stepSizeNCO_CA2x

  nco_ADC.reset := false.B
  nco_CA1x.reset := ncoReset
  nco_CA2x.reset := ncoReset

//  /**
//    * Global counter to track code phase change
//   */
//  // global counter
//  val extraCntBits = 16
//  val chipSampleNumRes = ConvertableTo[UInt].fromDouble(params.freqSample/params.freqChip*pow(2, extraCntBits))
//  val globalCountRes = RegInit(UInt((params.protoGlobalCnt+extraCntBits).W), 0.U)
//  val globalCount = RegInit(UInt((params.protoGlobalCnt).W), 0.U)
//
//  globalCountRes := globalCount + ConvertableTo[UInt].fromDouble(pow(2, extraCntBits))
//  when(globalCountRes >= chipSampleNumRes-1.U){
//    globalCountRes := globalCountRes - chipSampleNumRes + 1.U
//  }
//  globalCount := globalCountRes >> extraCntBits.U

  /**
    * Acquision FSM to do parellel search
    */
  // FSM states
  val init = WireInit(UInt(3.W), 0.U)
  val prep = WireInit(UInt(3.W), 1.U)
  val corr = WireInit(UInt(3.W), 2.U)
  val acqed = WireInit(UInt(3.W), 3.U)
  val done = WireInit(UInt(3.W), 4.U)

  // I/Q sum for each code phase
  val regSum_i = Reg(Vec(params.nChipCycle, params.protoSumIQ))
  val regSum_q = Reg(Vec(params.nChipCycle, params.protoSumIQ))

  // cos/sin Mux for debug
  val cos = Mux(io.in.debugNCO, io.in.cos, nco_ADC.io.cos)
  val sin = Mux(io.in.debugNCO, io.in.sin, nco_ADC.io.sin)

  // state register
  val regState = RegInit(UInt(3.W), init)  // state register

  // max frequency index
  val idxFreqMax = WireInit(params.protoIdxFreq, (params.nFreq).U)

  // count for 1 frequency
  val regCntLoop = RegInit(UInt((params.widthLoop+params.widthChipPhase+1).W), 0.U)
  val cntLoopMax = WireInit(UInt((params.widthLoop+params.widthChipPhase+1).W), (params.nLoop*params.nChipSample-1).U)

  val regCAReady = RegInit(Bool(), false.B)
  val regCACnt = RegInit(UInt((params.widthLoop+params.widthChipPhase+1).W), 0.U)
  val caCntMax = WireInit(UInt((params.widthLoop+params.widthChipPhase+1).W), (params.nChipSample-1).U)

  // shift register for CA code
  val regShiftCA = Reg(Vec(params.nChipSample, SInt(params.widthCA.W)))

  // regMax, regSum, regOptIdxFreq, regOptIdxChip
  val regMax = RegInit(SInt(params.widthCorr.W), ConvertableTo[SInt].fromInt(0))
  val regSum = RegInit(SInt(params.widthSumCorr.W), ConvertableTo[SInt].fromInt(0))

  val regOptIdxChip = RegInit(UInt(params.widthChipPhase.W), 0.U)
  val regOptIdxFreq = RegInit(UInt(params.widthFreq.W), params.freqMin.U)

  // chip offset
  val chipOffset = RegInit(params.protoChipPhase.cloneType, 0.U)

  // calculate correlation for each code phase
  val corrArr = Wire(Vec(params.nChipCycle, params.protoCorr))
  for (i <- 0 until params.nChipCycle) {
    corrArr(i) := regSum_i(i) * regSum_i(i) + regSum_q(i) * regSum_q(i)
  }

  // find the max code phase
  val sum = TreeReduce(corrArr, (x:SInt, y:SInt) => x +& y)
  val max = TreeReduce(corrArr, (x:SInt, y:SInt) => x.max(y))
  val optIdxChip =  WireInit(params.protoChipPhase, 0.U)
  for (i <- 0 until params.nChipCycle) {
    when(max === corrArr(i)) {
      optIdxChip := i.U
    }
  }

  // state transfer
  when (regState === init){
    regState := Mux(io.in.valid, prep, init) // may need to add CA code initialize here
  }.elsewhen (regState === prep) {
    regState := Mux(regCAReady, corr, prep)
  }.elsewhen (regState === corr) {
    regState := Mux(regCntLoop === cntLoopMax, acqed, corr)
  }.elsewhen (regState === acqed) {
    regState := Mux(regIdxFreq === idxFreqMax, done, corr)
  }.elsewhen (regState === done){
    regState := Mux(io.out.ready, init, done)
  }.otherwise{
    regState := init
  }

  // state work
  when (regState === init) {
    // initialize
    regMax := 0.S
    regSum := 0.S
    for (i <- 0 until params.nChipCycle){
      regSum_i(i) := ConvertableTo[T1].fromInt(0)
      regSum_q(i) := ConvertableTo[T1].fromInt(0)
    }
    // CA initialize
    regCACnt := 0.U
    regCAReady := false.B
    // frequency index initialize
    regIdxFreq := 0.U
    // correlator counter
    regCntLoop := 0.U
    // io.valid
    io.out.valid := false.B
    // io.ready
    io.in.ready := true.B
    // nco reset
    ncoReset := true.B
    // reset offset counter
    chipOffset := 0.U
    // reset offset counter
    chipOffset := 0.U
  }.elsewhen(regState === prep) {
    // initialize
    regMax := 0.S
    regSum := 0.S
    for (i <- 0 until params.nChipCycle){
      regSum_i(i) := ConvertableTo[T1].fromInt(0)
      regSum_q(i) := ConvertableTo[T1].fromInt(0)
    }
    // get CA code
    for (i <- 0 until params.nChipSample-1) {
      regShiftCA(i) := regShiftCA(i+1)
    }
    regShiftCA(params.nChipSample-1) := Mux(io.in.debugCA, io.in.CA, ca.io.punctual.asTypeOf(params.protoCA))
    // count cycles, if collect all the CA code, move to next state
    regCACnt := regCACnt + 1.U
    when (regCACnt === caCntMax) {
      regCAReady := true.B
    }
    // frequency index initialize
    regIdxFreq := 0.U
    // correlator counter
    regCntLoop := 0.U
    // io.valid
    io.out.valid := false.B
    // io.ready
    io.in.ready := false.B
    // nco reset
    ncoReset := false.B
    // reset offset counter
    chipOffset := 0.U
  }.elsewhen(regState === corr){
    // get CA code
    for (i <- 0 until params.nChipSample) {
      if (i == params.nChipSample-1){
        regShiftCA(params.nChipSample-1) := regShiftCA(0)
      } else{
        regShiftCA(i) := regShiftCA(i+1)
      }
    }
    // update regSum
    for (i <- 0 until params.nChipCycle) {
      regSum_i(i) := io.in.ADC * regShiftCA(i * params.chipStep + params.chipMin) * cos + regSum_i(i)
      regSum_q(i) := io.in.ADC * regShiftCA(i * params.chipStep + params.chipMin) * sin + regSum_q(i)
    }
    // update cntLoop
    regCntLoop := regCntLoop + 1.U
    when(regCntLoop === cntLoopMax){  // when finish this loop add frequency index
      regIdxFreq := regIdxFreq + 1.U
      // reset cntLoop
      regCntLoop := 0.U
    }
    // io.valid
    io.out.valid := false.B
    // io.ready
    io.in.ready := false.B
    // nco reset
    ncoReset := true.B
    // reset offset counter
    chipOffset := 0.U
  }.elsewhen(regState === acqed){
    // avoid gap between two frequency cycles
    for (i <- 0 until params.nChipCycle) {
      regSum_i(i) := io.in.ADC * regShiftCA(i * params.chipStep + params.chipMin) * cos
      regSum_q(i) := io.in.ADC * regShiftCA(i * params.chipStep + params.chipMin) * sin
    }
    // update regSum, regMax and optIdxFreq
    regSum := regSum + sum
    when (max > regMax) {
      regMax := max
      regOptIdxChip := optIdxChip
      regOptIdxFreq := regIdxFreq - 1.U
    }
    // update cntLoop
    regCntLoop := regCntLoop + 1.U
    // update index frequency
    //regIdxFreq := regIdxFreq + 1.U
    // io.valid
    io.out.valid := false.B
    // io.ready
    io.in.ready := false.B
    // nco reset
    ncoReset := true.B
    // reset offset counter
    chipOffset := 0.U
  }.elsewhen(regState === done) {
    // io.valid
    io.out.valid := true.B
    // io.ready
    io.in.ready := false.B
  }.otherwise{
    // io.valid
    io.out.valid := false.B
    // io.ready
    io.in.ready := false.B
    // nco reset
    ncoReset := true.B
    // reset offset counter
    chipOffset := chipOffset + 1.U
  }


  /**
    * Output update
    */
  io.out.idxFreqOpt := regOptIdxFreq
  io.out.freqOpt := regOptIdxFreq * params.freqStep.U + params.freqMin.U
  io.out.chipOpt := regOptIdxChip * params.chipStep.U + params.chipMin.U + chipOffset
  io.out.max := regMax
  io.out.sum := regSum
  io.out.svFound := regMax * ConvertableTo[SInt].fromDouble(params.nFreq*params.nChipCycle/6) > regSum

  // for debug and plot
  io.out.chipOptChk := regOptIdxChip * params.chipStep.U + params.chipMin.U
  io.out.acqed := regState === acqed
  io.out.corrArr := corrArr
}
