## Cordic Generator

The Cordic block is to do Cordic related calculation, such as rotation, vectoring(atan, atan2), dividing. 
In costas/DLL loop case, it is forced to be one-cycle, but should be fairly easy to change it to multi-cycle.

### Parameters
 - xyWidth: x, y total width
 - xyBPWidth: x, y binary-point width
 - zWidth: z total width
 - zBPWidth: z binary width
 - nStages: # of stages

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
The early/punctual/late signals

- satellite: UInt, 6 bits
- fco: SInt, NCO input, bits determined by fcoWidth in CAParams
- fco2x: SInt, an NCO input at a 2x rate, same bit width as fco

#### Outputs:

- early: SInt, bit width of codeWidth, either -1, or 1.
- punctual: SInt, bit width of codeWidth, either -1 or 1.
- late: SInt, bit width of codeWidth, either -1 or 1
- done: Bool, high when an entire 1023 length sequence has finished.
- currIndex: UInt, which index of the CA code the early output is currently on