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
  assert(c->type == SPLIT_JOIN ||
         c->type == FEEDBACK_LOOP);
  set_splitjoin(&c->type_data.splitjoin_data.splitter, type, n);
  if (type == ROUND_ROBIN)
  {
    va_list ap;
    va_start(ap, n);
    set_splitjoin_rr(&c->type_data.splitjoin_data.splitter, ap);
    va_end(ap);
  }
}

void set_joiner(stream_context *c, splitjoin_type type, int n, ...)
{
  assert(c);
  assert(c->type == SPLIT_JOIN ||
         c->type == FEEDBACK_LOOP);
  set_splitjoin(&c->type_data.splitjoin_data.joiner, type, n);
  if (type == ROUND_ROBIN)
  {
    va_list ap;
    va_start(ap, n);
    set_splitjoin_rr(&c->type_data.splitjoin_data.joiner, ap);
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
  p->one_tape = NULL;
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

void create_splitjoin_tape(stream_context *container,
                           split_or_join sj, in_or_out io, int slot,
                           stream_context *other,
                           int data_size, int tape_length)
{
  one_to_many *om;
  tape *new_tape;

  assert(container);
  assert(container->type == SPLIT_JOIN ||
         container->type == FEEDBACK_LOOP);
  assert(other);

  /* Sanity check: don't allow explicit connections to block-external
   * slots. */
  assert(!(container->type == SPLIT_JOIN &&
           sj == SPLITTER &&
           io == INPUT));
  assert(!(container->type == SPLIT_JOIN &&
           sj == JOINER &&
           io == OUTPUT));
  assert(!(container->type == FEEDBACK_LOOP &&
           sj == JOINER &&
           io == INPUT &&
           slot == 0));
  assert(!(container->type == FEEDBACK_LOOP &&
           sj == SPLITTER &&
           io == OUTPUT &&
           slot == 0));
  
  if (sj == SPLITTER)
    om = &container->type_data.splitjoin_data.splitter;
  else
    om = &container->type_data.splitjoin_data.joiner;
  
  new_tape = create_tape_internal(data_size, tape_length);
  
  /* Figure out if this is on the one side or the many side. */
  if ((sj == SPLITTER && io == INPUT) ||
      (sj == JOINER && io == OUTPUT))
  {
    /* One side. */
    om->one_tape = new_tape;
  }
  else
  {
    /* Many side. */
    assert(slot >= 0 && slot < om->fan);
    om->tape[slot] = new_tape;
  }
  
  /* Attach the tape to the other object.  If io is INPUT, then
   * we're attaching to the input of the splitter/joiner, and therefore
   * to the output of the other object. */
  if (io == INPUT)
    other->output_tape = new_tape;
  else
    other->input_tape = new_tape;
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
  tape *input_tape, *output_tape;
  int slot;
  
  assert(c);
  assert(c->type == SPLIT_JOIN ||
         c->type == FEEDBACK_LOOP);

  input_tape = c->type_data.splitjoin_data.splitter.one_tape;

  /* Make the splitter tape cache valid if it's needed. */
  if (c->type_data.splitjoin_data.splitter.type == ROUND_ROBIN &&
      !c->type_data.splitjoin_data.splitter.tcache)
    build_tape_cache(&c->type_data.splitjoin_data.splitter);

  switch(c->type_data.splitjoin_data.splitter.type)
  {
  case DUPLICATE:
    /* Read one item and distribute it. */
    INCR_TAPE_READ(input_tape);
    for (slot = 0; slot < c->type_data.splitjoin_data.splitter.fan; slot++)
    {
      output_tape = c->type_data.splitjoin_data.splitter.tape[slot];
      INCR_TAPE_WRITE(output_tape);
      COPY_TAPE_ITEM(input_tape, output_tape);
    }
    break;
      
  case ROUND_ROBIN:
    /* Read enough items to make one loop around. */
    for (slot = 0; slot < c->type_data.splitjoin_data.splitter.slots; slot++)
    {
      output_tape = c->type_data.splitjoin_data.splitter.tcache[slot];
      INCR_TAPE_READ(input_tape);
      INCR_TAPE_WRITE(output_tape);
      COPY_TAPE_ITEM(input_tape, output_tape);
    }
    break;
    
  default:
    assert(0);
  }
}

void run_joiner(stream_context *c)
{
  tape *input_tape, *output_tape;
  int slot;
  
  assert(c);
  assert(c->type == SPLIT_JOIN ||
         c->type == FEEDBACK_LOOP);

  output_tape = c->type_data.splitjoin_data.joiner.one_tape;

  /* Make the splitter tape cache valid if it's needed. */
  if (c->type_data.splitjoin_data.joiner.type == ROUND_ROBIN &&
      !c->type_data.splitjoin_data.joiner.tcache)
    build_tape_cache(&c->type_data.splitjoin_data.joiner);

  switch (c->type_data.splitjoin_data.joiner.type)
  {
  case ROUND_ROBIN:
    for (slot = 0; slot < c->type_data.splitjoin_data.joiner.slots; slot++)
    {
      input_tape = c->type_data.splitjoin_data.joiner.tcache[slot];
      INCR_TAPE_READ(input_tape);
      INCR_TAPE_WRITE(output_tape);
      COPY_TAPE_ITEM(input_tape, output_tape);
    }
    break;
    
  default:
    assert(0);
  }
}
