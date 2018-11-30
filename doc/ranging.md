# Ranging Code

The ranging code is reponsible for taking the hardware decoded satellite parameters and converting them into user location values. The process is split into 3 main steps:

1. Extract hardware parameters, scale and convert
2. Compute satellite positions
3. Compute user position

The various computation functions are spread amongst various files, but the overarching algorithm is contained within `main.c`.

## Extract hardware parameters
The hardware parameters will be described later, but they are effectively the data encoded in satellite transmissions necessary to compute the satellite locations.
The parameter extraction is done via a direct read from memory mapped registers. The mapping was decided on prior to software implementation, and is summarized in `regmap.h`

Relevant files:
* `params.h`
* `regmap.h`
* `ranging.c` - `extract_params(int sat_offset, struct rcv_params *params)`
  * Inputs:
      * `sat_offset`: Integer
      * `*params`: Struct rcv_params pointer
  * Outputs:
    * None

The satellite parameters are stored in continuous memory addresses and a starting address. 
Every field in the satellite paramters is of a known width, which is also included in `regmap.h`.
Interfacing is done by knowing the starting memory address for the satellite parameters of interest, and adding a specific offset to obtain the value requested.
The `sat_offset` parameter is the starting memory address for the regmap. This was done to support an arbitrary amount of hardware tracking channels with the same function.
If there is one tracking channel, all satellite parameters will have the same `sat_offset`.

The `rcv_params` struct (defined in `params.h`) is a convenient struct that consolidates all the SV parameters in a single location. A pointer to such a struct is 
passed into the extraction, and the function will update the referenced values.

## Compute Satellite Locations

GPS Satellites transmit information known as "Ephemeris data" that contains parameters like correction coefficients and describtions of their orbital motion.
Using these parameters, the SV position can be computed in terms of ECEF (Earth-Centered, Earth-Fixed) coordinates, as shown below.

<img src="https://upload.wikimedia.org/wikipedia/commons/8/88/Ecef.png" width="500" height="500" />
(source https://upload.wikimedia.org/wikipedia/commons/8/88/Ecef.png")

The computation of these locations is purely in terms of these parameters, and done in `calculations.c`. 

Relevant files:
* `params.h`
* `calculations.c` - `get_sat_loc(float t, struct sat_loc_params *loc_params, struct rcv_params *params)
   * Inputs:
     * `t`: Float
     * `*loc_params`: struct sat_loc_params pointer
     * `*params`: struct rcv_params pointer
   * Outputs:
    * None
    
Each satellite has a `sat_loc_params` struct pointer which contains the `(X, Y, Z)` coordinates in ECEF format as well as the aformentioned rcv_params pointer.
Passing both of these into `get_sat_loc` will update the pointers with the computed values.

## Computing user position

asd

