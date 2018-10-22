# EE 290C Individual Repository

This repo will house work for your individual assignments.

## CORDIC

Design and implement a flexible CORDIC generator in Chisel.

1) Support vectoring and rotation modes
2) Make widths for `X`, `Y`, and `Z` parameterized
3) Gain correction will be optionally supported (controlled by a parameter).
If gain correction is enabled, the output will be multiplied by the appropriate scaling factor.
Otherwise, the unscaled result will be passed directly to the output.
4) `Z` is modulo $2\pi$ with full range supported
5) The degree of unrolling should be set by a parameter.

### Parameters and IO
`Cordic.scala` defines some parameters objects, a `Bundle` to use as your IO, and an empty implementation of a FixedIterativeCordic (which you will need to implement).

### CordicApp
The template also includes an `App` (similar to a main function in Java) that can be used to run your generator.
It accepts commandline arguments.
To see the options, run
```
sbt "run --help"
```

### Tests
The template includes a simple test using `DspTester` and an associated ScalaTest specification.
You can run tests with
```
sbt test
```

# GPS L1 C/A Reciver
## Python models
### Packetizer
* `python/blocks/packet_model.py` defines the class of the `Packet` model. A `Packet` instance contains instances of two other classes:
        * `Parser`: scans the incoming stream for a preamble, indicating the start of a subframe, then splits the subframe into words and writes them to an output interface.
        * `Parity_Checker`: performs a parity check on each word after the parser is done, then writes the words and the results of the parity checks to an output interface.
* `python/packet_encode.py` contains functions for performing parity encoding. The function `generate_subframe()` is called in `packet_test.py`; it produces a subframe with the correct preamble and randomized data, then attaches the correct parity bits.
* `python/packet_test.py` generates a subframe and feeds it into an instance of `Packet`, which reports results to the user.
