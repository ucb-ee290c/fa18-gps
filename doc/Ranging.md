# Tape-in 2
## Location
### Overview
GPS locationing requires a lock on several satellites to determine the receiver's location on the Earth as well as the receiver reference clock bias with respect to true GPS time.  The locationing makes use of the pseudorange of the receiver to each satellite it is tracking.  The pseudorange is defined as
```
PR = [Tsent - Trec]*C
``` 
where `Tsent` is the time at which the message is transmitted from the satellite, `Trec` is the receiver time at the time of measurement, and C is the speed of light.  
We can use this information in conjunction with the locations of the tracked satellites (from the navigation message) to approximate our receiver position.  The calculation at a high level is iterative since it starts with assuming a nominal location and time bias (ie. the center of the Earth (0,0,0)) and calculates a set of deltas that it then adds onto the current nominal position.  In each iteration, an error magnitude is calculated using the deltas and uses that to determine when to keep iterating or stop (ie. if the delta (x,y,z) magnitude of smaller than some threshold then the calculation should be accurate up to that threshold). 

### Inputs and Outputs
* Input: `Tsent` from the tracking loop for each satellite
* Input: `Trec` from the tracking loops for each satellite
* Input: satellite positions in ECEF coordinates from navigation message
* Output: approximate location of receiver

### Results
In this example, we use a set of 4 dummy satellites located approximately 20,000 km above the surface of the Earth (the approximate altitude of real GPS satellites), with known locations in ECEF coordinates (this location would be calculated from each satellite's navigation message).  We chose the BWRC as a place to locate.  Because we know the locations of the satellites and the BWRC, we can approximately calculate the send and receive times of the satellites that would be calculated from the GPS satellite replica clocks as a product of the tracking loops.
The solution converges to a very small error in about 4 iterations.  Here are the results:
```
BWRC Calculated location (km in ECEF coordinates):
x: -2691.466
y: -4262.826
z: 3894.033
```
This is almost exactly the true position of the BWRC in ECEF coordinates.  While this is obviously using an ideal set of parameters, but this demonstrates that the algorithm will converge to the optimal location with the information it is given.

### Ongoing Work 
* Writing algorithm in C
* Finish `Tsent` and `Trec` calculations from tracking loops
* Adding in calculations and more non-idealities from ephemeris parameters
