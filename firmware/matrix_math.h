#ifndef MATRIX_HEADER
#define MATRIX_HEADER

void matrix_inverse(const double m[16], double invOut[16]);
void mat_vec_dot(const double m[16], double vec[4], double vec_out[4]);

#endif
