from .block import Block

class Quantizer(Block):

    def __init__(self, amp, num_bits):

        self.amp = amp
        self.num_bits = num_bits
        self.lsb = 2*amp / (2**num_bits - 1)

    def update(self, vol):

        #print(vol)

        # shift all voltage by half LSB to solve the imbalance of // calculation
        if -self.amp <= vol+self.lsb/2 <= self.amp:
            quant = (vol+self.lsb/2) // self.lsb

        elif vol+self.lsb/2 > self.amp:
            quant =  self.amp // self.lsb

        else:
            quant =  -self.amp // self.lsb

        return quant
