# include <stdio>
# include <stdlib>
# include <math>
# include "calculations.h"
# include "params.h"
# include "regmap.h"
# define SUBFRAME_VALID 0x2000 //replace once regmap made
# define SUBFRAME 0x2004 //Replace once regmap made

struct rcv_params *sat_1_params = (struct rcv_params*)malloc(sizeof(struct rcv_params));
struct rcv_params *sat_2_params = (struct rcv_params*)malloc(sizeof(struct rcv_params));
struct rcv_params *sat_3_params = (struct rcv_params*)malloc(sizeof(struct rcv_params));
struct rcv_params *sat_4_params = (struct rcv_params*)malloc(sizeof(struct rcv_params));

struct sat_loc_params *sat_1_loc = (struct sat_loc_params *)malloc(sizeof(struct sat_loc_params));
struct sat_loc_params *sat_2_loc = (struct sat_loc_params *)malloc(sizeof(struct sat_loc_params));
struct sat_loc_params *sat_3_loc = (struct sat_loc_params *)malloc(sizeof(struct sat_loc_params));
struct sat_loc_params *sat_4_loc = (struct sat_loc_params *)malloc(sizeof(struct sat_loc_params));

extract_params(SAT_1_OFFSET, sat_1_params);
extract_params(SAT_2_OFFSET, sat_2_params);
extract_params(SAT_3_OFFSET, sat_3_params);
extract_params(SAT_4_OFFSET, sat_4_params);

get_sv_pos(sat_1_params->gps_time, sat_1_loc, sat_1_params);
get_sv_pos(sat_2_params->gps_time, sat_2_loc, sat_2_params);
get_sv_pos(sat_3_params->gps_time, sat_3_loc, sat_3_params);
get_sv_pos(sat_4_params->gps_time, sat_4_loc, sat_4_params);
