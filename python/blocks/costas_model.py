import numpy as np
import math
import matplotlib.pyplot as plt

from .block import Block


class Costas(Block):
    """
    Costas loop.
    """
    def __init__(self, int_time, lf_coeff, pd_mode, cd_mode, fd_mode):
        """
        Costas loop.

        Parameters
        ----------
        int_time: Union[float, Int]
            Integragte and dump time Interval
        lf_coeff: List(Union[float, Int])
            loop filter coefficients.
        pd_mode: Int
            Phase discriminator mode.
                Mode 1: atan2(Qps, Ips)
                Mode 2: Qps * avg(Ips^2+Qps^2)
        cd_mode: Int
            Costas discriminator mode.
                Mode 1: Qps * Ips
                Mode 2: Qps * sign(Ips)
                Mode 3: Qps / Ips
                Mode 4: atan(Qps / Ips)
        fd_mode: Int
            frequency discriminator mode.
                Mode 1: Qps * Ips
                Mode 2: Qps * sign(Ips)
                Mode 3: Qps / Ips
                Mode 4: atan(Qps / Ips)
        """
        self._avg_mag = 0
        self._count = 0
        self._phase_err_sum = 0

        self._Ips_d = 0
        self._Qps_d = 0

        self._time_step = int_time
        self._lf_coeff = lf_coeff

        self._pd_mode = pd_mode
        self._cd_mode = cd_mode
        self._fd_mode = fd_mode

        self._costas_err = None
        self._freq_err = None
    
    def phase_detector(self, Ips, Qps, mode):
        """
        Phase discriminator.

        Parameters
        ----------
        Ips: Union[float, Int]
            I data, at present time.
        Qps: Union[float, Int]
            Q data, at present time.
        mode: Int
            Mode 1: atan2(Qps, Ips)
            Mode 2: Qps * avg(Ips^2+Qps^2)

        Returns
        -------
        phase_err:
            Phase error information.
        """
        # different modes
        if mode == 1:
            return math.atan2(Qps, Ips)
        elif mode == 2:
            self._count += 1
            self._avg_mag = (self._avg_mag * (self._count - 1) +
                             math.sqrt(Ips ** 2 + Qps ** 2)) / self._count
            return Qps
        else:
            raise ValueError("For phase discriminator, mode is only supported for 1, 2, 3 and 4.")

    def costas_detector(self, Ips, Qps, mode):
        """
        Costas discriminator.

        Parameters
        ----------
        Ips: Union[float, Int]
            I data, at present time.
        Qps: Union[float, Int]
            Q data, at present time.
        mode: Int
            Mode 1: Qps * Ips
            Mode 2: Qps * sign(Ips)
            Mode 3: Qps / Ips
            Mode 4: atan(Qps / Ips)

        Returns
        -------
        phase_err:
            Phase error information.
        """

        # define sign function
        sign = lambda a: (a >= 0) - (a < 0)

        # different modes
        if mode == 1:
            return Ips * Qps
        elif mode == 2:
            return Ips * sign(Qps)
        elif mode == 3:
            return Qps / Ips
        elif mode == 4:
            return math.atan(Qps / Ips)
        else:
            raise ValueError("For Costas discriminator, mode is only supported for 1, 2, 3 and 4.")

    def frequency_detector(self, Ips, Qps, mode):
        """
        Frequency discriminator.

        Parameters
        ----------
        Ips: Union[float, Int]
            I data, at present time.
        Qps: Union[float, Int]
            Q data, at present time.
        Ips_d: Union[float, Int]
            I data, at present time, delayed.
        Qps_d: Union[float, Int]
            Q data, at present time, delayed.
        mode: Union[float, Int]
            Mode 1: cross/(t2-t1)
            Mode 2: cross*sign(dot)/(t2-t1)
            Mode 3: atan2(dot, cross)/(t2-t1)

        Returns
        -------
        freq_err:
            Frequency error information.
        """

        # define sign function
        sign = lambda a: (a >= 0) - (a < 0)

        # cross and dot
        cross = Ips * self._Qps_d - self._Ips_d * Qps
        dot = Ips * self._Ips_d - Qps * self._Qps_d

        # different modes
        if mode == 1:
            return cross / self._time_step
        elif mode == 2:
            return cross * sign(dot) / self._time_step
        elif mode == 3:
            return math.atan2(dot, cross) / self._time_step
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

        self._phase_err_sum += phase_err
        return lf_coeff[0] * phase_err + lf_coeff[1] * self._phase_err_sum + lf_coeff[2] * freq_err

    def update(self, Ips, Qps, freq_bias):
        """
        Parameters
        ----------
       Ips: Union[float, Int]
            I data, at present time.
        Qps: Union[float, Int]
            Q data, at present time.
        Ips_d: Union[float, Int]
            I data, at present time, delayed.
        Qps_d: Union[float, Int]
            Q data, at present time, delayed.
        freq_os: Union[float, Int]
            frequency offset from acquisition.

        Returns
        -------
        lf_out:
            loop filter output.
        """

        # get phase error
        self._costas_err = self.costas_detector(Ips, Qps, mode=self._cd_mode)

        # get frequency error
        self._freq_err = self.frequency_detector(Ips, Qps, mode=self._fd_mode)

        # get loop filter output
        lf_out = self.loop_filter(self._costas_err, self._freq_err, self._lf_coeff) + freq_bias

        self._Ips_d = Ips
        self._Qps_d = Qps

        return lf_out
        
    
