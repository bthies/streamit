/*
 * streamit_io.c: implementation of built-in IO filters
 * $Id: streamit_io.c,v 1.2 2001-11-07 17:42:45 dmaze Exp $
 */

#include <stdlib.h>
#include <assert.h>
#include <stdio.h>

#include "streamit.h"
#include "streamit_internal.h"

typedef struct fileio_data
{
  FILE *fp;
} fileio_data;

stream_context *streamit_filereader_create(char *filename)
{
  stream_context *c;
  fileio_data *frd;
  
  frd = malloc(sizeof(struct fileio_data));
  c = create_context(frd);

  frd->fp = fopen(filename, "r");
  assert(frd->fp);

  set_stream_type(c, FILTER);
  set_push(c, 1);
  set_peek(c, 0);
  set_pop(c, 0);
  set_work(c, (work_fn)streamit_filereader_work);
  
  return c;
}

void streamit_filereader_work(ContextContainer cc)
{
  stream_context *c = cc->context;
  fileio_data *frd = c->stream_data;
  
  /* Do the fread() directly on to the output tape. */
  INCR_TAPE_WRITE(c->output_tape);
  fread(WRITE_ADDR(c->output_tape), c->output_tape->data_size, 1, frd->fp);
}

stream_context *streamit_filewriter_create(char *filename)
{
  stream_context *c;
  fileio_data *frd;
  
  frd = malloc(sizeof(struct fileio_data));
  c = create_context(frd);

  frd->fp = fopen(filename, "w");
  assert(frd->fp);
  
  set_stream_type(c, FILTER);
  set_push(c, 0);
  set_peek(c, 0);
  set_pop(c, 1);
  set_work(c, (work_fn)streamit_filewriter_work);
  
  return c;
}

void streamit_filewriter_work(ContextContainer cc)
{
  stream_context *c = cc->context;
  fileio_data *frd = c->stream_data;
  
  /* Do the fwrite() directly from the input tape. */
  INCR_TAPE_READ(c->input_tape);
  fwrite(READ_ADDR(c->input_tape), c->input_tape->data_size, 1, frd->fp);
}
