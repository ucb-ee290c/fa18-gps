import numpy as np
import matplotlib.pyplot as plt

from .block import Block

class NCO(Block):

    def __init__(self, count_width, code):
        self.count_max = 2**count_width - 1
        self.fast_count_max = 2**(count_width-1) - 1
        self.count = 0
        self.fast_count = 0
        self.code = code;

    def update(self, step_size):
        self.count += step_size
        self.fast_count += step_size
        if self.count > self.count_max:
            self.count -= self.count_max
        if self.fast_count > self.fast_count_max:
            self.fast_count -= self.fast_count_max
        if (self.code):
            # print(2*np.pi*self.count / self.count_max, 2*np.pi*self.count*2/self.count_max)
            return np.cos(2*np.pi*self.count / self.count_max), \
                   np.cos(2*np.pi*self.fast_count / self.fast_count_max)  
        else: 
            return np.sign(np.cos(2*np.pi*self.count / self.count_max)), \
                   np.sign(np.sin(2*np.pi*self.count / self.count_max))

