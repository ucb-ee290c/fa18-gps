#ifndef POS_HEADER
#define POS_HEADER
# include <math.h>
# include <stdlib.h>

#define c 299792458

struct delta_t {
  long double delta_t_1;
  long double delta_t_2;
  long double delta_t_3;
  long double delta_t_4;
};

struct ecef_pos {
  long double x;
  long double y;
  long double z;
  long double t_bias;
};

struct sat_loc_params {
  long double Xx;
  long double Yy;
  long double Zz;
};

long double calc_pseudorange(struct sat_loc_params* sat, long double nom[4]);
void find_position(struct sat_loc_params* sv_1, struct sat_loc_params* sv_2, struct sat_loc_params* sv_3, struct sat_loc_params* sv_4, struct delta_t* time_deltas, struct ecef_pos* position);

/*
struct delta_t {
  float delta_t_1;
  float delta_t_2;
  float delta_t_3;
  float delta_t_4;
};

struct ecef_pos {
  float x;
  float y;
  float z;
  float t_bias;
};

struct sat_loc_params {
  float Xx;
  float Yy;
  float Zz;
};

float calc_pseudorange(struct sat_loc_params* sat, float nom[4]);
void find_position(struct sat_loc_params* sv_1, struct sat_loc_params* sv_2, struct sat_loc_params* sv_3, struct sat_loc_params* sv_4, struct delta_t* time_deltas, struct ecef_pos* position);
*/

#endif
