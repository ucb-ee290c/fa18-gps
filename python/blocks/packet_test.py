from packet_model import *
import random

p = Packet()
data_in = []
for j in range(SUBFRAME_LENGTH):
    if (j == 0):
        input_seq = [1, 0, 0, 0, 1, 0, 1, 1] + [random.randint(0, 1) for i in range(WORD_LENGTH - len(PREAMBLE))]
    else:
        input_seq = [random.randint(0, 1) for i in range(WORD_LENGTH)]
    for i in range(len(input_seq)):
        p.update(i + (j * WORD_LENGTH), input_seq[i], 0)
        data_in.append(input_seq[i])
p.update(WORD_LENGTH * SUBFRAME_LENGTH, 0, 0)

flatten = lambda l: [item for sublist in l for item in sublist]
out_flat = flatten(p.out)
if (flatten(p.out) == data_in):
    print("Test successful.")
else:
    out_flat = flatten(p.out)
