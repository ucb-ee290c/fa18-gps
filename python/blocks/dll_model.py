import numpy as np
import matplotlib.pyplot as plt

from .block import Block

#TODO: Finish DLL class
class DLL(Block):
    
    def __init__(self, kp, ki):
        self.kp = kp
        self.ki = ki

    def update(self, I_sample, Q_sample):
        return 1

