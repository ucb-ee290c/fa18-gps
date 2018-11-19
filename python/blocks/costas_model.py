import numpy as np
import math
import matplotlib.pyplot as plt

from .block import Block

#TODO: Finish Costas Loop class
class Costas(Block):
    
    def __init__(self, lf_coeff, costas_mode, freq_mode, freq_bias):
        """
        Costas block

        Parameters
        ----------
        int_time: int 
        lf_coeff: list
        pd_mode: int
        costas_mode: int
        freq_mode: int

        Returns
        -------
        Costas object
        """
        self._avg_mag = 0
        self._count = 0

        self._Ips_d = 0
        self._Qps_d = 0

        self._lf_coeff = lf_coeff

        self._costas_mode = costas_mode
        self._freq_mode = freq_mode

        self.costas_err = 0
        self.freq_err = 0

        self.freq_bias = freq_bias
        self.d_lf_out = 0
        self.lf_out = freq_bias

        self._lf = 0
        self._lf_sum = 0
        self._lf_sum_sum = 0
        self.alpha = 0
        self.alpha_prev = 0
        self.beta = 0
        self.beta_prev = 0

        self.freq_update = False

    def costas_detector(self, Ips, Qps, mode):
        """
        Costas discriminator.

        Parameters
        ----------
        Ips: Union[float, int]
            I data, at present time.
        Qps: Union[float, int]
            Q data, at present time.
        mode: Union[float, int]
            Mode 1: Qps * Ips
            Mode 2: Qps * sign(Ips)
            Mode 3: Qps / Ips
            Mode 4: atan(Qps / Ips)

        Returns
        -------
        phase_err:
            Phase error information.
        """

        # different modes
        if mode == 0:       # magic function, need to verify and think more...
            if Ips >= 0:
                return math.atan2(Qps, Ips)
            else:
                return math.atan2(-Qps, -Ips)
        elif mode == 1:
            return math.atan2(Qps, Ips)
        elif mode == 2:
            return Qps * np.sign(Ips)
        elif mode == 3:
            return Qps / Ips
        elif mode == 4:
            return Qps * Ips
        else:
            raise ValueError("For Costas discriminator, mode is only supported for 1, 2, 3 and 4.")

    def frequency_detector(self, Ips, Qps, mode):
        """
        Frequency discriminator.

        Parameters
        ----------
        Ips: Union[float, int]
            I data, at present time.
        Qps: Union[float, int]
            Q data, at present time.
        Ips_d: Union[float, int]
            I data, at present time, delayed.
        Qps_d: Union[float, int]
            Q data, at present time, delayed.
        mode: Union[float, int]
            Mode 1: cross/(t2-t1)
            Mode 2: cross*sign(dot)/(t2-t1)
            Mode 3: atan2(dot, cross)/(t2-t1)

        Returns
        -------
        freq_err:
            Frequency error information.
        """

        # cross and dot
        cross = Ips * self._Qps_d - self._Ips_d * Qps
        dot = Ips * self._Ips_d + Qps * self._Qps_d

        # different modes
        if mode == 1:
            return cross
        elif mode == 2:
            return cross * np.sign(dot)
        elif mode == 3:
            print(f"Fll error {math.degrees(math.atan2(cross, dot))}")
            return math.atan2(cross, dot)/(0.002)
        else:
            raise ValueError("For frequency discriminator, mode is only supported for 1, 2, 3 and 4.")

    def loop_filter(self, phase_err, freq_err, lf_coeff):
        """
        Loop filter for PLL/FLL. Use second order for now.

        Parameters
        ----------
        phase_err: Union[float, Int]
            phase error.
        freq_err: Union[float, Int]
            frequency error.
        lf_coeff: Tuple[Union[float, Int]]
            loop filter coefficient.

        Returns
        -------
        lf_out:
            loop filter output.
        """
        #self._lf_sum_sum += lf_coeff[2] * phase_err + lf_coeff[4] * freq_err
        #self._lf_sum += lf_coeff[1] * phase_err + lf_coeff[3] * freq_err + self._lf_sum_sum
        #self._lf = lf_coeff[0] * phase_err + self._lf_sum
        bnp = 17
        bnf = 3
        T = 0.001
        w0f = bnf/0.53
        w0p = bnp/0.7845
        a2 = 1.414
        a3 = 1.1
        b3 = 2.4

        self.beta = (w0f**2)*T*freq_err + (w0p**3)*T*phase_err + self.beta_prev
        alpha2 = T*(a2*w0f*freq_err + a3*(w0p**2)*phase_err+0.5*(self.beta_prev + self.beta))
        self.alpha = alpha2 + self.alpha_prev
        self._lf = b3 * w0p * phase_err + 0.5*(self.alpha + self.alpha_prev)
      
        self.beta_prev = self.beta
        self.alpha_prev = self.alpha

        return self._lf

    def update(self, Ips, Qps, freq_bias):
        """
        Parameters
        ----------
       Ips: Union[float, int]
            I data, at present time.
        Qps: Union[float, int]
            Q data, at present time.
        Ips_d: Union[float, int]
            I data, at present time, delayed.
        Qps_d: Union[float, int]
            Q data, at present time, delayed.
        freq_os: Union[float, Int]
            frequency offset from acquisition.

        Returns
        -------
        lf_out:
            loop filter output.
        """

        # get phase error
        self.costas_err = self.costas_detector(Ips, Qps, mode=self._costas_mode)

        # get frequency error
        if self.freq_update: 
            self.freq_err = self.frequency_detector(Ips, Qps, mode=self._freq_mode)
            self.freq_update = False
        else:
            self.freq_update = True

        # get loop filter output
        self.d_lf_out = self.loop_filter(-self.costas_err,self.freq_err, self._lf_coeff)
        delta_freq = self.d_lf_out / (2*np.pi)
        code = delta_freq / (16*1023*1e3) * (2**30 - 1)
        self.lf_out = code + self.freq_bias

        self._Ips_d = Ips
        self._Qps_d = Qps

        return self.lf_out

