#include <stdlib.h>
#include <assert.h>
#include <stdarg.h>

#include "streamit.h"
#include "streamit_internal.h"

void set_splitter(stream_context *c, splitjoin_type type, int n, ...)
{
  int i;
  va_list ap;
  
  assert(c);
  assert(n > 0);

  c->split_type = type;
  c->num_splits = n;
  if (c->num_joins > 0)
  {
    assert(c->num_splits == c->num_joins);
  }
  if (type == ROUND_ROBIN)
  {
    c->split_ratio = malloc(n * sizeof(int));
    va_start(ap, n);
    for (i = 0; i < n; i++)
      c->split_ratio[i] = va_arg(ap, int);
    va_end(ap);
  }
  c->split_tape = malloc(n * sizeof(tape));
}

void set_joiner(stream_context *c, splitjoin_type type, int n, ...)
{
  int i;
  va_list ap;

  assert(c);
  assert(n > 0);

  c->join_type = type;
  c->num_joins = n;
  if (c->num_splits > 0)
  {
    assert(c->num_splits == c->num_joins);
  }
  if (type == ROUND_ROBIN)
  {
    c->join_ratio = malloc(n * sizeof(int));
    va_start(ap, n);
    for (i = 0; i < n; i++)
      c->join_ratio[i] = va_arg(ap, int);
    va_end(ap);
  }
  c->join_tape = malloc(n * sizeof(tape));
}

void create_split_tape(stream_context *container, int slot,
                       stream_context *dst,
                       int data_size, int tape_length)
{
  tape *new_tape;
  
  assert(container);
  assert(slot >= 0 && slot < container->num_splits);
  assert(dst);

  new_tape = create_tape_internal(data_size, tape_length);
  container->split_tape[slot] = new_tape;
  dst->input_tape = new_tape;
}

void create_join_tape(stream_context *src,
                      stream_context *container, int slot,
                      int data_size, int tape_length)
{
  tape *new_tape;
  
  assert(src);
  assert(container);
  assert(slot >= 0 && slot < container->num_joins);

  new_tape = create_tape_internal(data_size, tape_length);
  src->output_tape = new_tape;
  container->join_tape[slot] = new_tape;
}
