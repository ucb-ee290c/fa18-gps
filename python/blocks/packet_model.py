import numpy as np
import matplotlib.pyplot as plt
from enum import Enum

from block import Block

PREAMBLE = [1, 0, 0, 0, 1, 0, 1, 1]
WORD_LENGTH = 30
SUBFRAME_LENGTH = 10

class Parser_State(Enum):
    WAITING = 0
    PARSING = 1
    DONE = 2

#TODO: Finish Packetizer class
class Packet(Block):
    def __init__(self):
        self.parser = Parser()
        self.parity_checker = Parity_Checker()

    def update(self, cycle, I_int, Q_int):
        self.parser.update(cycle, I_int, Q_int)
        self.parity_checker.update(cycle, self.parser.out, self.parser.valid, self.parser.d_star)

class Parser(Block):
    def __init__(self):
        self.fifo = [0] * 8 # Bits are shifted into this register upon arrival
        self.state = Parser_State.WAITING # State machine
        self.data = [[] for i in range(SUBFRAME_LENGTH)] # Subframe contents - self.data[i][j] accesses the jth bit of the ith word of the subframe
        self.word_idx = 0 # Word number being read
        self.bit_idx = 0 # Bit number being read
        self.out = [] # MMIO registers
        self.valid = False # Valid signal
        self.d_star = [0, 0]

    def update(self, cycle, I_int, Q_int):
        self.fifo = self.fifo[1:] + [I_int]
        print(str(cycle) + ": Packetizer FIFO contains " + ''.join(map(str, self.fifo)))
        if self.state == Parser_State.WAITING: # Not currently reading a subframe
            if self.fifo == PREAMBLE:
                print("Preamble detected!")
                self.state = Parser_State.PARSING
                self.data[0][:] = self.fifo[:]
                self.bit_idx = 8
                self.word_idx = 0
        elif self.state == Parser_State.PARSING: # Reading a subframe
            self.data[self.word_idx].append(I_int)
            if self.bit_idx == WORD_LENGTH - 1:
                if self.word_idx == SUBFRAME_LENGTH - 1:
                    self.state = Parser_State.DONE
                    print("Writing data to memory map.")
                    self.out[:] = self.data[:]
                    self.data[:] = []
                else:
                    self.bit_idx = 0
                    self.word_idx += 1
            else:
                self.bit_idx += 1
        elif self.state == Parser_State.DONE: # Out is valid
            self.state = Parser_State.WAITING
            self.d_star[:] = self.out[SUBFRAME_LENGTH - 1][WORD_LENGTH - 2:]
        else:
            raise Exception("Invalid state detected in packetizer.")
        self.valid = (self.state == Parser_State.DONE)

class Parity_Checker(Block):
    def __init__(self):
        self.valid_out = False
        self.correct = [False for i in range(SUBFRAME_LENGTH)]
        self.data_out = [[] for i in range(SUBFRAME_LENGTH)]

    def update(self, cycle, data, valid_in, d_star):
        if valid_in:
            self.data_out[:] = data[:][:24]
            for i in range(len(data)):
                if i == 0:
                    print("Parity check result: " + str(self.check_parity(d_star, data[i])))
                else:
                    print("Parity check result: " + str(self.check_parity(data[i-1][WORD_LENGTH-2:], data[i])))
                pass
        else:
            print("Parity checker idle.")

    def check_parity(self, d_star, d):
        calc_parity = [0] * 6
        calc_parity[0] = d_star[0] ^ d[0] ^ d[1] ^ d[2] ^ d[4] ^ d[5] ^ d[9] ^ d[10] ^ d[11] ^ d[12] ^ d[13] ^ d[16] ^ d[17] ^ d[19] ^ d[22]
        calc_parity[1] = d_star[1] ^ d[1] ^ d[2] ^ d[3] ^ d[5] ^ d[6] ^ d[10] ^ d[11] ^ d[12] ^ d[13] ^ d[14] ^ d[17] ^ d[18] ^ d[20] ^ d[23]
        calc_parity[2] = d_star[0] ^ d[0] ^ d[2] ^ d[3] ^ d[4] ^ d[6] ^ d[7] ^ d[11] ^ d[12] ^ d[13] ^ d[14] ^ d[15] ^ d[18] ^ d[19] ^ d[21]
        calc_parity[3] = d_star[1] ^ d[1] ^ d[3] ^ d[4] ^ d[5] ^ d[7] ^ d[8] ^ d[12] ^ d[13] ^ d[14] ^ d[15] ^ d[16] ^ d[19] ^ d[20] ^ d[22]
        calc_parity[4] = d_star[1] ^ d[0] ^ d[2] ^ d[4] ^ d[5] ^ d[6] ^ d[8] ^ d[9] ^ d[13] ^ d[14] ^ d[15] ^ d[16] ^ d[17] ^ d[20] ^ d[21] ^ d[23]
        calc_parity[5] = d_star[0] ^ d[2] ^ d[4] ^ d[5] ^ d[7] ^ d[8] ^ d[9] ^ d[10] ^ d[12] ^ d[14] ^ d[18] ^ d[21] ^ d[22] ^ d[23]
        return (calc_parity == d[24:30])
