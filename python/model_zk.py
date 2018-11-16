import numpy as np
import matplotlib.pyplot as plt
import matplotlib as mpl

mpl.rcParams['agg.path.chunksize'] = 10000

from blocks import *


raw_data = np.fromfile('adc_sample_data.bin', dtype=np.int8)

# Data sample rate
fs = 16528600   # 1.023*16*1e6

# The data contains the following SV's with at the following frequencies:
# The frequencies are in MHz
sv_list = [1]
sv_freqs = [4.132100]

sv = sv_list[0]
sv_freq = sv_freqs[0] + 10e-6

# We'll start off with the NCO Width set to 10
carrier_nco_width = 30
carrier_count_max = 2**carrier_nco_width - 1
carrier_nco_freq = round(sv_freq*1e6 / fs * carrier_count_max)


code_nco_width = 30
code_count_max = 2**code_nco_width - 1
code_nco_freq = round(1.023e6 / fs * code_count_max)

# integrate time
int_time = 2e-3
int_num = round(int_time * fs)

# carrier(IF) NCO initial phase
# math.pi/2 is because of data is generated with sin() function
carrier_nco_init_phase = -math.pi/2 + math.pi/4


def main():

    # # of cycles to run
    num_cycles = 3600000    # len(raw_data)

    # read raw data
    adc = ADC(raw_data)

    # carrier(IF) NCO
    nco_carrier = NCO(count_width=carrier_nco_width, code=False,
                      init_phase=carrier_nco_init_phase)

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
    ca = CA(sv_num=sv, code_bias=0)

    # integrate and dump
    intdumpI = IntDump()
    intdumpQ = IntDump()

    # ki = 1, kp = 1, first discriminator
    dll = DLL(12000, 10, 1000, 1)

    # Costas loop for now and forcing the right frequency
    costas = Costas(lf_coeff=[1000, 5, 1e-6], costas_mode=0, freq_mode=1,
                    freq_bias=carrier_nco_freq)

    # NCO for code
    nco_code = NCO(count_width=code_nco_width, code=True,
                   init_phase=math.pi/8)

    # packetizer
    packet = Packet()
    
    # Initial DLL and Costas value
    dll_out = code_nco_freq
    costas_out = carrier_nco_freq

    # list to plot
    time_list = []
    I_int_list = []
    Q_int_list = []
    costas_err_list = []
    freq_err_list = []
    dll_err_list = []
    data_list = []
    freq_list = []
    d_freq_list = []
    for x in range(0, num_cycles):

        # ADC update, read data
        adc_data = adc.update()

        # carrier(IF) NCO update
        cos_out, sin_out = nco_carrier.update(freq_ctrl=costas_out, phase_ctrl=0)
        # cos_out, sin_out = nco_carrier.update(freq_ctrl=carrier_nco_freq, phase_ctrl=0)

        # mixer update
        I = multI.update(adc_data, cos_out) 
        Q = multQ.update(adc_data, sin_out)

        # code NCO update
        ck_code, ck2x_code = nco_code.update(freq_ctrl=dll_out, phase_ctrl=0)

        # CA code update
        e, p, l, clk_dump = ca.update(ck_code, ck2x_code, sv, 0)

        # code/data XOR update
        I_e = multIe.update(I, e)        
        I_p = multIp.update(I, p)
        I_l = multIl.update(I, l)
        Q_e = multQe.update(Q, e)
        Q_p = multQp.update(Q, p)
        Q_l = multQl.update(Q, l)

        I_sample = [I_e, I_p, I_l]
        Q_sample = [Q_e, Q_p, Q_l]

        # I_int and Q_int are lists of size 3
        I_int, _ = intdumpI.update(I_sample, int_num)
        Q_int, _ = intdumpQ.update(Q_sample, int_num)

        if clk_dump:
            # dll loop
            dll_out, dis_out = dll.update(I_int, Q_int, code_nco_freq, 0)

            # costas loop
            costas_out = costas.update(I_int[1], Q_int[1], freq_bias=carrier_nco_freq)
            pass

        # packet update
        # packet.update(x, I_int, Q_int)

        # add to list
        data_list.append(dll_out / code_count_max * fs)

        time_list.append(x/fs)
        I_int_list.append(I_int[1])
        Q_int_list.append(Q_int[1])

        # costas error and frequency error
        costas_err_list.append(costas.costas_err)
        freq_err_list.append(costas.freq_err)
        # dll error
        dll_err_list.append(dll.dis_out)

        # d_freq and freq
        d_freq_list.append(costas.d_lf_out / carrier_count_max * fs)
        freq_list.append(costas.lf_out / carrier_count_max * fs)

        # get completion amount
        if x/num_cycles*10 % 1 == 0:
            print("Percentage: {0:.0f}% ".format(x/num_cycles*100) +
                  ">>"*int(x/num_cycles*10))
    print("Percentage: 100% " + ">>" * 10)

    # plot data
    plt.close("all")

    plt.figure()
    plt.subplot(3, 1, 1)
    plt.plot(time_list, I_int_list)
    plt.plot(time_list, Q_int_list)
    plt.legend(["I_int", "Q_int"])
    plt.subplot(3, 1, 2)
    plt.plot(time_list, costas_err_list)
    plt.legend(["Costas error"])
    plt.subplot(3, 1, 3)
    plt.plot(time_list, freq_list)
    plt.legend(["Frequency"])
    plt.show(block=False)

    plt.figure()
    plt.plot(time_list, dll_err_list)
    plt.legend(["DLL error"])
    plt.show(block=False)

    plt.figure()
    plt.subplot(2, 1, 1)
    plt.plot(time_list, costas_err_list)
    plt.legend(["Costa_err"])
    plt.subplot(2, 1, 2)
    plt.plot(time_list, freq_err_list)
    plt.legend(["Freq_err"])
    plt.show(block=False)

    plt.figure()
    plt.plot(time_list, freq_list)
    plt.legend(["Frequency"])
    plt.show(block=False)

    plt.figure()
    plt.plot(time_list, data_list)
    plt.legend(["data"])
    plt.show(block=False)

    plt.figure()
    plt.plot(time_list, d_freq_list)
    plt.legend(["Delta Frequency"])
    plt.show(block=False)

    print("\n")
    print("Costas error")
    print(sum(costas_err_list)/len(costas_err_list))
    print("Frequency error")
    print(sum(freq_err_list)/len(freq_err_list))


if __name__ == "__main__":

    main()

