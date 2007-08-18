#include "filterdefs.h"
#include "workstate.h"

#define FILTER_NAME fft
#define HAS_STATE
#define ITEM_TYPE float
#include "beginfilter.h"

// m iters, pop/push 2n floats per iter
static void
FFTReorderSimple(int m, int n, float *in, float *out)
{
  n *= 2;
  for (; m != 0; m--) {
    int i;
    for (i = 0; i < n; i += 4) {
      *out++ = in[i];
      *out++ = in[i + 1];
    }
    for (i = 2; i < n; i += 4) {
      *out++ = in[i];
      *out++ = in[i + 1];
    }
    in += n;
  }
}

// m iters, pop/push 2n floats per iter
static void
CombineDFT(int m, int n, float wn_r, float wn_i, float *in, float *out)
{
  for (; m != 0; m--) {
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

    in += 2 * n;
    out += 2 * n;
  }
}

// one iteration does pop 512/push 512
BEGIN_WORK_FUNC
{
  float b0[512];
  float b1[512];
  FFTReorderSimple(  1, 256, get_input(), b0);
  FFTReorderSimple(  2, 128, b0, b1);
  FFTReorderSimple(  4,  64, b1, b0);
  FFTReorderSimple(  8,  32, b0, b1);
  FFTReorderSimple( 16,  16, b1, b0);
  FFTReorderSimple( 32,   8, b0, b1);
  FFTReorderSimple( 64,   4, b1, b0);
  CombineDFT(128,   2, state.c[0].wn_r, state.c[0].wn_i, b0, b1);
  CombineDFT( 64,   4, state.c[1].wn_r, state.c[1].wn_i, b1, b0);
  CombineDFT( 32,   8, state.c[2].wn_r, state.c[2].wn_i, b0, b1);
  CombineDFT( 16,  16, state.c[3].wn_r, state.c[3].wn_i, b1, b0);
  CombineDFT(  8,  32, state.c[4].wn_r, state.c[4].wn_i, b0, b1);
  CombineDFT(  4,  64, state.c[5].wn_r, state.c[5].wn_i, b1, b0);
  CombineDFT(  2, 128, state.c[6].wn_r, state.c[6].wn_i, b0, b1);
  CombineDFT(  1, 256, state.c[7].wn_r, state.c[7].wn_i, b1, get_output());
  advance_input(512);
  advance_output(512);
}
END_WORK_FUNC
