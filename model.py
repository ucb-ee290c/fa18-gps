from abc import ABC, abstractmethod
import numpy as np
import matplotlib.pyplot as plt

raw_data = np.fromfile('gioveAandB_short.bin', dtype=np.int8)

class Block(ABC):

    @abstractmethod
    def update(self):
        raise NotImplementedError


class NCO(Block):

    def __init__(self, out_frequency, sample_frequency):
        self.count_val = 1023
        self.count_factor = int(out_frequency / sample_frequency * count_val)
        self.count = 0

    def update(self):
        self.count += self.count_factor 
        if self.count > self.count_val:
            self.count -= self.count_val
        return np.cos(2*np.pi*self.count / self.count_val), 
               np.sin(2*np.pi*self.count / self.count_val)


class DLL(Block):
    
    def __init__(self, kp, ki):
        self.kp = kp
        self.ki = ki

    def update(self, I_sample, Q_sample):
        return 1


class ADC(Block):
    
    def __init__(self, data):
        self.data = data
        self.index = 0

    def update(self):
        self.index += 1
        if index > len(data):
            return data[index]

adc = ADC(np.fromfile('gioveAandB_short.bin', dtype=np.int8)
dll = DLL(1,1)
