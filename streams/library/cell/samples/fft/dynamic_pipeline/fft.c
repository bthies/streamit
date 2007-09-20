#include "ds.h"
#include "spusymbols.h"
#include <math.h>
#include <strings.h>
#include "workstate.h"

#define NUM_FILTERS  15
#define NUM_CHANNELS 16

FILTER filters[NUM_FILTERS];
CHANNEL channels[NUM_CHANNELS];

#define init_filter(_index, _name, _state, _io_items)                   \
  do {                                                                  \
    FILTER *f = &filters[_index];                                       \
    f->name = #_name;                                                   \
    f->desc.work_func = (LS_ADDRESS)&wf_##_name;                        \
    f->desc.state_size = ((_state) == NULL ? 0 : sizeof(*(_state)));    \
    f->desc.state_addr = (_state);                                      \
    f->inputs[0].input.pop_bytes = (_io_items) * sizeof(float);         \
    f->outputs[0].output.push_bytes = (_io_items) * sizeof(float);      \
  } while (FALSE)

COMBINEDFT_STATE fstate[8];

void init_ticks();
uint32_t ticks();

#include "spuinit.inc"
int
main(int argc, char **argv)
{
  spuinit();
  init_ticks();

  num_filters = NUM_FILTERS;
  num_channels = NUM_CHANNELS;

  uint32_t n = 10000;
  bool_t dp = FALSE;

  switch (argc) {
  default:
  case 4:
    dp = (strcasecmp(argv[3], "dp") == 0);
  case 3:
    n = atoi(argv[2]);
  case 2:
    num_spu = atoi(argv[1]);
    break;

  case 1:
  case 0:
    break;
  }

  printf("Data-parallel: %s\n", (dp ? "yes" : "no"));

  for (uint32_t i = 0; i < NUM_FILTERS; i++) {
    FILTER *f = &filters[i];
    f->inputs = &channels[i];
    f->inputs[0].input.f = f;
    f->outputs = &channels[i + 1];
    f->outputs[0].output.f = f;
    f->desc.num_inputs = 1;
    f->desc.num_outputs = 1;
    f->data_parallel = dp;
  }

  init_filter( 0, FFTReorderSimple_512, NULL      , 512);
  init_filter( 1, FFTReorderSimple_256, NULL      , 256);
  init_filter( 2, FFTReorderSimple_128, NULL      , 128);
  init_filter( 3, FFTReorderSimple_64 , NULL      ,  64);
  init_filter( 4, FFTReorderSimple_32 , NULL      ,  32);
  init_filter( 5, FFTReorderSimple_16 , NULL      ,  16);
  init_filter( 6, FFTReorderSimple_8  , NULL      ,   8);
  init_filter( 7, CombineDFT_4        , &fstate[0],   4);
  init_filter( 8, CombineDFT_8        , &fstate[1],   8);
  init_filter( 9, CombineDFT_16       , &fstate[2],  16);
  init_filter(10, CombineDFT_32       , &fstate[3],  32);
  init_filter(11, CombineDFT_64       , &fstate[4],  64);
  init_filter(12, CombineDFT_128      , &fstate[5], 128);
  init_filter(13, CombineDFT_256      , &fstate[6], 256);
  init_filter(14, CombineDFT_512      , &fstate[7], 512);

  for (uint32_t i = 1; i < NUM_CHANNELS - 1; i++) {
    channels[i].buf_size = 1024 * 1024;
  }

  channels[ 0].buf_size = n * 2048;
  channels[ 0].non_circular = TRUE;
  channels[15].buf_size = n * 2048;
  channels[15].non_circular = TRUE;

  ds_init();

  // CombineDFT constants
  {
    float n = 2;
    for (int i = 0; i < 8; i++) {
      fstate[i].wn_r = cos(2 * M_PI / n);
      fstate[i].wn_i = sin(2 * M_PI / n);
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
  fwrite(channels[15].buf.data, sizeof(float), n * 512, outf);
  fclose(outf);
}
