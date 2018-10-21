import numpy as np
import matplotlib.pyplot as plt

from .block import Block

class AcquisitionControl(Block):

    def __init__(self, n_satellite, freq_idx_max, k_max, nSample):
        self.n_satellite = n_satellite;
        self.freq_idx_max = freq_idx_max;
        self.k_max = k_max;
        self.threshold = self.k_to_threshold()
        self.nSample = nSample;
        self.satellite_idx = 0;
        self.freq_idx = 0;
        self.k_idx = 0;
        self.freq_cph_opt = [{'satellite_found': False, 'freq_idx_opt': 0, 'cph_opt': 0}
                             for i in range(0, n_satellite)]
        self.c = np.zeros((freq_idx_max+1, nSample), float)

    # TODO: complete the k -> threshold function
    def k_to_threshold(self):
        return self.k_max

    def update(self, c_row):

        self.c[self.freq_idx] += c_row
        # print('freq_idx and freq_idx_max:', self.freq_idx, self.freq_idx_max)
        if (self.k_idx < self.k_max - 1):
            self.k_idx += 1
        else:
            self.k_idx = 0
            print('freq_idx and freq_idx_max:', self.freq_idx, self.freq_idx_max)
            if (self.freq_idx < self.freq_idx_max):
                self.freq_idx += 1
                # if (self.freq_idx == self.freq_idx_max - 1):
                #
            # return the acquisition result for the current satellite and
            # begin acquisition for the next satellite
            else:
                idx_opt = np.argmax(self.c)
                freq_idx_opt = idx_opt // self.nSample
                cph_opt = idx_opt % self.nSample
                ratio = self.c.max() / self.c.mean()
                print(self.c.max(), self.c.mean(), ratio)
                satellite_found = ratio > self.threshold

                self.freq_cph_opt[self.satellite_idx] = {'satellite_found': (self.c.max() / self.c.mean() > self.threshold),
                                                         'freq_idx_opt': np.argmax(self.c) // self.nSample,
                                                         'cph_opt': np.argmax(self.c) % self.nSample}


                self.freq_idx = 0
                if (self.satellite_idx < self.n_satellite - 1):
                    self.satellite_idx += 1
                else:
                    self.satellite_idx = 0



