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
    
    def __init__(self, sv_num, sym_code=True):

        # check input satellite
        assert sv_num >= 1 and sv_num <= 32, "Invalid satelite number/ID."
        self.sv_num = sv_num
        self.prn_list = None
        self.prn_index = 0
        self.sym_code = sym_code

        # initialize codes
        self.code = 0
        self.prev_code = 0
        self.early = 0
        self.punct = 0
        self.late = 0

        # initial shifter registers
        self.SHIFT_REGISTER_LENGTH = 10
        self.CA_CODE_LENTH = 2**self.SHIFT_REGISTER_LENGTH - 1

        # genearate prn_list
        self.prn_gen(self.sv_num)

    def update(self, offset=0):
        """
        C/A code update. The order of the code is very important.
        Parameters
        ----------
        sine: Union[float, Int]
            sinusoid wave.
        sine2x: Union[float, Int]
            2x frequency sinusoid wave.
        Returns
        -------
            early, punct and late code.
        """
        # when sine ticks
        self.code = self.prn_list[(self.prn_index-offset)%1023]

        if self.sym_code:
            self.code = self.code * 2 - 1

        # update punct
        self.punct = self.early

        # prepare for next tick
        self.prn_index += 1
        if self.prn_index == 1023:
            self.prn_index = 0
        self.prev_code = self.code

        return self.early, self.punct, self.late

    def update2x(self):

        # when sine2x ticks
        self.early = self.code
        self.late = self.punct

        return self.early, self.punct, self.late

    def lsfr(self, register, fb_idx_list, out_idx_list):
        """
        GPS LSRF calculation.

        Parameters
        ----------
        register: list[Int]
            register list.
        fb_idx_list: list[Int]
            feedback index list.
        out_idx_list: Int
            output index

        Returns
        -------
        lsfr_out: Int
            C/A code output
        """
        # lsfr list
        lsfr_list = [register[i-1] for i in out_idx_list]

        if len(lsfr_list) > 1:
            lsfr_out = sum(lsfr_list) % 2
        else:
            lsfr_out = lsfr_list[0]
            
        # modulo 2 add feedback
        fb = sum([register[i-1] for i in fb_idx_list]) % 2
        
        # shift to the right
        for i in reversed(range(len(register[1:]))):
            register[i+1] = register[i]
            
        # put feedback in position 1
        register[0] = fb

        return lsfr_out

    def prn_gen(self, sv):
        """
        Build the CA code (PRN) for a given satellite ID
        """
        
        # init registers to all 1
        G1 = [1 for i in range(self.SHIFT_REGISTER_LENGTH)]
        G2 = [1 for i in range(self.SHIFT_REGISTER_LENGTH)]

        prn = [] # stuff output in here
        # create sequence
        for i in range(self.CA_CODE_LENTH):
            g1 = self.lsfr(G1, [3, 10], [10])
            # output chosen from SV table
            g2 = self.lsfr(G2, [2, 3, 6, 8, 9, 10], CA.SV[sv])
            
            # modulo 2 add and append to the code
            prn.append((g1 + g2) % 2)

        # return C/A code!
        self.prn_list = prn



