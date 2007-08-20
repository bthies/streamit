#ifndef _WORK_STATE_H_
#define _WORK_STATE_H_

typedef struct _FILTER_fft_STATE {
  struct {
    float wn_r, wn_i;
  } c[8];
} QWORD_ALIGNED FILTER_fft_STATE;

#endif
