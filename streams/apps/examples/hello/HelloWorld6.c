/*
 * HelloWorld6.c: translated "hello, world" StreaMIT example
 * $Id: HelloWorld6.c,v 1.6 2001-09-27 20:30:10 dmaze Exp $
 */

#include "streamit.h"
#include <stdlib.h>

/* dzm: I expect that the code will be output in roughly this order,
 * actually. */

typedef struct HelloWorld6_1_data
{
  stream_context *c;
  int x;
} HelloWorld6_1_data;

void HelloWorld6_1_init(HelloWorld6_1_data *d, void *p);
void HelloWorld6_1_work(HelloWorld6_1_data *d, tape *in_tape, tape *out_tape);

/* dzm: We need to tell the library the type/size of the channel. */
void HelloWorld6_1_init(HelloWorld6_1_data *d, void *p)
{
  set_stream_type(d->c, FILTER);
  set_push(d->c, 1);
  set_work(d->c, (work_fn)HelloWorld6_1_work);
  d->x = 0;
}

/* dzm: subtlety involving the ++ operator and macros.  Hmm. */
void HelloWorld6_1_work(HelloWorld6_1_data *d, tape *in_tape, tape *out_tape)
{
  d->x++;
  PUSH(out_tape, int, d->x);
}

typedef struct HelloWorld6_2_data
{
  stream_context *c;
} HelloWorld6_2_data;

void HelloWorld6_2_init(HelloWorld6_2_data *d, void *p);
void HelloWorld6_2_work(HelloWorld6_2_data *d, tape *in_tape, tape *out_tape);

void HelloWorld6_2_init(HelloWorld6_2_data *d, void *p)
{
  set_stream_type(d->c, FILTER);
  set_pop(d->c, 1);
  set_work(d->c, (work_fn)HelloWorld6_2_work);
}

/* dzm: Way magical. */
void HelloWorld6_2_work(HelloWorld6_2_data *d, tape *in_tape, tape *out_tape)
{
  printf("%d\n", POP(in_tape, int));
}

typedef struct HelloWorld6_data
{
  stream_context *c;
  HelloWorld6_1_data *child1;
  HelloWorld6_2_data *child2;
} HelloWorld6_data;

void HelloWorld6_init(HelloWorld6_data *d, void *p);
void HelloWorld6_work(HelloWorld6_data *d, tape *in_tape, tape *out_tape);

void HelloWorld6_init(HelloWorld6_data *d, void *p)
{
  set_stream_type(d->c, PIPELINE);
  set_work(d->c, (work_fn)HelloWorld6_work);
  
  d->child1 = malloc(sizeof(HelloWorld6_1_data));
  d->child1->c = create_context(d->child1);
  register_child(d->c, d->child1->c);
  HelloWorld6_1_init(d->child1, d);

  create_tape(d->child1->c, d->child2->c, sizeof(int), 1);

  d->child2 = malloc(sizeof(HelloWorld6_2_data));
  d->child2->c = create_context(d->child2);
  register_child(d->c, d->child2->c);
  HelloWorld6_2_init(d->child2, d);
}

void HelloWorld6_work(HelloWorld6_data *d, tape *in_tape, tape *out_tape)
{
  int itape[1];
  d->child1->x++;
  itape[0] = d->child1->x;
  printf("%d\n", itape[0]);
}

int main(int argc, char **argv)
{
  HelloWorld6_data *test = malloc(sizeof(HelloWorld6_data));
  test->c = create_context(test);
  HelloWorld6_init(test, NULL);
  
  streamit_run(test->c);
}
