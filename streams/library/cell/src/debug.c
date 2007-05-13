/*-----------------------------------------------------------------------------
 * debug.c
 *
 * Support for debugging.
 *---------------------------------------------------------------------------*/

#include "defs.h"

#if (DEBUG || CHECK)
#include <stdio.h>

/*-----------------------------------------------------------------------------
 * _error
 *
 * Custom error function that spins forever.
 *---------------------------------------------------------------------------*/
void
_error(const char *msg, const char *file, uint32_t line)
{
  printf("%s:%d: %s\n", file, line, msg);
  while (TRUE);
}

#endif
