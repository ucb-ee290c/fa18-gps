import numpy as np
import matplotlib.pyplot as plt
from mpl_toolkits.mplot3d import Axes3D
from acquisition_model_test import *


# ========================================================
#
# ========================================================
def acquisition_1cycle(k_max, k_curr, f_idx_curr):
    fcarrier = 4128460
    dopOffset = 3000
    dopStep = 100

    f_idx_max = 2 * dopOffset / dopStep - 1

    freq_step = dopStep
    freq_min = fcarrier - dopOffset
    freq_max = fcarrier + dopOffset


    if (k_curr != k_max - 1):
        k_next = k_curr + 1
        f_idx_next = f_idx_curr

    # data for one freqeuncy fully collected, can find out the optimal code phase for this frequency
    else:
        k_next = 0

        if (f_idx_curr == f_idx_max):
            f_idx_next = 0
        else:
            f_idx_next = f_idx_curr + 1

    output_dict = {'f_idx_next': f_idx_next, 'k_next': k_next}

    return output_dict


def acquisition_fullloop(k_max):
    fcarrier = 4128460
    fsample = 16367600
    dopOffset = 3000
    dopStep = 100
    nSample = 200000

    freq_init = fcarrier - dopOffset

    total_cycles = k_max * 2 * int(round(dopOffset / dopStep))

    f_idx_curr = 0
    k_curr = 0


    c = np.zeros((2 * dopOffset // dopStep + 1, nSample), float)
    data = readRawData();

    # i represents the clock cycle
    for i in range(0, total_cycles):
        if (k_curr == 0):
            dataIdx = 0

        _data = data[dataIdx: dataIdx + nSample]
        freq_curr = freq_init + dopStep * f_idx_curr
        c[f_idx_curr] = c[f_idx_curr] + FFTSearch(_data, fsample, freq_curr, nSample)

        idx_next = acquisition_1cycle(k_max, k_curr, f_idx_curr)
        k_curr = idx_next['k_next']

        f_idx_curr = idx_next['f_idx_next']
        dataIdx = dataIdx + nSample

    idx_max = np.argmax(c)
    freq_idx_max = idx_max // nSample
    cph_idx_max = idx_max % nSample
    print('optimal freq and code phase:', freq_idx_max, cph_idx_max)
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
    acquisition_fullloop(k_max=10)
    plt.show()