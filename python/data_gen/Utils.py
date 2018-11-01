# G1 Code Generator
# Created by Sijun Du on 08/31/2018

import numpy as np
from collections import deque

######## Generate G1 code for C/A PRN coding using shift registers #######
# Refer to IS-GPS-200 document Table 3-8
# https://www.gps.gov/technical/icwg/IS-GPS-200J.pdf
class G1CodeGenerator:

    g1ShiftRegister = deque([0b1, 0b1, 0b1, 0b1, 0b1, 0b1, 0b1, 0b1, 0b1, 0b1, 0b1] )
    g1Code = []
        
    i = 0
    while i < 1023: 
        g1Code.append(g1ShiftRegister[-1])
        g1ShiftRegister[0] = g1ShiftRegister[3] ^ g1ShiftRegister[10]
        g1ShiftRegister.rotate(1)
        i = i+1
        

######### Generate G2 code for C/A PRN coding using delay LUT and shift registers #######
# Refer to IS-GPS-200 document Table 3-Ia
# Refer to IS-GPS-200 document Table 3-9
# https://www.gps.gov/technical/icwg/IS-GPS-200J.pdf
def G2CodeGenerator(SVID):
    G2CodeDelayBank = [5, 6, 7, 8, 17,   18, 139, 140, 141, 251,    252, 254, 255, 256, 257,   258, 469, 470, 471, 472,    473, 474, 509, 512, 513,    514, 515, 516, 859, 860,    861, 862]
    g2ShiftRegister = deque([0b1, 0b1, 0b1, 0b1, 0b1, 0b1, 0b1, 0b1, 0b1, 0b1, 0b1] )
    g2Code = []
    g2Delay = G2CodeDelayBank[SVID]
    g2Delay = 0

    for i in range(0, 1023+g2Delay):
        if i >= g2Delay:
            #g2Code.append(g2ShiftRegister[-1])
            if SVID == 0:     g2Code.append(g2ShiftRegister[2] ^ g2ShiftRegister[6])
            elif SVID == 1:   g2Code.append(g2ShiftRegister[3] ^ g2ShiftRegister[7])
            elif SVID == 2:   g2Code.append(g2ShiftRegister[4] ^ g2ShiftRegister[8])
            elif SVID == 3:   g2Code.append(g2ShiftRegister[5] ^ g2ShiftRegister[9])
            elif SVID == 4:   g2Code.append(g2ShiftRegister[1] ^ g2ShiftRegister[9])
            elif SVID == 5:   g2Code.append(g2ShiftRegister[2] ^ g2ShiftRegister[10])
            elif SVID == 6:   g2Code.append(g2ShiftRegister[1] ^ g2ShiftRegister[8])
            elif SVID == 7:   g2Code.append(g2ShiftRegister[2] ^ g2ShiftRegister[9])
            elif SVID == 8:   g2Code.append(g2ShiftRegister[3] ^ g2ShiftRegister[10])
            elif SVID == 9:   g2Code.append(g2ShiftRegister[2] ^ g2ShiftRegister[3])
            elif SVID == 10:  g2Code.append(g2ShiftRegister[3] ^ g2ShiftRegister[4])
            elif SVID == 11:  g2Code.append(g2ShiftRegister[5] ^ g2ShiftRegister[6])
            elif SVID == 12:  g2Code.append(g2ShiftRegister[6] ^ g2ShiftRegister[7])
            elif SVID == 13:  g2Code.append(g2ShiftRegister[7] ^ g2ShiftRegister[8])
            elif SVID == 14:  g2Code.append(g2ShiftRegister[8] ^ g2ShiftRegister[9])
            elif SVID == 15:  g2Code.append(g2ShiftRegister[9] ^ g2ShiftRegister[10])
            elif SVID == 16:  g2Code.append(g2ShiftRegister[1] ^ g2ShiftRegister[4])
            elif SVID == 17:  g2Code.append(g2ShiftRegister[2] ^ g2ShiftRegister[5])
            elif SVID == 18:  g2Code.append(g2ShiftRegister[3] ^ g2ShiftRegister[6])
            elif SVID == 19:  g2Code.append(g2ShiftRegister[4] ^ g2ShiftRegister[7])
            elif SVID == 20:  g2Code.append(g2ShiftRegister[5] ^ g2ShiftRegister[8])
            elif SVID == 21:  g2Code.append(g2ShiftRegister[6] ^ g2ShiftRegister[9])
            elif SVID == 22:  g2Code.append(g2ShiftRegister[1] ^ g2ShiftRegister[3])
            elif SVID == 23:  g2Code.append(g2ShiftRegister[4] ^ g2ShiftRegister[6])
            elif SVID == 24:  g2Code.append(g2ShiftRegister[5] ^ g2ShiftRegister[7])
            elif SVID == 25:  g2Code.append(g2ShiftRegister[6] ^ g2ShiftRegister[8])
            elif SVID == 26:  g2Code.append(g2ShiftRegister[7] ^ g2ShiftRegister[9])
            elif SVID == 27:  g2Code.append(g2ShiftRegister[8] ^ g2ShiftRegister[10])
            elif SVID == 28:  g2Code.append(g2ShiftRegister[1] ^ g2ShiftRegister[6])
            elif SVID == 29:  g2Code.append(g2ShiftRegister[2] ^ g2ShiftRegister[7])
            elif SVID == 30:  g2Code.append(g2ShiftRegister[3] ^ g2ShiftRegister[8])
            elif SVID == 31:  g2Code.append(g2ShiftRegister[4] ^ g2ShiftRegister[9])
        mod2value = g2ShiftRegister[2] ^ g2ShiftRegister[3] ^ g2ShiftRegister[6] ^ g2ShiftRegister[8] ^ g2ShiftRegister[9] ^ g2ShiftRegister[10]
        g2ShiftRegister.rotate(1)
        g2ShiftRegister[0] = mod2value

    return g2Code


######### Generate C/A PRN code using G1 and G2 codes for any of the 32 satellite vehiches ######
def CACode(SVID):
    caCode = []
    g1Code = G1CodeGenerator.g1Code
    g2Code = G2CodeGenerator(SVID)
    #print(g1Code)
    #print(g2Code)
    for i in range(0, len(g1Code)):
        caCode.append(g1Code[i] ^ g2Code[i])
    return caCode

######## A function to perform bit-wise inversion in a list (or numpy array) #########
def Inverter(data):
    inverted = []
    i = 0
    while i < len(data):
        if data[i] == 1:
            inverted.append(0)
        else:
            inverted.append(1)
        i = i+1
    return  inverted


######## Modulate a Baseband data with the C/A code according to L1 C/A protocol #######
def CACodeModulation(data, caCode):
    modulated = []
    i = 0
    while i < len(data):
        if data[i] == 1:
            modulated.append(caCode * 20)
        else:
            modulated.append(Inverter(caCode) * 20)
        i = i+1
    return modulated


####### Non-return-to-zero ########
def NRZ(data):
    bufData = (np.array(data)-0.5)*2
    bufData = bufData.astype(int)
    return bufData


def shift(register, feedback, output):
    """GPS Shift Register

    :param list feedback: which positions to use as feedback (1 indexed)
    :param list output: which positions are output (1 indexed)
    :returns output of shift register:

    """

    # calculate output
    out = [register[i-1] for i in output]
    if len(out) > 1:
        out = sum(out) % 2
    else:
        out = out[0]

    # modulo 2 add feedback
    fb = sum([register[i-1] for i in feedback]) % 2

    # shift to the right
    for i in reversed(range(len(register[1:]))):
        register[i+1] = register[i]

    # put feedback in position 1
    register[0] = fb

    return out


G2CPS = {
   1: [2,6],
   2: [3,7],
   3: [4,8],
   4: [5,9],
   5: [1,9],
   6: [2,10],
   7: [1,8],
   8: [2,9],
   9: [3,10],
  10: [2,3],
  11: [3,4],
  12: [5,6],
  13: [6,7],
  14: [7,8],
  15: [8,9],
  16: [9,10],
  17: [1,4],
  18: [2,5],
  19: [3,6],
  20: [4,7],
  21: [5,8],
  22: [6,9],
  23: [1,3],
  24: [4,6],
  25: [5,7],
  26: [6,8],
  27: [7,9],
  28: [8,10],
  29: [1,6],
  30: [2,7],
  31: [3,8],
  32: [4,9],
}

def PRN(sv):
    """Build the CA code (PRN) for a given satellite ID

    :param int sv: satellite code (1-32)
    :returns list: ca code for chosen satellite

    """
    sv = sv + 1
    # init registers
    G1 = [1 for i in range(10)]
    G2 = [1 for i in range(10)]

    ca = [] # stuff output in here

    # create sequence
    for i in range(0, 1023):
        g1 = shift(G1, [3,10], [10])
        g2 = shift(G2, [2,3,6,8,9,10], G2CPS[sv]) # <- sat chosen here from table

        # modulo 2 add and append to the code
        ca.append((g1 + g2) % 2)

    # return C/A code!
    return ca


