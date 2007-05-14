#ifndef _STREAMIT_VECTORIZATION_ 
#define _STREAMIT_VECTORIZATION_

/* 
 * Support code for --cell-vector-library switch.  
 *
 * For compiling to ppu need something like:
 *   ppu32-gcc str.c -I. -I /opt/ibm/cell-sdk/prototype/sysroot/usr/ppu/include -lm
 * For compiling to spu need something like:
 *   spu-gcc str.c  -I. -I /opt/ibm/cell-sdk/prototype/sysroot/usr/spu/include -L /opt/cell/toolchain-3.3/spu/lib -lm
 */

#if ! (defined (__SPU__) || defined (__ALTIVEC__))
#error "error: including vectorization.h but not compiling for __SPU__ or __ALTIVEC__"
#endif 

#ifdef __SPU__
#include <spu_intrinsics.h>
#endif
#ifdef __ALTIVEC__
#include <altivec.h>
#endif

/* shift left by integer */

static __inline  vector signed int _lsl_v4i (vector signed int x, unsigned int count) {
#ifdef __SPU__
  vector signed int retval = spu_sl(x,count);
  return retval;
#elif defined __PPU__
  vector signed int retval = vec_sl(x,vec_splat_u32(count));
  return retval;
#endif
}

/*shift right logical  by integer */

static  __inline vector signed int  _srl_v4i (vector signed int x, unsigned int count) {
#ifdef __SPU__
  vector signed int retval = spu_rlmask(x,count);
  return retval;
#elif defined __PPU__
  return vec_sr(x,vec_splat_u32(count));
#endif
}

/* shift right arithmetic by integer */

static inline vector signed int _sra_v4i (vector signed int x, unsigned int count) {
#ifdef __SPU__
  vector signed int retval = spu_rlmaska(x,count);
  return retval;
#elif defined __PPU__
  return vec_sra(x,vec_splat_u32(count));
#endif
}

/* available as library function: how to override? */
/* #ifndef fabsf_v */
/* static __inline__ vector float fabsf_v(vector float value) */
/* { */
/* #ifdef __SPU__ */
/*   return (vector float)spu_andc((vector unsigned int)(value),  */
/* 				VEC_SPLAT_U32(0x80000000)); */
/* #else */
/*   return (vec_abs(value)); */
/* #endif */
/* } */
/* #endif */

static inline vector signed int _min_v4i (vector signed int x,
					  vector signed int y) {
#ifdef __SPU__
  vector unsigned int pattern = spu_cmpgt(x,y);
  return spu_sel(y,x,pattern);
#elif defined __PPU__
  return vec_min(x,y);
#endif
}

static inline vector signed int _max_v4i (vector signed int x,
					  vector signed int y) {
#ifdef __SPU__
 vector unsigned int pattern = spu_cmpgt(x,y);
  return spu_sel(x,y,pattern);
#elif defined __PPU__
  return vec_max(x,y);
#endif
}

static inline vector signed int _abs_v4i (vector signed int x) {
#ifdef __SPU__
  //  __v_4i zero = spu_splats(0);
  //  __v_4i minusx = spu_sub(zero,x);
  //  return _max_v4i(x,minusx);
  // above takes 4 instructions, can bit twiddle for 3 instructions:

  /*
    if negative: want to negate (complement and increment) so
    xor with -1 to complement and subtract -1 to increment.
    if positive, want noops, so
    xor with 0 is identity operation, and subtract 0 is identity operation.
   */
  vector signed int signbit =  spu_rlmaska(x,-31);
  vector signed int retval = spu_sub(spu_xor(x,signbit), signbit);
  return retval;
#elif defined __PPU__
  return vec_abs(x);
#endif
}


/* // floating division: not supported as "/" in xlc version 8.1 */
/* // or rather */
/* static __inline__ vector float _divide_v4f (vector float x, vector float y) { */
/* #ifdef __GNU__ */
/*   return x / y; */
/* #else */
/* #include <divide_v.h> */
/*   return _divide_v(x,y); */
/* #endif */
/* } */


/* // integer division: not supportted as "/" in in xlc version 8.1 */
/* // header is cell-sdk-1.1/src/lib/math/divide_i_v.h */
/* #include <divide_i_v.h> */
/* static __inline__ __v_4i _divide_v4i (__v_4i x, __v_4i y) { */
/*   return _divide_i_v(x,y); */
/* } */

// return second parameter oe negated second parameter such that sign
// of returned value is same as sign of first parameter
static inline vector signed int _signedOfFirst_v4i (vector signed int x, 
						    vector signed int y) {
#ifdef __SPU__
  // to be implemented
  return y;
#elif defined __PPU__
  // vec_xor(x,y) has high-order (sign) bit set if x,y differ in sign.
  // so 3rd parameter of sel is true if sign bits differ.
  vector signed int zero = vec_splat_s32(0);
  return vec_sel(vec_sub(zero,y), y, vec_cmpgt(zero, vec_xor(x,y)));
#endif
}

// return second parameter oe negated second parameter such that sign
// of returned value is same as sign of first parameter
static inline vector float _signedOfFirst_v4f (vector float x, 
					       vector float y) {
#ifdef __SPU__
  // to be implemented
  return y;
#elif defined __PPU__
  // operating on bit representations
  vector unsigned int xu = (vector unsigned int)x;
  vector unsigned int yu = (vector unsigned int)y;
  vector unsigned int signbit = vec_splat_u32(0x80000000);
  // 0 if same sign, signbit if different.
  vector unsigned int hibit = vec_and(vec_xor(xu,yu), signbit);
  vector unsigned int retval_us = vec_add(yu, signbit);
  vector float result = (vector float)retval_us;
  return result;
#endif
}

static inline vector signed int  _modulus_v4i (vector signed int x,
					       vector signed int y) {
  vector signed int quotient = x / y; //_divide_v4i(x, y);
  vector signed int remainder = x - quotient * y;
  /* the above is C semantics: sign not specified.
   * probably want java semantics: sign of result = sign of x
   */
  return _signedOfFirst_v4i(x,remainder);
}

static inline vector float _modulus_v4f (vector float x,
					 vector float y) {
  vector float quotient = x / y;
  vector float remainder = x - quotient * y;
  /* the above is C semantics: sign not specified.
   * probably want java semantics: sign of result = sign of x
   */
  return _signedOfFirst_v4f(x,remainder);
}



/* include supported vector operations in cell-sdk/src/lib/math */
/* (2007: /opt/cell/toolchain-3.3/sysroot/usr/include/simdmath.h) */

// simdmath.h defines the XXXf4 functions.
// the individual header files define the _XXXf4 macros with inline expansion.
#include <simdmath.h>
//#include <acosf4.h>
//#include <asinf4.h>
//#include <atanf4.h>

// atan2f4 not supplied.  Compute with extra division and supplied atan
// rather than computing directly: what accuracy is lost?
static inline vector float atan2_v(vector float y, vector float x) {
  vector float retval = atanf4(y / x);
  return retval;
}

//#include <ceilf4.h>
//#include <cosf4.h>
//#include <coshf4.h>
//#include <expf4.h>
//#include <fabsf4.h>
//#include <floorf4.h>
//#include <fmaxf4.h>
//#include <fminf4.h>
//#include <fmodf4.h>
//#include <frexpf4.h>
//#include <log10f4.h>
//#include <logf4.h>
//#include <roundf4.h>
//#include <sinf4.h>
//#include <sinhf4.h>
//#include <tanf4.h>
// tanhf4.h not currently supplied

#endif
