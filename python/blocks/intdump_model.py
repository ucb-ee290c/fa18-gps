import numpy as np
import matplotlib.pyplot as plt

from .block import Block

#TODO: Finish Integrate and Dump class
class IntDump(Block):

    def __init__(self):
        """ Int Dump

        Parameters
        ----------
        integ : int
            The sum for correlation that is dumped after an integration period.
        """
        self.integ = np.array([0, 0, 0])

    def update(self, sample, dump):
        if dump:
            self.integ = np.array([0, 0, 0])
        self.integ += sample
        return self.integ

