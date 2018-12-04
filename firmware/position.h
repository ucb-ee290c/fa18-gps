#ifndef POS_HEADER
#define POS_HEADER
# include <math.h>
# include <stdlib.h>

//speed of light
#define c 299792458

struct delta_t {
  double delta_t_1;
  double delta_t_2;
  double delta_t_3;
  double delta_t_4;
};

struct ecef_pos {
  double x;
  double y;
  double z;
  double t_bias;
};

struct sat_loc_params {
  double Xx;
  double Yy;
  double Zz;
};

double calc_pseudorange(struct sat_loc_params* sat, double nom[4]);
void find_position(struct sat_loc_params* sv_1, struct sat_loc_params* sv_2, struct sat_loc_params* sv_3, struct sat_loc_params* sv_4, struct delta_t* time_deltas, struct ecef_pos* position);

#endif
