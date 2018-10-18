import numpy as np
from ca_model import CA
ca = CA()
tick = [0, 0, 1, 1, 0, 0, 1, 1, 0, 0]
tick2x = [0, 1, 0, 1, 0, 1, 0, 1, 0, 1]
res_list = []
for i in range(len(tick2x)):
    res_list.append(ca.update(tick[i], tick2x[i], 1))
print("First 10 values of PRN list: ")
print(ca.curr_prn_list[:10])
print("result: ", res_list)
