/*-----------------------------------------------------------------------------
 * main.c
 *
 * Basic SPU program that starts up the event loop.
 *---------------------------------------------------------------------------*/

#include "defs.h"

void dep_init();
void dep_execute() NORETURN;

int
main(uint64_t speid, uint64_t argp, uint64_t envp)
{
  UNUSED_PARAM(speid);
  UNUSED_PARAM(argp);
  UNUSED_PARAM(envp);

  dep_init();
  dep_execute();
}
