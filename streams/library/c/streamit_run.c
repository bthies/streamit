#include <stdlib.h>
#include <assert.h>

#include "streamit.h"
#include "streamit_internal.h"

void connect_tapes (stream_context *c)
{
    stream_context_list *child;

    assert (c);

    switch (c->type)
    {
    case FILTER:
        // filter doesn't need to connect its children's tapes
        break;

    case PIPELINE:
        // if no children, quit
        if (c->type_data.pipeline_data.first_child == NULL) break;
        
        // set the first and last child's tapes
        c->type_data.pipeline_data.first_child->context->input_tape =
            c->input_tape;
        c->type_data.pipeline_data.last_child->context->output_tape =
            c->output_tape;

        // now go through all the children, and initialize them
        for (child = c->type_data.pipeline_data.first_child;
             child;
             child = child->next)
            connect_tapes (child->context);

        break;

    case SPLIT_JOIN:
        // Attach the input and output tapes to the "one" side of
        // the splitter and joiner.
        c->type_data.splitjoin_data.splitter.one_tape = c->input_tape;
        c->type_data.splitjoin_data.joiner.one_tape = c->output_tape;
        
        // Go through all of the children and initialize them.
        for (child = c->type_data.pipeline_data.first_child;
             child;
             child = child->next)
            connect_tapes (child->context);

        break;
    case FEEDBACK_LOOP:
        // Attach the input and output tapes to slot 0 on the
        // many side of the splitter and joiner.
        c->type_data.splitjoin_data.joiner.tape[0] = c->input_tape;
        c->type_data.splitjoin_data.splitter.tape[0] = c->output_tape;
        
        // Go through all of the children and initialize them.
        for (child = c->type_data.pipeline_data.first_child;
             child;
             child = child->next)
            connect_tapes (child->context);

        break;

    default:
        assert (0);
        break;
    }
}

void streamit_run (stream_context *c)
{
    connect_tapes (c);

    // run the work function indefinitely
    while (1)
    {
        c->work_function (c->stream_data);
    }
}
