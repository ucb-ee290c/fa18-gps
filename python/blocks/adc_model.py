import numpy as np
import matplotlib.pyplot as plt

from .block import Block

class ADC(Block):
    
    def __init__(self, data):
        self.data = data
        self.index = -1

    def update(self):
        self.index += 1
        if self.index < len(self.data):
            return self.data[self.index]


