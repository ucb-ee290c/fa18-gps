import numpy as np
from ca_model import CA
ca = CA()
tick = [0, 0, 0, 0, 0, 0, 0, 0, 0]
tick2x = tick * 2
res_list = []
for i in range(len(tick2x)):
    res_list.append(ca.update(tick[i//2], tick2x[i], 1))
if not any(res_list):
    print("Success")
else:
    print("Fail, result was: ")
    print(res_list)
