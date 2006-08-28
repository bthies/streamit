/*
 * Copyright 2006 by the Massachusetts Institute of Technology.
 *
 * Permission to use, copy, modify, and distribute this
 * software and its documentation for any purpose and without
 * fee is hereby granted, provided that the above copyright
 * notice appear in all copies and that both that copyright
 * notice and this permission notice appear in supporting
 * documentation, and that the name of M.I.T. not be used in
 * advertising or publicity pertaining to distribution of the
 * software without specific, written prior permission.
 * M.I.T. makes no representations about the suitability of
 * this software for any purpose.  It is provided "as is"
 * without express or implied warranty.
 */
//Defining class StreamItVectorLib
#ifndef __StreamItVectorLib_H__
#define __StreamItVectorLib_H__

#include <math.h>

typedef unsigned BOOL;
#define TRUE 1
#define FALSE 0

inline float2 StreamItVectorLib_add2 ( float2 a , float2 b ) { 
  float2 result; 
  result.x = a.x + b.x ; 
  result.y = a.y + b.y ; 
  return result ; 
  
} 

inline float3 StreamItVectorLib_add3 ( float3 a , float3 b ) { 
  float3 result; 
  result.x = a.x + b.x ; 
  result.y = a.y + b.y ; 
  result.z = a.z + b.z ; 
  return result ; 
  
} 

inline float4 StreamItVectorLib_add4 ( float4 a , float4 b ) { 
  float4 result; 
  result.x = a.x + b.x ; 
  result.y = a.y + b.y ; 
  result.z = a.z + b.z ; 
  result.w = a.w + b.w ; 
  return result ; 
  
} 

inline float2 StreamItVectorLib_sub2 ( float2 a , float2 b ) { 
  float2 result; 
  result.x = a.x - b.x ; 
  result.y = a.y - b.y ; 
  return result ; 
  
} 

inline float3 StreamItVectorLib_sub3 ( float3 a , float3 b ) { 
  float3 result; 
  result.x = a.x - b.x ; 
  result.y = a.y - b.y ; 
  result.z = a.z - b.z ; 
  return result ; 
  
} 

inline float4 StreamItVectorLib_sub4 ( float4 a , float4 b ) { 
  float4 result; 
  result.x = a.x - b.x ; 
  result.y = a.y - b.y ; 
  result.z = a.z - b.z ; 
  result.w = a.w - b.w ; 
  return result ; 
  
} 

inline float2 StreamItVectorLib_mul2 ( float2 a , float2 b ) { 
  float2 result; 
  result.x = a.x * b.x ; 
  result.y = a.y * b.y ; 
  return result ; 
  
} 

inline float3 StreamItVectorLib_mul3 ( float3 a , float3 b ) { 
  float3 result; 
  result.x = a.x * b.x ; 
  result.y = a.y * b.y ; 
  result.z = a.z * b.z ; 
  return result ; 
  
} 

inline float4 StreamItVectorLib_mul4 ( float4 a , float4 b ) { 
  float4 result; 
  result.x = a.x * b.x ; 
  result.y = a.y * b.y ; 
  result.z = a.z * b.z ; 
  result.w = a.w * b.w ; 
  return result ; 
  
} 

inline float2 StreamItVectorLib_div2 ( float2 a , float2 b ) { 
  float2 result; 
  result.x = a.x / b.x ; 
  result.y = a.y / b.y ; 
  return result ; 
  
} 

inline float3 StreamItVectorLib_div3 ( float3 a , float3 b ) { 
  float3 result; 
  result.x = a.x / b.x ; 
  result.y = a.y / b.y ; 
  result.z = a.z / b.z ; 
  return result ; 
  
} 

inline float4 StreamItVectorLib_div4 ( float4 a , float4 b ) { 
  float4 result; 
  result.x = a.x / b.x ; 
  result.y = a.y / b.y ; 
  result.z = a.z / b.z ; 
  result.w = a.w / b.w ; 
  return result ; 
  
} 

inline float2 StreamItVectorLib_neg2 ( float2 a ) { 
  float2 result; 
  result.x = - a.x ; 
  result.y = - a.y ; 
  return result ; 
  
} 

inline float3 StreamItVectorLib_neg3 ( float3 a ) { 
  float3 result; 
  result.x = - a.x ; 
  result.y = - a.y ; 
  result.z = - a.z ; 
  return result ; 
  
} 

inline float4 StreamItVectorLib_neg4 ( float4 a ) { 
  float4 result; 
  result.x = - a.x ; 
  result.y = - a.y ; 
  result.z = - a.z ; 
  result.w = - a.w ; 
  return result ; 
  
} 

inline float2 StreamItVectorLib_floor2 ( float2 a ) { 
  float2 result; 
  result.x = ( float ) floor ( a.x ) ; 
  result.y = ( float ) floor ( a.y ) ; 
  return result ; 
  
} 

inline float3 StreamItVectorLib_floor3 ( float3 a ) { 
  float3 result; 
  result.x = ( float ) floor ( a.x ) ; 
  result.y = ( float ) floor ( a.y ) ; 
  result.z = ( float ) floor ( a.z ) ; 
  return result ; 
  
} 

inline float4 StreamItVectorLib_floor4 ( float4 a ) { 
  float4 result; 
  result.x = ( float ) floor ( a.x ) ; 
  result.y = ( float ) floor ( a.y ) ; 
  result.z = ( float ) floor ( a.z ) ; 
  result.w = ( float ) floor ( a.w ) ; 
  return result ; 
  
} 

inline float2 StreamItVectorLib_normalize2 ( float2 a ) { 
  float t = ( float ) sqrt ( a.x * a.x + a.y * a.y ) ; 
  float2 result; 
  result.x = a.x / t ; 
  result.y = a.y / t ; 
  return result ; 
  
} 

inline float3 StreamItVectorLib_normalize3 ( float3 a ) { 
  float t = ( float ) sqrt ( a.x * a.x + a.y * a.y + a.z * a.z ) ; 
  float3 result; 
  result.x = a.x / t ; 
  result.y = a.y / t ; 
  result.z = a.z / t ; 
  return result ; 
  
} 

inline float4 StreamItVectorLib_normalize4 ( float4 a ) { 
  float t = ( float ) sqrt ( a.x * a.x + a.y * a.y + a.z * a.z + a.w * a.w ) ; 
  float4 result; 
  result.x = a.x / t ; 
  result.y = a.y / t ; 
  result.z = a.z / t ; 
  result.w = a.w / t ; 
  return result ; 
  
} 

inline float2 StreamItVectorLib_addScalar2 ( float2 a , float sc ) { 
  float2 result; 
  result.x = a.x + sc ; 
  result.y = a.y + sc ; 
  return result ; 
  
} 

inline float3 StreamItVectorLib_addScalar3 ( float3 a , float sc ) { 
  float3 result; 
  result.x = a.x + sc ; 
  result.y = a.y + sc ; 
  result.z = a.z + sc ; 
  return result ; 
  
} 

inline float4 StreamItVectorLib_addScalar4 ( float4 a , float sc ) { 
  float4 result; 
  result.x = a.x + sc ; 
  result.y = a.y + sc ; 
  result.z = a.z + sc ; 
  result.w = a.w + sc ; 
  return result ; 
  
} 

inline float2 StreamItVectorLib_subScalar2 ( float2 a , float sc ) { 
  float2 result; 
  result.x = a.x - sc ; 
  result.y = a.y - sc ; 
  return result ; 
  
} 

inline float3 StreamItVectorLib_subScalar3 ( float3 a , float sc ) { 
  float3 result; 
  result.x = a.x - sc ; 
  result.y = a.y - sc ; 
  result.z = a.z - sc ; 
  return result ; 
  
} 

inline float4 StreamItVectorLib_subScalar4 ( float4 a , float sc ) { 
  float4 result; 
  result.x = a.x - sc ; 
  result.y = a.y - sc ; 
  result.z = a.z - sc ; 
  result.w = a.w - sc ; 
  return result ; 
  
} 

inline float2 StreamItVectorLib_scale2 ( float2 a , float sc ) { 
  float2 result; 
  result.x = a.x * sc ; 
  result.y = a.y * sc ; 
  return result ; 
  
} 

inline float3 StreamItVectorLib_scale3 ( float3 a , float sc ) { 
  float3 result; 
  result.x = a.x * sc ; 
  result.y = a.y * sc ; 
  result.z = a.z * sc ; 
  return result ; 
  
} 

inline float4 StreamItVectorLib_scale4 ( float4 a , float sc ) { 
  float4 result; 
  result.x = a.x * sc ; 
  result.y = a.y * sc ; 
  result.z = a.z * sc ; 
  result.w = a.w * sc ; 
  return result ; 
  
} 

inline float2 StreamItVectorLib_scaleInv2 ( float2 a , float sc ) { 
  float2 result; 
  result.x = a.x / sc ; 
  result.y = a.y / sc ; 
  return result ; 
  
} 

inline float3 StreamItVectorLib_scaleInv3 ( float3 a , float sc ) { 
  float3 result; 
  result.x = a.x / sc ; 
  result.y = a.y / sc ; 
  result.z = a.z / sc ; 
  return result ; 
  
} 

inline float4 StreamItVectorLib_scaleInv4 ( float4 a , float sc ) { 
  float4 result; 
  result.x = a.x / sc ; 
  result.y = a.y / sc ; 
  result.z = a.z / sc ; 
  result.w = a.w / sc ; 
  return result ; 
  
} 

inline float StreamItVectorLib_sqrtDist2 ( float2 a , float2 b ) { 
  float x1 = a.x - b.x ; 
  float y1 = a.y - b.y ; 
  return ( float ) sqrt ( x1 * x1 + y1 * y1 ) ; 
  
} 

inline float StreamItVectorLib_sqrtDist3 ( float3 a , float3 b ) { 
  float x1 = a.x - b.x ; 
  float y1 = a.y - b.y ; 
  float z1 = a.z - b.z ; 
  return ( float ) sqrt ( x1 * x1 + y1 * y1 + z1 * z1 ) ; 
  
} 

inline float StreamItVectorLib_sqrtDist4 ( float4 a , float4 b ) { 
  float x1 = a.x - b.x ; 
  float y1 = a.y - b.y ; 
  float z1 = a.z - b.z ; 
  float w1 = a.w - b.w ; 
  return ( float ) sqrt ( x1 * x1 + y1 * y1 + z1 * z1 + w1 * w1 ) ; 
  
} 

inline float3 StreamItVectorLib_cross3 ( float3 a , float3 b ) { 
  float3 result; 
  result.x = a.y * b.z - b.y * a.z ; 
  result.y = a.z * b.x - b.z * a.x ; 
  result.z = a.x * b.y - b.x * a.y ; 
  return result ; 
  
} 

inline float StreamItVectorLib_dot3 ( float3 a , float3 b ) { 
  return ( a.x * b.x ) + ( a.y * b.y ) + ( a.z * b.z ) ; 
  
} 

inline float2 StreamItVectorLib_max2 ( float2 a , float2 b ) { 
  float2 result; 
  result.x = ( b.x < a.x ) ? a.x : b.x ; 
  result.y = ( b.y < a.y ) ? a.y : b.y ; 
  return result ; 
  
} 

inline float3 StreamItVectorLib_max3 ( float3 a , float3 b ) { 
  float3 result; 
  result.x = ( b.x < a.x ) ? a.x : b.x ; 
  result.y = ( b.y < a.y ) ? a.y : b.y ; 
  result.z = ( b.z < a.z ) ? a.z : b.z ; 
  return result ; 
  
} 

inline float2 StreamItVectorLib_min2 ( float2 a , float2 b ) { 
  float2 result; 
  result.x = ( b.x > a.x ) ? a.x : b.x ; 
  result.y = ( b.y > a.y ) ? a.y : b.y ; 
  return result ; 
  
} 

inline float3 StreamItVectorLib_min3 ( float3 a , float3 b ) { 
  float3 result; 
  result.x = ( b.x > a.x ) ? a.x : b.x ; 
  result.y = ( b.y > a.y ) ? a.y : b.y ; 
  result.z = ( b.z > a.z ) ? a.z : b.z ; 
  return result ; 
  
} 

inline BOOL StreamItVectorLib_greaterThan3 ( float3 a , float3 b ) { 
  if ( b.x < a.x ) return TRUE ; 
  if ( b.y < a.y ) return TRUE ; 
  if ( b.z < a.z ) return TRUE ; 
  return FALSE ; 
  
} 

inline BOOL StreamItVectorLib_lessThan3 ( float3 a , float3 b ) { 
  if ( b.x > a.x ) return TRUE ; 
  if ( b.y > a.y ) return TRUE ; 
  if ( b.z > a.z ) return TRUE ; 
  return FALSE ; 
  
} 

inline BOOL StreamItVectorLib_equals3 ( float3 a , float3 b ) { 
  if ( b.x != a.x ) return FALSE ; 
  if ( b.y != a.y ) return FALSE ; 
  if ( b.z != a.z ) return FALSE ; 
  return TRUE ; 
  
} 

#endif /* __StreamItVectorLib_H__ */
