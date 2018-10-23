import numpy as np
import matplotlib.pyplot as plt

from .block import Block

import numpy as np
import matplotlib.pyplot as plt

from .block import Block

#TODO: Finish Integrate and Dump class
class IntDump(Block):

    def __init__(self):

        self._count = 0
        self._sum = 0
        self._prev_sum = 0

    def update(self, data, num):

        if self._count < num:
            self._sum += data
            self._count += 1
        else:
            self._prev_sum = self._sum
            self._sum = 0
            self._count = 0

        return self._prev_sum


