# include <math.h>
# include <stdlib.h>
# include "params.h"

float t_k(float t, struct rcv_params *params) {
  //A helper function used in satellite position calculations.
  float tk = t - rcv_params->t_oe;
  //Correct for mid-week changes
  if tk > 302400.0f {
    tk = tk - 604800.0f;
  } else if tk < -302400.0f {
    tk = tk + 6048000.0f;
  }
  return tk;
}

float eccentric_anomaly(float Mk, struct rcv_params *params) {
  //Another helper function for satellite position calculation. Iteratively solves for the eccentric anomaly.
  float EkOLD = Mk;
  //Define first iteration value so the loop won't immediately terminate
  float EkNEW = Mk + (params->e * sinf(Mk));
  //fabs is float absolute
  while(fabs(EkOLD - EkNEW) > powf(10.0f, -8.0f)) {
    EkNEW = Mk + (params->e * sinf(EkNEW));
  }
  return EkNew;
}


void get_sv_pos(float t, float *Xx, float *Yy, float *Zz, float *xpl, float *ypl, struct rcv_params *params) {
  /*
  This function computes the position of an SV in ECEF coordinates from the ephemeris data.
  Params:
  rcv_params: An rcv_params struct pointer containing the ephemeris data
  t: GPS time in seconds
  *Xx, *Yy, *Zz: pointers to a variable where the result (X,Y,Z) coordinates will be placed. 
  *xpl, *ypl: pointers to a variable where the result of the positions in the orbital plane will be placed.
  */

  float Mk, Ek, diff, vk, aol, delr, delal, deline, rk, inc;
  float la, xp, yp, tk, A, mu, Omegae, w, i0, IDOT, OmegaDot, Omega0;
  float ARGX, ARGY;
  float n0, Xk, Yk, Zk;
  
  //Constants used in calculations
  mu = 3.986005f * powf(10, 14);
  Omegae = 7.2921151467f * powf(10, -5);
  //Compute tk, the time delta from the GPS time to the t_oe
  tk = t_k(t, params);  
 
  //All of these below are scaled by PI to convert from units of semi-circles to radians
  dn = params->delta_n * M_PI;
  m0 = params->m_0 * M_PI
  w = params->omega * M_PI;
  i0 = params->i_0 * M_PI;
  IDOT = params->idot * M_PI;
  OmegaDot = params->dot_omega * M_PI;
  Omega0 =  params->omega_0 * M_PI;
  //End scaling
  //Compute mean motion, n0
  A = params->sqrt_a * params->sqrt_a;
  n0 = sqrtf(mu/(A*A*A));
 
  //Compute mean anomaly, then eccentric anomaly by iteration 
  Mk = m0 + (tk * (n0 + dn));
  Ek = eccentric_anomaly(Mk, params);
  //Compute true anomaly and vk (angle from perigree)
  ARGX = sqrtf(1.0f - (params->e * params->e)) * sinf(Ek);
  ARGY = cosf(Ek) - params->e;
  vk = atan2f(ARGX, ARGY);
  //Calculate argument of lat. of SV and second harmonic perturbations of orbit
  aol = vk + w;
  delr = (params->c_rs * cosf(2.0f * aol)) + (params->c_rc * sinf(2.0f * aol));
  delal = (params->c_uc * cosf(2.0f * aol)) + (params->c_us * sinf(2.0f * aol));
  delinc = (params->c_ic * cosf(2.0f * aol)) + (params->c_is * sinf(2.0f * aol));
  //Correct aol
  aol = aol + delal;
  //rk is the radius of SV at time t
  rk = A * (1.0f - (params->e * cosf(Ek))) + delr;
  //correct inclination angle and compute orbital plane locations
  inc = i0 + delinc + (IDOT * tk);
  xp = rk * cosf(aol);
  yp = rk * sinf(aol);
  //Return the values
  *ypl = yp;
  *xpl = xp;
  //LA is corrected longitude of ascending node
  la = Omega0 + ((OmegaDot - Omegae) * tk) - (Omegae * params->t_oe);
  //Compute the X,Y,Z coordinates
  Xk = (xp * cosf(la)) - (yp * cosf(inc)*sinf(la));
  Yk = (xp * sinf(la)) + (yp * cosf(inc)*cosf(la));
  Zk = yp * sinf(inc);
  //Return the values
  *Xx = Xk;
  *Yy = Yk;
  *Zz = Zk;

  return 0;
}
