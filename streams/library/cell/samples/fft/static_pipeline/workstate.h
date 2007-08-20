#ifndef _WORK_STATE_H_
#define _WORK_STATE_H_

typedef struct _FILTER_fft_STATE {
  struct {
    float wn_r, wn_i;
  } c[8];
} QWORD_ALIGNED FILTER_fft_STATE;

typedef FILTER_fft_STATE FILTER_fft0_STATE;
typedef FILTER_fft_STATE FILTER_fft1_STATE;
typedef FILTER_fft_STATE FILTER_fft2_STATE;
typedef FILTER_fft_STATE FILTER_fft3_STATE;
typedef FILTER_fft_STATE FILTER_fft4_STATE;
typedef FILTER_fft_STATE FILTER_fft5_STATE;

#endif
