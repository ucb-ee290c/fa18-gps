# include <math.h>
# include <stdlib.h>
# include <stdio.h>
# include "params.h"
# include "calculations.h"

float t_k(float t, struct rcv_params *params) {
  //A helper function used in satellite position calculations.
  float tk = t - params->t_oe;
  //Correct for mid-week changes
  if (tk > 302400.0) {
    tk = tk - 604800.0;
  } else if (tk < -302400.0) {
    tk = tk + 6048000.0;
  }
  return tk;
}

float eccentric_anomaly(float Mk, struct rcv_params *params) {
  //Another helper function for satellite position calculation. Iteratively solves for the eccentric anomaly.
  float EkOLD = Mk;
  //Define first iteration value so the loop won't immediately terminate
  float EkNEW = Mk + (params->e * sinf(Mk));
  //fabs is float absolute
  while(fabs(EkOLD - EkNEW) > powf(10.0, -8.0)) {
    EkOLD = EkNEW;
    EkNEW = Mk + (params->e * sinf(EkNEW));
  }
  return EkNEW;
}


void get_sv_pos(float t, struct sat_loc_params *loc_params, struct rcv_params *params) {
  /*
  This function computes the position of an SV in ECEF coordinates from the ephemeris data.
  Params:
  rcv_params: An rcv_params struct pointer containing the ephemeris data
  t: GPS time in seconds
  *Xx, *Yy, *Zz: pointers to a variable where the result (X,Y,Z) coordinates will be placed. 
  *xpl, *ypl: pointers to a variable where the result of the positions in the orbital plane will be placed.
  */

  float Mk, Ek, diff, vk, aol, delr, delal, delinc, rk, inc;
  float la, xp, yp, tk, A, mu, Omegae, w, i0, IDOT, OmegaDot, Omega0, dn, m0;
  float ARGX, ARGY;
  float n0, Xk, Yk, Zk;
  
  //Constants used in calculations
  mu = 3.986005 * powf(10.0, 14.0);
  Omegae = 7.2921151467 * powf(10.0, -5.0);
  //Compute tk, the time delta from the GPS time to the t_oe
  tk = t_k(t, params);  
  //All of these below are scaled by PI to convert from units of semi-circles to radians
  dn = params->delta_n * M_PI;
  m0 = params->m_0 * M_PI;
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
  ARGX = sqrtf(1.0 - (params->e * params->e)) * sinf(Ek);
  ARGY = cosf(Ek) - params->e;
  vk = atan2f(ARGX, ARGY);
  //Calculate argument of lat. of SV and second harmonic perturbations of orbit
  aol = vk + w;
  delr = (params->c_rc * cosf(2.0 * aol)) + (params->c_rs * sinf(2.0 * aol));
  delal = (params->c_uc * cosf(2.0 * aol)) + (params->c_us * sinf(2.0 * aol));
  delinc = (params->c_ic * cosf(2.0 * aol)) + (params->c_is * sinf(2.0 * aol));
  //Correct aol
  aol = aol + delal;
  //rk is the radius of SV at time t
  rk = A * (1.0 - (params->e * cosf(Ek))) + delr;
  //correct inclination angle and compute orbital plane locations
  inc = i0 + delinc + (IDOT * tk);
  xp = rk * cosf(aol);
  yp = rk * sinf(aol);
  //Return the values
  loc_params->ypl = yp;
  loc_params->xpl = xp;
  //LA is corrected longitude of ascending node
  la = Omega0 + ((OmegaDot - Omegae) * tk) - (Omegae * params->t_oe);
  //Compute the X,Y,Z coordinates
  Xk = (xp * cosf(la)) - (yp * cosf(inc)*sinf(la));
  Yk = (xp * sinf(la)) + (yp * cosf(inc)*cosf(la));
  Zk = yp * sinf(inc);
  //Return the values
  loc_params->Xx = Xk;
  loc_params->Yy = Yk;
  loc_params->Zz = Zk;
  return;
}
