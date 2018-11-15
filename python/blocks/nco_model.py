import numpy as np
import matplotlib.pyplot as plt

from .block import Block


class NCO(Block):

    def __init__(self, count_width, code, init_phase):

        # count_max
        self.count_max = 2**count_width - 1
        self.fast_count_max = 2**(count_width-1) - 1

        # get initial count from initial phase
        self.init_phase = init_phase
        self.count = init_phase % (2*np.pi) / (2*np.pi) * \
                     self.count_max
        self.fast_count = init_phase % (2*np.pi) / (2*np.pi) * \
                          self.fast_count_max / 2

        # if a code NCO
        self.code = code

    def update(self, freq_ctrl, phase_ctrl):

        # update fctrl_sum, f_count, count and fast_count
        self.count += freq_ctrl
        self.fast_count += freq_ctrl

        if self.count >= self.count_max:
            self.count -= self.count_max

        if self.fast_count >= self.fast_count_max:
            self.fast_count -= self.fast_count_max

        if self.code:
            return np.sign(np.cos(2*np.pi*(self.count+phase_ctrl) / self.count_max)), \
                   np.sign(np.cos(2*np.pi*(self.fast_count+phase_ctrl/2) / self.fast_count_max))
        else: 
            return np.cos(2*np.pi*(self.count+phase_ctrl) / self.count_max), \
                   np.sin(2*np.pi*(self.count+phase_ctrl) / self.count_max)

