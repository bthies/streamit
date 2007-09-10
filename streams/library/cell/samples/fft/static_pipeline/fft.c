/*
 * Pipelined version of FFT with 6 filters in pipeline.
 */

#include "filterdefs.h"
#include "workstate.h"
#include <stdlib.h>
#include <string.h>
#include <stdio.h>
#include "spulib.h"
#include "spusymbols.h"
#include <math.h>

void init_ticks();
int ticks();

int busy;

void
cb(uint32_t tag)
{
  busy--;
}

#include "spuinit.inc"
int
main(int argc, char **argv)
{
  spuinit();

  FILTER_fft_STATE fftc[6];

  // Initialize filter state
  {
    float n = 2;
    for (int i = 0; i < 8; n *= 2, i++) {
      fftc[0].c[i].wn_r = cos(2 * M_PI / n);
      fftc[0].c[i].wn_i = sin(2 * M_PI / n);
    }
  }
  for (int i = 1; i < 6; i++) {
    fftc[i] = fftc[0];
  }

  FILE *inf, *outf;
  if (!(inf = fopen("../input/FFT5.in", "r"))) {
    fprintf(stderr, "error opening input\n");
    exit(1);
  }
  if (!(outf = fopen("fft.out", "w"))) {
    fprintf(stderr, "error opening output\n");
    exit(1);
  }

  // Read input
  int n = 10000;                 // Total filter runs
  int numspu = 6;
  switch (argc) {
  default:
  case 2:
    n = atoi(argv[1]);
    break;
  case 1:
  case 0:
    break;
  }
  int bufsz = 2048 * n;          // PPU buffer size

  // Read input and allocate output
  BUFFER_CB *inbuf = alloc_buffer(bufsz, FALSE, 0);
  fread(inbuf->data, sizeof(float), n * 512, inf);
  buf_set_tail(inbuf, n * 2048);

  BUFFER_CB *outbuf = alloc_buffer(bufsz, FALSE, 0);

  init_ticks();
  int start = ticks();

  spulib_init();

  // Setup info
  int sbsz = 64 * 1024;          // SPU buffer size
  int sdtsz = 16 * 1024;         // Data transfer bytes per iteration
  int sfi = sdtsz / 2048;        // Filter runs per iteration
  int iters = n / sfi;           // Iterations to run
  int fcb = 0;                   // Location of SPU filter control block
  int siba = fcb + 128 + 128;    // Location of SPU input buffer
  int soba = siba + sbsz + 128;  // Location of SPU output buffer
  int sfree = soba + sbsz;       // Start of free space on SPU
  void *wf[6] = {&wf_fft0, &wf_fft1, &wf_fft2, &wf_fft3, &wf_fft4, &wf_fft5};
  SPU_FILTER_DESC fd[numspu];
  for (int i = 0; i < numspu; i++) {
    fd[i].work_func = (LS_ADDRESS)wf[i];
    fd[i].state_size = sizeof(fftc[i]);
    fd[i].state_addr = &fftc[i];
    fd[i].num_inputs = 1;
    fd[i].num_outputs = 1;
  }

  // Initialize filter and buffer
  for (int i = 0; i < numspu; i++) {
    SPU_CMD_GROUP *g = spu_new_group(i, 0);
    spu_filter_load(g, fcb, &fd[i], 0, 0);
    spu_buffer_alloc(g, siba, sbsz, 0, 1, 0);
    spu_buffer_alloc(g, soba, sbsz, 0, 2, 0);
    spu_filter_attach_input(g, fcb, 0, siba, 3, 2, 0, 1);
    spu_filter_attach_output(g, fcb, 0, soba, 4, 2, 0, 2);
    spu_issue_group(i, 0, sfree);
  }

  // Setup pipeline info
  EXT_SPU_LAYOUT l[numspu];
  EXT_SPU_RATES r;
  l[0].cmd_id = 0;
  l[0].da = sfree;
  l[0].local_in_buf_data = siba;
  l[0].filt = fcb;
  l[0].local_out_buf_data = soba;
  r.in_bytes = sdtsz;
  r.run_iters = sfi;
  r.loop_iters = 1;
  r.out_bytes = sdtsz;
  for (int i = 0; i < numspu; i++) {
    l[i] = l[0];
    l[i].spu_id = i;
    if (i == 0) {
      l[i].remote_in_buf_ppu = TRUE;
      l[i].ppu_in_buf = inbuf;
    } else {
      l[i].remote_in_buf_ppu = FALSE;
      l[i].remote_spu_in_buf_data = spu_addr(i - 1, soba);
      l[i].remote_spu_in_buf_size = sbsz;
    }
    if (i == numspu - 1) {
      l[i].remote_out_buf_ppu = TRUE;
      l[i].ppu_out_buf = outbuf;
    } else {
      l[i].remote_out_buf_ppu = FALSE;
      l[i].remote_spu_out_buf_data = spu_addr(i + 1, siba);
      l[i].remote_spu_out_buf_size = sbsz;
    }
  }

  for (int i = 0; i < numspu; i++) {
    spulib_wait(i, 0x1f);
  }

  int startspu = ticks();

  // Run pipeline
  busy = numspu;
  for (int i = 0; i < numspu; i++) {
    ext_spu(&l[i], &r, iters, cb, 0);
  }
  spulib_poll_while(busy);

  // Unload filters
  for (int i = 0; i < numspu; i++) {
    SPU_CMD_GROUP *g = spu_new_group(i, 0);
    spu_filter_unload(g, fcb, 0, 0);
    spu_issue_group(i, 0, sfree);
  }

  for (int i = 0; i < numspu; i++) {
    spulib_wait(i, 1);
  }

  printf("spu time: %d ms\n", ticks() - startspu);
  printf("time: %d ms\n", ticks() - start);

  fwrite(outbuf->data, sizeof(float), n * 512, outf);
  fclose(inf);
  fclose(outf);
}
