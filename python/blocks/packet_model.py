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
        self.fifo = [0] * 8
        self.state = State.WAITING
        self.data = []
        self.word = []
        self.word_idx = 0
        self.bit_idx = 0
        self.out = []
        self.valid = False

    def update(self, cycle, I_int, Q_int):
        self.fifo = self.fifo[1:] + [I_int]
        print(str(cycle) + ": Packetizer FIFO contains " + ''.join(map(str, self.fifo)))
        if self.state == State.WAITING:
            if self.fifo == PREAMBLE:
                print("Preamble detected!")
                self.state = State.PARSING
                self.word[:] = self.fifo[:]
                self.bit_idx = 8
                self.word_idx = 0
        elif self.state == State.PARSING:
            self.word.append(I_int)
            self.bit_idx += 1
            if self.bit_idx == WORD_LENGTH:
                self.data.append(self.word[:])
                self.word[:] = []
                self.bit_idx = 0
                self.word_idx += 1
                if self.word_idx == SUBFRAME_LENGTH:
                    self.state = State.DONE
                    return
        elif self.state == State.DONE:
            print("Writing data to memory map.")
            self.out[:] = self.data[:]
            self.data[:] = []
            self.state = State.WAITING
            pass
        else:
            raise Exception("Invalid state detected in packetizer.")
        self.valid = (self.state == State.DONE)
