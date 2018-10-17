import numpy as np
import matplotlib.pyplot as plt
from enum import Enum

from block import Block

PREAMBLE = [1, 0, 0, 0, 1, 0, 1, 1]
WORD_LENGTH = 30
SUBFRAME_LENGTH = 10

class State(Enum):
    WAITING = 0
    PARSING = 1
    DONE = 2

#TODO: Finish Packetizer class
class Packet(Block):
    def __init__(self):
        self.fifo = [0] * 8 # Bits are shifted into this register upon arrival
        self.state = State.WAITING # State machine
        self.data = [[] for i in range(SUBFRAME_LENGTH)] # Subframe contents - self.data[i][j] accesses the jth bit of the ith word of the subframe
        self.word_idx = 0 # Word number being read
        self.bit_idx = 0 # Bit number being read
        self.out = [] # MMIO registers
        self.valid = False # Valid signal

    def update(self, cycle, I_int, Q_int):
        self.fifo = self.fifo[1:] + [I_int]
        print(str(cycle) + ": Packetizer FIFO contains " + ''.join(map(str, self.fifo)))
        if self.state == State.WAITING: # Not currently reading a subframe
            if self.fifo == PREAMBLE:
                print("Preamble detected!")
                self.state = State.PARSING
                self.data[0][:] = self.fifo[:]
                self.bit_idx = 8
                self.word_idx = 0
        elif self.state == State.PARSING: # Reading a subframe
            self.data[self.word_idx].append(I_int)
            if self.bit_idx == WORD_LENGTH - 1:
                if self.word_idx == SUBFRAME_LENGTH - 1:
                    self.state = State.DONE
                    return
                else:
                    self.bit_idx = 0
                    self.word_idx += 1
            else:
                self.bit_idx += 1
        elif self.state == State.DONE: # Out is valid
            print("Writing data to memory map.")
            self.out[:] = self.data[:]
            self.data[:] = []
            self.state = State.WAITING
            pass
        else:
            raise Exception("Invalid state detected in packetizer.")
        self.valid = (self.state == State.DONE)
