## 3rd Order Loop filter for Costas and FLL Loop

The loop filter uses the output from and FLL discriminator to complete initial frequency difference and 
follow doppler frequency change and output from Costas to lock phase. 

The loop filter refers "Understanding GPS" Ch5.5, For fll loop, it's a 2nd-order filter
    
    H(s) = gain * (w0^2/s^2 + a2*w0/s)
    noise bandwidth: Bn = 0.53*w0
    a2 = 1.414
    
For Costas loop, it's a 3rd-order filter, 
    
    H(s) = gain * (w0^3/s^3 + a3*w0^2/s^2 + b3*w0/s)
    noise bandwidth: Bn = 0,7845*w0
    a3 = 1.1
    b3 = 2.4

#### Parameters

 - fBandwidth: frequency bandwidth, 3Hz optimum
 - pBandwidth: phase bandwidth, 17Hz optimum
 - width: fixed-point total width
 - BPWidth: fixed-point binary-point width
 - a2, a3, b3: filter coefficients
 - fDCGain: frequency DC gain, default 1
 - pDCGain: phase DC gain, default 1

#### Inputs:
 
 - freqErr: frequency discriminator out
 - phaseErr: phase discriminator out
 - intTime: intDump time
 - valid: True when input are valid
 
#### Outputs:

 - out: loop filter output
