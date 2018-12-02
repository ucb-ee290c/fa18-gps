# include <math.h>
# include <stdlib.h>
# include <stdio.h>
# include "params.h"
# include "calculations.h"

void main() {
  /*
  data taken from jks.com/gps/gps.html
  */
  struct rcv_params *params = (struct rcv_params *)malloc(sizeof(struct rcv_params));
  
  params->week_number = 1608.0;
  params->sv_accuracy = 0.0;
  params->sv_health = 63.0;
  params->iodc = 29.0;
  params->t_gd = -1.909211 * powf(10.0, -8.0);
  params->a_f2 = 0.0;
  params->a_f1 = -3.63797880 * powf(10.0, -12.0);
  params->a_f0 = -0.00018144;

  params->iode = 0x1D;
  params->m_0 = 0.62771227/M_PI;
  params->delta_n = 4.55189 * powf(10.0, -9.0) * (1/M_PI);
  params->e = 0.0044012;
  params->sqrt_a = 5153.5262;
  params->omega_0 = 0.543838/M_PI;
  params->i_0 = 0.96682775/M_PI;
  params->omega = 0.9487046/M_PI;
  params->dot_omega = -8.1653401 * powf(10.0, -9.0) * (1/M_PI);
  params->idot = -5.75023952 * powf(10.0, -11.0) * (1/M_PI);
  params->c_uc = -3.64519655 * powf(10.0, -6.0);
  params->c_us = 6.5993518 * powf(10.0, -6.0);
  params->c_rc = 254.875;
  params->c_rs = -73.15625;
  params->c_ic = 1.3038516 * powf(10.0, -8.0);
  params->c_is = 1.3038516 * powf(10.0, -8.0);
  params->t_oe = 468000;
  
  float *Xx = (float *)malloc(sizeof(float));
  float *Yy = (float *)malloc(sizeof(float));
  float *Zz = (float *)malloc(sizeof(float));
  float t = 466728.880396;
  float *xpl = (float *)malloc(sizeof(float));
  float *ypl = (float *)malloc(sizeof(float));
  get_sv_pos(t, Xx, Yy, Zz, xpl, ypl, params);
  printf("Xx: %.6f\n", *Xx);
  printf("Yy: %.6f\n", *Yy);
  printf("Zz: %.6f\n", *Zz);
  
  free(params);
  free(Xx);
  free(Yy);
  free(Zz);
  free(xpl);
  free(ypl);
}

