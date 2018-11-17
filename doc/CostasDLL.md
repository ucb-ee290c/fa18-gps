# Tapein 2:

## Costas Loop and DLL

The figure below shows the complete Tracking Loop. The discriminators and loop
filters (highlighted in orange) are simulated in scala to test the loop. 

With the correct loop filter settings, the tracking loops converge on test
input data shown in the plot below.


## Carrier Recovery

### Discriminators

The carrier recovery cosists of a costas discriminator based PLL and a FLL to
improve the dynamic range of the loop. We use an arctangent based
discriminator for phase locking, represented by the equation shown below:

*ATAN*(Q<sub>ps</sub>/I<sub>ps</sub>)

Where I<sub>ps</sub> and Q<sub>ps</sub> are the integrated I and Q. For
frequency locking, we use another arctangent based discriminator represented by
the function shown below: 

*dot* = I<sub>ps1</sub>I<sub>ps2</sub> + Q<sub>ps1</sub>Q<sub>ps2</sub>

*cross* = I<sub>ps1</sub>Q<sub>ps2</sub> + Q<sub>ps1</sub>I<sub>ps2</sub>

*ATAN2*(cross, dot) / (t<sub>2</sub> - t<sub>1</sub>)

Where X<sub>ps1</sub> and X<sub>ps2</sub> are integrated samples that are
spaced (t<sub>2</sub> - t<sub>1</sub>) apart. Integrated samples are not reused in
the FLL discriminator calculation. That is, X<sub>ps2</sub> is not reused in
the next calculation.

The discriminator outputs are fed into the loop filter shown shown below. The
quantity T is the time between samples, and is variable depending on the
current integration time.



The loop filter is a third order loop filter, with a second order filter on the
FLL input. The coefficients a<sub>2</sub>, a<sub>3</sub>, and b<sub>3</sub>
are selected based on the results presented in the book "Understanding GPS:
Principles and Applications". They are copied here, 

|             |     |
|-------------|-----|
|a<sub>2</sub>|1.414|
|a<sub>3</sub>|1.1  |
|b<sub>3</sub>|2.4  |

The quantity &omega;<sub>0p</sub> is selected based on the target bandwidth for
the phase locking loop. The &omega;<sub>0f</sub> is selected based the target
bandwidth for the frequency locking loop. The bandwidth of the phase locked
loop was selected to be 18Hz and the bandwidth of the frequency locked loop was
selected to be 3 Hz. Given this, &omega;<sub>0p</sub> = 18/0.7845 and
&omega;<sub>0p</sub> = 3/0.53. The constants 0.53 and 0.7845 were again
selected based on results from "Understanding GPS: principles and
applications". 
