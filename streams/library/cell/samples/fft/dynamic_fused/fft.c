#include "ds.h"
#include "spusymbols.h"
#include <math.h>
#include "workstate.h"

FILTER filters[1];
CHANNEL channels[2];

FILTER_FFT_STATE fstate;

void init_ticks();
uint32_t ticks();

#include "spuinit.inc"
int
main(int argc, char **argv)
{
  spuinit();
  init_ticks();

  num_filters = 1;
  num_channels = 2;

  uint32_t n = 10000;

  switch (argc) {
  default:
  case 3:
    n = atoi(argv[2]);
  case 2:
    num_spu = atoi(argv[1]);
    break;

  case 1:
  case 0:
    break;
  }

  FILTER *f = &filters[0];

  f->inputs[0] = &channels[0];
  f->inputs[0]->input.f = f;
  f->outputs[0] = &channels[1];
  f->outputs[0]->output.f = f;
  f->name = "FFT";
  f->desc.work_func = (LS_ADDRESS)&wf_FFT;
  f->desc.state_size = sizeof(fstate);
  f->desc.state_addr = &fstate;
  f->desc.num_inputs = 1;
  f->desc.num_outputs = 1;
  f->inputs[0]->input.pop_bytes = 2048;
  f->outputs[0]->output.push_bytes = 2048;
  f->data_parallel = TRUE;

  channels[0].buf_size = n * 2048;
  channels[0].non_circular = TRUE;
  channels[1].buf_size = n * 2048;
  channels[1].non_circular = TRUE;

  ds_init();

  // CombineDFT constants
  {
    float n = 2;
    for (int i = 0; i < 8; i++) {
      fstate.c[i].wn_r = cos(2 * M_PI / n);
      fstate.c[i].wn_i = sin(2 * M_PI / n);
      n *= 2;
    }
  }

  FILE *inf;
  if (!(inf = fopen("../input/FFT5.in", "r"))) {
    perror("Unable to open input file FFT5.in");
    abort();
  }

  safe_dec(channels[0].free_bytes, n * 2048);
  fread(channels[0].buf.data, sizeof(float), n * 512, inf);
  buf_inc_tail(&channels[0].buf, n * 2048);
  init_update_down_channel_used(&channels[0], n * 2048);

  fclose(inf);

  uint32_t start_ticks = ticks();
  ds_run();
  printf("Time: %d ms\n", ticks() - start_ticks);

  FILE *outf;
  if (!(outf = fopen("fft.out", "w"))) {
    perror("Unable to open output file fft.out");
    abort();
  }
  fwrite(channels[1].buf.data, sizeof(float), n * 512, outf);
  fclose(outf);
}
