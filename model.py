from abc import ABC, abstractmethod
import numpy as np
import matplotlib.pyplot as plt

raw_data = np.fromfile('adc_sample_data.bin', dtype=np.int8)

# Abstract class that each module should extend. Defines an update method where
# it takes in all of the block inputs each "cycle" and should return the output
# values of that block  
class Block(ABC):

    @abstractmethod
    def update(self):
        raise NotImplementedError


class NCO(Block):

    def __init__(self, count_width, code):
        self.count_max = 2**count_width - 1
        self.count = 0
        self.code = code;

    def update(self, step_size):
        self.count += step_size
        if self.count > self.count_max:
            self.count -= self.count_max
        if (self.code):
            return np.sin(2*np.pi*self.count / self.count_max), \
                   np.sin(2*np.pi*2*self.count / self.count_max)  
        else: 
            return np.cos(2*np.pi*self.count / self.count_max), \
                   np.sin(2*np.pi*self.count / self.count_max)

#TODO: Finish DLL class
class DLL(Block):
    
    def __init__(self, kp, ki):
        self.kp = kp
        self.ki = ki

    def update(self, I_sample, Q_sample):
        return 1


class ADC(Block):
    
    def __init__(self, data):
        self.data = data
        self.index = -1

    def update(self):
        self.index += 1
        if self.index < len(self.data):
            return self.data[self.index]

#FIXME: Figure out sizing for multiplier class
class Mult(Block):

    def update(self, in1, in2):
        return in1*in2

#TODO: Finish Code Gen class
class CA(Block):
    
    def __init__(self):
        self.prev_tick = 0
        self.curr_out = 0

    SV = {
       1: [2,6],
       2: [3,7],
       3: [4,8],
       4: [5,9],
       5: [1,9],
       6: [2,10],
       7: [1,8],
       8: [2,9],
       9: [3,10],
      10: [2,3],
      11: [3,4],
      12: [5,6],
      13: [6,7],
      14: [7,8],
      15: [8,9],
      16: [9,10],
      17: [1,4],
      18: [2,5],
      19: [3,6],
      20: [4,7],
      21: [5,8],
      22: [6,9],
      23: [1,3],
      24: [4,6],
      25: [5,7],
      26: [6,8],
      27: [7,9],
      28: [8,10],
      29: [1,6],
      30: [2,7],
      31: [3,8],
      32: [4,9],
    }

    def update(self, tick, sv_num):
        if self.prev_tick == 0 and tick == 1:
            self.curr_out = self.shift()
        self.prev_tick = tick
        return self.curr_out

    def shift(register, feedback, output):
        """GPS Shift Register
        
        :param list feedback: which positions to use as feedback (1 indexed)
        :param list output: which positions are output (1 indexed)
        :returns output of shift register:
        
        """
        
        # calculate output
        out = [register[i-1] for i in output]
        if len(out) > 1:
            out = sum(out) % 2
        else:
            out = out[0]
            
        # modulo 2 add feedback
        fb = sum([register[i-1] for i in feedback]) % 2
        
        # shift to the right
        for i in reversed(range(len(register[1:]))):
            register[i+1] = register[i]
            
        # put feedback in position 1
        register[0] = fb
        
        return out


    def PRN(sv):
        """Build the CA code (PRN) for a given satellite ID
        
        :param int sv: satellite code (1-32)
        :returns list: ca code for chosen satellite
        
        """
        
        # init registers
        G1 = [1 for i in range(10)]
        G2 = [1 for i in range(10)]

        ca = [] # stuff output in here
        
        # create sequence
        for i in range(1023):
            g1 = shift(G1, [3,10], [10])
            g2 = shift(G2, [2,3,6,8,9,10], SV[sv]) # <- sat chosen here from table
            
            # modulo 2 add and append to the code
            ca.append((g1 + g2) % 2)

        # return C/A code!
        return ca

#TODO: Finish Costas Loop class
class Costas(Block):
    
    def update(self, I_pint, Q_pint):
        return 1

#TODO: Finish Integrate and Dump class
class IntDump(Block):
    def update(self, sample):
        return [1, 1, 1]

#TODO: Finish Packetizer class
class Packet(Block):
    def update(self, cycle, I_int, Q_int):
        print(str(cycle) + ": Packetized data should be printed here")
        return 0

def main():
    num_cycles = 100

    adc = ADC(raw_data)
    nco_carrier = NCO(10, False)

    # Technically don't need to make multiple multiplier objects as they all
    # behave the same.  But in the code we are creating multiple object
    # instances to know how many hardware multipliers we will need. 
    mult1 = Mult()
    mult2 = Mult()

    #FIXME: CA needs correct args, if any
    ca = CA()

    mult3 = Mult()
    mult4 = Mult()
    mult5 = Mult()
    mult6 = Mult()
    mult7 = Mult()
    mult8 = Mult()
    
    #FIXME: Integrate and Dump may need more args
    intdump = IntDump()

    dll = DLL(1,1)
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
        e, p, l = ca.update(f_out, f2_out)

        I_e = mult3.update(I, e)        
        I_p = mult4.update(I, p)
        I_l = mult5.update(I, l)
        Q_e = mult6.update(Q, e)
        Q_p = mult7.update(Q, p)
        Q_l = mult8.update(Q, l)

        I_sample = [I_e, I_p, I_l]
        Q_sample = [Q_e, Q_p, Q_l]

        # I_int and Q_int are lists of size 3
        I_int = intdump.update(I_sample)
        Q_int = intdump.update(Q_sample)

        dll_out = dll.update(I_int, Q_int)
        costas_out = costas.update(I_int[1], I_int[1])

        packet.update(x, I_int, Q_int)


if __name__ == "__main__": 
    main()

