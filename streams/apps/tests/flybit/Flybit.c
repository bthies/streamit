#include "streamit.h"

#include <stdio.h>

#include <stdlib.h>


typedef struct SIRFilter_1 {
  stream_context* context;
  tape inTape;
  tape outTape;
  int x;
} _SIRFilter_1, *SIRFilter_1;
typedef struct SIRFilter_2_1 {
  stream_context* context;
  tape inTape;
  tape outTape;
} _SIRFilter_2_1, *SIRFilter_2_1;
typedef struct SIRFilter_2_2 {
  stream_context* context;
  tape inTape;
  tape outTape;
} _SIRFilter_2_2, *SIRFilter_2_2;
typedef struct SIRPipeline_2 {
  stream_context* context;
  tape inTape;
  tape outTape;
  SIRFilter_2_1 stream1;
  SIRFilter_2_2 stream2;
} _SIRPipeline_2, *SIRPipeline_2;
typedef struct SIRFilter_3 {
  stream_context* context;
  tape inTape;
  tape outTape;
} _SIRFilter_3, *SIRFilter_3;
typedef struct SIRPipeline {
  stream_context* context;
  tape inTape;
  tape outTape;
  SIRFilter_1 stream1;
  SIRPipeline_2 stream2;
  SIRFilter_3 stream3;
} _SIRPipeline, *SIRPipeline;
typedef struct Main {
} _Main, *Main;
void main();
void hierarchical_work_2(SIRPipeline data);
void hierarchical_work_1(SIRPipeline data);
void SIRFilter_1_work(SIRFilter_1 data);
void SIRFilter_1_init(SIRFilter_1 data);
void SIRFilter_2_1_work(SIRFilter_2_1 data);
void SIRFilter_2_1_init(SIRFilter_2_1 data);
void SIRFilter_2_2_work(SIRFilter_2_2 data);
void SIRFilter_2_2_init(SIRFilter_2_2 data);
void SIRPipeline_2_init(SIRPipeline_2 data);
void SIRFilter_3_work(SIRFilter_3 data);
void SIRFilter_3_init(SIRFilter_3 data);
void SIRPipeline_init(SIRPipeline data);
void main() {
  SIRPipeline s = malloc(sizeof(_SIRPipeline));
  s->context = create_context(s);
  SIRPipeline_init(s);
  streamit_run(s->context);
}

void hierarchical_work_2(SIRPipeline data) {
  SIRFilter_1_work(data->stream1);
  SIRFilter_1_work(data->stream1);
  hierarchical_work_1(data);
  SIRFilter_3_work(data->stream3);
  SIRFilter_3_work(data->stream3);
}

void hierarchical_work_1(SIRPipeline data) {
  run_splitter(data->stream2->context);
  run_splitter(data->stream2->context);
  SIRFilter_2_1_work(data->stream2->stream1);
  SIRFilter_2_2_work(data->stream2->stream2);
  run_joiner(data->stream2->context);
}

void SIRFilter_1_work(SIRFilter_1 data) {
  PUSH(data->context, int, data->x++);
}

void SIRFilter_1_init(SIRFilter_1 data) {
  data->x = 0;
  set_stream_type(data->context, FILTER);
  set_push(data->context, 1);
  set_peek(data->context, 1);
  set_pop(data->context, 1);
  set_work(data->context, (work_fn)SIRFilter_1_work);
}

void SIRFilter_2_1_work(SIRFilter_2_1 data) {
  PUSH(data->context, int, POP(data->context, int) - POP(data->context, int));
}

void SIRFilter_2_1_init(SIRFilter_2_1 data) {
  set_stream_type(data->context, FILTER);
  set_push(data->context, 1);
  set_peek(data->context, 1);
  set_pop(data->context, 1);
  set_work(data->context, (work_fn)SIRFilter_2_1_work);
}

void SIRFilter_2_2_work(SIRFilter_2_2 data) {
  PUSH(data->context, int, POP(data->context, int) + POP(data->context, int));
}

void SIRFilter_2_2_init(SIRFilter_2_2 data) {
  set_stream_type(data->context, FILTER);
  set_push(data->context, 1);
  set_peek(data->context, 1);
  set_pop(data->context, 1);
  set_work(data->context, (work_fn)SIRFilter_2_2_work);
}

void SIRPipeline_2_init(SIRPipeline_2 data) {
  set_stream_type(data->context, SPLIT_JOIN);
  data->stream1 = malloc(sizeof(_SIRFilter_2_1));
  data->stream1->context = create_context(data->stream1);
  register_child(data->context, data->stream1->context);
  data->stream2 = malloc(sizeof(_SIRFilter_2_2));
  data->stream2->context = create_context(data->stream2);
  register_child(data->context, data->stream2->context);
  SIRFilter_2_1_init(data->stream1);
  SIRFilter_2_2_init(data->stream2);
  set_splitter(data->context, DUPLICATE, 2);
  set_joiner(data->context, ROUND_ROBIN, 2, 1, 1);
  create_splitjoin_tape(data->context, SPLITTER, OUTPUT, 0,
		        data->stream1->context, sizeof(int), 2);
  create_splitjoin_tape(data->context, SPLITTER, OUTPUT, 1,
		        data->stream2->context, sizeof(int), 2);
  create_splitjoin_tape(data->context, JOINER, INPUT, 0,
		        data->stream1->context, sizeof(int), 1);
  create_splitjoin_tape(data->context, JOINER, INPUT, 1,
		        data->stream2->context, sizeof(int), 1);  
}

void SIRFilter_3_work(SIRFilter_3 data) {
  printf("%d\n", POP(data->context, int));
}

void SIRFilter_3_init(SIRFilter_3 data) {
  set_stream_type(data->context, FILTER);
  set_push(data->context, 0);
  set_peek(data->context, 0);
  set_pop(data->context, 0);
  set_work(data->context, (work_fn)SIRFilter_3_work);
}

void SIRPipeline_init(SIRPipeline data) {
  set_stream_type(data->context, PIPELINE);
  data->stream1 = malloc(sizeof(_SIRFilter_1));
  data->stream1->context = create_context(data->stream1);
  register_child(data->context, data->stream1->context);
  data->stream2 = malloc(sizeof(_SIRPipeline_2));
  data->stream2->context = create_context(data->stream2);
  register_child(data->context, data->stream2->context);
  data->stream3 = malloc(sizeof(_SIRFilter_3));
  data->stream3->context = create_context(data->stream3);
  register_child(data->context, data->stream3->context);
  SIRFilter_1_init(data->stream1);
  SIRPipeline_2_init(data->stream2);
  SIRFilter_3_init(data->stream3);
  create_tape(data->stream1->context, data->stream2->context, sizeof(int), 4);
  create_tape(data->stream2->context, data->stream3->context, sizeof(int), 2);
  set_work(data->context, (work_fn)hierarchical_work_2);
}

