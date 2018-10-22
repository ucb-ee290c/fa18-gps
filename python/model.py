import numpy as np
import matplotlib.pyplot as plt

from blocks import *


raw_data = np.fromfile('adc_sample_data.bin', dtype=np.int8)[15040:]

# Data sample rate
fs = 1.023*16*1e6

# The data contains the following SV's with at the following frequencies:
# The frequencies are in MHz
sv_list = [22, 3, 19, 14, 18, 11, 32, 6]
sv_freqs = [4.128460, 4.127190, 4.129280,
            4.133130, 4.127310, 4.133280,
            4.134060,4.127220]

sv = sv_list[0]
sv_freq = sv_freqs[0]

# We'll start off with the NCO Width set to 10
nco_width = 10
ideal_carrier_nco_code = sv_freq*1e6 * (2**nco_width) / fs
print(f"""Ideal NCO frequency for SV {sv} with frequency {sv_freq} is
{ideal_carrier_nco_code}""")
carrier_nco_code = round(ideal_carrier_nco_code)
print(f"Rounding the NCO frequency to {carrier_nco_code}")

def main():
    num_cycles = len(raw_data)//10
    print(num_cycles)

    adc = ADC(np.sign(raw_data))
    nco_carrier = NCO(10, False)

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
    
    ca = CA()
    
    intdumpI = IntDump()
    intdumpQ = IntDump()

    # ki = 1, kp = 1, second discriminator
    dll = DLL(1, 1, 1)
    # Disabling the Costas loop for now and forcing the right frequency
    costas = Costas(1,[1,1,1],1,1,1)    

    nco_code = NCO(10, True)
    packet = Packet()
    
    # FIXME: Initial DLL and Costas loop values
    dll_out = 1023e3*(2**nco_width)/fs
    costas_out = carrier_nco_code
    last_integ_I = [0,0,0]
    last_integ_Q = [0,0,0]
    dll_out_arr = []
    processed = False
    dll_I_e = []
    dll_I_p = []
    dll_I_l = []
    dis_out_arr = []

    for x in range(0, num_cycles):
        adc_data = adc.update()
        cos_out, sin_out  = nco_carrier.update(costas_out)        
        I = multI.update(adc_data, cos_out)
        Q = multQ.update(adc_data, sin_out)

        f_out, f2_out = nco_code.update(dll_out)
        e, p, l, dump = ca.update(f_out, f2_out, sv)
        # print(e, p, l, f_out, f2_out)

        I_e = multIe.update(I, e)        
        I_p = multIp.update(I, p)
        I_l = multIl.update(I, l)
        Q_e = multQe.update(Q, e)
        Q_p = multQp.update(Q, p)
        Q_l = multQl.update(Q, l)

        I_sample = [I_e, I_p, I_l]
        Q_sample = [Q_e, Q_p, Q_l]

        # We'll do this calc here, since intdump throws things away when dump=1
        if dump and not processed:
            print(x)
            dll_out, dis_out = dll.update(I_int, Q_int, 1023e3*(2**nco_width)/fs, 0)
            print(f"Early: {I_int[0]}, Late: {I_int[2]}, Out: {dll_out}")
            dll_I_e.append(I_int[0])
            dll_I_p.append(I_int[1])
            dll_I_l.append(I_int[2])
            dll_out = np.round(dll_out)
            dll_out_arr.append(dll_out)
            processed = True
        elif not dump: 
            processed = False
        
        # Commenting this out for now
        # costas_out = costas.update(I_int[1], I_int[1], 0)
        
        # I_int and Q_int are lists of size 3
        I_int, reset = intdumpI.update(I_sample, dump)
        Q_int, reset = intdumpQ.update(Q_sample, dump)

        # packet.update(x, I_int, Q_int)

    plt.figure()
    plt.plot(dll_out_arr)
    plt.figure()
    plt.plot(dll_I_e, label='early')
    plt.plot(dll_I_p, label='prompt')
    plt.plot(dll_I_l, label='late')
    plt.legend()
    
    
    plt.show()


if __name__ == "__main__": 
    main()

