import random

def parity_encode(d_star, d):
    assert len(d_star) == 2
    assert len(d) == 24
    d_xor = [i ^ d_star[1] for i in d]
    calc_parity = calculate_parity(d_star, d_xor)
    return d_xor + calc_parity


def parity_check(D_star, D):
    # Following the convention from Fundamentals of Global Positioning System Receivers: A Software Approach
    # D_star is the last two parity bits of the previous received word
    # D is the current received word
    assert len(D_star) == 2
    assert len(D) == 30
    return calculate_parity(D_star, D) == D[24:30]

def calculate_parity(d_star, d):
    calc_parity = [0] * 6
    calc_parity[0] = d_star[0] ^ d[0] ^ d[1] ^ d[2] ^ d[4] ^ d[5] ^ d[9] ^ d[10] ^ d[11] ^ d[12] ^ d[13] ^ d[16] ^ d[17] ^ d[19] ^ d[22]
    calc_parity[1] = d_star[1] ^ d[1] ^ d[2] ^ d[3] ^ d[5] ^ d[6] ^ d[10] ^ d[11] ^ d[12] ^ d[13] ^ d[14] ^ d[17] ^ d[18] ^ d[20] ^ d[23]
    calc_parity[2] = d_star[0] ^ d[0] ^ d[2] ^ d[3] ^ d[4] ^ d[6] ^ d[7] ^ d[11] ^ d[12] ^ d[13] ^ d[14] ^ d[15] ^ d[18] ^ d[19] ^ d[21]
    calc_parity[3] = d_star[1] ^ d[1] ^ d[3] ^ d[4] ^ d[5] ^ d[7] ^ d[8] ^ d[12] ^ d[13] ^ d[14] ^ d[15] ^ d[16] ^ d[19] ^ d[20] ^ d[22]
    calc_parity[4] = d_star[1] ^ d[0] ^ d[2] ^ d[4] ^ d[5] ^ d[6] ^ d[8] ^ d[9] ^ d[13] ^ d[14] ^ d[15] ^ d[16] ^ d[17] ^ d[20] ^ d[21] ^ d[23]
    calc_parity[5] = d_star[0] ^ d[2] ^ d[4] ^ d[5] ^ d[7] ^ d[8] ^ d[9] ^ d[10] ^ d[12] ^ d[14] ^ d[18] ^ d[21] ^ d[22] ^ d[23]
    return calc_parity

# dstar = [random.randint(0, 1) for i in range(2)]
# d = [random.randint(0, 1) for i in range(24)]
# a = parity_encode(dstar, d)
# print(parity_check(dstar, a))
