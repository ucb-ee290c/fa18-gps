struct rcv_params {
  float subframe_id;

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

uint32_t words[10];
bool word_valid[10];

void extract_params(uint32_t* words, struct rcv_params* params) {
  while (1) {
    if (reg_read32(SUBFRAME_VALID)) {
      switch (reg_read32(SUBFRAME_ID)) {
        case 1:
          params->week_number = (float) reg_read32(WEEK_NUMBER);
          params->sv_accuracy = (float) reg_read32(SV_ACCURACY);
          params->sv_health = (float) reg_read32(SV_HEALTH);
          params->iodc = (float) reg_read32(IODC);
          params->t_gd = (float) reg_read32(T_GD) * (1 << 4);
          params->a_f2 = ((float) ((int32_t) reg_read32(A_F2))) / ((float) ((int64_t) 1 << 55));
          params->a_f1 = ((float) ((int32_t) reg_read32(A_F1))) / ((float) ((int64_t) 1 << 43));
          params->a_f0 = ((float) ((int32_t) reg_read32(C_RS))) / ((float) (1 << 31));
          break;
        case 2:
          params->iode = (float) reg_read32(IODE);
          params->c_rs = ((float) ((int32_t) reg_read32(C_RS))) / ((float) (1 << 5));
          params->delta_n = ((float) ((int32_t) reg_read32(DELTA_N))) / ((float) ((int64_t) 1 << 43));
          params->m_0 = ((float) ((int32_t) reg_read32(M_0))) / ((float) (1 << 31));
          params->c_uc = ((float) ((int32_t) reg_read32(C_UC))) / ((float) (1 << 29));
          params->e = ((float) reg_read32(E)) / ((float) ((uint64_t) 1 << 33));
          params->c_us = ((float) ((int32_t) reg_read32(C_US))) / ((float) (1 << 29));
          params->sqrt_a = ((float) reg_read32(SQRT_A)) / ((float) (1 << 19));
          params->t_oe = (float) reg_read32(T_OE) * (1 << 4);
          break;
        case 3:
          params->c_ic = ((float) ((int32_t) reg_read32(C_IC))) / ((float) (1 << 29));
          params->omega_0 = ((float) ((int32_t) reg_read32(OMEGA_0))) / ((float) (1 << 31));
          params->c_is = ((float) ((int32_t) reg_read32(C_IS))) / ((float) (1 << 29));
          params->i_0 = ((float) ((int32_t) reg_read32(I_0))) / ((float) (1 << 31));
          params->c_rc = ((float) ((int32_t) reg_read32(C_RC))) / ((float) (1 << 5));
          params->omega = ((float) ((int32_t) reg_read32(OMEGA))) / ((float) (1 << 31));
          params->dot_omega = ((float) ((int32_t) reg_read32(DOT_OMEGA))) / ((float) ((int64_t) 1 << 43));
          params->idot = ((float) ((int32_t) reg_read32(IDOT))) / ((float) ((int64_t) 1 << 43));
          break;
      }
    }
  }
}
