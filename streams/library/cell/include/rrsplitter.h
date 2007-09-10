#ifndef SPLITTER_SCALE
#define SPLITTER_SCALE 1
#endif

#ifndef _SPULIB_BEGINFILTER_H_
#define BEGINFILTER_INCLUDED
#include "beginfilter.h"
#endif

BEGIN_WORK_FUNC
{
  static const uint32_t rates[] = SPLITTER_RATES;

  for (uint32_t k = 0; k < SPLITTER_SCALE; k++) {
    for (uint32_t i = 0; i < NUM_OUTPUT_TAPES; i++) {
      for (uint32_t j = 0; j < rates[i]; j++) {
        push(i, pop());
      }
    }
  }
}
END_WORK_FUNC

#ifdef BEGINFILTER_INCLUDED
#undef BEGINFILTER_INCLUDED
#include "endfilter.h"
#endif

#undef SPLITTER_RATES
#undef SPLITTER_SCALE
