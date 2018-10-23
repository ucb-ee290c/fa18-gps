from blocks.nco_model import NCO
import matplotlib.pyplot as plt

if __name__ == '__main__':
    fc = 10e6
    nco = NCO(fc=fc, amp=1, freq2x=True, quantized=False, num_bits=3)

    sin_data = []
    cos_data = []
    sin2x_data = []
    cos2x_data = []
    xin = []
    for i in range(1000):
        xin.append(i/fc)
        sin, cos, sin2x, cos2x, = nco.update(freq=10e3, phase=0)
        sin_data.append(sin)
        cos_data.append(cos)
        sin2x_data.append(sin2x)
        cos2x_data.append(cos2x)

    plt.figure()
    plt.plot(xin, sin_data)
    plt.hold
    plt.plot(xin, cos_data)
    plt.hold
    plt.plot(xin, sin2x_data)
    plt.hold
    plt.plot(xin, cos2x_data)
    plt.show(block=False)