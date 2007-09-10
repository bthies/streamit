#ifndef JOINER_SCALE
#define JOINER_SCALE 1
#endif

#ifndef _SPULIB_BEGINFILTER_H_
#define BEGINFILTER_INCLUDED
#include "beginfilter.h"
#endif

BEGIN_WORK_FUNC
{
  static const uint32_t rates[] = JOINER_RATES;

  for (uint32_t k = 0; k < JOINER_SCALE; k++) {
    for (uint32_t i = 0; i < NUM_INPUT_TAPES; i++) {
      for (uint32_t j = 0; j < rates[i]; j++) {
        push(pop(i));
      }
    }
  }
}
END_WORK_FUNC

#ifdef BEGINFILTER_INCLUDED
#undef BEGINFILTER_INCLUDED
#include "endfilter.h"
#endif

#undef JOINER_RATES
#undef JOINER_SCALE
