import numpy as np
import matplotlib.pyplot as plt

from block import Block

#TODO: Finish Packetizer class
class Packet(Block):
    def update(self, cycle, I_int, Q_int):
        print(str(cycle) + ": Packetized data should be printed here")
        return 0


