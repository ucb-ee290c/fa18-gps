from abc import ABC


class Tick(ABC):
    """
    Check tick of clock sequence
    """

    def __init__(self):
        self.prev_clk = 0

    def check_tick(self, clk):
        """
        Check clock tick.
        Parameters
        ----------
        clk: Union[float, Int]
            input clock value.
        Returns
        -------
        tick: Int
            1 when tick
        """
        if self.prev_clk < 0 and clk >= 0:
            tick = 1
        else:
            tick = 0

        self.prev_clk = clk
        return tick