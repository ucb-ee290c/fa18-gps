import numpy as np
import matplotlib.pyplot as plt
from mpl_toolkits.mplot3d import Axes3D
from acquisition_model_test import FFTSearch, SFFTSearch, readRawData, caGen
from blocks.acq_ctrl_model import *
from blocks.block import Block
from blocks.nco_model import NCO
from gps_fft import FFT, SFFT
import math
from functools import partial
from array import array
import struct



def test(k_max, sparse):

    fchip = 1023000
    fcarrier = 4130400
    fsample = 16367600
    dopOffset = 10000
    dopStep = 500
    nSample = 16368
    freq_idx_max = 2 * int(round(dopOffset / dopStep))
    p = 4


    fft_data_arr = []

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

        subsamprate = int(math.sqrt(math.log2(nSample)))

        caCode = caGen(21, fchip, fsample, nSample)
        t = np.linspace(0, (nSample - 1) * freq_curr / fsample, nSample)
        sin = np.sin(2 * np.pi * t)
        cos = np.cos(2 * np.pi * t)

        nco_model = NCO(count_width=3, code=False)
        cos = []
        sin = []
        nco_count_max = nco_model.count_max
        nco_step_size = freq_curr/fsample*nco_count_max
        for j in range(0, nSample):
            cos_e, sin_e = nco_model.update(step_size=nco_step_size)
            cos.append(cos_e)
            sin.append(sin_e)


        dataArray = np.asarray(_data)
        i = np.multiply(dataArray, cos)
        q = np.multiply(dataArray, sin)

        _data_iq = i + 1j * q


        if (sparse):
            FFT_result = np.repeat(SFFT(_data_iq, caCode, p=p), p)
        else:
            FFT_result = FFT(_data_iq, caCode)

        fft_data_arr.extend(FFT_result)

        acq_ctrl_model.update(FFT_result)

        dataIdx = dataIdx + nSample

    c = acq_ctrl_model.c
    freq_cph_opt = acq_ctrl_model.freq_cph_opt
    # idx_max = np.argmax(c)
    satellite_found = acq_ctrl_model.freq_cph_opt[0]['satellite_found']
    freq_idx_max = acq_ctrl_model.freq_cph_opt[0]['freq_idx_opt']
    cph_idx_max = acq_ctrl_model.freq_cph_opt[0]['cph_opt']
    print('acquisition result:', acq_ctrl_model.freq_cph_opt)
    print('optimal freq and code phase:', fcarrier - dopOffset + freq_idx_max * dopStep, nSample-cph_idx_max,
          'satellite_found =', satellite_found)
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

    print('size of data')
    print(len(fft_data_arr))
    print(max(fft_data_arr))
    print(fft_data_arr[0], fft_data_arr[1])
    # print(fft_data_arr[100])
    # output_file = open('python/data/acqctrl_test_vec.bin', 'wb')
    # float_array = array('d', fft_data_arr)
    # float_array.tofile(output_file)
    # output_file.close()
    # print(len(fft_data_arr[0]))
    # s = struct.pack('f' * len(fft_data_arr), *fft_data_arr)
    # f = open('python/data/acqctrl_test_vec.bin', 'wb')
    # f.write(s)
    # f.close()


if __name__ == "__main__":
    # test(k_max=10, sparse = False)
    # plt.show()
    with open("python/data/acqctrl_test_vec.bin", "rb") as binary_file:
        data = binary_file.read()

    ndata = 410 * 16368

    output_file = open('python/data/acqctrl_test_vec.bin', 'rb')
    firstco = struct.unpack('d'*ndata, output_file.read(8*ndata))
    print('length', len(firstco))
    output_file.close()

    print(len(data))
    print(data[0], data[1], data[2], data[3], data[4], data[5], data[6], data[7])
    print(data[8], data[9], data[10], data[11], data[12], data[13], data[14], data[15])


    print(data[-1], data[-2], data[-3], data[-4], data[-5], data[-6], data[-7], data[-8])
    print(data[-9], data[-10], data[-11], data[-12], data[-13], data[-14], data[-15], data[-16])

    # input_file = open('python/data/acqctrl_test_vec.bin', 'r')
    # float_array = array('d')
    # float_array.fromstring(input_file.read())
    # print(len(float_array))