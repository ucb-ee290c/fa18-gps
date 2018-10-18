"""
@Author: Zhaokai Liu
@File: raw_data_reader.py
@Time: 10/12/18 14:15
@Description: 

"""
import numpy as np
import matplotlib.pyplot as plt
import math
from mpl_toolkits.mplot3d import Axes3D


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


