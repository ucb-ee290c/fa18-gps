from blocks.block import Block
from blocks.nco_model import NCO
from blocks.ca_model import CA
from blocks.tick import Tick
from blocks.intdump_model import IntDump
from blocks.mul_model import MUL
from blocks.dll_model import DLL
from blocks.costas_model import Costas
import random
import math
import numpy as np
import pdb

import matplotlib.pyplot as plt

class Tracking(Block):

    def __init__(self, fs, sv_num, f_code=1.023e6, sine_amp=1):

        # sampling frequency
        self.fs = fs
        # IF frequency
        self.fc = None
        self.phi_c = None
        # code bias, phase
        self.code_bias = None
        self.code_phase = None
        # ca code chip frequency
        self.f_code = f_code
        # integration time
        self.int_time = None
        # ca code length
        self.code_length = 1023

        # IF clock
        self.fc_sin = 0
        self.fc_cos = 0

        # IF mixer output
        self.data_i = 0
        self.data_q = 0

        # CA code xor output
        self.xor_i_early = 0
        self.xor_i_punct = 0
        self.xor_i_late = 0
        self.xor_q_early = 0
        self.xor_q_punct = 0
        self.xor_q_late = 0

        # code clock
        self.code_sin = 0
        self.code_cos = 0
        self.code_sin2x = 0
        self.code_sin2x_n = 0

        # loop/post process clock
        self.loop_sin = 0
        self.loop_cos = 0

        # CA code
        self.ca_i_early = 0
        self.ca_i_punct = 0
        self.ca_i_late = 0
        self.ca_q_early = 0
        self.ca_q_punct = 0
        self.ca_q_late = 0

        # xor output data
        self.data_i_early = 0
        self.data_i_punct = 0
        self.data_i_late = 0
        self.data_q_early = 0
        self.data_q_punct = 0
        self.data_q_late = 0

        # previous xor output data
        self.data_i_punct_prev = 0
        self.data_q_punct_prev = 0

        # freq NCO
        self.fc_nco = NCO(fs=fs, amp=sine_amp, freq2x=False)

        # code NCO
        self.code_nco = NCO(fs=fs, amp=sine_amp, freq2x=True)

        # post NCO
        self.loop_nco = NCO(fs=fs, amp=sine_amp, freq2x=False)

        # CA model
        self.ca_i = CA(sv_num)
        self.ca_q = CA(sv_num)

        # code clock tick
        self.tick_sin = Tick()
        self.tick2x_sin = Tick()
        self.tick_cos = Tick()
        self.tick2x_sin_n = Tick()

        # loop/cost clock tick
        self.tick_sin_loop = Tick()
        self.tick_cos_loop = Tick()

        # multiplier
        # mixers
        self.mul_i = MUL()
        self.mul_q = MUL()
        # CA xor
        self.mul_i_early = MUL()
        self.mul_i_punct = MUL()
        self.mul_i_late = MUL()
        self.mul_q_early = MUL()
        self.mul_q_punct = MUL()
        self.mul_q_late = MUL()

        # int_dump
        self.intdump_i_early = IntDump()
        self.intdump_i_punct = IntDump()
        self.intdump_i_late = IntDump()
        self.intdump_q_early = IntDump()
        self.intdump_q_punct = IntDump()
        self.intdump_q_late = IntDump()

        # DLL loop and filter
        self.dll = DLL(kp=0, ki=0, discriminator_num=1)

        # Costas loop and filter
        # self.code_costas = Costas()

    def update(self, data, fc, phi_c, int_time, code_bias=940, code_phase=math.pi/2):

        # update parameters
        self.fc = fc
        self.phi_c = phi_c
        self.code_bias = code_bias
        self.code_phase = code_phase
        self.int_time = int_time
        self.int_num = self.int_time * self.fs

        # get clock for freq_nco
        self.fc_sin, self.fc_cos = self.fc_nco.update(freq=self.fc, phase=self.phi_c)
        # get clock for code_nco
        self.code_sin, self.code_cos, self.code_sin2x, self.code_sin2x_n = \
            self.code_nco.update(freq=self.f_code, phase=self.code_phase)    # phase=math.pi/2
        # get clock for loop nco
        self.loop_sin, self.loop_cos = self.loop_nco.update(freq=1/self.int_time)

        # mixer
        self.data_i = self.mul_i.update(data, self.fc_cos)
        self.data_q = self.mul_q.update(data, self.fc_sin)

        # I path early, punct, late code generation
        if self.tick2x_sin.check_tick(self.code_sin2x_n):
            self.ca_i_early, self.ca_i_punct, self.ca_i_late = self.ca_i.update2x()
        if self.tick_cos.check_tick(self.code_cos):
            self.ca_i_early, self.ca_i_punct, self.ca_i_late = self.ca_i.update(offset=self.code_bias-1)   # offset=939

        # Q path, early, punct, late code generation
        if self.tick2x_sin_n.check_tick(self.code_sin2x):
            self.ca_q_early, self.ca_q_punct, self.ca_q_late = self.ca_q.update2x()
        if self.tick_sin.check_tick(self.code_sin):
            self.ca_q_early, self.ca_q_punct, self.ca_q_late = self.ca_q.update(offset=self.code_bias-1)

        # xors
        self.xor_i_early = self.mul_i_early.update(self.data_i, self.ca_i_early)
        self.xor_i_punct = self.mul_i_early.update(self.data_i, self.ca_i_punct)
        self.xor_i_late = self.mul_i_early.update(self.data_i, self.ca_i_late)

        self.xor_q_early = self.mul_i_early.update(self.data_q, self.ca_q_early)
        self.xor_q_punct = self.mul_i_early.update(self.data_q, self.ca_q_punct)
        self.xor_q_late = self.mul_i_early.update(self.data_q, self.ca_q_late)

        # intdump
        data_i_early = self.intdump_i_early.update(self.xor_i_early, self.int_num)
        data_i_punct = self.intdump_i_punct.update(self.xor_i_punct, self.int_num)
        data_i_late = self.intdump_i_late.update(self.xor_i_late, self.int_num)

        data_q_early = self.intdump_q_early.update(self.xor_q_early, self.int_num)
        data_q_punct = self.intdump_q_punct.update(self.xor_q_punct, self.int_num)
        data_q_late = self.intdump_q_late.update(self.xor_q_late, self.int_num)

        # re-synchronize
        if self.tick_cos_loop.check_tick(self.loop_cos):
            self.data_i_punct_prev = self.data_i_punct
            self.data_q_punct_prev = self.data_q_punct

            self.data_i_punct = data_i_punct
            self.data_i_early = data_i_early
            self.data_i_late = data_i_late
            self.data_q_punct = data_q_punct
            self.data_q_early = data_q_early
            self.data_q_late = data_q_late


def read_raw_data():
    rawData = []
    with open("data/gioveAandB_short.bin", "rb") as binary_file:
        data = binary_file.read()
        for i in data:
            if i > 254:
                t = -1
            elif i > 200:
                t = -3
            elif i > 2:
                t = 3
            else:
                t = 1
            rawData.append(t)
    return rawData


def pd_char():

    # get parameters
    sample_num = 1600000
    fs = 16367600
    fc = 4128469
    sv_num = 22
    int_time = 2e-3

    # read data
    data_list = read_raw_data()

    # sweep phase
    pd_avg_list = []
    dphi_list = []
    for dphi in np.arange(-math.pi/2, math.pi/2+0.001, math.pi / 8):

        # get track instance
        track = Tracking(fs=fs, sv_num=sv_num)

        # sweep time
        time_list = []
        pd_list = []
        zero = []
        for i in range(sample_num):
            time_list.append(i / fs)
            zero.append(0)

            # update track
            track.update(data=data_list[i], fc=fc, phi_c=dphi, int_time=int_time)   # optimal phase = 0

            # phase detector
            pd_list.append(track.data_q_punct)

        pd_avg = sum(pd_list) / len(pd_list)
        # get lists
        dphi_list.append(dphi)
        pd_avg_list.append(pd_avg)
        print([dphi, pd_avg])

    plt.plot(dphi_list, pd_avg_list)
    plt.show(block=False)


def fd_char():
    # get parameters
    sample_num = 1600000
    fs = 16367600
    fc = 4128469
    sv_num = 22
    int_time = 2e-3

    # read data
    data_list = read_raw_data()

    # sweep frequency
    fd_avg_list = []
    df_list = []
    for df in np.arange(-300, 300+1, 20):

        # get track instance
        track = Tracking(fs=fs, sv_num=sv_num)

        # sweep time
        time_list = []
        fd_list = []
        zero = []
        for i in range(sample_num):
            time_list.append(i/fs)
            zero.append(0)

            # update track
            track.update(data=data_list[i], fc=fc+df, phi_c=0, int_time=int_time)

            # frequency detector
            fd_list.append(track.data_i_punct * track.data_q_punct_prev -
                         track.data_q_punct * track.data_i_punct_prev)

        fd_avg = sum(fd_list)/len(fd_list)
        # get lists
        df_list.append(df)
        fd_avg_list.append(fd_avg)
        print([df, fd_avg])

    plt.plot(df_list, fd_avg_list)
    plt.show(block=False)


def dll_char():

    # get parameters
    sample_num = 1600000
    fs = 16367600
    fc = 4128469
    sv_num = 22
    int_time = 2e-3

    # read data
    data_list = read_raw_data()

    # sweep frequency
    diff_avg_list = []
    dphi_list = []
    for dphi in np.arange(-math.pi/2, math.pi/2, math.pi/8):

        # get track instance
        track = Tracking(fs=fs, sv_num=sv_num)

        # time
        time_list = []
        # ca code
        data1 = []
        data2 = []
        data3 = []
        zero = []
        for i in range(sample_num):
            time_list.append(i/fs)
            zero.append(0)

            # update track
            track.update(data=data_list[i], fc=fc, phi_c=0, int_time=int_time, code_phase=math.pi/2+dphi)
            # optimal phase=math.pi/2

            # DLL detector
            data1.append(track.data_i_early**2 + track.data_q_early**2)
            data2.append(track.data_i_punct**2 + track.data_q_punct**2)
            data3.append(track.data_i_late**2 + track.data_q_late**2)

        diff_avg = (sum(data1) - sum(data3)) / len(data1)
        # get lists
        dphi_list.append(dphi)
        diff_avg_list.append(diff_avg)
        print([dphi, diff_avg])

    plt.plot(dphi_list, diff_avg_list)
    plt.show(block=False)


if __name__ == '__main__':

    pd_char()
    fd_char()
    dll_char()


