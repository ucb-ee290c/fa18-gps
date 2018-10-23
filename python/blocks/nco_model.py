import numpy as np
from .block import Block


class NCO(Block):

    def __init__(self, fs, amp, freq2x=False, quantized=False, num_bits=None):

        self._fs = fs
        self._time_step = 0
        self._quantized = quantized
        self._num_bits = num_bits
        self._freq2x = freq2x
        self._amp = amp

        self._sine = 0
        self._cosine = 0
        self._sine2x = 0
        self._cosine2x = 0

    def quantizer(self, vol, amp, num_bits):
        lsb = amp / 2 ** (num_bits - 1)  # = 2 * amp / 2**num_bits
        if isinstance(vol, int) or isinstance(vol, float):
            # shift all voltage by half LSB to solve the imbalance of // calculation
            return (vol + lsb / 2) // lsb * lsb
        elif isinstance(vol[0], int) or isinstance(vol[0], float):
            quant_data = []
            for v in vol:
                quant_data.append((v + lsb / 2) // lsb * lsb)
            return quant_data
        else:
            raise ValueError("Vol could only be float, int, list or numpy array of float and int.")

    def sincos(self, time_step, amp, freq, phase):

        sine = amp * np.sin(2 * np.pi * freq * time_step + phase)
        cosine = amp * np.cos(2 * np.pi * freq * time_step + phase)
        sine2x = amp * np.sin(4 * np.pi * freq * time_step + phase)
        sine2x_n = -1 * amp * np.sin(4 * np.pi * freq * time_step + phase)

        if self._quantized is False:
            self._sine = sine
            self._cosine = cosine
            self._sine2x = sine2x
            self._cosine2x = sine2x_n
        else:
            if self._num_bits is None:
                raise ValueError("In quantization mode, num_bits should be given.")
            self._sine = self.quantizer(sine, amp, self._num_bits)
            self._cosine = self.quantizer(cosine, amp, self._num_bits)
            self._sine2x = self.quantizer(sine2x, amp, self._num_bits)
            self._cosine2x = self.quantizer(sine2x_n, amp, self._num_bits)

    def update(self, freq, phase=0):

        self._time_step += 1 / self._fs
        self.sincos(self._time_step, self._amp, freq, phase)

        if not self._freq2x:
            return self._sine, self._cosine
        else:
            return self._sine, self._cosine, self._sine2x, self._cosine2x

