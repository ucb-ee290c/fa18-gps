import numpy as np
import matplotlib.pyplot as plt

from .block import Block


class DLL(Block):
    """ The DLL is responsible for phase aligning signals
    """
    def __init__(self, kp, ki, discriminator_num):
        """ DLL setup

        Parameters
        ----------
        ki : float
            The integral scale factor
        kp : float
            The proportional scale factor
        discriminator_num : int : [1 2]
            The discriminator to use
        """
        self.kp = kp
        self.ki = ki
        self.discriminator_num = discriminator_num

    @staticmethod
    def discriminator1(ie, il, qe, ql):
        e = np.sqrt(ie**2 + qe**2)
        l = np.sqrt(il**2 + ql**2)
        return 1/2(e-l)/(e+l)

    @staticmethod
    def discriminator2(ie, il, qe, ql):
        e = ie**2 + qe**2
        l = il**2 + ql**2
        return 1/2(e-l)

    def update(self, I_sample, Q_sample, carrier_aid, code_bias):
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
        else self.discriminator_num == 2:
            dis_out = self.discriminator2(I_sample[0], I_sample[2],
                Q_sample[0], Q_sample[2])

        return carrier_aid + code_aid + dis_out

