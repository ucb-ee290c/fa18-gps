import numpy as np
import matplotlib.pyplot as plt

from block import Block

class NCO(Block):

    def __init__(self, count_width, code):
        self.count_max = 2**count_width - 1
        self.count = 0
        self.code = code;

    def update(self, step_size):
        self.count += step_size
        if self.count > self.count_max:
            self.count -= self.count_max
        if (self.code):
            return np.sin(2*np.pi*self.count / self.count_max), \
                   np.sin(2*np.pi*2*self.count / self.count_max)  
        else: 
            return np.cos(2*np.pi*self.count / self.count_max), \
                   np.sin(2*np.pi*self.count / self.count_max)

