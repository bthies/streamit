/*
 * Copyright 2003 by the Massachusetts Institute of Technology.
 *
 * Permission to use, copy, modify, and distribute this
 * software and its documentation for any purpose and without
 * fee is hereby granted, provided that the above copyright
 * notice appear in all copies and that both that copyright
 * notice and this permission notice appear in supporting
 * documentation, and that the name of M.I.T. not be used in
 * advertising or publicity pertaining to distribution of the
 * software without specific, written prior permission.
 * M.I.T. makes no representations about the suitability of
 * this software for any purpose.  It is provided "as is"
 * without express or implied warranty.
 */

#include <stdlib.h>
#include <assert.h>
#include <stdarg.h>
#include <stdio.h>

#include <memory.h>

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
  if (type == WEIGHTED_ROUND_ROBIN)
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
  if (type == WEIGHTED_ROUND_ROBIN)
  {
    va_list ap;
    va_start(ap, n);
    set_splitjoin_rr(&c->type_data.splitjoin_data.joiner, ap);
    va_end(ap);
  }
}

static void set_splitjoin(one_to_many *p, splitjoin_type type, int n)
{
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

  assert(p->type == WEIGHTED_ROUND_ROBIN);
  assert(p->tcache == NULL);

  p->tcache = malloc(p->slots * sizeof(tape *));
  for (i = 0, j = 0, slot = 0; slot < p->slots; j++, slot++)
  {
    while (j >= p->ratio[i])
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
    if (c->type_data.splitjoin_data.splitter.type == WEIGHTED_ROUND_ROBIN &&
        !c->type_data.splitjoin_data.splitter.tcache)
        build_tape_cache(&c->type_data.splitjoin_data.splitter);
    
    switch(c->type_data.splitjoin_data.splitter.type)
    {
    case NULL_SJ:
      /* do nothing */
      break;
    case DUPLICATE:
        /* Read one item and distribute it. */
        INCR_TAPE_READ(input_tape, input_tape->data_size);
        for (slot = 0; slot < c->type_data.splitjoin_data.splitter.fan; slot++)
        {
            output_tape = c->type_data.splitjoin_data.splitter.tape[slot];
            INCR_TAPE_WRITE(output_tape, output_tape->data_size);
            COPY_TAPE_ITEM(input_tape, output_tape);
        }
        break;
        
    case ROUND_ROBIN:
        /* Read enough items to make one loop around. */
        for (slot = 0; slot < c->type_data.splitjoin_data.splitter.fan; slot++)
        {
            output_tape = c->type_data.splitjoin_data.splitter.tape[slot];
            INCR_TAPE_READ(input_tape, input_tape->data_size);
            INCR_TAPE_WRITE(output_tape, output_tape->data_size);
            COPY_TAPE_ITEM(input_tape, output_tape);
        }
        break;    
        
    case WEIGHTED_ROUND_ROBIN:
        {
            int size = input_tape->data_size;
            int fan = c->type_data.splitjoin_data.splitter.fan;
            int *ratios = c->type_data.splitjoin_data.splitter.ratio;
            tape **tapes = c->type_data.splitjoin_data.splitter.tape;
            int nTape;
            /* Read enough items to make one loop around. */
            for (nTape = 0; nTape < fan; nTape++)
            {
                int ratio = ratios [nTape];
                if (ratio < 3)
                {
                    output_tape = tapes[nTape];
                    switch (ratio)
                    {
                    case 2:
                        INCR_TAPE_READ(input_tape, size);
                        INCR_TAPE_WRITE(output_tape, size);
                        COPY_TAPE_ITEM(input_tape, output_tape);
                    case 1:
                        INCR_TAPE_READ(input_tape, size);
                        INCR_TAPE_WRITE(output_tape, size);
                        COPY_TAPE_ITEM(input_tape, output_tape);
                    case 0:
                        break;
                    }
                } else {
                  /* there will be four cases here:
                     one will not need to break up any tapes
                     another will need to break up both tapes
                     and two for breaking one tape each */
                    tape *output_tape = tapes[nTape];
                    int read_mask = input_tape->mask;
                    int write_mask = output_tape->mask;
                    
                    int read_pos = (input_tape->read_pos + size) & read_mask;
                    int write_pos = (output_tape->write_pos + size) & write_mask;
                    int data_size = ratio * size;
                    int read_max = read_mask + 1;
                    int write_max = write_mask + 1;
                    
                    if (read_pos + data_size <= read_max)
                    {
                      /* don't break the read tape */
                      if (write_pos + data_size <= write_max)
                      {
                        /* no breaking of tapes! */
                        memcpy ((char *)(output_tape->data) + write_pos, 
                                (char *)(input_tape->data) + read_pos, 
                                data_size);
                            
                        input_tape->read_pos = read_pos + data_size - size;
                        output_tape->write_pos = write_pos + data_size - size;
                      } else {
                        /* break the write tape */
                        int copy_first = write_max - write_pos;
                        memcpy ((char *)(output_tape->data) + write_pos,
                                (char *)(input_tape->data) + read_pos,
                                copy_first);
                        memcpy ((char *)output_tape->data,
                                (char *)(input_tape->data) + read_pos + 
                                copy_first,
                                data_size - copy_first);
                            
                        input_tape->read_pos = read_pos + data_size - size;
                        output_tape->write_pos = data_size - copy_first - size;
                      }
                    } else
                    {
                      /* break the read tape */
                      if (write_pos + data_size <= write_max)
                      {
                        /* don't break the write tape */
                        int copy_first = read_max - read_pos;
                        memcpy ((char *)(output_tape->data) + write_pos,
                                (char *)(input_tape->data) + read_pos,
                                copy_first);
                        memcpy ((char *)(output_tape->data) + write_pos +
                                copy_first,
                                input_tape->data,
                                data_size - copy_first);
                            
                        input_tape->read_pos = data_size - copy_first - size;
                        output_tape->write_pos = write_pos + data_size - size; 
                      } else {
                        if (write_max - write_pos > read_max - read_pos)
                        {
                          /* there is more data on the write tape - break the
                             read tape first */
                          int copy_second, copy_third;
                          int copy_first = read_max - read_pos;
                          memcpy ((char *)(output_tape->data) + write_pos,
                                  (char *)(input_tape->data) + read_pos,
                                  copy_first);
                                
                          /* break the read tape */
                          copy_second = write_max - write_pos - copy_first;
                          memcpy ((char *)(output_tape->data) +
                                  write_pos + copy_first,
                                  input_tape->data,
                                  copy_second);
                                
                          /* break the write tape */
                          copy_third = data_size - copy_second - copy_first;
                          memcpy (output_tape->data,
                                  (char *)(input_tape->data) + copy_second,
                                  copy_third);
                                
                          input_tape->read_pos = copy_second + copy_third - size;
                          output_tape->write_pos = copy_third - size;
                        } else {
                          /* there is more data on the read tape - break the
                             write tape first */
                          int copy_first = write_max - write_pos;
                          int copy_second, copy_third;
                          memcpy ((char *)(output_tape->data) + write_pos,
                                  (char *)(input_tape->data) + read_pos,
                                  copy_first);
                                
                          /* break the write tape */
                          copy_second = read_max - read_pos - copy_first;
                          memcpy (output_tape->data,
                                  (char *)(input_tape->data) +
                                  read_pos + copy_first,
                                  copy_second);
                                
                          /* break the read tape */
                          copy_third = data_size - copy_first - copy_second;
                          memcpy ((char *)(output_tape->data) + copy_second,
                                  input_tape->data,
                                  copy_third);
                                
                          input_tape->read_pos = copy_third - size;
                          output_tape->write_pos = copy_second + copy_third - size;
                        }
                      }
                    }
                }
            }
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
  if (c->type_data.splitjoin_data.joiner.type == WEIGHTED_ROUND_ROBIN &&
      !c->type_data.splitjoin_data.joiner.tcache)
    build_tape_cache(&c->type_data.splitjoin_data.joiner);

  switch (c->type_data.splitjoin_data.joiner.type)
  {
  case NULL_SJ:
    /* do nothing */
    break;
  case ROUND_ROBIN:
    for (slot = 0; slot < c->type_data.splitjoin_data.joiner.fan; slot++)
    {
      input_tape = c->type_data.splitjoin_data.joiner.tape[slot];
      INCR_TAPE_READ(input_tape, input_tape->data_size);
      INCR_TAPE_WRITE(output_tape, output_tape->data_size);
      COPY_TAPE_ITEM(input_tape, output_tape);
    }
    break;
    
  case WEIGHTED_ROUND_ROBIN:
    for (slot = 0; slot < c->type_data.splitjoin_data.joiner.slots; slot++)
    {
      input_tape = c->type_data.splitjoin_data.joiner.tcache[slot];
      INCR_TAPE_READ(input_tape, input_tape->data_size);
      INCR_TAPE_WRITE(output_tape, output_tape->data_size);
      COPY_TAPE_ITEM(input_tape, output_tape);
    }
    break;
    
  default:
    assert(0);
  }
}
