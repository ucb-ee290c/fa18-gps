import matplotlib.pyplot as plt
from mpl_toolkits.mplot3d import Axes3D
import numpy as np
import pdb
# ca generation
def caGen(i, chiprate, fs, nSamples):

    phase = [[2, 6], [3, 7], [4, 8], [5, 9], [1, 9], [2, 10], [1, 8], [2, 9], [3, 10],
             [2, 3], [3, 4], [5, 6], [6, 7], [7, 8], [8, 9], [9, 10], [1, 4], [2, 5],
             [3, 6], [4, 7], [5, 8], [6, 9], [1, 3], [4, 6], [5, 7], [6, 8], [7, 9],
             [8, 10], [1, 6], [2, 7], [3, 8], [4, 9]]

    g1 = [1] * 10
    g2 = [1] * 10
    g = [0] * 1023
    s1 = phase[i][0]
    s2 = phase[i][1]
    # print("phase are",s1,s2)
    for i in range(1023):
        g[i] = (g2[s1 - 1] + g2[s2 - 1] + g1[9]) % 2
        _t = g1[0]
        g1[0] = (g1[2] + g1[9]) % 2
        g1[1:10] = [_t] + g1[1:9]
        _t = g2[0]
        g2[0] = (g2[1] + g2[2] + g2[5] + g2[7] + g2[8] + g2[9]) % 2
        # print(g2[0])
        g2[1:10] = [_t] + g2[1:9]

    # plt.plot(np.linspace(0, 1023, 1023), g)
    ca = []

    # Because input signal range from -3 to 3, so use -3 and 3 here, +-1 should also work
    for i in range(int(nSamples)):
        if g[int(i * chiprate // fs) % 1023] > 0:
            ca.append(1)
        else:
            ca.append(-1)

    return ca


def readRawData():
    rawData = []
    with open("data/gioveAandB_short.bin", "rb") as binary_file:
        data = binary_file.read()
        for i in data:
            if i > 254:
                t = -1
            elif i > 200:
                t = -3
            elif i > 2:
                t = 3
            else:
                t = 1
            rawData.append(t)
    return rawData

if __name__ == '__main__':
    n_data = int(16367.6*2)
    fs = 16367600
    fc = 4128460
    f_range = 2000
    fc_step = 1000
    fc_low = fc - f_range
    fc_high = fc + f_range

    f_num = 2*f_range / fc_step

    chiprate = 1.023e6
    sv = 21
    # get all ca code at sampling rate
    ca = caGen(sv, chiprate=chiprate, fs=fs, nSamples=n_data//2)
    t = np.arange(n_data)/fs

    raw_data = readRawData()
    data = raw_data[n_data:2*n_data]

    doppler_idx = 0
    cc = []
    for fc in range(fc_low, fc_high, fc_step):
        doppler_idx += 1
        i_comp = np.sin(2*np.pi*fc*t) * data
        q_comp = np.cos(2*np.pi*fc*t) * data

        c_f = []
        for phase_index in np.arange(1, n_data//2):

            i = sum(i_comp[phase_index:phase_index+n_data//2] * ca)
            q = sum(q_comp[phase_index:phase_index+n_data//2] * ca)
            c_f.append(i**2 + q**2)

        cc.append(c_f)

    cc = np.array(cc)
    fig = plt.figure()
    ax = fig.add_subplot(111, projection="3d")
    X, Y = np.meshgrid(f_num, int(n_data//2))
    print('max', cc.max(), 'mean', cc.mean(), 'ratio', cc.max() / cc.mean())
    print(np.where(cc == cc.max()))

    pdb.set_trace()

    ax.plot_surface(X, Y, cc.transpose())
    plt.show(block=False)





