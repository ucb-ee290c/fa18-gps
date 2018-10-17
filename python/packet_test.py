from blocks.packet_model import *
import random
import packet_encode

p = Packet()
data_in = []
input_seq = packet_encode.generate_subframe()
for j in range(SUBFRAME_LENGTH):
    for i in range(WORD_LENGTH):
        p.update(i + (j * WORD_LENGTH), input_seq[j][i], 0)
        data_in.append(input_seq[j][i])
p.update(WORD_LENGTH * SUBFRAME_LENGTH, 0, 0)

flatten = lambda l: [item for sublist in l for item in sublist]
out_flat = flatten(p.parser.out)
if (flatten(p.parser.out) == data_in):
    print("Test successful.")
else:
    print("Test failed.")
