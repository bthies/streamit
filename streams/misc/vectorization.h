#ifndef _STREAMIT_VECTORIZATION_ 
#define _STREAMIT_VECTORIZATION_

/* 
 * support code for --cell-vector-library switch.  
 */

/* from cell-sdk/sysroot/usr/lib/gcc/spu/4.0.2/include */

#if ! (defined (__SPU__) || defined (__PPU__))
#error "error: including vectorization.h but not compiling for __SPU__ or __PPU__"
#endif 


#ifdef __SPU__
#include <spu_intrinsics.h>
#endif

/* vector literals from cell-sdk/src/include */

#include <vec_literal.h>


/* include supported vector operations in cell-sdk/src/lib/math */

// libmath.h defines the XXXf_v functions.
// the individual header files define the _XXXf_v macros with inline expansion.
#include <libmath.h>
//#include <acosf_v.h>
//#include <asinf_v.h>
//#include <atanf_v.h>

// atan2f_v not supplied.  Compute with extra division and supplied atan
// rather than computing directly: what accuracy is lost?
static __inline vector float atan2_v(vector float y, vector float x) {
  return atan_v(y/x);
}

//#include <ceilf_v.h>
//#include <cosf_v.h>
// coshv_v not currently supplied
//#include <expf_v.h>
//#include <fabsf_v.h>
//#include <floorf_v.h>
//#include <fmaxf_v.h>
//#include <fminf_v.h>
//#include <fmodf_v.h>
//#include <frexpf_v.h>
//#include <log10f_v.h>
//#include <logf_v.h>
//#include <roundf_v.h>
//#include <sinf_v.h>
// sinhf_v not currently supplied.
//#include <tanf_v.h>
// tanhf_v.h not currently supplied

/* shift left by integer */

static __inline vector int _lsl_v4i (vector int x, int count) {
#ifdef __SPU__
  return spu_sl(x,count);
#elif defined __PPU__
  return vec_sl(x,count);
#endif
}

/*shift right logical  by integer */

static __inline vector int _srl_v4i (vector int x, int count) {
#ifdef __SPU__
  return spu_rlmask(x,count);
#elif defined __PPU__
  return vec_sr(x,count);
#endif
}

/* shift right arithmetic by integer */

static __inline vector int _sra_v4i (vector int x, int count) {
#ifdef __SPU__
  return spu_rlmaska(x,count);
#elif defined __PPU__
  return vec_sra(x,count);
#endif
}

#ifndef fabsf_v
static __inline vector float fabsf_v(vector float value)
{
#ifdef __SPU__
  return (vector float)spu_andc((vector unsigned int)(value), 
				VEC_SPLAT_U32(0x80000000));
#else
  return (vec_abs(value));
#endif
}
#endif

static __inline vector int _min_v4i (vector int x, vector int y) {
#ifdef __SPU__
  vector int pattern = spu_cmpgt(x,y);
  return spu_sel(y,x,pattern);
#elif defined __PPU__
  return vec_min(x,y);
#endif
}

static __inline vector int _max_v4i (vector int x, vector int y) {
#ifdef __SPU__
  vector int pattern = spu_cmpgt(x,y);
  return spu_sel(x,y,pattern);
#elif defined __PPU__
  return vec_max(x,y);
#endif
}

static __inline vector int _abs_v4i (vector int x) {
#ifdef __SPU__
  //  vector int zero = spu_splats(0);
  //  vector int minusx = spu_sub(zero,x);
  //  return _max_v4i(x,minusx);
  // above takes 4 instructions, can bit twiddle for 3 instructions:

  /*
    if negative: want to negate (complement and increment) so
    xor with -1 to complement and subtract -1 to increment.
    if positive, want noops, so
    xor with 0 is identity operation, and subtract 0 is identity operation.
   */
  vector int signbit =  spu_rlmaska(x,-31);
  return spu_sub(spu_xor(x,signbit) - signbit);
#elif defined __PPU__
  return vec_abs(x);
#endif
}

/* The following algorithm performs an integer division without looping 
 * It is based on the idea that a floating division with one Newton-Raphson
 * iteration gets you almost close enough:
 * 3 * |x| >= (int)((float)|x| / (float)|y|) * |y|  >= |x|
 */

/* not implemented until i have time: correctness proof would also be nice */

// header is cell-sdk-1.1/src/lib/math/divide_i_v.h
#include <divide_i_v.h>
static __inline vector int _divide_v4i (vector int x, vector int y) {

  /* Quick & dirty: use supplied routine which loops. 
   * (but use inlined version.)
   */
  return _divide_i_v(x,y);
}

// return second parameter oe negated second parameter such that sign
// of returned value is same as sign of first parameter
static __inline vector int _signedOfFirst_v4i (vector int x, vector int y) {
#ifdef __SPU__
  // to be implemented
  return y;
#elif defined __PPU__
  // vec_xor(x,y) has high-order (sign) bit set if x,y differ in sign.
  // so 3rd parameter of sel is true if sign bits differ.
  vector int zero = vec_splat_s32(0);
  return vec_sel(vec_sub(zero,y), y, vec_cmpgt(zero, vec_xor(x,y)))
#endif
}

// return second parameter oe negated second parameter such that sign
// of returned value is same as sign of first parameter
static __inline vector float _signedOfFirst_v4f (vector float x, vector float y) {
#ifdef __SPU__
  // to be implemented
  return y;
#elif defined __PPU__
  // operating on bit representations
  vector unsigned int xu = (vector unsigned int)x;
  vector unsigned int yu = (vector unsigned int)y;
  vector unsigned int signbit = vec_splat_u32(0x80000000);
  // 0 if same sign, signbit if different.
  vector unsigned int hibit = vec_and(vec_xor(x,y), signbit);
  vector unsigned int retval_us = vec_add(y, signbit);
  vector float result = (vector float)retval_us;
  return retval_us;
#endif
}

static __inline vector int _modulus_v4i (vector int x, vector int y) {
  vector int quotient = _divide_v4i(x, y);
  vector int remainder = x - quotient * y;
  /* the above is C semantics: sign not specified.
   * probably want java semantics: sign of result = sign of x
   */
  return _signedOfFirst_v4i(x,remainder);
}

static __inline vector float _modulus_v4f (vector int x, vector int y) {
  vector int quotient = x / y;
  vector int remainder = x - quotient * y;
  /* the above is C semantics: sign not specified.
   * probably want java semantics: sign of result = sign of x
   */
  return _signedOfFirst_v4f(x,remainder);
}



#endif
