#include <assert.h>
#include <stdio.h>
#include <stdlib.h>
#include <setjmp.h>
#include <signal.h>
#include <sys/time.h>
#include <unistd.h>

#include "streamit.h"
#include "streamit_internal.h"

void ERROR (void *data, char *error_msg)
{
    fprintf (stderr, "ERROR:%s\nExiting...\n", error_msg);
    exit (1);
}

void connect_tapes (stream_context *c)
{
    stream_context_list *child;

    assert (c);

    switch (c->type)
    {
    case FILTER:
      /* filter doesn't need to connect its children's tapes */
        break;

    case PIPELINE:
      /* if no children, quit */
        if (c->type_data.pipeline_data.first_child == NULL) break;
        
        /* set the first and last child's tapes */
        c->type_data.pipeline_data.first_child->context->input_tape =
            c->input_tape;
        c->type_data.pipeline_data.last_child->context->output_tape =
            c->output_tape;

        /* now go through all the children, and initialize them */
        for (child = c->type_data.pipeline_data.first_child;
             child;
             child = child->next)
            connect_tapes (child->context);

        break;

    case SPLIT_JOIN:
      /* Attach the input and output tapes to the "one" side of
         the splitter and joiner. */
        c->type_data.splitjoin_data.splitter.one_tape = c->input_tape;
        c->type_data.splitjoin_data.joiner.one_tape = c->output_tape;
        
        /* Go through all of the children and initialize them. */
        for (child = c->type_data.pipeline_data.first_child;
             child;
             child = child->next)
            connect_tapes (child->context);

        break;
    case FEEDBACK_LOOP:
      /* Attach the input and output tapes to slot 0 on the
         many side of the splitter and joiner. */
        c->type_data.splitjoin_data.joiner.tape[0] = c->input_tape;
        c->type_data.splitjoin_data.splitter.tape[0] = c->output_tape;
        
        /* Go through all of the children and initialize them. */
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

/* Things to worry about for signal handlers: */
static void handle_sigterm(int signum);
static jmp_buf jmp_env;

void streamit_run (stream_context *c, int argc, char **argv)
{
    /* NB: volatile to force preservation across setjmp()/longjmp() */
    volatile int niters = -1, do_time = 0;
    volatile int iters_run = 0;
    struct timeval start, now;
    volatile struct timeval diff;
    struct sigaction sa;
    int flag;
    
    while ((flag = getopt(argc, argv, "i:t")) != -1)
        switch (flag)
        {
        case 'i':
            niters = atoi(optarg);
            break;
        case 't':
            do_time = 1;
            break;
        case '?':
            fprintf(stderr, "Unrecognized option '-%c'\n", optopt);
            return;
        }
    
    connect_tapes (c);

    /* Trap SIGTERM (normal kill) and SIGINT (C-c). */
    sa.sa_handler = handle_sigterm;
    sigemptyset(&sa.sa_mask);
    sa.sa_flags = 0;
    sigaction(SIGTERM, &sa, NULL);
    sigaction(SIGINT, &sa, NULL);

    /* Record the start time now. */
    gettimeofday(&start, NULL);

    /* Don't run if we're calling back from the signal handler. */
    if (!sigsetjmp(jmp_env, 1))
      /* run the work function indefinitely */
      while (niters != 0)
      {
        c->work_function (c->stream_data);
        dispatch_messages();
        if (niters > 0)
          niters--;
        iters_run++;
      }

    if (do_time)
    {
      double udiff;
      
      gettimeofday(&now, NULL);
      diff.tv_sec = now.tv_sec - start.tv_sec;
      diff.tv_usec = now.tv_usec - start.tv_usec;
      if (diff.tv_usec < 0)
      {
        diff.tv_usec += 1000000;
        diff.tv_sec--;
      }
      udiff = diff.tv_sec + (double)diff.tv_usec/1.0e6;
  
      fprintf(stderr, "Ran %d iterations in %g seconds (avg. %g/iter)\n",
              iters_run, udiff, udiff/(double)(iters_run));
    }
}

static void handle_sigterm(int signum)
{
  siglongjmp(jmp_env, 1);
}
