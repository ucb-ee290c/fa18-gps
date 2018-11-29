# include "params.h"
# include "regmap.h"

void extract_params(int sat_offset, struct rcv_params* params) {
  uint32_t state = 0;
  bool done = 0;
  while (!done) {
    if (reg_read32(sat_offset + SUBFRAME_VALID)) {
      switch (reg_read32(sat_offset + SUBFRAME_ID)) {
        case 1:
          if ((state == 0) && (reg_read32(sat_offset + SUBFRAME_PARITY) == (1 << 10) - 1)) { // SUBFRAME_PARITY should be 10 ones
            params->week_number = (float) reg_read32(sat_offset + WEEK_NUMBER);
            params->sv_accuracy = (float) reg_read32(sat_offset + SV_ACCURACY);
            params->sv_health = (float) reg_read32(sat_offset + SV_HEALTH);
            params->iodc = (float) reg_read32(sat_offset + IODC);
            params->t_gd = (float) reg_read32(sat_offset + T_GD) * (1 << 4);
            params->a_f2 = ((float) ((int32_t) reg_read32(sat_offset + A_F2))) / ((float) ((int64_t) 1 << 55));
            params->a_f1 = ((float) ((int32_t) reg_read32(sat_offset + A_F1))) / ((float) ((int64_t) 1 << 43));
            params->a_f0 = ((float) ((int32_t) reg_read32(sat_offset + C_RS))) / ((float) (1 << 31));
            state = 1;
          }
          break;
        case 2:
          if ((state == 1) && (reg_read32(sat_offset + SUBFRAME_PARITY) == (1 << 10) - 1)) { // SUBFRAME_PARITY should be 10 ones
            params->iode = (float) reg_read32(sat_offset + IODE);
            params->c_rs = ((float) ((int32_t) reg_read32(sat_offset + C_RS))) / ((float) (1 << 5));
            params->delta_n = ((float) ((int32_t) reg_read32(sat_offset + DELTA_N))) / ((float) ((int64_t) 1 << 43));
            params->m_0 = ((float) ((int32_t) reg_read32(sat_offset + M_0))) / ((float) (1 << 31));
            params->c_uc = ((float) ((int32_t) reg_read32(sat_offset + C_UC))) / ((float) (1 << 29));
            params->e = ((float) reg_read32(sat_offset + E)) / ((float) ((uint64_t) 1 << 33));
            params->c_us = ((float) ((int32_t) reg_read32(sat_offset + C_US))) / ((float) (1 << 29));
            params->sqrt_a = ((float) reg_read32(sat_offset + SQRT_A)) / ((float) (1 << 19));
            params->t_oe = (float) reg_read32(sat_offset + T_OE) * (1 << 4);
            state = 2;
          }
          break;
        case 3:
          if ((state == 2) && (reg_read32(sat_offset + SUBFRAME_PARITY) == (1 << 10) - 1)) { // SUBFRAME_PARITY should be 10 ones
            params->c_ic = ((float) ((int32_t) reg_read32(sat_offset + C_IC))) / ((float) (1 << 29));
            params->omega_0 = ((float) ((int32_t) reg_read32(sat_offset + OMEGA_0))) / ((float) (1 << 31));
            params->c_is = ((float) ((int32_t) reg_read32(sat_offset + C_IS))) / ((float) (1 << 29));
            params->i_0 = ((float) ((int32_t) reg_read32(sat_offset + I_0))) / ((float) (1 << 31));
            params->c_rc = ((float) ((int32_t) reg_read32(sat_offset + C_RC))) / ((float) (1 << 5));
            params->omega = ((float) ((int32_t) reg_read32(sat_offset + OMEGA))) / ((float) (1 << 31));
            params->dot_omega = ((float) ((int32_t) reg_read32(sat_offset + DOT_OMEGA))) / ((float) ((int64_t) 1 << 43));
            params->idot = ((float) ((int32_t) reg_read32(sat_offset + IDOT))) / ((float) ((int64_t) 1 << 43));
            state = 3;
            done = true;
          }
          break;
      }
    }
  }
  return;
}
