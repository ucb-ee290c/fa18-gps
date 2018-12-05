package gps

import chisel3._
import chisel3.experimental.FixedPoint
import chisel3.util._
import dsptools.numbers._

/** A set of parameters for CA modules.
 *
 *  @param fcoWidth the bit width of the NCO output for both 1x and 2x frequencies
 *  @param codeWidth the bit width of the code to be generated. The code is either +1 or -1, so it must be at least 2.
 */
case class CAParams (
  val fcoWidth: Int,
  val codeWidth: Int
)

/** A convenience class that replicates a shift register of 2 elements. The reason for having a second class is 
 * due to the fact that the shift register needs to be clocked at a different frequency. This class takes in a single
 * code input and then has a 2 element shift register. The updating is done by checking the 0 crossings of the 2x frequency clock input
 *
 *  IO: 
 *  codeIN: Input(SInt), single bit of a CA code
 *  fco2x: Input(SInt), NCO output, clocked at 2x the frequency of the NCO used in the CA generator
 *  punctual: Output(SInt), codeIn, but delayed by one cycle. Updates at zero-crossings of the fco2x input 
 *  late: Output(SInt), codeIn, but delayed by two cycles. Updates at zero-crossings of the fco2x input 
 *  @param params same parameters used to generate the CA module
 */
class CACodeShiftReg(params: CAParams) extends Module {
  val io = IO(new Bundle {
    val codeIn = Input(SInt(params.codeWidth.W))
    val fco2x = Input(SInt(params.fcoWidth.W))
    val punctual = Output(SInt(params.codeWidth.W))
    val late = Output(SInt(params.codeWidth.W))
  })
  val punctual = RegInit(1.S(params.codeWidth.W))
  val late = RegInit(1.S(params.codeWidth.W))
  val prev_tick = RegInit(0.S(params.fcoWidth.W))
  prev_tick := io.fco2x
  when(prev_tick < 0.S && io.fco2x >= 0.S) {
    punctual := io.codeIn
    late := punctual
  }
  io.punctual := punctual
  io.late := late
}

/** A GPS L1 C/A PRN code generator module. 
 *  IO:
 *  satellite: Input(UInt), an int between 1 and 32 that selects which satellite's CA code to generate.
 *  fco: Input(SInt), an NCO that drives the update frequency of the CA code
 *  fco2x: Input(SInt), an NCO at 2x the frequency of fco that drives the update of the CA code
 *  early: Output(SInt), The CA code, updated at zero-crossings of fco
 *  late: Output(SInt), see CACodeShiftReg
 *  punctual: Output(SInt), see CACodeShiftReg
 *  done: Output(Bool), true for the cycle that a 1023-length CA code has fully finished
 *  currIndex: Output(UInt), the current index of the 1023 length CA code being outputted
 *  @constructor create a CA code generator module
 *  @param params an instance of the case class CAParams that includes all io parameters
 */
class CA(params: CAParams) extends Module {
    val io = IO(new Bundle {
        val satellite = Input(UInt(6.W)) //There are 32 possible feedbacks. Need 6 bits
        val fco = Input(SInt(params.fcoWidth.W))
        val fco2x = Input(SInt(params.fcoWidth.W))

        val early = Output(SInt(params.codeWidth.W)) //
        val punctual = Output(SInt(params.codeWidth.W))
        val late = Output(SInt(params.codeWidth.W))
        val done = Output(Bool()) //Goes high when the full length of the code has finished
        val currIndex = Output(UInt(log2Ceil(1024).W))
    })
    require(params.fcoWidth > 0)
    require(params.codeWidth > 0)
    //Can hardcode these widths because the max they can be is 10, so we need 4 bits
    val feedbackPos = RegInit(VecInit(0.U(4.W), 0.U(4.W)))
    switch(io.satellite) {
     is(1.U){ feedbackPos := Seq(2.U,6.U) }
     is(2.U){ feedbackPos := Seq(3.U,7.U) }
     is(3.U){ feedbackPos := Seq(4.U,8.U) }
     is(4.U){ feedbackPos := Seq(5.U,9.U) }
     is(5.U){ feedbackPos := Seq(1.U,9.U) }
     is(6.U){ feedbackPos :=  Seq(2.U,10.U) }
     is(7.U){ feedbackPos :=  Seq(1.U,8.U) }
     is(8.U){ feedbackPos :=  Seq(2.U,9.U) }
     is(9.U){ feedbackPos :=  Seq(3.U,10.U) }
     is(10.U){ feedbackPos :=  Seq(2.U,3.U) }
     is(11.U){ feedbackPos :=  Seq(3.U,4.U) }
     is(12.U){ feedbackPos :=  Seq(5.U,6.U) }
     is(13.U){ feedbackPos := Seq(6.U,7.U) }
     is(14.U){ feedbackPos := Seq(7.U,8.U) }
     is(15.U){ feedbackPos := Seq(8.U,9.U) }
     is(16.U){ feedbackPos := Seq(9.U,10.U) }
     is(17.U){ feedbackPos := Seq(1.U,4.U) }
     is(18.U){ feedbackPos := Seq(2.U,5.U) }
     is(19.U){ feedbackPos := Seq(3.U,6.U) }
     is(20.U){ feedbackPos := Seq(4.U,7.U) }
     is(21.U){ feedbackPos := Seq(5.U,8.U) }
     is(22.U){ feedbackPos := Seq(6.U,9.U) }
     is(23.U){ feedbackPos := Seq(1.U,3.U) }
     is(24.U){ feedbackPos := Seq(4.U,6.U) }
     is(25.U){ feedbackPos := Seq(5.U,7.U) }
     is(26.U){ feedbackPos := Seq(6.U,8.U) }
     is(27.U){ feedbackPos := Seq(7.U,9.U) }
     is(28.U){ feedbackPos := Seq(8.U,10.U) }
     is(29.U){ feedbackPos := Seq(1.U,6.U) }
     is(30.U){ feedbackPos := Seq(2.U,7.U) }
     is(31.U){ feedbackPos := Seq(3.U,8.U) }
     is(32.U){ feedbackPos := Seq(4.U,9.U) }
    }
    val prev_tick = RegInit(0.S(params.fcoWidth.W))
    val curr_sv = RegInit(0.U(6.W)) //32 sattelites, can hardcode this width
    val counter = RegInit(0.U(log2Ceil(1024).W)) //Code length always 1023, can hardcode width
    val g1 = RegInit(VecInit(Seq.fill(10)(1.U(params.codeWidth.W))))
    val g2 = RegInit(VecInit(Seq.fill(10)(1.U(params.codeWidth.W))))
    prev_tick := io.fco
    //Want to restart the sequence if the satellite changes or if we complete one full sequence 
    when((curr_sv =/= io.satellite) || (counter === 1023.U)) {
        counter := 0.U 
        curr_sv := io.satellite
        //prev_tick := 0.S(params.fcoWidth.W)
        g1 := VecInit(Seq.fill(10)(1.U(params.codeWidth.W)))
        g2 := VecInit(Seq.fill(10)(1.U(params.codeWidth.W)))
    }.elsewhen(prev_tick < 0.S && io.fco >= 0.S) {
      counter := counter + 1.U
      //Feedback to the first element in the shift register by adding mod 2 
      //Feedback is 2, 3, 6, 8, 9, 10, off by 1 for the same reason 
      //Push the rest of the elements down the list.
      g1(0.U) := (g1(2.U) + g1(9.U)) % 2.U
      g2(0.U) := (g2(1.U) + g2(2.U) + g2(5.U) + g2(7.U) + g2(8.U) + g2(9.U)) % 2.U
      for (i <- 1 until 10) {
          g1(i.U) := g1(i.U-1.U)
          g2(i.U) := g2(i.U-1.U)
      }
    }
    val shifts = Module(new CACodeShiftReg(params))
    //Feedback for g1 is always from position 10 (off by 1)
    //Feedback for g2 is an xor of 2 positions based on the sattelite
    //Have to subtract 1 because satellite feedback positions are 1 indexed
    val res = (g1(9.U) + (g2(feedbackPos(0.U) - 1.U) + g2(feedbackPos(1.U) - 1.U))) % 2.U
    io.early := Mux(res === 1.U, 1.S(params.codeWidth.W), -1.S(params.codeWidth.W)) 
    io.done := counter === 1023.U
    shifts.io.fco2x := io.fco2x
    shifts.io.codeIn := io.early
    io.punctual := shifts.io.punctual
    io.currIndex := counter
    io.late := shifts.io.late
}
