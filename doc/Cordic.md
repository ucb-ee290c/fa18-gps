## Cordic Generator

The Cordic block is doing Cordic related calculations. In this design, we can support 
rotation, vectoring(atan, atan2), and dividing, which we are using in the DLL, Costas, and FLL discriminator. 

### Parameters
The following are the parameters for a type generic Cordic block. 

 - nStages: # of stages
 - stagesPerCycle: # number of stages in a cycle, need to be divisible by nStages
 - correctGain: true to correct gain for x and y, in rotation and vectoring
 - calAtan2: true to calculate atan2 in vectoring mode
 - dividing: true to work in dividing mode
 
To make sure the block working correctly, we need to carefully define the size of *x*, *y* and *z* 
as well as *nStages*. The following are the suggestions:

    First of all, for *x* and *y* with fixed-point number with total width *Width* and binary point width *BPWidth*, the 
    integer number (absolute value) we can use without error is 2^(Width-BPWidth-2)-1. MSB bit is from sign, and MSB-1 
    bit to avoid overflow in cordic calculation.
    
    In rotation and vectoring (including atan and atan2 calculation) modes: 
        For x and y, the max integer part of input <= 2**(N-2) - 1; for z is from -pi to pi, 
        which only needs 3 bits integers.
        
        We suggest that 
            nStages <= xyWidth and zWidth.
            zWidth - zBPWidth = 3, for atan function, zWidth - zBPWidth could even be 2.

    In dividing mode:
        |y| must not be greater than 2*|x| or else the cordic division will fail. 
        The gps tracking loop does not encounter the division failing since the values passed into the cordic are already normalized 
        
        We also suggest that
            x, y and z have same width.
            The integer bits <= fixed-point bits: xyWidth-xyBPWidth <= xyBPWidth, zWidth-zBPWidth <= zBPWidth
            nStages = max(xyWidth, zWidth)
        

#### Inputs:
The explicit inputs are: 

 - in(x, y, z): cordic inputs
 - vectoring: true in vectoring mode

#### Outputs:

 - out(x, y, z): cordic outputs
