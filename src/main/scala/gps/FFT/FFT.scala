// See LICENSE for license details.

// Author: Stevo Bailey (stevo.bailey@berkeley.edu)

package gps

import chisel3._
import chisel3.experimental._
import chisel3.internal.firrtl.KnownBinaryPoint
import chisel3.util._
import craft._
import dspblocks._
import dspjunctions._
import dsptools._
import dsptools.numbers._
import dsptools.numbers.implicits._
import freechips.rocketchip.config.Parameters

import scala.math._


class DirectFFTIO[T <: Data : Real](genMid: DspComplex[T], genOut: DspComplex[T], lanes: Int)(implicit val p: Parameters) extends Bundle {
  val in = Input(ValidWithSync(Vec(lanes, genMid)))
  val out = Output(ValidWithSync(Vec(lanes, genOut)))
}

/**
  * fast fourier transform - cooley-tukey algorithm, decimation-in-time
  * direct form version
  * note, this is always a p-point FFT, though the twiddle factors will be different if p < n
  *
  * @tparam T
  */
class DirectFFT[T <: Data : Real](config: FFTConfig[T], genMid: DspComplex[T], genTwiddle: DspComplex[T], genOutFull: DspComplex[T])(implicit val p: Parameters) extends Module {
  val io = IO(new DirectFFTIO[T](genMid, genOutFull, config.lanes))

  // synchronize
  val valid_delay = RegNext(io.in.valid)
  val sync = Wire(UInt(log2Ceil(config.bp).W))
  sync := CounterWithReset(true.B, config.bp, io.in.sync, ~valid_delay & io.in.valid)._1
  io.out.sync := ShiftRegisterWithReset(io.in.valid && sync === (config.bp - 1).U, config.direct_pipe, 0.U) // should valid keep sync from propagating?
  io.out.valid := ShiftRegisterWithReset(io.in.valid, config.direct_pipe, 0.U)

  // wire up twiddles
  val genTwiddleReal = genTwiddle.real
  val genTwiddleImag = genTwiddle.imag
  // This should work and would simplify the firrtl, but... it doesn't seem to work
  //val twiddle_rom = Vec(config.twiddle.map(x =>
  //  DspComplex(genTwiddleReal.fromDoubleWithFixedWidth(x(0)), genTwiddleImag.fromDoubleWithFixedWidth(x(1)))
  //))
  //  val twiddle_rom = VecInit(config.twiddle.map( x => {
  //    val real = Wire(genTwiddleReal.cloneType)
  //    val imag = Wire(genTwiddleImag.cloneType)
  //    real := Real[T].fromDouble(x(0), genTwiddleReal)
  //    imag := Real[T].fromDouble(x(1), genTwiddleImag)
  //    val twiddle = Wire(DspComplex(genTwiddleReal, genTwiddleImag))
  //    twiddle.real := real
  //    twiddle.imag := imag
  //    twiddle
  //  }))

  val twiddle_rom = config.dtwiddles.map(i => {
    VecInit(i.map(j => {
      val real = Wire(genTwiddleReal.cloneType)
      val imag = Wire(genTwiddleImag.cloneType)
      real := Real[T].fromDouble(j(0), genTwiddleReal)
      imag := Real[T].fromDouble(j(1), genTwiddleImag)
      val twiddle = Wire(DspComplex(genTwiddleReal, genTwiddleImag))
      twiddle.real := real
      twiddle.imag := imag
      twiddle
    }))
  })

  //  // p-point decimation-in-time direct form FFT with inputs in normal order
  //  // (outputs bit reversed)
  //  val indices_rom = VecInit(config.dindices.map(x => x.U))
  //  // TODO: make this not a multiply
  //  val start = sync*(config.lanes-1).U
  //  val twiddle = Wire(Vec(config.lanes-1, genTwiddle.cloneType))
  //  // special case when n = 4, because the pattern breaks down
  //  if (config.n == 4) {
  //    twiddle := VecInit((0 until config.lanes-1).map(x => {
  //      val true_branch  = Wire(genTwiddle)
  //      true_branch     := twiddle_rom(0).divj()
  //      val false_branch = Wire(genTwiddle)
  //      false_branch    := twiddle_rom(0)
  //      Mux(
  //        indices_rom(start+x.U)(log2Ceil(config.n/4)),
  //        true_branch,
  //        false_branch
  //      )
  //    }))
  //  } else {
  //    twiddle.zipWithIndex.foreach { case (t, x) =>
  //      t := {
  //        val true_branch = twiddle_rom(indices_rom(start+x.U)(log2Ceil(config.n/4)-1, 0)).divj().asTypeOf(genTwiddle)
  //        val false_branch = twiddle_rom(indices_rom(start+x.U)).asTypeOf(genTwiddle)
  //        val index = indices_rom(start+x.U)
  //        Mux(index(log2Ceil(config.n/4)),
  //          true_branch,
  //          false_branch
  //        )
  //      }
  //    }
  //  }
  //  val stage_outputs = List.fill(log2Ceil(config.lanes)+1)(List.fill(config.lanes)(Wire(genOutFull)))
  //  io.in.bits.zip(stage_outputs(0)).foreach { case(in, out) => out := in }
  //
  //  // indices to the twiddle Vec
  //  var indices = List(List(0,1),List(0,2))
  //  for (i <- 0 until log2Ceil(config.lanes)-2) {
  //    indices = indices.map(x => x.map(y => y+1))
  //    val indices_max = indices.foldLeft(0)((b,a) => max(b,a.reduceLeft((d,c) => max(c,d))))
  //    indices = indices ++ indices.map(x => x.map(y => y+indices_max))
  //    indices = indices.map(x => 0 +: x)
  //  }
  //
  //  // create the FFT hardware
  //  for (i <- 0 until log2Ceil(config.lanes)) {
  //    for (j <- 0 until config.lanes/2) {
  //
  //      val skip = pow(2,log2Ceil(config.n/2)-(i+log2Ceil(config.bp))).toInt
  //      val start = ((j % skip) + floor(j/skip) * skip*2).toInt
  //
  //      // hook it up
  //      val outputs           = List(stage_outputs(i+1)(start), stage_outputs(i+1)(start+skip))
  //      val shr_delay         = config.pipe.drop(log2Ceil(config.bp)).dropRight(log2Ceil(config.lanes)-i).foldLeft(0)(_+_)
  //      val shr               = ShiftRegisterMem[DspComplex[T]](twiddle(indices(j)(i)), shr_delay, name = this.name + s"_${i}_${j}_twiddle_sram")
  //      val butterfly_outputs = Butterfly[T](Seq(stage_outputs(i)(start), stage_outputs(i)(start+skip)), shr)
  //      outputs.zip(butterfly_outputs).foreach { x =>
  //        x._1 := ShiftRegisterMem(x._2, config.pipe(i+log2Ceil(config.bp)), name = this.name + s"_${i}_${j}_pipeline_sram")
  //      }
  //
  //    }
  //  }
  val stage_outputs = List.fill(log2Ceil(config.lanes) + 1)(List.fill(config.lanes)(Wire(genOutFull)))
  io.in.bits.zip(stage_outputs(0)).foreach { case (in, out) => out := in }

  // create the FFT hardware
  for (i <- 0 until log2Ceil(config.lanes)) {
    for (j <- 0 until config.lanes / 2) {

      var skip = pow(2, log2Ceil(config.n / 2) - (i + log2Ceil(config.bp))).toInt
      skip = if (config.unscrambleIn == false) skip else bit_reverse(skip, log2Ceil(config.lanes))
      val j_rev = j
      val i_rev =if (config.unscrambleIn == false) i else log2Ceil(config.lanes) - i- 1
      val start = ((j_rev % skip) + floor(j_rev / skip) * skip * 2).toInt
      println("skip", i_rev, skip, start)
      // hook it up
      val outputs = List(stage_outputs(i + 1)(start), stage_outputs(i + 1)(start + skip))
      if (config.unscrambleIn == false) {
        val butterfly_outputs = Butterfly[T](Seq(stage_outputs(i)(start), stage_outputs(i)(start + skip)), twiddle_rom(config.tdindices(j)(i_rev))(sync))
        outputs.zip(butterfly_outputs).foreach { x => x._1 := ShiftRegisterMem(x._2, config.pipe(i + log2Ceil(config.bp)), name = this.name + s"_${i}_${j}_pipeline_sram") }
      } else {
        // TODO: here might need to bit reverse sync signal from twiddle rom, keep use sync for now?
        val butterfly_outputs = ButterflyDIF[T](Seq(stage_outputs(i)(start), stage_outputs(i)(start + skip)), twiddle_rom(config.tdindices(j)(i_rev))(sync))
        outputs.zip(butterfly_outputs).foreach { x => x._1 := ShiftRegisterMem(x._2, config.pipe(i + log2Ceil(config.bp)), name = this.name + s"_${i}_${j}_pipeline_sram") }
      }
      // TODO: pipeline reorder
////      if (io.in.valid == true) {
//        printf("SYNCC %d %d \n", sync, (Reverse(sync.asUInt())))
//      }
    }
  }


  // wire up top-level outputs
  // note, truncation happens here!
  //  io.out.bits := stage_outputs(log2Ceil(config.lanes))

  if (config.unscrambleOut == true) {
    io.out.bits := unscramble(stage_outputs(log2Ceil(config.lanes)), config.lanes)
  } else {
    io.out.bits := stage_outputs(log2Ceil(config.lanes))
  }
}

class BiplexFFTIO[T <: Data : Real](lanes: Int, genIn: DspComplex[T], genMid: DspComplex[T])(implicit val p: Parameters) extends Bundle {
  val in = Input(ValidWithSync(Vec(lanes, genIn)))
  val out = Output(ValidWithSync(Vec(lanes, genMid)))
}

/**
  * fast fourier transform - cooley-tukey algorithm, decimation-in-time
  * biplex pipelined version
  * note, this is always a bp-point FFT
  *
  * @tparam T
  */
class BiplexFFT[T <: Data : Real](config: FFTConfig[T], genIn: DspComplex[T], genMid: DspComplex[T], genTwiddle: DspComplex[T])(implicit val p: Parameters) extends Module {
  val io = IO(new BiplexFFTIO[T](config.lanes, genIn, genMid))

  // synchronize
  var stage_delays = (0 until log2Ceil(config.bp) + 1).map(x => {
    if (x == log2Ceil(config.bp)) config.bp / 2 else (config.bp / pow(2, x + 1)).toInt
  })

  stage_delays = if (config.unscrambleIn==false) stage_delays else stage_delays.reverse
//  if (config.unscrambleIn == true){
//    stage_delays = stage_delays.reverse
//    stage_delays = stage_delays.drop(1) ++ stage_delays.take(1)
//    seq.drop(size - (i % size)) ++ seq.take(size - (i % size))
//  }

  val sync = List.fill(log2Ceil(config.bp) + 1)(Wire(UInt(width = log2Ceil(config.bp).W)))
  val valid_delay = RegNext(io.in.valid)
  sync(0) := CounterWithReset(true.B, config.bp, io.in.sync, ~valid_delay & io.in.valid)._1
  sync.drop(1).zip(sync).zip(stage_delays).foreach { case ((next, prev), delay) => next := ShiftRegisterWithReset(prev, delay, 0.U) }
  io.out.sync := (sync(log2Ceil(config.bp)) === ((config.bp / 2 - 1 + config.biplex_pipe) % config.bp).U) && (sync(log2Ceil(config.bp)) != 0.U)
  io.out.valid := ShiftRegisterWithReset(io.in.valid, stage_delays.reduce(_ + _) + config.biplex_pipe, 0.U)

  // wire up twiddles
  val genTwiddleReal = genTwiddle.real
  val genTwiddleImag = genTwiddle.imag

  println("[--DEBUG--]Stage_delays are:", stage_delays)
  //  val twiddle_rom = VecInit(config.twiddle.map(x => {
  //    val real = Wire(genTwiddleReal.cloneType)
  //    val imag = Wire(genTwiddleImag.cloneType)
  //    real := Real[T].fromDouble(x(0), genTwiddleReal)
  //    imag := Real[T].fromDouble(x(1), genTwiddleImag)
  //    val twiddle = Wire(DspComplex(genTwiddleReal, genTwiddleImag))
  //    twiddle.real := real
  //    twiddle.imag := imag
  //    twiddle
  //  }))
  val twiddle_rom = config.btwiddles.map(i => {
    Vec(i.map(j => {
      val real = Wire(genTwiddleReal.cloneType)
      val imag = Wire(genTwiddleImag.cloneType)
      real := Real[T].fromDouble(j(0), genTwiddleReal)
      imag := Real[T].fromDouble(j(1), genTwiddleImag)
      val twiddle = Wire(DspComplex(genTwiddleReal, genTwiddleImag))
      twiddle.real := real
      twiddle.imag := imag
      twiddle
    }))
  })
  //
  //  val indices_rom = VecInit(config.bindices.map(x => x.U))
  //  val indices = (0 until log2Ceil(config.bp)).map(x => indices_rom((pow(2,x)-1).toInt.U +& { if (x == 0) 0.U else ShiftRegisterMem(sync(x+1), config.pipe.dropRight(log2Ceil(config.n)-x).reduceRight(_+_), name = this.name + s"_twiddle_sram")(log2Ceil(config.bp)-2,log2Ceil(config.bp)-1-x) }))
  //  val twiddle = Wire(Vec(log2Ceil(config.bp), genTwiddle))
  //  // special cases
  //  if (config.n == 4) {
  //    twiddle := VecInit((0 until log2Ceil(config.bp)).map(x => {
  //      val true_branch  = Wire(genTwiddle)
  //      val false_branch = Wire(genTwiddle)
  //      true_branch     := twiddle_rom(0).divj()
  //      false_branch    := twiddle_rom(0)
  //      Mux(indices(x)(log2Ceil(config.n/4)), true_branch, false_branch)
  //    }))
  //  } else if (config.bp == 2) {
  //    twiddle := VecInit((0 until log2Ceil(config.bp)).map(x =>
  //      twiddle_rom(indices(x))
  //    ))
  //  } else {
  //    twiddle := VecInit((0 until log2Ceil(config.bp)).map(x => {
  //      val true_branch  = Wire(genTwiddle)
  //      val false_branch = Wire(genTwiddle)
  //      true_branch     := twiddle_rom(indices(x)(log2Ceil(config.n/4)-1, 0)).divj()
  //      false_branch    := twiddle_rom(indices(x))
  //      Mux(indices(x)(log2Ceil(config.n/4)), true_branch, false_branch)
  //    }))
  //  }
  //
  //  // bp-point decimation-in-time biplex pipelined FFT with outputs in bit-reversed order
  //  // up-scale to genMid immediately for simplicity
  //  val stage_outputs = List.fill(log2Ceil(config.bp)+2)(List.fill(config.lanes)(Wire(genMid)))
  //  io.in.bits.zip(stage_outputs(0)).foreach { case(in, out) => out := in }
  //
  //  // create the FFT hardware
  //  for (i <- 0 until log2Ceil(config.bp)+1) {
  //    for (j <- 0 until config.lanes/2) {
  //
  //      val skip = 1
  //      val start = j*2
  //
  //      // hook it up
  //      // last stage just has one extra permutation, no butterfly
  //      val mux_out = BarrelShifter(VecInit(stage_outputs(i)(start), ShiftRegisterMem(stage_outputs(i)(start+skip), stage_delays(i), name = this.name + s"_${i}_${j}_mux1_sram")), ShiftRegisterMem(sync(i)(log2Ceil(config.bp)-1 - { if (i == log2Ceil(config.bp)) 0 else i }), {if (i == 0) 0 else config.pipe.dropRight(log2Ceil(config.n)-i).reduceRight(_+_)},  name = this.name + s"_${i}_${j}_mux1_sram"))
  //      if (i == log2Ceil(config.bp)) {
  //        Seq(stage_outputs(i+1)(start), stage_outputs(i+1)(start+skip)).zip(Seq(ShiftRegisterMem(mux_out(0), stage_delays(i), name = this.name + s"_${i}_${j}_last_sram" ), mux_out(1))).foreach { x => x._1 := x._2 }
  //      } else {
  //        Seq(stage_outputs(i+1)(start), stage_outputs(i+1)(start+skip)).zip(Butterfly(Seq(ShiftRegisterMem(mux_out(0), stage_delays(i), name = this.name + s"_${i}_${j}_pipeline0_sram"), mux_out(1)), twiddle(i))).foreach { x => x._1 := ShiftRegisterMem(x._2, config.pipe(i), name = this.name + s"_${i}_${j}_pipeline1_sram") }
  //      }
  //
  //    }
  //  }
  //
  //  // wire up top-level outputs
  //  io.out.bits := stage_outputs(log2Ceil(config.bp)+1)
  val stage_outputs = List.fill(log2Ceil(config.bp) + 2)(List.fill(config.lanes)(Wire(genIn)))
  io.in.bits.zip(stage_outputs(0)).foreach { case (in, out) => out := in }

  printf("[--DEBUG--]SYNCS ARE:")
  for (i <- 0 until log2Ceil(config.bp)+1){
    printf("SYNC[%d]: %d ", i.U, sync(i))
  }
  printf("\n")
  // create the FFT hardware
  for (i <- 0 until log2Ceil(config.bp) + 1) {
    for (j <- 0 until config.lanes / 2) {

      val skip = 1
      val start = j * 2
      println("[--DEBUG--]Biplex:", i, j)
      // hook it up
      // last stage just has one extra permutation, no butterfly

      if (config.unscrambleIn == false) {
        val mux_out = BarrelShifter(VecInit(stage_outputs(i)(start),
          ShiftRegisterMem(stage_outputs(i)(start + skip), stage_delays(i), name = this.name + s"_${i}_${j}_mux0_sram")),
          ShiftRegisterMem(sync(i)(log2Ceil(config.bp) - 1 - {if (i == log2Ceil(config.bp)) 0 else i}),
          {if (i == 0) 0 else config.pipe.dropRight(log2Ceil(config.n) - i).reduceRight(_ + _)},
            name = this.name + s"_${i}_${j}_mux1_sram"))
        if (i == log2Ceil(config.bp)) {
          Seq(stage_outputs(i + 1)(start), stage_outputs(i + 1)(start + skip)).zip(
            Seq(
              ShiftRegisterMem(
                  mux_out(0),
                  stage_delays(i),
                  name = this.name + s"_${i}_${j}_last_sram"
                ),
              mux_out(1)
            )
          ).foreach { x => x._1 := x._2 }
        } else {
          Seq(stage_outputs(i + 1)(start), stage_outputs(i + 1)(start + skip)).zip(
            Butterfly(
              Seq(
                ShiftRegisterMem(
                  mux_out(0),
                  stage_delays(i),
                  name = this.name + s"_${i}_${j}_pipeline0_sram"
                ),
                mux_out(1)
              ),
              twiddle_rom(i)(sync(i + 1))
            )
          ).foreach { x => x._1 := ShiftRegisterMem(x._2, config.pipe(i), name = this.name + s"_${i}_${j}_pipeline1_sram") }
          printf("[--DEBUG--] %d, %d, ", i.U, j.U)
          printf("SYNC %d \n", sync(i+1))
        }
      } else {
        val mux_out = BarrelShifter(VecInit(stage_outputs(i)(start),
          ShiftRegisterMem(stage_outputs(i)(start + skip), stage_delays(i), name = this.name + s"_${i}_${j}_mux0_sram")),
          ShiftRegisterMem(sync(i)({if (i == 0) log2Ceil(config.bp)-1 else i-1}),
            {if (i == 0) 0 else config.pipe.dropRight(log2Ceil(config.n) - i).reduceRight(_ + _)}, name = this.name + s"_${i}_${j}_mux1_sram"))
        //TODO: why 0 is special here?

        if (i == log2Ceil(config.bp)) {
          Seq(stage_outputs(i + 1)(start), stage_outputs(i + 1)(start + skip)).zip(
            Seq(
              ShiftRegisterMem(
                mux_out(0),
                stage_delays(i),
                name = this.name + s"_${i}_${j}_last_sram"
              ),
              mux_out(1)
            )
          ).foreach { x => x._1 := x._2 }
        } else {
          Seq(stage_outputs(i + 1)(start), stage_outputs(i + 1)(start + skip)).zip(
            ButterflyDIF(
              Seq(
                ShiftRegisterMem(
                  mux_out(0),
                  stage_delays(i),
                  name = this.name + s"_${i}_${j}_pipeline0_sram"
                ),
                mux_out(1)
              ),
//              twiddle_rom(i)((Reverse(sync(i+1).asUInt())))
              twiddle_rom(log2Ceil(config.bp)-1-i)(sync(i+1))
//                twiddle_rom(i)(sync(i + 1))
            )
          ).foreach { x => x._1 := ShiftRegisterMem(x._2, config.pipe(i), name = this.name + s"_${i}_${j}_pipeline1_sram") }
          printf("[--DEBUG--] %d, %d, ", i.U, j.U)
//          printf("%d, %d",               twiddle_rom(log2Ceil(config.bp)-1-i)(sync(i+1)).real,               twiddle_rom(log2Ceil(config.bp)-1-i)(sync(i+1)).imag)
          printf("SYNC %d \n", sync(i+1))
        }
      }

    }
  }
  println("twiddle_rom")
  twiddle_rom.foreach{x=>
   x.foreach{ y=>
     print(y,'/')
   }
  }
  println("twiddle_rom2")
//      print(twiddle_rom(0)(0.U),twiddle_rom(1)(3.U))
//  (0 until 4).foreach{i=>
    printf("sssss%d", twiddle_rom(1)(3.U).asUInt())
  printf("xxxxx%d", twiddle_rom(1)(2.U).asUInt())
  printf("ssxxx%d", twiddle_rom(1)(1.U).asUInt())
  printf("aaaaa%d", twiddle_rom(1)(0.U).asUInt())
//    print(twiddle_rom(0)(i.U),'/')
//    print(twiddle_rom(1)(i.U),'/')
//
//  }
  // wire up top-level output
  io.out.bits := stage_outputs(log2Ceil(config.bp) + 1)

}

//TODO: Biplex's width should expand for unscrambleIn config
/**
  * IO Bundle for FFT
  *
  * @tparam T
  */
class FFTIO[T <: Data : Real](lanes: Int, genIn: DspComplex[T], genOut: DspComplex[T])(implicit val p: Parameters) extends Bundle {

  val in = Input(ValidWithSync(Vec(lanes, genIn)))
  val out = Output(ValidWithSync(Vec(lanes, genOut)))

  val data_set_end_status = Output(Bool())
  val data_set_end_clear = Input(Bool())
}

/**
  * fast fourier transform - cooley-tukey algorithm, decimation-in-time
  * mixed version
  * note, this is always an n-point FFT
  *
  * @tparam T
  */
class FFT[T <: Data : Real](val config: FFTConfig[T])(implicit val p: Parameters) extends Module {

  require(config.lanes >= 2, "Must have at least 2 parallel inputs")
  require(isPow2(config.lanes), "FFT parallelism must be a power of 2")
  require(config.lanes <= config.n, "An n-point FFT cannot have more than n inputs (p must be less than or equal to n)")

  val io = IO(new FFTIO(config.lanes, config.genIn, config.genOut))

  // calculate direct FFT input bitwidth
  // this is just the input total width + growth of 1 bit per biplex stage
  val genMid: DspComplex[T] = {
    if (config.bp == 1) {
      config.genIn
    }
    else {
//      val growth = log2Ceil(config.bp)
      val growth = if(config.unscrambleIn==false )log2Ceil(config.n) else log2Ceil(config.n)
      config.genIn.underlyingType() match {
        case "fixed" =>
          config.genIn.real.asInstanceOf[FixedPoint].binaryPoint match {
            case KnownBinaryPoint(binaryPoint) =>
              val totalBits = config.genIn.real.getWidth + growth
              DspComplex(FixedPoint(totalBits.W, binaryPoint.BP), FixedPoint(totalBits.W, binaryPoint.BP)).asInstanceOf[DspComplex[T]]
            case _ => throw new DspException("Error: unknown binary point when calculating FFT bitwdiths")
          }
        case "sint" => {
          val totalBits = config.genIn.real.getWidth + growth
          DspComplex(SInt(totalBits.W), SInt(totalBits.W)).asInstanceOf[DspComplex[T]]
        }
        case _ => throw new DspException("Error: unknown type when calculating FFT bitwidths")
      }
    }
  }

  // calculate twiddle factor bitwidth
  // total input bits
  val genTwiddleBiplex: DspComplex[T] = {
//    val growth = log2Ceil(config.bp)
    //TODO: figure out the right way to calculate width
    val growth = if(config.unscrambleIn==false )log2Ceil(config.n) else log2Ceil(config.n)
    config.genIn.underlyingType() match {
      case "fixed" =>
        config.genIn.asInstanceOf[DspComplex[T]].real.asInstanceOf[FixedPoint].binaryPoint match {
          case KnownBinaryPoint(binaryPoint) =>
            val totalBits = config.genIn.asInstanceOf[DspComplex[T]].real.getWidth + growth
            DspComplex(FixedPoint(totalBits.W, (totalBits - 2).BP), FixedPoint(totalBits.W, (totalBits - 2).BP)).asInstanceOf[DspComplex[T]]
          case _ => throw new DspException("Error: unknown binary point when calculating FFT bitwdiths")
        }
      case "sint" => {
        val totalBits = config.genIn.asInstanceOf[DspComplex[T]].real.getWidth + growth
        DspComplex(SInt(totalBits.W), SInt(totalBits.W)).asInstanceOf[DspComplex[T]]
      }
      case _ => throw new DspException("Error: unknown type when calculating FFT bitwidths")
    }
  }

  val genTwiddleDirect: DspComplex[T] = {
    val growth = if(config.unscrambleIn==false )log2Ceil(config.n) else log2Ceil(config.n)
    config.genIn.underlyingType() match {
      case "fixed" =>
        config.genIn.asInstanceOf[DspComplex[T]].real.asInstanceOf[FixedPoint].binaryPoint match {
          case KnownBinaryPoint(binaryPoint) =>
            val totalBits = config.genIn.asInstanceOf[DspComplex[T]].real.getWidth + growth
            DspComplex(FixedPoint(totalBits.W, (totalBits - 2).BP), FixedPoint(totalBits.W, (totalBits - 2).BP)).asInstanceOf[DspComplex[T]]
          case _ => throw new DspException("Error: unknown binary point when calculating FFT bitwdiths")
        }
      case "sint" => {
        val totalBits = config.genIn.asInstanceOf[DspComplex[T]].real.getWidth + growth
        DspComplex(SInt(totalBits.W), SInt(totalBits.W)).asInstanceOf[DspComplex[T]]
      }
      case _ => throw new DspException("Error: unknown type when calculating FFT bitwidths")
    }
  }

  // calculate direct FFT output bitwidth
  // this is just the input total width + growth of 1 bit per FFT stage
  val genOutDirect: DspComplex[T] = {
    if (config.bp == 1) {
      config.genIn
    }
    else {
//      val growth = log2Ceil(config.n)
      val growth = if(config.unscrambleIn==false )log2Ceil(config.n) else log2Ceil(config.n)
      config.genIn.underlyingType() match {
        case "fixed" =>
          config.genIn.asInstanceOf[DspComplex[T]].real.asInstanceOf[FixedPoint].binaryPoint match {
            case KnownBinaryPoint(binaryPoint) =>
              val totalBits = config.genIn.asInstanceOf[DspComplex[T]].real.getWidth + growth
              DspComplex(FixedPoint(totalBits.W, binaryPoint.BP), FixedPoint(totalBits.W, binaryPoint.BP)).asInstanceOf[DspComplex[T]]
            case _ => throw new DspException("Error: unknown binary point when calculating FFT bitwdiths")
          }
        case "sint" => {
          val totalBits = config.genIn.asInstanceOf[DspComplex[T]].real.getWidth + growth
          DspComplex(SInt(totalBits.W), SInt(totalBits.W)).asInstanceOf[DspComplex[T]]
        }
        case _ => throw new DspException("Error: unknown type when calculating FFT bitwidths")
      }
    }
  }

  // feed in zeros when invalid
  val in = Wire(ValidWithSync(Vec(config.lanes, config.genIn)))
  when(io.in.valid) {
    in.bits := io.in.bits
  }.otherwise {
    in.bits.foreach { case b =>
      b.real := Real[T].zero
      b.imag := Real[T].zero
    }
  }
  in.valid := io.in.valid
  in.sync := io.in.sync

  // data set end flag
  val valid_delay = RegNext(io.out.valid)
  val dses = RegInit(false.B)
  when(io.data_set_end_clear) {
    dses := false.B
  }.elsewhen(valid_delay & ~io.out.valid) {
    dses := true.B
  }
  io.data_set_end_status := dses

  // instantiate sub-FFTs
  if (config.unscrambleIn == false) {
    val direct = Module(new DirectFFT[T](
      config = config,
      genMid = genMid,
      genTwiddle = genTwiddleDirect,
      genOutFull = genOutDirect
    ))
    io.out <> direct.io.out

    if (config.n != config.lanes) {
      val biplex = Module(new BiplexFFT[T](config, config.genIn, genMid, genTwiddleBiplex))
      direct.io.in := biplex.io.out
      biplex.io.in <> in
    } else {
      direct.io.in <> in
    }
  } else {
    val direct = Module(new DirectFFT[T](
      config = config,
      genMid = config.genIn,
      genTwiddle = genTwiddleBiplex,
      genOutFull = genMid
    ))

    direct.io.in <> in
    if (config.n != config.lanes) {
      val biplex = Module(new BiplexFFT[T](config, genMid, genOutDirect, genTwiddleDirect))
      biplex.io.in := direct.io.out
      io.out <> biplex.io.out
    } else {
      io.out <> direct.io.out
    }
  }
}


object unscramble {
  def apply[T <: Data : Ring](in: Seq[T], p: Int): Seq[T] = {
    val n = in.size
    val bp = n / p
    val res: Array[Int] = Array.fill(n)(0)
    in.zipWithIndex.grouped(p).zipWithIndex.foreach { case (set, sindex) =>
      set.zipWithIndex.foreach { case ((_, bin), bindex) =>
        if (bp > 1) {
          val p1 = if (sindex / (bp / 2) >= 1) 1 else 0
          val new_index = bit_reverse((sindex % (bp / 2)) * 2 + p1, log2Ceil(bp)) + bit_reverse(bindex, log2Ceil(n))
          res(new_index) = bin
        } else {
          val new_index = bit_reverse(bindex, log2Ceil(n))
          res(new_index) = bin
        }
      }
    }
    res.map(in(_))
  }
}

object bit_reverse {
  // bit reverse a value
  def apply(in: Int, width: Int): Int = {
    var test = in
    var out = 0
    for (i <- 0 until width) {
      if (test / pow(2, width - i - 1) >= 1) {
        out += pow(2, i).toInt
        test -= pow(2, width - i - 1).toInt
      }
    }
    out
  }
}

