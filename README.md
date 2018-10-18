# GPS L1 C/A Reciver
## Python models
### Packetizer
* `python/blocks/packet_model.py` defines the class of the `Packet` model. A `Packet` instance contains instances of two other classes:
	* `Parser`: scans the incoming stream for a preamble, indicating the start of a subframe, then splits the subframe into words and writes them to an output interface.
	* `Parity_Checker`: performs a parity check on each word after the parser is done, then writes the words and the results of the parity checks to an output interface.
* `python/packet_encode.py` contains functions for performing parity encoding. The function `generate_subframe()` is called in `packet_test.py`; it produces a subframe with the correct preamble and randomized data, then attaches the correct parity bits.
* `python/packet_test.py` generates a subframe and feeds it into an instance of `Packet`, which reports results to the user.
