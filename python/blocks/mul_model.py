import numpy as np
import matplotlib.pyplot as plt

from .block import Block

#FIXME: Figure out sizing for multiplier class
class MUL(Block):

	#for the python model, dont pay attention to the multiplier bitwidth for now.
    def update(self, in1, in2):
        return in1*in2


