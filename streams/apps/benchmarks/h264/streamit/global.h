#ifndef __GLOBAL_H
#define __GLOBAL_H

#include <math.h>
#include "structs.h"
#include <StreamItVectorLib.h>

#define max(A,B) (((A)>(B))?(A):(B))
#define min(A,B) (((A)<(B))?(A):(B))

extern void __global__init();

#endif // __GLOBAL_H
