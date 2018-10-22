import numpy as np
import matplotlib.pyplot as plt
from mpl_toolkits.mplot3d import Axes3D
from acquisition_model_test import FFTSearch, readRawData
from blocks.acq_ctrl_model import *
from blocks.block import Block




def test(k_max):

    fcarrier = 4128460
    fsample = 16367600
    dopOffset = 3000
    dopStep = 100
    nSample = 200000
    freq_idx_max = 2 * int(round(dopOffset / dopStep))

    acq_ctrl_model = AcquisitionControl(n_satellite=1,
                                        freq_idx_max=freq_idx_max,
                                        k_max=k_max,
                                        nSample=nSample)

    freq_init = fcarrier - dopOffset

    total_cycles = k_max * (freq_idx_max + 1)




    # c = np.zeros((2 * dopOffset // dopStep + 1, nSample), float)
    data = readRawData();

    # i represents the clock cycle
    for i in range(0, total_cycles):
        k_curr = acq_ctrl_model.k_idx

        if (k_curr == 0):
            dataIdx = 0

        _data = data[dataIdx: dataIdx + nSample]
        freq_curr = freq_init + dopStep * acq_ctrl_model.freq_idx
        FFT_result = FFTSearch(_data, fsample, freq_curr, nSample)

        acq_ctrl_model.update(FFT_result)

        dataIdx = dataIdx + nSample

    c = acq_ctrl_model.c
    freq_cph_opt = acq_ctrl_model.freq_cph_opt
    # idx_max = np.argmax(c)
    satellite_found = acq_ctrl_model.freq_cph_opt[0]['satellite_found']
    freq_idx_max = acq_ctrl_model.freq_cph_opt[0]['freq_idx_opt']
    cph_idx_max = acq_ctrl_model.freq_cph_opt[0]['cph_opt']
    print('acquisition result:', acq_ctrl_model.freq_cph_opt)
    print('optimal freq and code phase:', freq_idx_max, cph_idx_max, 'satellite_found =', satellite_found)
    cmax = c.max()
    cmean = c.mean()
    ratio = cmax / cmean



    xxx = np.linspace(0, (2 * dopOffset // dopStep), (2 * dopOffset // dopStep) + 1)
    xy = np.linspace(0, nSample, nSample)

    ax = plt.axes(projection='3d')
    X, Y = np.meshgrid(xxx, xy)
    print(np.shape(c), np.shape(X), np.shape(Y))
    print('max', c.max(), 'mean', c.mean(), 'ratio', c.max() / c.mean())
    ax.plot_surface(X, Y, c.transpose())




if __name__ == "__main__":
    test(k_max=10)
    plt.show()