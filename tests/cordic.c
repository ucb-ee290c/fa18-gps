#define CORDIC_WRITE 0x2000
#define CORDIC_WRITE_COUNT 0x2008
#define CORDIC_READ 0x2100
#define CORDIC_READ_COUNT 0x2108

#include <stdio.h>
#include <math.h>

#include "mmio.h"

/**
 * Make sure these #defines are correct for your chosen parameters.
 * You'll get really strange (and wrong) results if they do not match.
 */
#define XY_WIDTH 8
#define XY_BP (XY_WIDTH - 2)
#define XY_MASK ((1L << XY_WIDTH) - 1)
#define Z_WIDTH 10
#define Z_BP (Z_WIDTH - 2)
#define Z_MASK ((1L << Z_WIDTH) - 1)

/**
 * Pack cordic fields into 64-bit unsigned integer.
 * Make sure the #defines above are correct!
 * You will need to pack into multiple uint64_t if 2 * XY_WIDTH + Z_WIDTH + 1 > 64
 */
uint64_t pack_cordic(double x, double y, double z, uint8_t vectoring) {
  int64_t xint = (int64_t)(x * (1L << XY_BP));
  int64_t yint = (int64_t)(y * (1L << XY_BP));
  int64_t zint = (int64_t)(z * (1L << Z_BP));

  uint64_t xpack = ((uint64_t)xint) & XY_MASK;
  uint64_t ypack = ((uint64_t)yint) & XY_MASK;
  uint64_t zpack = ((uint64_t)zint) & Z_MASK;
  uint64_t vpack = vectoring ? 1 : 0;

  return 
    (vpack << (2 * XY_WIDTH + Z_WIDTH)) |
    (xpack << (XY_WIDTH + Z_WIDTH))     |
    (ypack << Z_WIDTH)                  |
    zpack;
}

/*
 * Unpack output of cordic and get an integer version of x.
 * We can't printf() a double, so printing ints is the way to go.
 * To get the floating point version, divide this by 2^XY_BP
 */
int64_t unpack_cordic_x(uint64_t packed) {
  uint64_t xpack = (packed >> (XY_WIDTH + Z_WIDTH)) & XY_MASK;
  int shift = 64 - XY_WIDTH;
  return ((int64_t)(xpack << shift)) >> shift;
}

/*
 * Unpack output of cordic and get an integer version of y.
 * We can't printf() a double, so printing ints is the way to go.
 * To get the floating point version, divide this by 2^XY_BP
 */
int64_t unpack_cordic_y(uint64_t packed) {
  uint64_t ypack = (packed >> Z_WIDTH) & XY_MASK;
  int shift = 64 - XY_WIDTH;
  return ((int64_t)(ypack << shift)) >> shift;
}

/*
 * Unpack output of cordic and get an integer version of z.
 * We can't printf() a double, so printing ints is the way to go.
 * To get the floating point version, divide this by 2^Z_BP
 */
int64_t unpack_cordic_z(uint64_t packed) {
  uint64_t zpack = packed & Z_MASK;
  int shift = 64 - Z_WIDTH;
  return ((int64_t)(zpack << shift)) >> shift;
}

void printDouble(double v, int decimalDigits)
{
    if(v<0){
      printf("-");
      v = -v;
    }
    printf("%d.", (int)v);
    for (int i=1; i<=decimalDigits; i++){
      v = v - (int)v;
      v *= 10;
      printf("%d", (int)v);
    }
    printf("\n");
}

int main(void)
{
  int len_list = 3;
  double x = 1.0;
  double y = 0.0;
  double z = 0.5;
  double z_list[3]={-0.8,0.7,0.6};
  int vectoring = 0;

  for (int i=0; i <len_list; i++) {
    printf("XIN=");
    printDouble(x,10);
    printf("YIN=");
    printDouble(y,10);
    printf("ZIN=");
    printDouble(z_list[i],10);
    printf("VEC= %d \n", vectoring);
    reg_write64(CORDIC_WRITE, pack_cordic(x, y, z_list[i], vectoring));
//    printf("The digits into mem is %d (decimal) \n", pack_cordic(x, y, z, vectoring));
  }

  int regout;
  for (int i=0; i<len_list; i++) {
    regout = reg_read64(CORDIC_READ);
    printf("The binary value for x is %d ", unpack_cordic_x(regout));
    printDouble((double)unpack_cordic_x(regout)/pow(2.0, XY_BP), 10);
    printf("The binary value for y is %d ", unpack_cordic_y(regout));
    printDouble((double)unpack_cordic_y(regout)/pow(2.0, XY_BP), 10);
    printf("The binary value for z is %d ", unpack_cordic_z(regout));
    printDouble((double)unpack_cordic_z(regout)/pow(2.0, Z_BP), 10);
  }
	return 0;
}
