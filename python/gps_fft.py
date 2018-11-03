"""
@Author: Zhaokai Liu
@File: gps_fft.py
@Time: 10/12/18 13:32
@Description: 

"""
import numpy as np
import matplotlib.pyplot as plt
import math

# // data = i+j*q

def FFT(data, caCode):
    inFFT = np.fft.fft(data)
    caFFT = np.fft.fft(caCode)
    C = np.square(np.abs(np.fft.ifft(np.multiply(np.conjugate(inFFT), caFFT))))

    return C

# // check acquisition model test for the definition of p
def SFFT(data, caCode, p=4):

    sublen = math.ceil(len(data)/p)
    subsig = [0]*sublen
    subca = [0]*sublen
    for i in range(sublen):
        subsig[i] = sum(data[i::sublen])
        subca[i] = sum(caCode[i::sublen])

    inFFT = np.fft.fft(subsig)
    caFFT = np.fft.fft(subca)
    C = np.square(np.abs(np.fft.ifft(np.multiply(np.conjugate(inFFT), caFFT))))
    return C