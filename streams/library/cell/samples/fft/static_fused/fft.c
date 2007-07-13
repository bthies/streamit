/*
 * Hand-coded data parallel version of FFT. FFT pipeline is fused into a single
 * filter (the filter operates on data in-place).
 *
 * First argument (optional) specifies number of SPUs
 */

#include "workstate.h"
#include <stdlib.h>
#include <stdio.h>
#include "spulib.h"
#include "spusymbols.h"
#include "filterdefs.h"
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
  int n = 10000;      // Total filter runs
  int numspu;
  int bufsz[7] = {0,  // PPU buffer size
                  64 * 1024 * 1024,
                  32 * 1024 * 1024,
                  16 * 1024 * 1024,
                  16 * 1024 * 1024,
                  8 * 1024 * 1024,
                  8 * 1024 * 1024};
  if (argc != 2) {
    numspu = 6;
  } else {
    numspu = atoi(argv[1]);
  }
  busy = numspu;

  int sdtsz = 16 * 1024;   // Data transfer bytes per iteration
  int sfi = sdtsz / 2048;  // Filter runs per iteration
  int iters = n / sfi;     // Iterations to run
  int spuiters[numspu];    // Iterations split among SPUs
  spuiters[0] = iters / numspu;
  for (int i = 1; i < numspu; i++) spuiters[i] = spuiters[0];
  spuiters[numspu - 1] = iters - spuiters[0] * (numspu - 1);

  // Read input
  float *buf[numspu];
  BUFFER_CB *bcb[numspu];
  for (int i = 0; i < numspu; i++) {
    int spuitems = spuiters[i] * sfi * 512;
    buf[i] = alloc_buffer(bufsz[numspu], 0);
    bcb[i] = buf_get_cb(buf[i]);
    fread(buf[i], sizeof(float), spuitems, inf);
    bcb[i]->tail = spuitems * 4;
    IF_CHECK(bcb[i]->otail = bcb[i]->tail);
    // *** This touches all pages that are used - there is a significant
    // slowdown if SPUs need to access invalid pages
    for (int j = 0; j < spuitems / 1024; j++) {
      buf[i][spuitems + j * 1024] = 0;
    }
  }

  init_ticks();
  int start = ticks();

  spulib_init();

  // Setup info
  int sbsz = 64 * 1024;       // SPU buffer size
  int fcb = 0;                // Location of SPU filter control block
  int sba = fcb + 128 + 128;  // Location of SPU buffer
  int sfree = sba + sbsz;     // Start of free space on SPU
  SPU_FILTER_DESC fd;
  fd.work_func = (LS_ADDRESS)&wf_fft;
  fd.param = spu_lsa(0, fcb + 124);
  fd.state_size = sizeof(fftc);
  fd.state_addr = &fftc;
  fd.num_inputs = 1;
  fd.num_outputs = 1;

  // Initialize filter and buffer
  for (int i = 0; i < numspu; i++) {
    *(int *)spu_addr(0, fcb + 124) = 0;
    SPU_CMD_GROUP *g = spu_new_group(i, 0);
    spu_filter_load(g, fcb, &fd, 0, 0);
    spu_buffer_alloc(g, sba, sbsz, 0, 1, 0);
    spu_filter_attach_input(g, fcb, 0, sba, 2, 2, 0, 1);
    spu_filter_attach_output(g, fcb, 0, sba, 3, 2, 0, 1);
    spu_issue_group(i, 0, sfree);
  }

  // Setup execution info
  EXT_SPU_LAYOUT l;
  EXT_SPU_RATES r;
  l.cmd_id = 0;
  l.da = sfree;
  l.spu_in_buf_data = sba;
  l.filt = fcb;
  l.spu_out_buf_data = sba;
  r.in_bytes = sdtsz;
  r.run_iters = sfi;
  r.out_bytes = sdtsz;

  for (int i = 0; i < numspu; i++) {
    spulib_wait(i, 0xf);
  }

  int startspu = ticks();

  // Run data parallel
  for (int i = 0; i < numspu; i++) {
    l.spu_id = i;
    l.ppu_in_buf_data = buf[i];
    l.ppu_out_buf_data = buf[i];
    ext_ppu_spu_ppu(&l, &r, spuiters[i], cb, 0);
  }

  spulib_poll_while(busy);

  printf("spu time: %d ms\n", ticks() - startspu);
  printf("time: %d ms\n", ticks() - start);

  for (int i = 0; i < numspu; i++) {
    int spuitems = spuiters[i] * sfi * 512;
    fwrite(buf[i] + spuitems, sizeof(float), spuitems, outf);
  }
  fclose(inf);
  fclose(outf);
}
