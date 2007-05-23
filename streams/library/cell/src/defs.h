/*-----------------------------------------------------------------------------
 * defs.h
 *
 * Basic definitions. Include in every source file. Do not include in headers.
 *---------------------------------------------------------------------------*/

#ifndef _SPULIB_DEFS_H_
#define _SPULIB_DEFS_H_

#if (defined(__SPU__) && !defined(SPULIB_INTERNAL))
#define SPULIB_INTERNAL
#endif

#include <stdint.h>
#include <stddef.h>
#ifdef __SPU__  // SPU
#include <spu_intrinsics.h>
#else           // PPU
#include <ppu_intrinsics.h>
#include <altivec.h>
#endif
#include <vec_literal.h>
#ifdef SPULIB_CONFIG_FILE
#include SPULIB_CONFIG_FILE
#else
#include "config.h"
#endif

typedef uint8_t bool_t;

#define FALSE 0
#define TRUE  1

typedef uint32_t uintptr32_t;
typedef uint64_t uintptr64_t;

#ifdef __SPU__  // SPU
typedef void         *LS_ADDRESS;
typedef uintptr32_t   MEM_ADDRESS;
#else           // PPU
typedef uintptr32_t   LS_ADDRESS;
typedef void         *MEM_ADDRESS;
#endif

typedef vector unsigned char vec16_uint8_t;
typedef vector unsigned int vec4_uint32_t;

// Type for DMA tags.
typedef uint8_t SPU_DMA_TAG;

#ifdef __SPU__
#define DMA_TAG SPU_DMA_TAG
#endif

#define INLINE __inline__

// Compile-time assertions allowing any constant expression. Before using in a
// file, first #undef C_FILE, then #define C_FILE to a unique identifier.
#define C_ASSERT(exp)               _C_ASSERT(exp, C_FILE, __LINE__)
#define _C_ASSERT(exp, file, line)  __C_ASSERT(exp, file, line)
#define __C_ASSERT(exp, file, line) \
  typedef int _c_assert_##file##_##line[((exp) ? 0 : -1)]

// Returns number of elements in an array.
#define arraysize(exp) (sizeof(exp) / sizeof((exp)[0]))

// Performs a volatile read on an lvalue.
#define volatile_read(exp) (*(volatile typeof(exp) *)&(exp))

// Rounding macros - rounds a to a multiple of n/(mask + 1) (must be power of
// 2). For rounding up, n/mask must not have side-effects.
#define ROUND_DOWN_MASK(a, mask)  ((a) & ~(mask))
#define ROUND_UP_MASK(a, mask)    (((a) + (mask)) & ~(mask))
#define ROUND_DOWN(a, n)          ROUND_DOWN_MASK(a, n - 1)
#define ROUND_UP(a, n)            ROUND_UP_MASK(a, n - 1)

#define QWORD_SIZE  16
#define QWORD_MASK  15
#define QWORD_SHIFT 4

#define CACHE_SIZE  128
#define CACHE_MASK  127
#define LS_SIZE     (256 * 1024)

// Alignment macros.
#define ALIGNED(n)    __attribute__((aligned((n))))
#define QWORD_ALIGNED ALIGNED(QWORD_SIZE)
#define CACHE_ALIGNED ALIGNED(CACHE_SIZE)

#define PACKED        __attribute__((packed))

// Branch hint macros.
#define LIKELY(exp)   __builtin_expect(exp, TRUE)
#define UNLIKELY(exp) __builtin_expect(exp, FALSE)

#define NORETURN __attribute__((noreturn))

#define UNUSED_PARAM(a) ((void)a)

/*-----------------------------------------------------------------------------
 * count_ls_zeros
 *
 * Returns the number of 0s that occur before the first 1 in a number, starting
 * from the LSB. Undefined if called with 0.
 *---------------------------------------------------------------------------*/
static INLINE uint32_t
count_ls_zeros(uint32_t a)
{
#ifdef __IBMC__ // XLC
  return __cnttz4(a);
#else           // GCC
  return __builtin_ctz(a);
#endif
}

#ifndef __SPU__ // PPU

/*-----------------------------------------------------------------------------
 * write64
 *
 * Generates a 64-bit store as a single instruction. For 32-bit targets the
 * compiler normally does this as two 32-bit stores.
 *---------------------------------------------------------------------------*/
static INLINE void
write64(volatile uint64_t *addr, uint64_t data)
{
  union {
    uint32_t word[2];
    uint64_t dword;
  } d;
  uint32_t unused;
  d.dword = data;
  asm volatile(
    ""     "rldimi  %0,%2,32,0"
    "\n\t" "std     %0,%3"
    : "=&r"(unused)
    : "0"(d.word[1]),
      "r"(d.word[0]),
      "m"(*addr)
    );
}

#endif

#include "debug.h"

#endif
