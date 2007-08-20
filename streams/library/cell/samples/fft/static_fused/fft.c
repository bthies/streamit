/*
 * Hand-coded data parallel version of FFT. FFT pipeline is fused into a single
 * filter.
 *
 * First argument (optional) specifies number of SPUs
 */

#include "filterdefs.h"
#include "workstate.h"
#include <stdlib.h>
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

  FILTER_fft_STATE fftc;

  // Initialize filter state
  {
    float n = 2;
    for (int i = 0; i < 8; n *= 2, i++) {
      fftc.c[i].wn_r = cos(2 * M_PI / n);
      fftc.c[i].wn_i = sin(2 * M_PI / n);
    }
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
  int n = 10000;         // Total filter runs
  int numspu = 6;
  switch (argc) {
  default:
  case 3:
    n = atoi(argv[2]);
  case 2:
    numspu = atoi(argv[1]);
    break;
  case 1:
  case 0:
    break;
  }
  int bufsz = 2048 * n;  // PPU buffer size

  // Read input and allocate output
  float *inbuf = alloc_buffer_ex(bufsz, FALSE, 0);
  BUFFER_CB *bicb = buf_get_cb(inbuf);
  fread(inbuf, sizeof(float), n * 512, inf);
  buf_inc_tail(bicb, n * 2048);

  float *outbuf = alloc_buffer_ex(bufsz, FALSE, 0);
  BUFFER_CB *bocb = buf_get_cb(outbuf);

  init_ticks();
  int start = ticks();

  spulib_init();

  // Setup info
  int spuiters[numspu];           // Iterations split among SPUs
  spuiters[0] = n / numspu;
  for (int i = 1; i < numspu; i++) spuiters[i] = spuiters[0];
  spuiters[0] = n - spuiters[0] * (numspu - 1);
  int sbsz = 64 * 1024;           // SPU buffer size
  int sdtsz = 16 * 1024;          // Data transfer bytes per iteration
  int sfi = sdtsz / 2048;         // Filter runs per iteration
  int fcb = 0;                    // Location of SPU filter control block
  int siba = fcb + 128;           // Location of SPU input buffer
  int soba = siba + 128 + sbsz;   // Location of SPU output buffer
  int sfree = soba + 128 + sbsz;  // Start of free space on SPU
  SPU_FILTER_DESC fd;
  fd.work_func = (LS_ADDRESS)&wf_fft;
  fd.state_size = sizeof(fftc);
  fd.state_addr = &fftc;
  fd.num_inputs = 1;
  fd.num_outputs = 1;

  // Initialize filter
  for (int i = 0; i < numspu; i++) {
    SPU_CMD_GROUP *g = spu_new_group(i, 0);
    spu_filter_load(g, fcb, &fd, 0, 0);
    spu_issue_group(i, 0, sfree);
  }

  // Setup execution info
  EXT_PSP_EX_PARAMS f;
  EXT_PSP_EX_LAYOUT l;
  f.num_inputs = 1;
  f.num_outputs = 1;
  f.inputs[0].pop_bytes = 2048;
  f.inputs[0].peek_extra_bytes = 0;
  f.inputs[0].spu_buf_size = sbsz;
  f.outputs[0].push_bytes = 2048;
  f.outputs[0].spu_buf_size = sbsz;
  f.data_parallel = TRUE;
  f.group_iters = sfi;
  l.desc = &fd;
  l.filt_cb = fcb;
  l.in_buf_start = siba;
  l.out_buf_start = soba;
  l.cmd_data_start = sfree;
  l.cmd_id_start = 0;
  l.load_filter = FALSE;

  for (int i = 0; i < numspu; i++) {
    spulib_wait(i, 1);
  }

  int startspu = ticks();

  // Run data parallel
  busy = numspu;
  for (int i = 0; i < numspu; i++) {
    l.spu_id = i;
    ext_ppu_spu_ppu_ex(&l, &f, &bicb, &bocb, spuiters[i], cb, 0);
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

  fwrite(outbuf, sizeof(float), n * 512, outf);
  fclose(inf);
  fclose(outf);
}
