package gps

import chisel3._
import chisel3.experimental.FixedPoint
import chisel3.util._

import dsptools.numbers._

class NCO(w: Int) extends Module {
    val io = IO(new Bundle {
        val code = Input(Bool())
        val stepSize = Input(UInt(w.W))

        val cosine = Output(SInt(w.W))
    })
    
    io.cosine := io.stepSize.asSInt    
}
