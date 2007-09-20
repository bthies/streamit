#include "ds.h"
#include "filterstate.h"
#include "spusymbols.h"

FILTER filters[4];
CHANNEL channels[6];

FILTER_2_STATE state_2;

#include "spuinit.inc"
int
main(int argc, char **argv)
{
  spuinit();

  int n = 1320;
  num_spu = 2;

  switch (argc) {
  default:
  case 3:
    n = atoi(argv[2]);
  case 2:
    num_spu = atoi(argv[1]);
  case 1:
    break;
  }

  num_filters = 4;
  num_channels = 6;

  channels[0].buf_size = n * 1612;
  channels[0].non_circular = TRUE;

  for (int i = 1; i < 5; i++) {
    channels[i].buf_size = 1024 * 1024;
  }

  channels[5].buf_size = n * 1800;
  channels[5].non_circular = TRUE;

  filters[0].name = "splitter";
  filters[0].desc.work_func = (LS_ADDRESS)&wf_0;
  filters[0].desc.state_size = 0;
  filters[0].desc.num_inputs = 1;
  filters[0].desc.num_outputs = 2;
  filters[0].inputs = &channels[0];
  filters[0].outputs = &channels[1];
  filters[0].inputs[0].input.f = &filters[0];
  filters[0].outputs[0].output.f = &filters[0];
  filters[0].outputs[1].output.f = &filters[0];
  filters[0].inputs[0].input.pop_bytes = 403 * sizeof(int);
  filters[0].outputs[0].output.push_bytes = 384 * sizeof(int);
  filters[0].outputs[1].output.push_bytes = 19 * sizeof(int);
  filters[0].data_parallel = TRUE;

  filters[1].name = "stateless";
  filters[1].desc.work_func = (LS_ADDRESS)&wf_1;
  filters[1].desc.state_size = 0;
  filters[1].desc.num_inputs = 1;
  filters[1].desc.num_outputs = 1;
  filters[1].inputs = &channels[1];
  filters[1].outputs = &channels[3];
  filters[1].inputs[0].input.f = &filters[1];
  filters[1].outputs[0].output.f = &filters[1];
  filters[1].inputs[0].input.pop_bytes = 64 * sizeof(int);
  filters[1].outputs[0].output.push_bytes = 64 * sizeof(int);
  filters[1].data_parallel = TRUE;

  filters[2].name = "stateful";
  filters[2].desc.work_func = (LS_ADDRESS)&wf_2;
  filters[2].desc.state_addr = &state_2;
  filters[2].desc.state_size = sizeof(state_2);
  filters[2].desc.num_inputs = 1;
  filters[2].desc.num_outputs = 1;
  filters[2].inputs = &channels[2];
  filters[2].outputs = &channels[4];
  filters[2].inputs[0].input.f = &filters[2];
  filters[2].outputs[0].output.f = &filters[2];
  filters[2].inputs[0].input.pop_bytes = 19 * sizeof(int);
  filters[2].outputs[0].output.push_bytes = 66 * sizeof(int);

  filters[3].name = "joiner";
  filters[3].desc.work_func = (LS_ADDRESS)&wf_3;
  filters[3].desc.state_size = 0;
  filters[3].desc.num_inputs = 2;
  filters[3].desc.num_outputs = 1;
  filters[3].inputs = &channels[3];
  filters[3].outputs = &channels[5];
  filters[3].inputs[0].input.f = &filters[3];
  filters[3].inputs[1].input.f = &filters[3];
  filters[3].outputs[0].output.f = &filters[3];
  filters[3].inputs[0].input.pop_bytes = 64 * sizeof(int);
  filters[3].inputs[1].input.pop_bytes = 11 * sizeof(int);
  filters[3].outputs[0].output.push_bytes = 75 * sizeof(int);
  filters[3].data_parallel = TRUE;

  ds_init();

  FILE *inf = fopen("../input/dec_nm_parsed.int", "r");
  if (inf == NULL) {
    fprintf(stderr, "error opening input\n");
    exit(1);
  }

  safe_dec(channels[0].free_bytes, n * 1612);
  fread(channels[0].buf.data, sizeof(int), n * 403, inf);
  buf_inc_tail(&channels[0].buf, n * 1612);
  init_update_down_channel_used(&channels[0], n * 1612);

  fclose(inf);

  for (int i = 0; i < num_spu; i++) {
    SPU_CMD_GROUP *g = spu_new_group(i, 0);
    spu_call_func(g, (LS_ADDRESS)&init_1, 0, 0);
    spu_issue_group(i, 0, 0);
  }
  for (int i = 0; i < num_spu; i++) {
    spulib_wait(i, 1);
  }

  ds_run();

  FILE *outf = fopen("mpeg.out", "w");
  if (outf == NULL) {
    fprintf(stderr, "error opening output\n");
    exit(1);
  }
  fwrite(channels[5].buf.data, sizeof(int), n * 450, outf);
  fclose(outf);
}
