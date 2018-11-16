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
nco_width = 18
ideal_carrier_nco_code = sv_freq*1e6 * (2**nco_width) / fs
print(f"""Ideal NCO frequency for SV {sv} with frequency {sv_freq} is
{ideal_carrier_nco_code}""")
carrier_nco_code = round(ideal_carrier_nco_code)
print(f"Rounding the NCO frequency to {carrier_nco_code}")

#Set the integration period
integration_period = 5


def main():
    num_cycles = len(raw_data)//2
    print(f' Simulating for {num_cycles}')

    adc = ADC(np.sign(raw_data))
    nco_carrier = NCO(nco_width, False)

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

    # DC Gain of 2, bandwidth=10Hz, sample_rate=1kHz, discriminator=1
    dll = DLL(10, 20, 1000, 1)
    # Disabling the Costas loop for now and forcing the right frequency
    costas = Costas(1,[1,1,1],1,1,1)    

    nco_code = NCO(nco_width, True)
    packet = Packet()

    # Acquisition is not in the loop yet, we'll correctly set these for now
    dll_out = 1023e3*(2**nco_width)/fs
    costas_out = carrier_nco_code
   
    # Instrumentation: 
    dll_out_arr = []
    e_mags = []
    p_mags = []
    l_mags = []

    integration_count = 0

    for x in range(num_cycles):
        adc_data = adc.update()
        cos_out, sin_out  = nco_carrier.update(costas_out)        
        I = multI.update(adc_data, cos_out)
        Q = multQ.update(adc_data, sin_out)

        f_out, f2_out = nco_code.update(dll_out)
        e, p, l, done = ca.update(f_out, f2_out, sv)

        I_e = multIe.update(I, e)        
        I_p = multIp.update(I, p)
        I_l = multIl.update(I, l)
        Q_e = multQe.update(Q, e)
        Q_p = multQp.update(Q, p)
        Q_l = multQl.update(Q, l)

        I_sample = [I_e, I_p, I_l]
        Q_sample = [Q_e, Q_p, Q_l]

        if done:
            integration_count += 1

        # We'll do this calc here, since intdump throws things away when dump=1
        if integration_count == integration_period:
            e_mags.append(np.sqrt(I_int[0]**2 + Q_int[0]**2))
            p_mags.append(np.sqrt(I_int[1]**2 + Q_int[1]**2))
            l_mags.append(np.sqrt(I_int[2]**2 + Q_int[2]**2))
            print(f'Time: {x}, e: {e_mags[-1]}, p: {p_mags[-1]}, l: {l_mags[-1]}')
            dll_out, dis_out = dll.update(I_int, Q_int, 1023e3*(2**nco_width)/fs, 0)
            dll_out_arr.append(dll_out)
            integration_count = 0
        
        # Commenting this out for now
        # costas_out = costas.update(I_int[1], I_int[1], 0)
        
        # I_int and Q_int are lists of size 3
        I_int, reset = intdumpI.update(I_sample, 
            integration_count == integration_period)
        Q_int, reset = intdumpQ.update(Q_sample,
            integration_count == integration_period)
 
        # packet.update(x, I_int, Q_int)

    plt.figure()
    plt.plot(dll_out_arr)
    plt.figure()
    plt.plot(e_mags, label='early')
    plt.plot(p_mags, label='prompt')
    plt.plot(l_mags, label='late')
    plt.legend()
    
    plt.show()


if __name__ == "__main__": 
    main()

