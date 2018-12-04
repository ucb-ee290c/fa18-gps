#ifndef MATRIX_HEADER
#define MATRIX_HEADER

void matrix_inverse(const long double m[16], long double invOut[16]);
void mat_vec_dot(const long double m[16], long double vec[4], long double vec_out[4]);

#endif
