# include <math.h>
# include <stdlib.h>
# include "params.h"

double eta_0(double mu, struct rcv_params *params) {
  return sqrt(mu/pow(params->sqrt_a, 6));
}

double t_k(double t, struct rcv_params *params) {
  return t - (rcv_params->t_oe);
}

double eta(double mu, struct rcv_params *params) {
  return eta_0(mu, rcv_params) + params->delta_n;
}

double mean_anomaly(double t, double mu, struct rcv_params *params) {
  return params->m_0 + (eta(mu, rcv_params) * t_k(t, params));
}

double v_k_iter(double v_k_old, struct rcv_params *params) {
  return arctan((sin(v_k_old))/(cos(v_k_old)));
}

double e_k(double v_k, struct rcv_params *params) {
  return arccos((params->e + cos(v_k))/(1 + params->e * cos(v_k)));
}

double m_k(double v_k, struct rcv_params *params) {
  return e_k(v_k, params) - (params->e * sin(e_k(v_k, params)));
}

double v_k(double v_k_old, struct rcv_params *params) {
  double ek = e_k(v_k_old, params);
  double num1 = sqrt(1-pow(params->e, 2)) * sin(ek);
  double num2 = 1 - (params->e * cos(ek));
  double denom1 = cos(ek) - params->e;
  double denom2 = (1-(params->e * cos(ek)));
  return arctan((num1 / num2)/(denom1 / denom2));
}

double phi_k(double v_k, struct rcv_params *params) {
  return v_k + params->omega;
}

double delta_uk(double v_k, struct rcv_params *params) {
  double phi = phi_k(v_k, params);
  return (params->c_us * cos(2*phi)) + (params->c_uc * cos(2*phi));
}

double delta_rk(double v_k, struct rcv_params *params) {
  double phi = phi_k(v_k, params);
  return (params->c_rs * sin(2*phi)) + (params->c_rc * cos(2*phi));
}

double delta_ik(double v_k, struct rcv_params *params) {
  double phi = phi_k(v_k, params);
  return (params->c_is * sin(2*phi)) + (params->c_ic * cos(2*phi));
}

double uk(double v_k, struct rcv_params *params) {
  return phi_k(v_k, params) + delta_uk(v_k, params);
}

double rk(double v_k, struct rcv_params *params) {
  double d_rk = delta_rk(v_k, params);
  double ek = e_k(v_k, params);
  return (pow(params->sqrt_a, 2) * (1 - (params->e * cos(ek))) + d_rk;
}

double ik(double t, double v_k, struct rcv_params *params) {
  double tk = t_k(t, params);
  double d_ik = delta_ik(v_k, params);
  return params->i_0 + d_ik + (params->idot * tk);
}

double x_k_prime(double v_k, struct rcv_params *params) {
  return rk(v_k, params) * cos(uk(v_k, params));
}

double y_k_prime(double v_k, struct rcv_params *params) {
  return rk(v_k, params) * sin(uk(v_k, params));
}

double omega_k(double t, double omega_e, struct rcv_params *params) {
  double tk = t_k(t, params);
  return params->omega_0 + (params->dot_omega - omega_e)*tk - (omega_e * params->t_oe);
}

double x_k(double v_k, double t, double omega_e, struct rcv_params *params) {
  double tk = t_k(t, params);
  double omegak = omega_k(t, omega_e, params);
  double ykp = y_k_prime(v_k, params);
  double xkp = x_k_prime(v_k, params);
  double i_k = ik(t, v_k, params);
  return (xkp*cos(omegak)) - (ykp*cos(i_k)*sin(omegak));
}

double y_k(double v_k, double t, double omega_e, struct rcv_params *params) {
  double tk = t_k(t, params);
  double omegak = omega_k(t, omega_e, params);
  double ykp = y_k_prime(v_k, params);
  double xkp = x_k_prime(v_k, params);
  double i_k = ik(t, v_k, params);
  return (xkp*sin(omegak)) - (ykp*cos(i_k)*cos(omegak));
}

double z_k(double t, double v_k, struct rcv_params *params) {
  double i_k = ik(t, v_k, params);
  double ykp = y_k_prime(v_k, params);
  return ykp * sin(i_k);
}
