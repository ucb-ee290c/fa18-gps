#define SUBFRAME_VALID 0x2000 // replace once regmap made
#define SUBFRAME 0x2004 // replace once regmap made

struct rcv_params {
  float IODE;
  float C_rs;
  float delta_n;
  float M_0;
  float C_uc;
  float e;
  float C_us;
  float sqrt_A;
  float t_oe;
  float C_ic;
  float Omega_0;
  float C_is;
  float i_0;
  float C_rc;
  float omega;
  float dot_Omega;
  float IDOT;
};

uint32_t words[10];
bool word_valid[10];

void read_subframe(uint32_t* words, bool* word_valid) {
  if (reg_read32(SUBFRAME_VALID)) {
    uint32_t curr;
    for (int i = 0; i < 10; i++) {
      curr = reg_read32(SUBFRAME_VALID + (4 * i));
      words[i] = curr & 0x3FFFFFFF;
      word_valid[i] = (curr & 0x40000000) >> 30;
    }
  }
}

void extract_params(uint32_t* words, struct rcv_params* params) {
  switch get_subframe_id(words) {
    case 1:
      break;
    case 2:
      params->IODE = (float)(words[2] >> 22);
      params->C_rs = ((float) ((*((int32_t*) (words + 2)) << 8) >> 14)) / ((float) (1 << 5));
      params->delta_n = ((float) (*((int32_t*) (words + 3)) >> 14)) / ((float) (1 << 43));

      // This part is really yikes.
      // int32_t M_0_MSB = (*((int32_t*) (words + 3)) << 18);
      // uint32_t M_0_LSB = words[4] >> 6;
      // params->M_0 = M_0_MSB | *((int32_t*) &M_0_LSB);

      params->C_uc = ((float) (*((int32_t*) (words + 5)) >> 14)) / ((float) (1 << 29));
      // Extracting e is also very yikes.
      params->C_us = ((float) (*((int32_t*) (words + 7)) >> 14)) / ((float) (1 << 29));
      // Extracting sqrt_A is also very yikes.
      params->t_oe = (float) ((*((int32_t*) (words + 9)) >> 14) << 4);
      break;
  }
}

uint32_t get_TLM(uint32_t* words) {
  return (words[0] & 0x00FFFF00) >> 8;
}

uint32_t get_time_of_week(uint32_t* words) {
  return words[1] >> 15;
}

uint32_t get_subframe_id(uint32_t* words) {
  return (words[1] & 0x00000700) >> 8;
}
