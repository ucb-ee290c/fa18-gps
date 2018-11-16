# Tapein 2:

## Costas Loop and DLL

The figure below shows the complete Tracking Loop. The discriminators and loop
filters (highlighted in orange) are simulated in scala to test the loop. 

With the correct loop filter settings, the tracking loops converge on test
input data shown in the plot below.


## Costas Loop

### Discriminators

For the costas loop, we use an arctangent based discriminator, represented by
the equation shown below: 

\[ \text{ATAN}\left(\frac{Q_{ps}}{I_{ps}}\right) \]
