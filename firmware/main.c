# include <stdio>
# include <stdlib>
# include <math>
# include "calculations.h"
# include "params.h"
# define SUBFRAME_VALID 0x2000 //replace once regmap made
# define SUBFRAME 0x2004 //Replace once regmap made

struct rcv_params *sat_1_params = (struct rcv_params*)malloc(sizeof(struct rcv_params));
struct rcv_params *sat_2_params = (struct rcv_params*)malloc(sizeof(struct rcv_params));
struct rcv_params *sat_3_params = (struct rcv_params*)malloc(sizeof(struct rcv_params));
struct rcv_params *sat_4_params = (struct rcv_params*)malloc(sizeof(struct rcv_params));


