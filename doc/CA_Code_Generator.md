## GPS CA Code Generator

The CA Code generator is a block responsible for creating the "coarse aquisition" pseudo-random codes transmitted by satellites to allow a 
tracking loop to lock on to the incoming signal. The delay-locked loop requires the CA code as well as two delayed versions of the
same CA code to calibrate time delay. These are referred to as the early/punctual/late signals. The early signal is clocked by the zero-crossings
of the NCO. Punctual is equal to the early output, but delayed by a one NCO2X zero-crossing "clock cycle." Late is similar, but 2 NCO2X cycles delayed. This block is used in both the tracking and aquisiton blocks to determine which satellite is transmitting, as well as helping to lock on.

### Parameters

#### CAParams Class

To allow some degree of parametrization, the CA block has a specific class of params that can be passed in. 

```
case class CAParams (
  val fcoWidth: Int,
  val codeWidth: Int
)
```

#### Inputs:
This block accepts in a number corresponding to which satellite's code should be generated, as well as two numerically controlled oscillator
(NCO) inputs to clock how fast each bit of the code should come out. This allows for an NCO to change the phase of the CA generation on the fly.

The early/punctual/late signals are used by the DLL to help lock on, as described above.

- `satellite`: UInt, 6 bits
- `fco`: SInt, NCO input, bits determined by fcoWidth in CAParams
- `fco2x`: SInt, an NCO input at a 2x rate, same bit width as fco

`satellite` must be between 1 and 32.

#### Outputs:

- `early`: SInt, bit width of codeWidth, either -1, or 1.
- `punctual`: SInt, bit width of codeWidth, either -1 or 1.
- `late`: SInt, bit width of codeWidth, either -1 or 1
- `done`: Bool, high when an entire 1023 length sequence has finished.
- `currIndex`: UInt, which index of the CA code the early output is currently on
