import numpy as np
import matplotlib.pyplot as plt

from blocks import *


raw_data = np.fromfile('adc_sample_data.bin', dtype=np.int8)
# The data contains the following SV's with at the following frequencies:
# The frequencies are in MHz
sv_list = [22, 3, 19, 14, 18, 11, 32, 6]
sv_freqs = [4.128460, 4.127190, 4.129280,
            4.133130, 4.127310, 4.133280,
            4.134060,4.127220]

sv = sv_list[0]
sv_freq = sv_freqs[0]

def main():
    num_cycles = 100

    adc = ADC(raw_data)
    nco_carrier = NCO(10, False)

    # Technically don't need to make multiple multiplier objects as they all
    # behave the same. But in the code we are creating multiple object
    # instances to know how many hardware multipliers we will need. 
    mult1 = MUL()
    mult2 = MUL()

    #FIXME: CA needs correct args, if any
    ca = CA()

    mult3 = MUL()
    mult4 = MUL()
    mult5 = MUL()
    mult6 = MUL()
    mult7 = MUL()
    mult8 = MUL()
    
    #FIXME: Integrate and Dump may need more args
    intdump = IntDump()

    dll = DLL(1, 1, 1)
    costas = Costas()    

    nco_code = NCO(10, True)
    packet = Packet()
    
    # FIXME: Initial DLL and Costas loop values
    dll_out = 1
    costas_out = 1

    for x in range(0, num_cycles):
        adc_data = adc.update() 
        cos_out, sin_out  = nco_carrier.update(costas_out)        
        I = mult1.update(adc_data, cos_out) 
        Q = mult2.update(adc_data, sin_out)

        f_out, f2_out = nco_code.update(dll_out)
        e, p, l = ca.update(f_out, sv)

        I_e = mult3.update(I, e)        
        I_p = mult4.update(I, p)
        I_l = mult5.update(I, l)
        Q_e = mult6.update(Q, e)
        Q_p = mult7.update(Q, p)
        Q_l = mult8.update(Q, l)

        I_sample = [I_e, I_p, I_l]
        Q_sample = [Q_e, Q_p, Q_l]

        # I_int and Q_int are lists of size 3
        I_int = intdump.update(I_sample, False)
        Q_int = intdump.update(Q_sample, False)

        dll_out = dll.update(I_int, Q_int, 0, 0)
        costas_out = costas.update(I_int[1], I_int[1])

        packet.update(x, I_int, Q_int)


if __name__ == "__main__": 
    main()

