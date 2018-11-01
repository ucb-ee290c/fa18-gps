# ADC output generator for tape-in 0

import sys
sys.path.append('../testvector')
import math
import numpy as np
import matplotlib.pyplot as plt
import csv
from data_gen.Utils import *

import os
data_base = os.getcwd() + '/data_gen'

print(" ---------- Generating ADC output data ------------")
print(" -- This python code can take a few mins to run ---")
print(" ------ Use existing csv files if possible --------")
print("---------------------------------------------------")

# Preamble definition
preamble = [1, 0, 0, 0, 1, 0, 1, 1, 0, 0]

# Parameters to modify

T = 2    # Data duration in time (seconds)
SV = [0, 5, 10, 15, 20, 25]
dopplerVaryRate = 20.0   # The artificially added doppler shift variation (Hz/s)

adcBitWidth = 3
dopplerRange = 5000.0      # Doppler shift range in Hz, in +/-
codePhaseRange = 1023    # Code phase shift range in unit of one PRN chip
#IF = 2.6e6                # IF in Hz
#adcSamplingFreq = 9.5e6  # ADC Sampling frequency in Hz
IF = 4132100               # IF in Hz (1.23M + 10K) *4 + 100
adcSamplingFreq = 16528600  # ADC Sampling frequency in Hz (IF * 4) + 200

# Constants (not supposed to be changed)
prnFreq = 1.023e6  # 1.023 MHz of PRN code
basebandFreq = prnFreq/1023/20  # should get 50 (Hz)
totalDataLength = np.floor(T*adcSamplingFreq).astype(int)  # total data length of at the ADC Output

# Variables calculated from parameters and constants
basebandLength = np.floor(T*basebandFreq).astype(int)
dopplerStepPerSample = dopplerVaryRate/adcSamplingFreq

# Baseband data for all available satellites
allBasebandData = np.zeros((len(SV), basebandLength)).astype(int)
for i in range(0, len(SV)):
    allBasebandData[i] = np.append(preamble, np.random.randint(2, size=(basebandLength-10)), axis=0)

# C/A PRN code for all available satellites
allPRN = np.zeros((len(SV), 1023)).astype(int)
for i in range (0, len(SV)):
    allPRN[i] = PRN(SV[i])

# Random Doppler shift for all available satellite
allInitialDoppler = np.random.randint(2*dopplerRange, size=len(SV)) * 1.0
allDopplerVaryDirection = np.random.randint(2, size=len(SV))*2 - 1

# Generate all doppler shift values for all samples
allDoppler = np.zeros((len(SV), totalDataLength))
for i in range (0, len(SV)):
    allDoppler[i] = (np.linspace(allInitialDoppler[i], allInitialDoppler[i]+allDopplerVaryDirection[i]*T\
            *dopplerVaryRate, totalDataLength)) - dopplerRange

# Function to limit the doppler shift in the predefined range
def limitBoundary(a):
    a = np.abs(a + dopplerRange) - dopplerRange
    a = (-1) * (np.abs((-1)*a + dopplerRange) - dopplerRange)
    return a

# Reconstruct the doppler shift values
allDoppler = limitBoundary(allDoppler)

# Random Code phase shift for all available satellite
allCodePhase = (np.random.randint(codePhaseRange*np.around(adcSamplingFreq/prnFreq), size=len(SV))).astype(int)

print("Initial Doppler shift is " + str(allInitialDoppler-dopplerRange))
print("Doppler Vary direction is " + str(allDopplerVaryDirection))
print("Random Code Phase shift is " + str(allCodePhase))

# Data modulation with PRN code (at PRN chip rate)
dataModulated = np.zeros((len(SV), basebandLength*20*1023)).astype(int)
for i in range(0, len(SV)):
    dataModulated[i] = NRZ((np.array(CACodeModulation(list(allBasebandData[i]), list(allPRN[i])))).flatten())

print("Modulated data in PRN chip rate is with shape : " + str(dataModulated.shape))

# Extend to ADC sampling rate
dataModulatedAtSamplingRate = np.zeros((len(SV), totalDataLength)).astype(int)
for i in range(0, len(SV)):
    print("Generating modulated data at sampling rate for satellite No. " + str(SV[i]))
    for j in range(0, totalDataLength):
        dataModulatedAtSamplingRate[i][j] = dataModulated[i][np.floor((j/adcSamplingFreq)/(1/prnFreq)).astype(int)]

print('Modulated data shape at ADC sampling rate is : ' + str(dataModulatedAtSamplingRate.shape))

# Generate IF carrier by applying doppler shift
allSampledCarrier = np.zeros((len(SV), totalDataLength))
t = np.linspace(0, T, totalDataLength)
deltaT = T/totalDataLength

for i in range(0, len(SV)): 
    #allSampledCarrier[i] = np.cos(2*math.pi*(IF+allDoppler[i])*t)
    phi = 0.0
    for j in range(0, totalDataLength):
        allSampledCarrier[i][j] = np.cos(phi)
        phi = (phi + 2*math.pi*(IF+allDoppler[i][j])*deltaT) % (math.pi*2)

# Apply BPSK modulation
print("Applying BPSK modulation ...")
allBPSKdata = dataModulatedAtSamplingRate * allSampledCarrier

# Add code phase shift for all satellites
for i in range(0, len(SV)):
    allBPSKdata[i] = np.roll(allBPSKdata[i], -1*allCodePhase[i])

print("Mixing BPSK modulated data from all available satellites ...")
mixedDataBPSK = np.sum(allBPSKdata, axis=0)

print("Total data length is " + str(totalDataLength))

# Add some noise
mixedDataBPSKNoise = mixedDataBPSK + (np.random.rand(np.shape(mixedDataBPSK)[0])-0.5)*4.0   # 1

print("Passing mixed BPSK data into ADC and generating ADC output ...")
# Generating ADC Output
maxData = np.max(np.abs(mixedDataBPSKNoise))
adcDataRange = (2**(adcBitWidth-1))-0.01
if adcDataRange < maxData:
    adcOutScale = adcDataRange/maxData
else:
    adcOutScale = 1
dataScaled = mixedDataBPSKNoise * adcOutScale
adcOutputBPSK = dataScaled - 0.5
adcOutputBPSK = np.around(adcOutputBPSK).astype(int)
adcOutputBPSK *= 2
adcOutputBPSK += 1

########## Writting into csv files ###############

print("Writting ADC Output data into binary file ADCOutputBPSK.bin")
#adcOutputBPSK.tofile('ADCOutput.csv', sep=',', format='%d')
adcOutputBPSKbin = np.array(adcOutputBPSK, dtype = np.int8)
adcOutputBPSKbin.tofile(os.path.join(data_base, 'ADCOutput.bin'), sep='')

print("Writting random Doppler Shift into file 'DopplerShift.csv' ...")
(allInitialDoppler-dopplerRange).tofile(os.path.join(data_base, 'DopplerShift.csv'), sep=',', format='%d')
(allDopplerVaryDirection).tofile('DopplerShiftDirection.csv', sep=',', format='%d')

print("Writting random Code Phase into file 'CodePhase.csv' ...")
#(allCodePhase/np.around(adcSamplingFreq/prnFreq)).tofile('CodePhase.csv', sep=',', format='%.2f')
allCodePhase.tofile(os.path.join(data_base, 'CodePhase1.csv'), sep=',', format='%d')
(np.around(codePhaseRange*(adcSamplingFreq/prnFreq))-allCodePhase).tofile(os.path.join(data_base, 'CodePhase2.csv'),
                                                                          sep=',', format='%d')

#print("Writting ADC input data into binary file 'MixedDataBPSK.bin' ...")
#mixedDataBPSK = np.array(mixedDataBPSK, dtype = np.int8)
#mixedDataBPSK.tofile('MixedDataBPSK.bin', sep='')

print("Writting Baseband Data for target satellite into 'BasebandData.csv'...")
with open(os.path.join(data_base, 'BasebandData.csv'),'w') as f:
    writer = csv.writer(f)
    writer.writerows(allBasebandData)

#allSampledCarrier[0].tofile('allSampledCarrier0.csv', sep=',', format='%.3f')
#allSampledCarrier[1].tofile('allSampledCarrier1.csv', sep=',', format='%.3f')
#allSampledCarrier[2].tofile('allSampledCarrier2.csv', sep=',', format='%.3f')
#allSampledCarrier[3].tofile('allSampledCarrier3.csv', sep=',', format='%.3f')
#allSampledCarrier[4].tofile('allSampledCarrier4.csv', sep=',', format='%.3f')
#allSampledCarrier[5].tofile('allSampledCarrier5.csv', sep=',', format='%.3f')

#print("shape of all BPSK data" + str(allSampledCarrier.shape))

#allBPSKdata[0].tofile('allBPSKdata0.csv', sep=',', format='%.3f')
#allBPSKdata[1].tofile('allBPSKdata1.csv', sep=',', format='%.3f')
#allBPSKdata[2].tofile('allBPSKdata2.csv', sep=',', format='%.3f')
#allBPSKdata[3].tofile('allBPSKdata3.csv', sep=',', format='%.3f')


print("--- Finish! ---")

