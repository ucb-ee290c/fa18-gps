"""
@Author: Zhaokai Liu
@File: raw_data_reader.py
@Time: 10/12/18 14:15
@Description: 

"""
import timeit
import numpy as np
from functools import partial
import matplotlib.pyplot as plt
import math
from matplotlib import cm
from mpl_toolkits.mplot3d import Axes3D
from matplotlib.ticker import LinearLocator, FormatStrFormatter


def readRawData():
    rawData = []
    with open("data/gioveAandB_short.bin", "rb") as binary_file:
        data = binary_file.read()
        # print(data[0:1000])
        # Data are stored in signed bytes, there must be better way to read...
        # the result is compared with the distribution and fft in the website.
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
    for i in range(nSamples):
        if g[(i * chiprate // fs) % 1023] > 0:
            ca.append(3)
        else:
            ca.append(-3)

    return ca


def FFTSearch(data, fs, fc, nSamples, sv):
    fchip = 1023000
    caCode = caGen(sv, fchip, fs, nSamples)
    t = np.linspace(0, (nSamples - 1) * fc / fs, nSamples)
    sin = np.sin(2 * np.pi * t)
    cos = np.cos(2 * np.pi * t)

    dataArray = np.asarray(data)

    i = np.multiply(dataArray, cos)
    q = np.multiply(dataArray, sin)

    _start = timeit.default_timer()
    inFFT = np.fft.fft(i + 1j * q)
    caFFT = np.fft.fft(caCode)
    C = np.square(np.abs(np.fft.ifft(np.multiply(np.conjugate(inFFT), caFFT))))
    _stop = timeit.default_timer()
    # print("[-FFT analysis]: runtime %f" % (_stop-_start))

    return C, _stop-_start


def SFFTSearch(data, fs, fc, nSamples, sv,p=4):
    fchip = 1023000
    caCode = caGen(sv, fchip, fs, nSamples)
    t = np.linspace(0, (nSamples - 1) * fc / fs, nSamples)
    sin = np.sin(2 * np.pi * t)
    cos = np.cos(2 * np.pi * t)

    dataArray = np.asarray(data)

    i = np.multiply(dataArray, cos)
    q = np.multiply(dataArray, sin)

    sig = i + 1j * q
    _start0 = timeit.default_timer()

    sublen = math.ceil(nSamples/p)
    subsig = [0]*sublen
    subca = [0]*sublen
    for i in range(sublen):
        subsig[i] = sum(sig[i::sublen])
        subca[i] = sum(caCode[i::sublen])

    _start = timeit.default_timer()
    inFFT = np.fft.fft(subsig)
    caFFT = np.fft.fft(subca)
    # print(len(sig),len(caCode))
    C = np.square(np.abs(np.fft.ifft(np.multiply(np.conjugate(inFFT), caFFT))))
    _stop = timeit.default_timer()
    # print("[-FFT analysis]: fft runtime %f" % (_stop-_start))
    # print("[-FFT analysis]: fft runtime + data preproccess%f" % (_stop-_start0))
    return C, _stop-_start


def acquisition(sv, k, sfft=False):
    fcarrier = 4130400
    fsample = 16367600
    dopOffset = 10000
    dopStep = 500
    nSample = 16368

    subsamprate = int(math.sqrt(math.log2(nSample)))
    print("SUBSAMPLERATE", subsamprate)
    # subsamprate = 4
    size = nSample // subsamprate if sfft else nSample
    c = np.zeros((2 * dopOffset // dopStep + 1, size), float)
    data = readRawData()
    t = 0
    # print(list(range(fcarrier - dopOffset, fcarrier + dopOffset + dopStep, dopStep)))
    fftfunc = partial(SFFTSearch,  fs=fsample, nSamples=nSample, sv=sv, p=subsamprate) if sfft else partial(FFTSearch, fs=fsample, nSamples=nSample, sv=sv)
    search_start = timeit.default_timer()
    for idx, freq in enumerate(list(range(fcarrier - dopOffset, fcarrier + dopOffset + dopStep, dopStep))):
        dataIdx = 0
        for j in range(k):
            _data = data[dataIdx: dataIdx + nSample]
            dataIdx = dataIdx + nSample
            # print("debugfreq:", idx,k)
            _fft, _time = fftfunc(data=_data, fc=freq)
            t += _time
            c[idx] = c[idx] + _fft

            # c[idx] = c[idx] + SFFTSearch(_data, fsample, freq, nSample, sv)
    search_stop = timeit.default_timer()
    print("[FFT analysis]: total runtime of search %f" % (t))

    xxx = np.linspace(0, (2 * dopOffset // dopStep), (2 * dopOffset // dopStep) + 1)
    xy = np.linspace(size, 0, size)

    # print('max', c.max(), 'mean', c.mean(), 'ratio', c.max() / c.mean())
    if c.max() / c.mean() > 20:
        fig = plt.figure()
        ax = plt.axes(projection='3d')
        X, Y = np.meshgrid(xxx, xy)
        # print(np.shape(c), np.shape(X), np.shape(Y))
        print("[RESULT]#%d :FOUND" % (sv+1))
        print("LOC: freq:%d, phase:%d" % (np.argmax(c) // size * dopStep+fcarrier - dopOffset,
                                          subsamprate*size - np.argmax(c) % size))
        surf = ax.plot_surface(X, Y, c.transpose(), cmap=cm.coolwarm, antialiased=False, linewidth=0)
        fig.colorbar(surf, shrink=0.5, aspect=5)
        fig.suptitle("[RESULT]#%d :FOUND, k=%d" % (sv+1, k))
        # ax.zaxis.set_major_locator(LinearLocator(10))
        # ax.zaxis.set_major_formatter(FormatStrFormatter('%.02f'))
    else:
        print("[RESULT]#%d :NOT FOUND" % (sv+1))


if __name__ == "__main__":
    ffttime=0
    # ---- Search for all SV ----
    # for i in range(32):
    #     acquisition(sv=i, k=5)
    # ---- Test the effect of k ----
    # for k in [1,2,5,10,20,40]:
    #     acquisition(sv=21, k=k)
    acquisition(sv=21, k=10, sfft=True)
    plt.show()

    # ---- check the data properties, should be compared with the data source(fft, time, bar) -----
    # data = readRawData()
    # n = nSample
    # plt.figure()
    # dataarray = np.asarray(data[0:n])
    # yf = np.abs(np.fft.fft(dataarray))[0:n // 2]
    # plt.plot(np.linspace(0,0.5*16367600,n//2), 2*yf/n)
    # plt.bar([-3,-1,1,3],[data.count(-3),data.count(-1),data.count(1),data.count(3)])
