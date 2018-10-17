import numpy as np
import matplotlib.pyplot as plt
from enum import Enum

from block import Block

PREAMBLE = [1, 0, 0, 0, 1, 0, 1, 1]

class State(Enum):
    WAITING = 0
    PARSING = 1
    DONE = 2

#TODO: Finish Packetizer class
class Packet(Block):
    def __init__(self):
        self.data = [0] * 8
        self.state = State.WAITING

    def update(self, cycle, I_int, Q_int):
        self.data = self.data[1:] + [I_int]
        print(str(cycle) + ": Packetizer FIFO contains " + ''.join(map(str, self.data)))
        if self.state == State.WAITING:
            if self.data == PREAMBLE:
                print("Preamble detected!")
                self.state = State.PARSING
        elif self.state == State.PARSING:
            pass
        elif self.state == State.DONE:
            pass
        else:
            raise Exception("Invalid state detected in packetizer.")
