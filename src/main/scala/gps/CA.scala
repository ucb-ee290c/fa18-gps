package gps

import chisel3._
import chisel3.experimental.FixedPoint
import chisel3.util._
import dsptools.numbers._

case class CAParams (
  val fcoWidth: Int,
  val codeWidth: Int
)


class CA(params: CAParams) extends Module {
    val io = IO(new Bundle {
        val satellite = Input(UInt(6.W)) //There are 32 possible feedbacks. Need 6 bits
        val fco = Input(SInt(params.fcoWidth.W))
        val fco_2x = Input(SInt(params.fcoWidth.W))

        val early = Output(SInt(params.codeWidth.W)) //
        val punctual = Output(SInt(params.codeWidth.W))
        val late = Output(SInt(params.codeWidth.W))
        val done = Output(Bool()) //Goes high when the full length of the code has finished
    })
    //require((io.satellite >= 1.U) && (io.satellite <= 32.U))
    val feedback_pos = Reg(VecInit(Seq(0.U, 0.U)))
    switch(io.satellite) {
     is(1.U){ feedback_pos := Seq(2.U,6.U) }
     /*
     is(2.U){ feedback_pos := Seq(3.U,7.U) }
     is(3.U){ feedback_pos := Seq(4.U,8.U) }
     is(4.U){ feedback_pos := Seq(5.U,9.U) }
     */
     /*
     case 5.U => Seq(1.U,9.U)
     case 6.U => Seq(2.U,10.U)
     case 7.U => Seq(1.U,8.U)
     case 8.U => Seq(2.U,9.U)
     case 9.U => Seq(3.U,10.U)
     case 10.U => Seq(2.U,3.U)
     case 11.U => Seq(3.U,4.U)
     case 12.U => Seq(5.U,6.U)
     case 13.U => Seq(6.U,7.U)
     case 14.U => Seq(7.U,8.U)
     case 15.U => Seq(8.U,9.U)
     case 16.U => Seq(9.U,10.U)
     case 17.U => Seq(1.U,4.U)
     case 18.U => Seq(2.U,5.U)
     case 19.U => Seq(3.U,6.U)
     case 20.U => Seq(4.U,7.U)
     case 21.U => Seq(5.U,8.U)
     case 22.U => Seq(6.U,9.U)
     case 23.U => Seq(1.U,3.U)
     case 24.U => Seq(4.U,6.U)
     case 25.U => Seq(5.U,7.U)
     case 26.U => Seq(6.U,8.U)
     case 27.U => Seq(7.U,9.U)
     case 28.U => Seq(8.U,10.U)
     case 29.U => Seq(1.U,6.U)
     case 30.U => Seq(2.U,7.U)
     case 31.U => Seq(3.U,8.U)
     case 32.U => Seq(4.U,9.U)
     */
    }
    
    val prev_tick = Reg(SInt(params.fcoWidth.W))
    val curr_index = Reg(UInt(10.W)) //Codes are a fixed length of 1023. Can hardcode this
    val curr_sv = Reg(UInt(6.W)) //32 sattelites, can hardcode this width
    val counter = RegInit(0.U(log2Ceil(1024).W))
 
    val g1 = RegInit(VecInit(Seq.fill(10)(1.S(params.codeWidth.W))))
    val g2 = RegInit(VecInit(Seq.fill(10)(1.S(params.codeWidth.W))))
   
    //Feedback to the first element in the shift register by adding mod 2, AKA XOR 
    g1(0.U) := g1(2.U) ^ g1(9.U) //feedback is always 3 and 9, but that's for 1 indexing
    g2(0.U) := g2(1.U) ^ g2(2.U) ^ g2(5.U) ^ g2(7.U) ^ g2(8.U) ^ g2(9.U)
    //Feedback is 2, 3, 6, 8, 9, 10, off by 1 for the same reason 
    //Push the rest of the elements down the list.
    for (i <- 1 until 10) {
        g1(i.U) := g1(i.U-1.U)
        g2(i.U) := g2(i.U-1.U)
    }
    //Feedback for g1 is always from position 10 (off by 1)
    //Feedback for g2 is an xor of 2 positions based on the sattelite
    val res = g1(9.U) ^ (g2(feedback_pos(0.U)) ^ g2(feedback_pos(1.U)))
    io.early := Mux(res === 1.S, 1.S(params.codeWidth.W), -1.S(params.codeWidth.W)) 
    io.done := counter === 1023.U
}
