/*
 * Fib.c: translated fib StreaMIT example
 * $Id: Fib.c,v 1.1 2001-09-27 16:58:35 dmaze Exp $
 */

#include "streamit.h"
#include <stdlib.h>

typedef struct Fib_1_1_data
{
  stream_context *c;
} Fib_1_1_data;

void Fib_1_1_init(Fib_1_1_data *d, void *p);
void Fib_1_1_work(Fib_1_1_data *d, tape *in_tape, tape *out_tape);

void Fib_1_1_init(Fib_1_1_data *d, void *p)
{
  set_stream_type(d->c, FILTER);
  set_push(d->c, 1);
  set_pop(d->c, 1);
  set_peek(d->c, 1);
  set_work(d->c, (work_fn)Fib_1_1_work);
}

void Fib_1_1_work(Fib_1_1_data *d, tape *in_tape, tape *out_tape)
{
  PUSH(out_tape, int, (PEEK(in_tape, int, 1) + PEEK(in_tape, int, 0)));
  POP(in_tape, int);
}

typedef struct Fib_1_data
{
  stream_context *c;
  Fib_1_1_data *child1;
  int itape[2];
} Fib_1_data;

void Fib_1_init(Fib_1_data *d, void *p);
void Fib_1_work(Fib_1_data *d, tape *in_tape, tape *out_tape);
int Fib_1_initPath(Fib_1_data *d, int index);

void Fib_1_init(Fib_1_data *d, void *p)
{
  set_stream_type(d->c, FEEDBACK_LOOP);
  set_delay(d->c, 2, (delay_fn)Fib_1_initPath);
  set_joiner(d->c, ROUND_ROBIN, 2, 0, 1);
  set_work(d->c, (work_fn)Fib_1_work);
  set_splitter(d->c, DUPLICATE, 0);

  d->child1 = malloc(sizeof(Fib_1_1_data));
  d->child1->c = create_context(d->child1);
  register_child(d->c, d->child1->c);
  Fib_1_1_init(d->child1, d);
}

void Fib_1_work(Fib_1_data *d, tape *tape_in, tape *tape_out)
{
  int val = d->itape[0] + d->itape[1];
  d->itape[0] = d->itape[1];
  d->itape[1] = val;
  PUSH(tape_out, int, val);
}

int Fib_1_initPath(Fib_1_data *d, int index)
{
    return 1;
}

/* TODO: we need to_canon/from_canon functions.  This depends on
 * knowing how tapes work. */

typedef struct Fib_2_data
{
    stream_context *c;
} Fib_2_data;

void Fib_2_init(Fib_2_data *d, void *p);
void Fib_2_work(Fib_2_data *d, tape *in_tape, tape *out_tape);

void Fib_2_init(Fib_2_data *d, void *p)
{
    set_stream_type(d->c, FILTER);
    set_pop(d->c, 1);
    set_work(d->c, (work_fn)Fib_2_work);
}

void Fib_2_work(Fib_2_data *d, tape *in_tape, tape *out_tape)
{
    printf("%d\n", POP(in_tape, int));
}

typedef struct Fib_data
{
    stream_context *c;
    Fib_1_data *child1;
    Fib_2_data *child2;
    int itape[2];
} Fib_data;

void Fib_init(Fib_data *d, void *p)
{
    set_stream_type(d->c, PIPELINE);

    d->child1 = malloc(sizeof(Fib_1_data));
    d->child1->c = create_context(d->child1);
    register_child(d->c, d->child1->c);
    Fib_1_init(d->child1, d);

    d->child2 = malloc(sizeof(Fib_2_data));
    d->child2->c = create_context(d->child2);
    register_child(d->c, d->child2->c);
    Fib_2_init(d->child2, d);
}

void Fib_work(Fib_data *d, tape *in_tape, tape *out_tape)
{
  int val = d->child1->itape[0] + d->child1->itape[1];
  d->child1->itape[0] = d->child1->itape[1];
  d->child1->itape[1] = val;
  printf("%d\n", val);
}

int main(int argc, char **argv)
{
    Fib_data *test = malloc(sizeof(Fib_data));
    test->c = create_context(test);
    Fib_init(test, NULL);
    
    streamit_run(test);
}
