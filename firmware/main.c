# include <stdio>
# include <stdlib>
# include <math>
# include "calculations.h"
# include "params.h"
# include "regmap.h"
# include "position.h"
# define SUBFRAME_VALID 0x2000 //replace once regmap made
# define SUBFRAME 0x2004 //Replace once regmap made


int main () {
  float REGMAX = powf(2.0, 20.0);
  struct rcv_params *sat_1_params = (struct rcv_params*)malloc(sizeof(struct rcv_params));
  struct rcv_params *sat_2_params = (struct rcv_params*)malloc(sizeof(struct rcv_params));
  struct rcv_params *sat_3_params = (struct rcv_params*)malloc(sizeof(struct rcv_params));
  struct rcv_params *sat_4_params = (struct rcv_params*)malloc(sizeof(struct rcv_params));
  
  struct sat_loc_params *sat_1_loc = (struct sat_loc_params *)malloc(sizeof(struct sat_loc_params));
  struct sat_loc_params *sat_2_loc = (struct sat_loc_params *)malloc(sizeof(struct sat_loc_params));
  struct sat_loc_params *sat_3_loc = (struct sat_loc_params *)malloc(sizeof(struct sat_loc_params));
  struct sat_loc_params *sat_4_loc = (struct sat_loc_params *)malloc(sizeof(struct sat_loc_params));

  float sat_1_time, sat_2_time, sat_3_time, sat_4_time;
  
  //Updates the param struct pointers 
  extract_params(SAT_1_OFFSET, sat_1_params);
  extract_params(SAT_2_OFFSET, sat_2_params);
  extract_params(SAT_3_OFFSET, sat_3_params);
  extract_params(SAT_4_OFFSET, sat_4_params);
  
  //Compute proper time using hardware parameters
  sat_1_time = sat_1_params->gps_sec + (sat_1_params->gps_ms/1000.0) + (sat_1_params->gps_chips * 0.977 * powf(10.0, -6.0)) + (0.977 * powf(10.0, -6.0) * sat_1_params->gps_phase/REGMAX);
  sat_2_time = sat_2_params->gps_sec + (sat_2_params->gps_ms/1000.0) + (sat_2_params->gps_chips * 0.977 * powf(10.0, -6.0)) + (0.977 * powf(10.0, -6.0) * sat_2_params->gps_phase/REGMAX);
  sat_3_time = sat_3_params->gps_sec + (sat_3_params->gps_ms/1000.0) + (sat_3_params->gps_chips * 0.977 * powf(10.0, -6.0)) + (0.977 * powf(10.0, -6.0) * sat_3_params->gps_phase/REGMAX);
  sat_4_time = sat_4_params->gps_sec + (sat_4_params->gps_ms/1000.0) + (sat_4_params->gps_chips * 0.977 * powf(10.0, -6.0)) + (0.977 * powf(10.0, -6.0) * sat_4_params->gps_phase/REGMAX);
  
  //Updates the struct location pointers with X,Y,Z positions for the satellites
  get_sv_pos(sat_1_time, sat_1_loc, sat_1_params);
  get_sv_pos(sat_2_time, sat_2_loc, sat_2_params);
  get_sv_pos(sat_3_time, sat_3_loc, sat_3_params);
  get_sv_pos(sat_4_time, sat_4_loc, sat_4_params);
  
  struct double_sat_loc_params sv_1 = { (double) sat_1_loc->Xx, (double) sat_1_loc->Yy, (double) sat_1_loc->Zz}; 
  struct double_sat_loc_params sv_2 = { (double) sat_2_loc->Xx, (double) sat_2_loc->Yy, (double) sat_2_loc->Zz};
  struct double_sat_loc_params sv_3 = { (double) sat_3_loc->Xx, (double) sat_3_loc->Yy, (double) sat_3_loc->Zz};
  struct double_sat_loc_params sv_4 = { (double) sat_4_loc->Xx, (double) sat_4_loc->Yy, (double) sat_4_loc->Zz};
  struct delta_t time_deltas = { (double) sat_1_time, (double) sat_2_time, (double) sat_3_time, (double) sat_4_time };
  struct ecef_pos final_result;
  
  find_position(&sv_1, &sv_2, &sv_3, &sv_4, &time_deltas, &final_result);

  free(sat_1_params);
  free(sat_2_params);
  free(sat_3_params);
  free(sat_4_params);
  
  free(sat_1_loc);
  free(sat_2_loc);
  free(sat_3_loc);
  free(sat_4_loc);
}
