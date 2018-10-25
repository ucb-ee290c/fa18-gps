package gps

import dsptools.DspTester 

class NcoTester(c: NCO) extends DspTester(c) {

    val maxCyclesWait = 50
    poke(c.io.code, true)
    poke(c.io.stepSize, 1)

    expect(c.io.cosine, 1)

}

object NcoTester {
    def apply(): Boolean = { 
        chisel3.iotesters.Driver.execute(Array("-tbn", "firrtl", "-fiwv"), () => new NCO(10)) {
        c => new NcoTester(c)
        }   
    }
}