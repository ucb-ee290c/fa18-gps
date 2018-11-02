import numpy as np
import matplotlib.pyplot as plt
from .block import Block

#FIXME: Fix CA Code Gen class to match output of skeleton
class TimeKeeper(Block):

    def __init__(self):
        self.counter = 0
        self.en = 0

    def update(self, reset, code_bias):

        if reset == 1:
            self.counter = 0
            self.en = 0
        else:
            if self.en == 0:
                self.counter += 1
                if self.counter >= code_bias:
                    self.en = 1
                    self.counter = 0

        return self.en
