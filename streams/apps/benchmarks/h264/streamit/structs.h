#ifndef __STRUCTS_H
#define __STRUCTS_H
typedef struct __Complex {
	float real;
	float imag;
} Complex;
typedef struct __float2 {
	float x;
	float y;
} float2;
typedef struct __float3 {
	float x;
	float y;
	float z;
} float3;
typedef struct __float4 {
	float x;
	float y;
	float z;
	float w;
} float4;
typedef int bit;
#ifndef round
#define round(x) (floor((x)+0.5))
#endif
#endif // __STRUCTS_H
