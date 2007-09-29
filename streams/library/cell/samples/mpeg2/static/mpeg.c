/* work ratios per steady state:

   0 splitter    11.7
   1 stateless   47.2
   2 stateful  >  2.8
   3 joiner      14.9

   (int splitters/joiners are slower than float for some reason)

   quick layout for 5 spus is:

   0  3  1  1  1
   2
*/

#include "spulib.h"
#include <stdlib.h>
#include <stdio.h>
#include "filterstate.h"
#include "spusymbols.h"

SPU_FILTER_DESC fd[4];
FILTER_2_STATE fs_2;
EXT_PSP_PARAMS fp[4];
EXT_PSP_LAYOUT sl[4];

BUFFER_CB *inbuf, *outbuf, *buf[4];

int busy;

void
cb(uint32_t tag)
{
  busy--;
}

void
cb0(uint32_t tag)
{
  ext_ppu_spu_ppu(&sl[2], &fp[2], &buf[1], &buf[3], tag, &cb, 0);
}

#include "spuinit.inc"
int
main(int argc, char **argv)
{
  spuinit();
  spulib_init();

  int n = 1320;
  int m = 100;
  int dummy[(n >= 2 * m ? 0 : -1)];

  switch (argc) {
  default:
  case 2:
    n = atoi(argv[1]);
  case 1:
    break;
  }

  fd[0].work_func = (LS_ADDRESS)&wf_0;
  fd[0].state_size = 0;
  fd[0].num_inputs = 1;
  fd[0].num_outputs = 2;

  fd[1].work_func = (LS_ADDRESS)&wf_1;
  fd[1].state_size = 0;
  fd[1].num_inputs = 1;
  fd[1].num_outputs = 1;

  fd[2].work_func = (LS_ADDRESS)&wf_2;
  fd[2].state_addr = &fs_2;
  fd[2].state_size = sizeof(fs_2);
  fd[2].num_inputs = 1;
  fd[2].num_outputs = 1;

  fd[3].work_func = (LS_ADDRESS)&wf_3;
  fd[3].state_size = 0;
  fd[3].num_inputs = 2;
  fd[3].num_outputs = 1;

  fp[0].num_inputs = 1;
  fp[0].num_outputs = 2;
  fp[0].inputs[0].pop_bytes = 403 * sizeof(int);
  fp[0].outputs[0].push_bytes = 384 * sizeof(int);
  fp[0].outputs[1].push_bytes = 19 * sizeof(int);
  fp[0].data_parallel = TRUE;
  fp[0].group_iters = 10;
  fp[0].inputs[0].spu_buf_size = 32 * 1024;
  fp[0].outputs[0].spu_buf_size = 32 * 1024;
  fp[0].outputs[1].spu_buf_size = 2 * 1024;
  fp[0].loop_iters = fp[0].group_iters / 5;

  fp[1].num_inputs = 1;
  fp[1].num_outputs = 1;
  fp[1].inputs[0].pop_bytes = 64 * sizeof(int);
  fp[1].outputs[0].push_bytes = 64 * sizeof(int);
  fp[1].data_parallel = TRUE;
  fp[1].group_iters = 50;
  fp[1].inputs[0].spu_buf_size = 32 * 1024;
  fp[1].outputs[0].spu_buf_size = 32 * 1024;
  fp[1].loop_iters = fp[1].group_iters / 5;

  fp[2].num_inputs = 1;
  fp[2].num_outputs = 1;
  fp[2].inputs[0].pop_bytes = 19 * sizeof(int);
  fp[2].outputs[0].push_bytes = 66 * sizeof(int);
  fp[2].group_iters = 50;
  fp[2].inputs[0].spu_buf_size = 8 * 1024;
  fp[2].outputs[0].spu_buf_size = 32 * 1024;
  fp[2].loop_iters = fp[2].group_iters / 5;

  fp[3].num_inputs = 2;
  fp[3].num_outputs = 1;
  fp[3].inputs[0].pop_bytes = 64 * sizeof(int);
  fp[3].inputs[1].pop_bytes = 11 * sizeof(int);
  fp[3].outputs[0].push_bytes = 75 * sizeof(int);
  fp[3].data_parallel = TRUE;
  fp[3].group_iters = 50;
  fp[3].inputs[0].spu_buf_size = 32 * 1024;
  fp[3].inputs[1].spu_buf_size = 8 * 1024;
  fp[3].outputs[0].spu_buf_size = 32 * 1024;
  fp[3].loop_iters = fp[3].group_iters / 5;

  sl[0].desc = &fd[0];
  sl[0].filt_cb = 128;
  sl[0].in_buf_start = 256;
  sl[0].out_buf_start = sl[0].in_buf_start +
    128 + fp[0].inputs[0].spu_buf_size;
  sl[0].cmd_data_start = sl[0].out_buf_start +
    128 + fp[0].outputs[0].spu_buf_size + 128 + fp[0].outputs[1].spu_buf_size;
  sl[0].cmd_id_start = 0;
  sl[0].load_filter = TRUE;

  sl[1].desc = &fd[1];
  sl[1].filt_cb = 128;
  sl[1].in_buf_start = 256;
  sl[1].out_buf_start = sl[1].in_buf_start +
    128 + fp[1].inputs[0].spu_buf_size;
  sl[1].cmd_data_start = sl[1].out_buf_start +
    128 + fp[1].outputs[0].spu_buf_size;
  sl[1].cmd_id_start = 0;
  sl[1].load_filter = TRUE;

  sl[2].spu_id = 0;
  sl[2].desc = &fd[2];
  sl[2].filt_cb = 0;
  sl[2].in_buf_start = 256;
  sl[2].out_buf_start = sl[2].in_buf_start +
    128 + fp[2].inputs[0].spu_buf_size;
  sl[2].cmd_data_start = sl[2].out_buf_start +
    128 + fp[2].outputs[0].spu_buf_size;
  sl[2].cmd_id_start = 0;

  sl[3].desc = &fd[3];
  sl[3].filt_cb = 128;
  sl[3].in_buf_start = 256;
  sl[3].out_buf_start = sl[3].in_buf_start +
    128 + fp[3].inputs[0].spu_buf_size + 128 + fp[3].inputs[1].spu_buf_size;
  sl[3].cmd_data_start = sl[3].out_buf_start +
    128 + fp[3].outputs[0].spu_buf_size;
  sl[3].cmd_id_start = 0;
  sl[3].load_filter = TRUE;

  inbuf = alloc_buffer(n * 1612, FALSE, 0);
  FILE *inf = fopen("../input/dec_nm_parsed.int", "r");
  if (inf == NULL) {
    fprintf(stderr, "error opening input\n");
    exit(1);
  }
  fread(inbuf->data, sizeof(int), n * 403, inf);
  buf_inc_tail(inbuf, n * 1612);
  fclose(inf);

  outbuf = alloc_buffer(n * 1800, FALSE, 0);

  buf[0] = alloc_buffer(512 * 1024, TRUE, 0);
  buf[1] = alloc_buffer(16 * 1024, TRUE, 0);
  buf[2] = alloc_buffer(512 * 1024, TRUE, 0);
  buf[3] = alloc_buffer(64 * 1024, TRUE, 0);

  {
    SPU_CMD_GROUP *g = spu_new_group(0, 0);
    spu_filter_load(g, sl[2].filt_cb, &fd[2], 0, 0);
    spu_call_func(g, (LS_ADDRESS)&init_1, 1, 0);
    spu_issue_group(0, 0, 128);
  }
  for (int i = 1; i < 5; i++) {
    SPU_CMD_GROUP *g = spu_new_group(i, 0);
    spu_call_func(g, (LS_ADDRESS)&init_1, 0, 0);
    spu_issue_group(i, 0, 0);
  }
  spulib_wait(0, 3);
  for (int i = 1; i < 5; i++) {
    spulib_wait(i, 1);
  }

  for (int i = 0; i < 5; i++) {
    sl[0].spu_id = i;
    ext_ppu_spu_ppu(&sl[0], &fp[0], &inbuf, &buf[0], m * 2 / 5, &cb, 0);
  }
  busy = 5;
  spulib_poll_while(busy != 0);

  ext_ppu_spu_ppu(&sl[2], &fp[2], &buf[1], &buf[3], m, &cb, 0);
  for (int i = 1; i < 5; i++) {
    sl[1].spu_id = i;
    ext_ppu_spu_ppu(&sl[1], &fp[1], &buf[0], &buf[2], m * 3 / 2, &cb, 0);
  }
  busy = 5;
  spulib_poll_while(busy != 0);

  sl[0].spu_id = 0;
  sl[3].spu_id = 1;
  int t = n - 2 * m;

  while (TRUE) {
    ext_ppu_spu_ppu(&sl[0], &fp[0], &inbuf, &buf[0], (t <= m ? t : m), &cb0, m);
    ext_ppu_spu_ppu(&sl[3], &fp[3], &buf[2], &outbuf, m * 6, &cb, 0);
    for (int i = 2; i < 5; i++) {
      sl[1].spu_id = i;
      ext_ppu_spu_ppu(&sl[1], &fp[1], &buf[0], &buf[2], m * 2, &cb, 0);
    }
    busy = 5;
    spulib_poll_while(busy != 0);

    if (t <= m) {
      break;
    }
    t -= m;
  }

  ext_ppu_spu_ppu(&sl[2], &fp[2], &buf[1], &buf[3], t, &cb, 0);
  for (int i = 1; i < 5; i++) {
    sl[1].spu_id = i;
    ext_ppu_spu_ppu(&sl[1], &fp[1], &buf[0], &buf[2], t * 3 / 2, &cb, 0);
  }
  busy = 5;
  spulib_poll_while(busy != 0);

  for (int i = 0; i < 5; i++) {
    sl[3].spu_id = i;
    ext_ppu_spu_ppu(&sl[3], &fp[3], &buf[2], &outbuf, (m + t) * 6 / 5, &cb, 0);
  }
  busy = 5;
  spulib_poll_while(busy != 0);

  {
    SPU_CMD_GROUP *g = spu_new_group(0, 0);
    spu_filter_unload(g, sl[2].filt_cb, 0, 0);
    spu_stats_update(g, 1, 1, 0);
    spu_issue_group(0, 0, 128);
  }  
  for (int i = 1; i < 5; i++) {
    SPU_CMD_GROUP *g = spu_new_group(i, 0);
    spu_stats_update(g, 0, 0);
    spu_issue_group(i, 0, 0);
  }
  spulib_wait(0, 2);
  for (int i = 0; i < 5; i++) {
    spulib_wait(i, 1);
    spu_stats_print(spu_new_group(i, 0), 0, 0);
    spu_issue_group(i, 0, 0);
    spulib_wait(i, 1);
  }

  FILE *outf = fopen("mpeg.out", "w");
  if (outf == NULL) {
    fprintf(stderr, "error opening output\n");
    exit(1);
  }
  fwrite(outbuf->data, sizeof(int), n * 450, outf);
  fclose(outf);
}
