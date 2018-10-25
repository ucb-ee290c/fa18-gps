import numpy as np
import matplotlib.pyplot as plt

from .block import Block


class DLL(Block):
    """ The DLL is responsible for phase aligning signals"""

    def __init__(self, dc_gain, bandwidth, sample_rate, discriminator_num):
        """ DLL setup

        Parameters
        ----------
        dc_gain : float
            The dc_gain of the of the loop filter
        bandwidth : float
            The bandwidth (in Hz) of the loop filter in Hz, assuming the loop filter
            has a 1/(1+s/w) response
        sample_rate : float
            The sample rate (in Hz) of things coming into the filter
        discriminator_num : int : [1 2]
            The discriminator to use
        """
        self.tau = 1/(2*np.pi*bandwidth)
        self.T = 1/sample_rate
        self.a = 1 + 2*self.tau/self.T
        self.b = 1 - 2*self.tau/self.T 
        self.dc_gain = dc_gain
        self.prev_x = 0
        self.prev_y = 0
        self.discriminator_num = discriminator_num

    @staticmethod
    def discriminator1(ie, il, qe, ql):
        e = np.sqrt(ie**2 + qe**2)
        l = np.sqrt(il**2 + ql**2)
        return 1/2*(e-l)/(e+l)

    @staticmethod
    def discriminator2(ie, il, qe, ql):
        e = ie**2 + qe**2
        l = il**2 + ql**2
        return 1/2*(e-l)/(e + l)

    # TODO Figure out good saturation points for this
    def loop_filter(self, x):
        y = self.dc_gain/self.a * (x + self.prev_x) - self.b / self.a * self.prev_y
        if y > 1000:
            self.acc = 1000
        if y < -1000:
            y = -1000
        self.prev_x = x
        self.prev_y = y
        print(y)
        return y

    def update(self, I_sample, Q_sample, carrier_bias, code_bias):
        """ DLL update

        Parameters
        ----------
        I_sample : array
            The 3 integrated I samples, I_E, I_P and I_L in that order
        Q_sample : array
            The 3 integrated Q samples, Q_E, Q_P and Q_L in that order
        """
        if self.discriminator_num == 1:
            dis_out = self.discriminator1(I_sample[0], I_sample[2],
                Q_sample[0], Q_sample[2])
        elif self.discriminator_num == 2:
            dis_out = self.discriminator2(I_sample[0], I_sample[2],
                Q_sample[0], Q_sample[2])

        lf_out = self.loop_filter(dis_out)
        return carrier_bias + code_bias + lf_out, lf_out

