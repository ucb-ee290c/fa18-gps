from blocks import *
import numpy as np
import matplotlib.pyplot as plt


if __name__ == '__main__':

    t = np.arange(0, 1.1e-3, 1e-6)
    sin = np.sin(2*np.pi*1000*t)

    quant = Quantizer(1.1, 4)

    sin_q = []
    for i in range(len(t)):
        sin_q.append(quant.update(sin[i]))

    plt.figure()
    plt.plot(t, sin)
    plt.show(block=False)

    plt.figure()
    plt.plot(t, sin_q)
    plt.show(block=False)