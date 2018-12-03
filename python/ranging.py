import numpy as np
from numpy.linalg import inv
from math import sqrt, atan2

C = 3.0e8

#ECEF coordinates of BWRC
#Latitude: 37.869254
#Longitude: -122.267526
#Height: 57 m
x = -2691.466e3
y = -4262.826e3
z = 3894.033e3 #if doesn't work, plug in a known location like north pole

#nominal operating altitude of GPS satellite is 20,200e3 m

x_n = 0.0
y_n = 0.0
z_n = 0.0
t_bias_n = 0.0

nom = [x_n, y_n, z_n, t_bias_n]

#x, y, z in ECEF, delta_t
sv_1 = [0.0, (-20200.0e3 - 3894.033e3), 0.0, 0.06796112069175156]
sv_2 = [(20200.0e3 + 3894.033e3), 0.0, 0.0, 0.09133565296265844]
sv_3 = [(-20200.0e3 - 3894.033e3), 0.0, 0.0, 0.07389219269395515]
sv_4 = [0.0, 0.0, (20200.0e3 + 3894.033e3), 0.06939866110903742]

sv = [sv_1, sv_2, sv_3, sv_4]

def calc_pseudorange(loc, sat):
	return sqrt((loc[0]-sat[0])**2 + (loc[1]-sat[1])**2 + (loc[2]-sat[2])**2) + t_bias_n*C

def calc_time_delay(loc, sat):
	return sqrt((loc[0]-sat[0])**2 + (loc[1]-sat[1])**2 + (loc[2]-sat[2])**2) / C

# print(calc_time_delay([x, y, z], sv_1))
# print(calc_time_delay([x, y, z], sv_2))
# print(calc_time_delay([x, y, z], sv_3))
# print(calc_time_delay([x, y, z], sv_4))

def convert_ecef_to_latlong(x, y, z):
	altitude = sqrt(x*x + y*y + z*z)
	latitude = z / sqrt(x*x + y*y)
	longitude = atan2(y, x)

err_mag = 1000
deltas = [0.0, 0.0, 0.0, 0.0]

#find location
for i in range(10):
	nom[0] += deltas[0]
	nom[1] += deltas[1]
	nom[2] += deltas[2]
	nom[3] += deltas[3]

	pr_n_1 = calc_pseudorange(nom, sv_1)
	pr_n_2 = calc_pseudorange(nom, sv_2)
	pr_n_3 = calc_pseudorange(nom, sv_3)
	pr_n_4 = calc_pseudorange(nom, sv_4)

	pr_n = [pr_n_1, pr_n_2, pr_n_3, pr_n_4]
	delta_pr = [C*sv_1[3] - pr_n_1, C*sv_2[3] - pr_n_2, C*sv_3[3] - pr_n_3, C*sv_4[3] - pr_n_4]

	alpha = np.zeros((4,4))

	for i in range(4):
		for j in range(4):
			if j == 3:
				alpha[i,j] = C
				continue
			else:
				alpha[i,j] = (nom[j] - sv[i][j]) / (pr_n[i] - t_bias_n*C)

	alpha_inv = inv(alpha)
	deltas = np.dot(alpha_inv, np.array(delta_pr))

	err_mag = sqrt(deltas[0]*deltas[0] + deltas[1]*deltas[1] + deltas[2]*deltas[2])

	print(str(err_mag))

print('Final location:')
print(str(nom[0]))
print(str(nom[1]))
print(str(nom[2]))
print(str(nom[3]))
