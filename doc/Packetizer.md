# Packetizer
The GPS packetizer serves as the final interface between hardware and firmware, and processes the decoded bits to verify data integrity and extract ephemeris data for the ranging algorithm.

The packetizer is structured into three stages.

* **Parser**: The parser assembles data packets by searching the incoming data stream for the 8-bit GPS preamble (10001011), or its bit inversion (tracking channel bit streams may arrive inverted). This preamble indicates the start of a subframe - the basic unit of a GPS transmission, consisting of ten 30-bit words. Five subframes constitute a frame, and each of these subframes carries different data - the first three are used in the ranging calculation. The parser reads in one subframe at a time, then passes all 300 bits out to the next stage.
* **Parity checker**: Each 30-bit word contains 24 data bits (D1-D24) and 6 parity bits (D25-D30) that are used to verify the integrity of the word. The parity calculation involves the 24 data bits, plus the last two bits of the previously-received word, and is calculated as follows (source: *Fundamentals of Global Positioning System Receivers: A Software Approach*):
![](../../parity.png)
The parity checker outputs the result of the parity check on each of the ten words, then passes its data to the next stage.
* **Param extractor**: initially, we had intended to regmap the ten words and their corresponding parity check results to the processor. However, the bit manipulations necessary to extract the ephemeris parameters are unwieldy to perform in software, so an extra layer of combinational logic was added to extract these parameters and pass them directly to the processor. This is the param extractor. This module was not part of the initial design, and therefore is not included in the Python models.

## Python models
* `python/blocks/packet_model.py` defines the class of the `Packet` model. A `Packet` instance contains instances of two other classes:
        * `Parser`: scans the incoming stream for a preamble, indicating the start of a subframe, then splits the subframe into words and writes them to an output interface.
        * `Parity_Checker`: performs a parity check on each word after the parser is done, then writes the words and the results of the parity checks to an output interface.
* `python/packet_encode.py` contains functions for performing parity encoding. The function `generate_subframe()` is called in `packet_test.py`; it produces a subframe with the correct preamble and randomized data, then attaches the correct parity bits.
* `python/packet_test.py` generates a subframe and feeds it into an instance of `Packet`, which reports results to the user.
