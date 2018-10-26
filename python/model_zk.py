import numpy as np
import matplotlib.pyplot as plt
import matplotlib as mpl

mpl.rcParams['agg.path.chunksize'] = 10000

from blocks import *


raw_data = np.fromfile('adc_sample_data.bin', dtype=np.int8)

# Data sample rate
fs = 1652860   # 1.023*16*1e6

# The data contains the following SV's with at the following frequencies:
# The frequencies are in MHz
# sv_list = [22, 3, 19, 14, 18, 11, 32, 6]
# sv_freqs = [4.128460, 4.127190, 4.129280,
#             4.133130, 4.127310, 4.133280,
#             4.134060,4.127220]
sv_list = [1]
sv_freqs = [4.132100]

sv = sv_list[0]
sv_freq = sv_freqs[0]

# We'll start off with the NCO Width set to 10
# #### - By Zhongkai, personally don't think this is the right way, we are wasting the resolution.
carrier_nco_width = 30
ideal_carrier_nco_code = sv_freq*1e6 * (2**carrier_nco_width) / fs
print(f"""Ideal NCO frequency for SV {sv} with frequency {sv_freq} is
{ideal_carrier_nco_code}""")
carrier_nco_code = round(ideal_carrier_nco_code)
print(f"Rounding the NCO frequency to {carrier_nco_code}")

code_nco_width = 30
ideal_code_nco_code = 1.023e6 * (2**code_nco_width) / fs
code_nco_code = round(ideal_code_nco_code)


def main():

    # # of cycles to run
    num_cycles = 16000     # len(raw_data)

    # read raw data
    adc = ADC(raw_data)

    # carrier(IF) NCO
    nco_carrier = NCO(count_width=carrier_nco_width, code=False)

    # Technically don't need to make multiple multiplier objects as they all
    # behave the same. But in the code we are creating multiple object
    # instances to know how many hardware multipliers we will need. 
    multI = MUL()
    multQ = MUL()

    multIe = MUL()
    multIp = MUL()
    multIl = MUL()
    multQe = MUL()
    multQp = MUL()
    multQl = MUL()

    # ca code generators
    ca = CA()

    # integrate and dump
    intdumpI = IntDump()
    intdumpQ = IntDump()

    # ki = 1, kp = 1, first discriminator
    dll = DLL(1, 1, 1)

    # Costas loop for now and forcing the right frequency
    costas = Costas(1,[1,1,1],1,1,1)    

    # NCO for code
    nco_code = NCO(count_width=code_nco_width, code=True)

    # packetizer
    packet = Packet()
    
    # Initial DLL and Costas value
    dll_out = code_nco_code
    costas_out = carrier_nco_code

    # integ I/Q
    last_integ_I = [0, 0, 0]
    last_integ_Q = [0, 0, 0]

    # list to plot
    time_list = []
    I_int_list = []
    Q_int_list = []
    data_list = []
    for x in range(0, num_cycles):

        # ADC update, read data
        adc_data = adc.update()

        # carrier(IF) NCO update
        cos_out, sin_out = nco_carrier.update(costas_out)

        # mixer update
        I = multI.update(adc_data, cos_out) 
        Q = multQ.update(adc_data, sin_out)

        # code NCO update
        f_out, f2_out = nco_code.update(dll_out)
        # CA code update
        e, p, l, dump = ca.update(f_out, f2_out, sv)

        # code/data XOR update
        I_e = multIe.update(I, e)        
        I_p = multIp.update(I, p)
        I_l = multIl.update(I, l)
        Q_e = multQe.update(Q, e)
        Q_p = multQp.update(Q, p)
        Q_l = multQl.update(Q, l)

        I_sample = [I_e, I_p, I_l]
        Q_sample = [Q_e, Q_p, Q_l]
        # print(Q_sample, e, p, l)

        # I_int and Q_int are lists of size 3
        I_int = intdumpI.update(I_sample, dump)
        Q_int = intdumpQ.update(Q_sample, dump)

        if dump:
            # dll loop
            # print(last_integ_I, last_integ_Q)
            # dll_out = round(dll.update(last_integ_I, last_integ_Q,
            #     carrier_nco_code, 0))
            # costas loop
            # costas_out = costas.update(I_int[1], Q_int[1], 0)
            pass

        # last data
        last_integ_I = I_int
        last_integ_q = Q_int

        # packet update
        # packet.update(x, I_int, Q_int)

        # add to list
        time_list.append(x/fs)
        I_int_list.append(e)
        Q_int_list.append(p)
        data_list.append(l)

    # plot data
    plt.figure()
    plt.plot(time_list, I_int_list)
    plt.hold
    plt.plot(time_list, Q_int_list)
    plt.hold
    plt.plot(time_list, data_list)
    plt.hold
    plt.show(block=False)

    plt.figure()
    plt.plot(time_list, data_list)
    plt.hold()
    plt.show(block=False)


if __name__ == "__main__": 
    main()

