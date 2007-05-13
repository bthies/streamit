/*-----------------------------------------------------------------------------
 * debug.h
 *
 * Support for debugging. Automatically included.
 *---------------------------------------------------------------------------*/

#ifndef _SPULIB_DEBUG_H_
#define _SPULIB_DEBUG_H_

#if DEBUG
#define IF_DEBUG(exp) exp
#define assert(exp)   ((exp) ? (void)0 : error("assert: " #exp))
#define unreached()   error("unreached")
#else
#define IF_DEBUG(exp) ((void)0)
#define assert(exp)   ((void)0)
#define unreached()   ((void)0)
#endif

#if CHECK
#define IF_CHECK(exp) exp
#define check(exp)    ((exp) ? (void)0 : error("check: " #exp))
#else
#define IF_CHECK(exp) ((void)0)
#define check(exp)    ((void)0)
#endif

#if (CHECK && CHECK_PARAMS)
#define pcheck(exp)   check(exp)
#else
#define pcheck(exp)   ((void)0)
#endif

#if (DEBUG || CHECK)
#define error(msg) _error(msg, __FILE__, __LINE__)

void _error(const char *msg, const char *file, uint32_t line) NORETURN;
#endif

#endif
