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

#include <assert.h>
#include <stdlib.h>

#include "streamit.h"

static void add_child_to_list(stream_context *child,
                              stream_context_list **first,
                              stream_context_list **last);

stream_context *create_context(void *p)
{
    stream_context *c = NULL;

    /* allocate the space */
    c = (stream_context*) malloc (sizeof (stream_context));

    /* and fill in the default fields
       there's nothing I can do for the stream_type_data */
    c->input_tape = NULL;
    c->output_tape = NULL;
    c->parent = NULL;
    c->peek_size = -1;
    c->pop_size = -1;
    c->push_size = -1;
    c->type = INVALID_STREAM_TYPE;
    c->work_function = NULL;

    /* initialize the stream_data */
    c->stream_data = p;

    /* done */
    return c;
}

void set_work(stream_context *c, work_fn f)
{
    assert (c);
    assert (f);

    c->work_function = f;
}

void set_peek(stream_context *c, int peeks)
{
    assert (c);
    assert (peeks >= 0);

    c->peek_size = peeks;
}

void set_pop(stream_context *c, int pops)
{
    assert (c);
    assert (pops >= 0);

    c->pop_size = pops;
}

void set_push(stream_context *c, int pushes)
{
    assert (c);
    assert (pushes >= 0);

    c->push_size = pushes;
}

void set_stream_type(stream_context *c, stream_type type)
{
    assert (c);
    assert (type >= FILTER && type <= FEEDBACK_LOOP);

    c->type = type;

    /* now initialize the type_data */
    switch (type)
    {
    case FILTER:
      /* no data to initialize */
        break;
    case PIPELINE:
        c->type_data.pipeline_data.first_child = NULL;
        c->type_data.pipeline_data.last_child = NULL;
        break;
    case SPLIT_JOIN:
    case FEEDBACK_LOOP:
        c->type_data.splitjoin_data.first_child = NULL;
        c->type_data.splitjoin_data.last_child = NULL;
        /* set_splitter() and set_joiner() do the rest of the
         * initialization. */
        break;
    default:
        assert (0);
        break;
    }
}

void register_child(stream_context *c, stream_context *child)
{
    assert (c);
    assert (child);

    /* do some general house keeping */
    child->parent = c;

    /* enter the child into the actual parent's data structures
       those depend on the parent's type */
    switch (c->type)
    {
    case FILTER:
      /* filter cannot register children! */
        assert (0);
        break;
    case PIPELINE:
        add_child_to_list(child,
                          &c->type_data.pipeline_data.first_child,
                          &c->type_data.pipeline_data.last_child);
        break;
    case SPLIT_JOIN:
    case FEEDBACK_LOOP:
        add_child_to_list(child,
                          &c->type_data.splitjoin_data.first_child,
                          &c->type_data.splitjoin_data.last_child);
        break;
    default:
        assert (0);

    }
}

static void add_child_to_list(stream_context *child,
                              stream_context_list **first,
                              stream_context_list **last)
{
    stream_context_list *new_child;
    /* allocate the new node */
    new_child = (stream_context_list*) malloc (sizeof (stream_context_list));
    assert (new_child);
        
    /* initialize the new node */
    new_child->context = child;
    new_child->next = NULL;
        
    /* insert the new node into the list */
    if (*last != NULL)
    {
      /* add the node to the list */
        assert (*first);
        (*last)->next = new_child;
        *last = new_child;
    } else {
      /* this is the first node in the list */
        *first = new_child;
        *last = new_child;
    }
}
