import numpy as np
import matplotlib.pyplot as plt
from .block import Block

#FIXME: Fix CA Code Gen class to match output of skeleton
class CA(Block):

    SV = {
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
    
    def __init__(self, sv_num, code_bias, code_length=1023):
        self.prev_tick = 0
        self.curr_index = 0
        self.curr_sv = None
        self.curr_prn_list = None
        self.shift_reg = ShiftRegister()
        self.done = 0
        self.code_length = code_length
        self.code_bias = code_bias
        self.sv_num = sv_num

    def update(self, tick, tick_2x, sv_num, code_bias):

        # updates
        self.code_bias = code_bias
        self.sv_num = sv_num

        # check satellite id
        assert sv_num >= 1 and sv_num <= 32, "Invalid satellite choice"

        self.done = 0
        if self.curr_sv is None or self.curr_sv != sv_num:
           self.curr_prn_list = self.PRN(sv_num)
           self.curr_sv = sv_num
           self.curr_index = 0
           self.prev_tick = 0

        early = self.check_tick(tick, code_bias)
        self.shift_reg.insert(early)
        punctual, late = self.shift_reg.update(tick_2x)

        # Convert from [0, 1] to [-1, 1]
        early = 2*early - 1
        punctual = 2*punctual - 1
        late = 2*late - 1
        return early, punctual, late, self.done

    def check_tick(self, tick, code_bias):

        if self.prev_tick < 0 and tick >= 0:
            self.curr_index += 1
            if self.curr_index >= len(self.curr_prn_list):
                self.curr_index = 0
                print("PRN GENERATION DONE")
                self.done = 1

        self.prev_tick = tick
        return self.curr_prn_list[(self.curr_index-code_bias) % self.code_length]
    
    def shift(self, register, feedback, output):
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

    def PRN(self, sv):
        """Build the CA code (PRN) for a given satellite ID
        
        :param int sv: satellite code (1-32)
        :returns list: ca code for chosen satellite
        
        """
        
        # init registers
        G1 = [1 for i in range(10)]
        G2 = [1 for i in range(10)]

        ca = []     # stuff output in here

        # create sequence
        for i in range(1023):
            g1 = self.shift(G1, [3,10], [10])
            g2 = self.shift(G2, [2,3,6,8,9,10], CA.SV[sv]) # <- sat chosen here from table
            
            # modulo 2 add and append to the code
            ca.append((g1 + g2) % 2)

        # return C/A code!
        return ca


class ShiftRegister(Block):
    def __init__(self):
        self.input = None
        self.curr_index = 0
        self.prev_tick = -1
        self.my_list = [1, 1, 1]    # always length 2

    def update(self, tick):

        if self.prev_tick < 0 and tick >= 0:
            self.my_list[2] = self.my_list[1]
            self.my_list[1] = self.my_list[0]
            self.my_list[0] = self.input
        self.prev_tick = tick
        return self.my_list[1], self.my_list[2]

    def insert(self, val):
        self.input = val
