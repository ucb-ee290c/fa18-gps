#ifndef CALCS_HEADER
#define CALCS_HEADER
# include <math.h>
# include <stdlib.h>
# include "params.h"

double eta_0(double mu, struct rcv_params *params) 
double t_k(double t, struct rcv_params *params)
double eta(double mu, struct rcv_params *params)
double mean_anomaly(double t, double mu, struct rcv_params *params)
double v_k_iter(double v_k_old, struct rcv_params *params)
double e_k(double v_k, struct rcv_params *params)
double m_k(double v_k, struct rcv_params *params)
double v_k(double v_k_old, struct rcv_params *params)
double phi_k(double v_k, struct rcv_params *params)
double delta_uk(double v_k, struct rcv_params *params)
double delta_rk(double v_k, struct rcv_params *params)
double delta_ik(double v_k, struct rcv_params *params)
double uk(double v_k, struct rcv_params *params)
double rk(double v_k, struct rcv_params *params)
double ik(double t, double v_k, struct rcv_params *params)
double x_k_prime(double v_k, struct rcv_params *params)
double y_k_prime(double v_k, struct rcv_params *params)
double omega_k(double t, double omega_e, struct rcv_params *params)
double x_k(double v_k, double t, double omega_e, struct rcv_params *params)
double y_k(double v_k, double t, double omega_e, struct rcv_params *params)
double z_k(double t, double v_k, struct rcv_params *params)

#endif
