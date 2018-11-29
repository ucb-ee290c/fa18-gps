#ifndef PARAMS_HEADER
#define PARAMS_HEADER

struct sat_loc_params {
  float Xx, Yy, Zz;
  float xpl, ypl;
};

struct rcv_params {
  float subframe_id;
  float gps_time;
  // from subframe 1
  float week_number;
  float sv_accuracy;
  float sv_health;
  float iodc;
  float t_gd;
  float a_f2;
  float a_f1;
  float a_f0;

  // from subframe 2
  float iode;
  float c_rs;
  float delta_n;
  float m_0;
  float c_uc;
  float e;
  float c_us;
  float sqrt_a;
  float t_oe;

  // from subframe 3
  float c_ic;
  float omega_0;
  float c_is;
  float i_0;
  float c_rc;
  float omega;
  float dot_omega;
  float idot;
};

#endif
