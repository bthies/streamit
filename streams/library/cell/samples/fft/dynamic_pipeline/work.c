// FFTReorderSimple_512
// FFTReorderSimple_256
// FFTReorderSimple_128
// FFTReorderSimple_64
// FFTReorderSimple_32
// FFTReorderSimple_16
// FFTReorderSimple_8
// CombineDFT_4
// CombineDFT_8
// CombineDFT_16
// CombineDFT_32
// CombineDFT_64
// CombineDFT_128
// CombineDFT_256
// CombineDFT_512

#include "filterdefs.h"
#include "workstate.h"

// pop/push n
static void
FFTReorderSimple(int n, float *in, float *out)
{
  int i;
  for (i = 0; i < n; i += 4) {
    *out++ = in[i];
    *out++ = in[i + 1];
  }
  for (i = 2; i < n; i += 4) {
    *out++ = in[i];
    *out++ = in[i + 1];
  }
}

// pop/push n
static void
CombineDFT(int n, float wn_r, float wn_i, float *in, float *out)
{
  n = n >> 1;

  int i;
  float w_r = 1;
  float w_i = 0;

  for (i = 0; i < n; i += 2) {
    float y0_r = in[i];
    float y0_i = in[i + 1];
    float y1_r = in[n + i];
    float y1_i = in[n + i + 1];
    float y1w_r = y1_r * w_r - y1_i * w_i;
    float y1w_i = y1_r * w_i + y1_i * w_r;
    float w_r_next;
    float w_i_next;

    out[i] = y0_r + y1w_r;
    out[i + 1] = y0_i + y1w_i;
    out[n + i] = y0_r - y1w_r;
    out[n + i + 1] = y0_i - y1w_i;

    w_r_next = w_r * wn_r - w_i * wn_i;
    w_i_next = w_r * wn_i + w_i * wn_r;
    w_r = w_r_next;
    w_i = w_i_next;
  }
}

#define FILTER_NAME FFTReorderSimple_512
#define ITEM_TYPE float
#include "beginfilter.h"

BEGIN_WORK_FUNC
{
  int n = 512;
  FFTReorderSimple(n, get_input(), get_output());
  advance_input(n);
  advance_output(n);
}
END_WORK_FUNC

#include "endfilter.h"

#define FILTER_NAME FFTReorderSimple_256
#define ITEM_TYPE float
#include "beginfilter.h"

BEGIN_WORK_FUNC
{
  int n = 256;
  FFTReorderSimple(n, get_input(), get_output());
  advance_input(n);
  advance_output(n);
}
END_WORK_FUNC

#include "endfilter.h"

#define FILTER_NAME FFTReorderSimple_128
#define ITEM_TYPE float
#include "beginfilter.h"

BEGIN_WORK_FUNC
{
  int n = 128;
  FFTReorderSimple(n, get_input(), get_output());
  advance_input(n);
  advance_output(n);
}
END_WORK_FUNC

#include "endfilter.h"

#define FILTER_NAME FFTReorderSimple_64
#define ITEM_TYPE float
#include "beginfilter.h"

BEGIN_WORK_FUNC
{
  int n = 64;
  FFTReorderSimple(n, get_input(), get_output());
  advance_input(n);
  advance_output(n);
}
END_WORK_FUNC

#include "endfilter.h"

#define FILTER_NAME FFTReorderSimple_32
#define ITEM_TYPE float
#include "beginfilter.h"

BEGIN_WORK_FUNC
{
  int n = 32;
  FFTReorderSimple(n, get_input(), get_output());
  advance_input(n);
  advance_output(n);
}
END_WORK_FUNC

#include "endfilter.h"

#define FILTER_NAME FFTReorderSimple_16
#define ITEM_TYPE float
#include "beginfilter.h"

BEGIN_WORK_FUNC
{
  int n = 16;
  FFTReorderSimple(n, get_input(), get_output());
  advance_input(n);
  advance_output(n);
}
END_WORK_FUNC

#include "endfilter.h"

#define FILTER_NAME FFTReorderSimple_8
#define ITEM_TYPE float
#include "beginfilter.h"

BEGIN_WORK_FUNC
{
  int n = 8;
  FFTReorderSimple(n, get_input(), get_output());
  advance_input(n);
  advance_output(n);
}
END_WORK_FUNC

#include "endfilter.h"

#define FILTER_NAME CombineDFT_4
#define HAS_STATE
#define ITEM_TYPE float
#include "beginfilter.h"

BEGIN_WORK_FUNC
{
  int n = 4;
  CombineDFT(n, state.wn_r, state.wn_i, get_input(), get_output());
  advance_input(n);
  advance_output(n);
}
END_WORK_FUNC

#include "endfilter.h"

#define FILTER_NAME CombineDFT_8
#define HAS_STATE
#define ITEM_TYPE float
#include "beginfilter.h"

BEGIN_WORK_FUNC
{
  int n = 8;
  CombineDFT(n, state.wn_r, state.wn_i, get_input(), get_output());
  advance_input(n);
  advance_output(n);
}
END_WORK_FUNC

#include "endfilter.h"

#define FILTER_NAME CombineDFT_16
#define HAS_STATE
#define ITEM_TYPE float
#include "beginfilter.h"

BEGIN_WORK_FUNC
{
  int n = 16;
  CombineDFT(n, state.wn_r, state.wn_i, get_input(), get_output());
  advance_input(n);
  advance_output(n);
}
END_WORK_FUNC

#include "endfilter.h"

#define FILTER_NAME CombineDFT_32
#define HAS_STATE
#define ITEM_TYPE float
#include "beginfilter.h"

BEGIN_WORK_FUNC
{
  int n = 32;
  CombineDFT(n, state.wn_r, state.wn_i, get_input(), get_output());
  advance_input(n);
  advance_output(n);
}
END_WORK_FUNC

#include "endfilter.h"

#define FILTER_NAME CombineDFT_64
#define HAS_STATE
#define ITEM_TYPE float
#include "beginfilter.h"

BEGIN_WORK_FUNC
{
  int n = 64;
  CombineDFT(n, state.wn_r, state.wn_i, get_input(), get_output());
  advance_input(n);
  advance_output(n);
}
END_WORK_FUNC

#include "endfilter.h"

#define FILTER_NAME CombineDFT_128
#define HAS_STATE
#define ITEM_TYPE float
#include "beginfilter.h"

BEGIN_WORK_FUNC
{
  int n = 128;
  CombineDFT(n, state.wn_r, state.wn_i, get_input(), get_output());
  advance_input(n);
  advance_output(n);
}
END_WORK_FUNC

#include "endfilter.h"

#define FILTER_NAME CombineDFT_256
#define HAS_STATE
#define ITEM_TYPE float
#include "beginfilter.h"

BEGIN_WORK_FUNC
{
  int n = 256;
  CombineDFT(n, state.wn_r, state.wn_i, get_input(), get_output());
  advance_input(n);
  advance_output(n);
}
END_WORK_FUNC

#include "endfilter.h"

#define FILTER_NAME CombineDFT_512
#define HAS_STATE
#define ITEM_TYPE float
#include "beginfilter.h"

BEGIN_WORK_FUNC
{
  int n = 512;
  CombineDFT(n, state.wn_r, state.wn_i, get_input(), get_output());
  advance_input(n);
  advance_output(n);
}
END_WORK_FUNC

#include "endfilter.h"
