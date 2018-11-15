import numpy as np
import matplotlib.pyplot as plt

from .block import Block


class IntDump(Block):

    def __init__(self):
        """ Int Dump """
        self.integ = np.array([0, 0, 0], dtype=np.float64)

    def update(self, sample, dump):
        """
        Parameters 
        ----------
        sample : List[Int]
            The current sample to be added
        dump: bool
            Whether after this input the integrator resets

        Returns
        -------
        List[Int]
            The current integrated input
        """
        self.integ += sample
        rv = self.integ
        if dump:
            self.integ = np.array([0, 0, 0], dtype=np.float64)

        return rv

