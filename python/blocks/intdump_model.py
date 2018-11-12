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
        self.integ = np.array([0, 0, 0], dtype=np.float64)
        self.count = 0
        # use integ_prev to keep IntDump output not change
        self.integ_prev = np.array([0, 0, 0], dtype=np.float64)

    def update(self, sample, max_count):

        # update max count
        self.max_count = max_count

        # update counter
        self.count += 1
        reset = False

        if self.count >= self.max_count:
            self.integ_prev = self.integ
            self.count = 0
            self.integ = np.array([0, 0, 0], dtype=np.float64)
            reset = True

        self.integ += sample
        return self.integ_prev, reset

