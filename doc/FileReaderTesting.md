# File Reader Module

`FileReader` in `src/main/scala/gps/FileReader.scala` is a blackbox verilog module that is used to read test vectors into the test harness for Rocketchip integration tests using the Verilator backend. The verilog file is at `src/main/resources/BBFileReader.sv`. This gets around the issue of having to load large test vectors into a header file. This module was developed together with the MIMO 290C group.

## Params
* `ReadBitWidth` the bitwidth of the data to read
* `FileName` the name of the test data file to read

## Inputs
* `run`: (Bool) signal to tell module to read a line from the file

## Outputs
* `valid`: (Bool) high when the data is valid (ie. not at the end of the file yet)
* `out`: (SInt) test data read from the file

## Test
* run `sbt test:testOnly gps.FileReaderSpec` and see that it passes the test.  It will read and display several lines from the file `src/test/resources/BBFileReaderData.txt`. 
