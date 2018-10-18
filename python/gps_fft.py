"""
@Author: Zhaokai Liu
@File: gps_fft.py
@Time: 10/12/18 13:32
@Description: 

"""
import numpy as np
import matplotlib.pyplot as plt


def FFT():
    # Number of sample points
    N = 2**10
    cycN = 3
    # Signal Frequency and sample frequency
    fsig = 0.7e6
    fs = 100e6


    # Sample the input signal

    x = np.linspace(0.0, cycN*2*np.pi, N+1)
    x = x[0:N]
    y = np.sin(x)
    # Quantize sampled result
    # y_quantized = y//lsb*lsb
    y_quantized = y
    plt.plot(x,y,'o')
    # FFT analysis
    # yf = np.abs(np.fft.fft(y))
    # yf_quantized = np.abs(np.fft.fft(y_quantized))
    # yf_quantized[np.where(abs(yf_quantized)<1e-20)] = 1e-3
    #
    # yf = yf[0:N//2]
    # yf_dbfs = 20*np.log10(2*yf/N/1)
    # xf = np.linspace(0.0, 0.5, N//2)

    # plt.plot(xf, yf_dbfs)

    # plt.figure()
    # yf_quantized = yf_quantized[0:N//2]
    # yf_quantized_dbfs = 20*np.log10(2*yf_quantized/N/1)
    # plt.plot(xf, yf_quantized_dbfs)
    plt.grid()

    plt.figure()
    yf = np.abs(np.fft.fft(y))[0:N//2]
    xf = np.linspace(0.0, 0.5, N//2)
    # yf_quantized = np.abs(np.fft.fft(y_quantized))
    plt.plot(xf, yf)


    plt.figure()
    y_ifft = np.fft.ifft(np.fft.fft(y))
    print(y_ifft)
    plt.plot(x, y_ifft.real)
    plt.plot(x, y_ifft.imag)

    plt.show()

if __name__ == '__main__':
    FFT()