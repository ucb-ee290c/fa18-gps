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
        self.freq_cph_opt = [{'sum': 0,
                              'max': 0,
                              'mean': 0,
                              'satellite_found': False,
                              'freq_idx_opt': 0,
                              'cph_opt': 0
                              }
                             for i in range(0, n_satellite)]
        self.c = np.zeros((freq_idx_max+1, nSample), float)
        self.c_row = np.zeros(nSample, float)

    # TODO: complete the k -> threshold function
    def k_to_threshold(self):
        return self.k_max

    def reset_acq_result(self, satellite_idx):
        self.freq_cph_opt[satellite_idx]['sum'] = 0
        self.freq_cph_opt[satellite_idx]['max'] = 0
        self.freq_cph_opt[satellite_idx]['mean'] = 0

    def update(self, c_row):

        self.c[self.freq_idx] += c_row
        self.c_row += c_row


        # print('freq_idx and freq_idx_max:', self.freq_idx, self.freq_idx_max)
        if (self.k_idx < self.k_max - 1):
            self.k_idx += 1
        else:
            self.freq_cph_opt[self.satellite_idx]['sum'] += np.sum(self.c_row)
            print(np.max(self.c_row), self.freq_cph_opt[self.satellite_idx]['max'])
            if (np.max(self.c_row) > self.freq_cph_opt[self.satellite_idx]['max']):
                print('max updated', np.max(self.c_row), self.freq_idx, np.argmax(self.c_row))
                self.freq_cph_opt[self.satellite_idx]['max'] = np.max(self.c_row)
                self.freq_cph_opt[self.satellite_idx]['mean'] = np.mean(self.c_row)
                self.freq_cph_opt[self.satellite_idx]['freq_idx_opt'] = self.freq_idx
                self.freq_cph_opt[self.satellite_idx]['cph_opt'] = np.argmax(self.c_row)

            self.c_row = np.zeros(self.nSample, float)

            self.k_idx = 0
            print('freq_idx and freq_idx_max:', self.freq_idx, self.freq_idx_max)
            if (self.freq_idx < self.freq_idx_max):
                self.freq_idx += 1

            # return the acquisition result for the current satellite and
            # begin acquisition for the next satellite
            else:

                # mean = self.freq_cph_opt[self.satellite_idx]['sum'] / (self.nSample * (self.freq_idx_max + 1))
                max = self.freq_cph_opt[self.satellite_idx]['max']
                mean = self.freq_cph_opt[self.satellite_idx]['mean']
                ratio = max / mean
                print(max, mean, ratio)
                satellite_found = ratio > self.threshold

                self.freq_cph_opt[self.satellite_idx]['satellite_found'] = satellite_found


                self.freq_idx = 0
                if (self.satellite_idx < self.n_satellite - 1):
                    self.satellite_idx += 1
                else:
                    self.satellite_idx = 0

                self.reset_acq_result(self.satellite_idx)



