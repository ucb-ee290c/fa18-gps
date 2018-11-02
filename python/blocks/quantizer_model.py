from .block import Block

class Quantizer(block):

    def __init__(self, amp, num_bits):

        self.amp = amp
        self.num_bits = num_bits
        self.lsb = amp / 2 ** (num_bits - 1)  # = 2 * amp / 2**num_bits

    def update(self, vol):

        if isinstance(vol, int) or isinstance(vol, float):
            # shift all voltage by half LSB to solve the imbalance of // calculation
            return (vol + self.lsb / 2) // self.lsb * self.lsb
        elif isinstance(vol[0], int) or isinstance(vol[0], float):
            quant_data = []
            for v in vol:
                quant_data.append((v + lsb / 2) // lsb * lsb)
            return quant_data
        else:
            raise ValueError("Vol could only be float, int, list or numpy array of float and int.")