#include <stdlib.h>
#include <assert.h>
#include <stdarg.h>

#include "streamit.h"
#include "streamit_internal.h"

static void set_splitjoin(one_to_many *p, splitjoin_type type, int n);
static void set_splitjoin_rr(one_to_many *p, va_list ap);
static void build_tape_cache(one_to_many *p);

void set_splitter(stream_context *c, splitjoin_type type, int n, ...)
{
  assert(c);
  set_splitjoin(&c->splitter, type, n);
  if (type == ROUND_ROBIN)
  {
    va_list ap;
    va_start(ap, n);
    set_splitjoin_rr(&c->splitter, ap);
    va_end(ap);
  }
}

void set_joiner(stream_context *c, splitjoin_type type, int n, ...)
{
  assert(c);
  set_splitjoin(&c->joiner, type, n);
  if (type == ROUND_ROBIN)
  {
    va_list ap;
    va_start(ap, n);
    set_splitjoin_rr(&c->joiner, ap);
    va_end(ap);
  }
}

static void set_splitjoin(one_to_many *p, splitjoin_type type, int n)
{
  int i, total;
  
  assert(p);
  assert(n > 0);

  p->type = type;
  p->fan = n;
  p->ratio = NULL;
  p->slots = 0;
  p->tape = malloc(n * sizeof(tape *));
  p->tcache = NULL;
}

static void set_splitjoin_rr(one_to_many *p, va_list ap)
{
  int i, total;
  
  p->ratio = malloc(p->fan * sizeof(int));
  total = 0;
  for (i = 0; i < p->fan; i++)
  {
    p->ratio[i] = va_arg(ap, int);
    total += p->ratio[i];
  }
  p->slots = total;
}

void create_split_tape(stream_context *container, int slot,
                       stream_context *dst,
                       int data_size, int tape_length)
{
  tape *new_tape;
  
  assert(container);
  assert(slot >= 0 && slot < container->splitter.fan);
  assert(dst);

  new_tape = create_tape_internal(data_size, tape_length);
  container->splitter.tape[slot] = new_tape;
  dst->input_tape = new_tape;
}

void create_join_tape(stream_context *src,
                      stream_context *container, int slot,
                      int data_size, int tape_length)
{
  tape *new_tape;
  
  assert(src);
  assert(container);
  assert(slot >= 0 && slot < container->joiner.fan);

  new_tape = create_tape_internal(data_size, tape_length);
  src->output_tape = new_tape;
  container->joiner.tape[slot] = new_tape;
}

static void build_tape_cache(one_to_many *p)
{
  int i, j, slot;

  assert(p->type == ROUND_ROBIN);
  assert(p->tcache == NULL);

  p->tcache = malloc(p->slots * sizeof(tape *));
  for (i = 0, j = 0, slot = 0; slot < p->slots; j++, slot++)
  {
    if (j >= p->ratio[i])
    {
      j = 0;
      i++;
    }
    p->tcache[slot] = p->tape[i];
  }
}

void run_splitter(stream_context *c)
{
  assert(c);
  assert(c->type == SPLIT_JOIN);
}
