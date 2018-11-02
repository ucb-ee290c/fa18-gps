import numpy as np
import matplotlib.pyplot as plt
import matplotlib as mpl
mpl.rcParams['agg.path.chunksize'] = 10000

from blocks import *

# TODO: add LOCK signals to two loops
# TODO: add enable signal

class Track(Block):

    def __init__(self,
                 raw_data,
                 # IF nco
                 if_nco_width,
                 if_nco_init_phase,
                 if_nco_freq,
                 # code nco
                 code_nco_width,
                 code_nco_init_phase,
                 code_nco_freq,
                 # ca
                 sv_num,
                 code_bias,
                 # dll loop
                 dll_dc_gain,
                 dll_bandwidth,
                 dll_sample_rate,
                 dll_discriminator_num,
                 # int dump
                 int_num,
                 # costas loop
                 costas_lf_coeff,
                 costas_pll_mode=0,
                 costas_fll_mode=1,
                 ):

        # Initial DLL and Costas value
        self.costas_out = if_nco_freq
        self.dll_out = code_nco_freq

        #####################################
        # instances
        #####################################

        # read raw data
        self.adc = ADC(raw_data)

        # carrier(IF) NCO
        self.nco_carrier = NCO(count_width=if_nco_width, code=False,
                               init_phase=if_nco_init_phase)

        # NCO for code
        self.nco_code = NCO(count_width=code_nco_width, code=True,
                            init_phase=code_nco_init_phase)
        # multipliers
        # Technically don't need to make multiple multiplier objects as they all
        # behave the same. But in the code we are creating multiple object
        # instances to know how many hardware multipliers we will need.
        self.multI = MUL()
        self.multQ = MUL()

        self.multIe = MUL()
        self.multIp = MUL()
        self.multIl = MUL()
        self.multQe = MUL()
        self.multQp = MUL()
        self.multQl = MUL()

        # ca code generators
        self.ca = CA(sv_num=sv_num)

        # integrate and dump
        self.intdumpI = IntDump()
        self.intdumpQ = IntDump()

        # dll loop
        # ki = 1, kp = 1, first discriminator
        self.dll = DLL(dc_gain=dll_dc_gain, bandwidth=dll_bandwidth,
                       sample_rate=dll_sample_rate,
                       discriminator_num=dll_discriminator_num)

        # Costas loop
        self.costas = Costas(lf_coeff=costas_lf_coeff, costas_mode=costas_pll_mode,
                             freq_mode=costas_fll_mode, freq_bias=if_nco_freq)

        # packetizer
        self.packet = Packet()

        #####################################
        # initial values
        #####################################

        self.I_int = [0, 0, 0]
        self.Q_int = [0, 0, 0]
        # delayed version
        self.I_int_d = [0, 0, 0]
        self.Q_int_d = [0, 0, 0]

        self.if_nco_freq = if_nco_freq
        self.code_nco_freq = code_nco_freq

        self.code_bias = code_bias
        self.sv_num = sv_num
        self.int_num = int_num

        self.dll_out = code_nco_freq
        self.dll_lf_out = 0
        self.costas_out = if_nco_freq

    def update(self, if_nco_freq, code_nco_freq, sv_num, code_bias, int_num, en):

        # update some parameters
        self.sv_num = sv_num
        self.code_bias = code_bias
        self.if_nco_freq = if_nco_freq
        self.code_nco_freq = code_nco_freq
        self.int_num = int_num

        # ADC update, read data
        adc_data = self.adc.update()

        # time keeper
        ca_en = self.time_keeper.update(reset=timekeeper_reset, code_bias=code_bias)

        # carrier(IF) NCO update
        cos_if, sin_if = self.nco_carrier.update(freq_ctrl=self.costas_out, phase_ctrl=0)

        # mixer update
        I = self.multI.update(adc_data, cos_if)
        Q = self.multQ.update(adc_data, sin_if)

        # code NCO update
        ck_code, ck2x_code = self.nco_code.update(freq_ctrl=self.dll_out, phase_ctrl=0)

        # CA code update
        e, p, l, clk_ca = self.ca.update(tick=ck_code, tick_2x=ck2x_code,
                                         sv_num=sv_num)

        # code/data XOR update
        I_e = self.multIe.update(I, e)
        I_p = self.multIp.update(I, p)
        I_l = self.multIl.update(I, l)
        Q_e = self.multQe.update(Q, e)
        Q_p = self.multQp.update(Q, p)
        Q_l = self.multQl.update(Q, l)

        I_sample = [I_e, I_p, I_l]
        Q_sample = [Q_e, Q_p, Q_l]

        # I_int and Q_int are lists of size 3
        I_int, _ = self.intdumpI.update(I_sample, int_num)
        Q_int, _ = self.intdumpQ.update(Q_sample, int_num)

        self.I_int = I_int
        self.Q_int = Q_int

        if clk_ca:
            # dll loop
            dll_out, dll_lf_out = self.dll.update(I_sample=self.I_int, Q_sample=self.Q_int,
                                                  freq_bias=code_nco_freq,
                                                  carrier_assist=0)
            # costas loop
            costas_out = self.costas.update(Ips=self.I_int[1], Qps=self.Q_int[1], freq_bias=if_nco_freq)

            self.dll_out = dll_out
            self.dll_lf_out = dll_lf_out
            self.costas_out = costas_out

            self.I_int_d, self.Q_int_d = I_int, Q_int



        # packet update
        # self.packet.update(x, I_int, Q_int)




