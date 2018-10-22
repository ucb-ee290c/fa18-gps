import numpy as np
import matplotlib.pyplot as plt

from .block import Block

#TODO: Finish Integrate and Dump class
class IntDump(Block):

    def __init__(self, max_count=5):
        """ Int Dump

        Parameters
        ----------
        integ : int
            The sum for correlation that is dumped after an integration period.
        """
        self.integ = np.array([0, 0, 0], dtype=np.float64)
        self.count = 0
        self.max_count = max_count

    def update(self, sample, dump):
        reset = False
        if dump:
            self.count += 1
        if self.count == self.max_count:
            self.count = 0
            self.integ = np.array([0, 0, 0], dtype=np.float64)
            reset = True
        self.integ += sample
        return self.integ, reset

