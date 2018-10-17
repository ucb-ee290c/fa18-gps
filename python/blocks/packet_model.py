import numpy as np
import matplotlib.pyplot as plt

from block import Block

#TODO: Finish Packetizer class
class Packet(Block):
    def __init__(self):
        self.data = [0] * 8

    def update(self, cycle, I_int, Q_int):
        self.data = self.data[1:] + [I_int]
        print(str(cycle) + ": Packetizer FIFO contains " + ''.join(map(str, self.data)))
