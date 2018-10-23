from blocks.ca_model import CA
from blocks.tick import Tick


if __name__ == '__main__':

    # set ca code sequence
    ca = CA(22)

    # clocks
    clk =   [-1, -1, 1, 1, -1, -1, 1, 1, -1, -1, 1, 1, -1, -1, 1, 1, -1, -1, 1, 1, -1, -1, 1, 1, -1, -1, 1, 1, -1, -1, 1, 1]
    clk2x = [1, -1, 1, -1, 1, -1, 1, -1, 1, -1, 1, -1, 1, -1, 1, -1, 1, -1, 1, -1, 1, -1, 1, -1, 1, -1, 1, -1, 1, -1, 1, -1]

    tick = Tick()     # define sine clock domain
    tick2x = Tick()   # define sine2x clock domain

    res_list = []
    early = punct = late = 0
    for i in range(len(clk2x)):

        if tick2x.check_tick(clk2x[i]):
            early, punct, late = ca.update2x()

        if tick.check_tick(clk[i]):
            early, punct, late = ca.update()

        res_list.append((early, punct, late))

    # print results
    print("First 10 values of PRN list: ")
    print(ca.prn_list[:10])
    print("Result: ", res_list)