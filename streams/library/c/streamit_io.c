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

/*
 * streamit_io.c: implementation of built-in IO filters
 * $Id: streamit_io.c,v 1.10 2003-10-09 20:42:06 dmaze Exp $
 */

#include <stdlib.h>
#include <assert.h>
#include <stdio.h>
#include <string.h>

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
  stream_context *c = cc->_context;
  fileio_data *frd = c->stream_data;
  
  /* Do the fread() directly on to the output tape. */
  INCR_TAPE_WRITE(c->output_tape, c->output_tape->data_size);
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
  stream_context *c = cc->_context;
  fileio_data *frd = c->stream_data;
  
  /* Do the fwrite() directly from the input tape. */
  INCR_TAPE_READ(c->input_tape, c->input_tape->data_size);
  fwrite(READ_ADDR(c->input_tape), c->input_tape->data_size, 1, frd->fp);
}

/* These don't really belong here (as I/O), but they do (as predefined
 * filters). */
stream_context *streamit_identity_create(void)
{
  stream_context *c;
  
  c = create_context(NULL);

  set_stream_type(c, FILTER);
  set_push(c, 1);
  set_peek(c, 1);
  set_pop(c, 1);
  set_work(c, (work_fn)streamit_identity_work);
  
  return c;
}

void streamit_identity_work(ContextContainer cc)
{
  VARS_DEFAULTB();
  stream_context *c = cc->_context;
  int s = c->input_tape->data_size;
  LOCALIZE_DEFAULTB(c);
  /* We don't have the type (though we could), do this the hard way... */
  streamit_memcpy((char *)__wd + INCR_TAPE_LOCALB(__wp, __wm, s),
                  (char *)__rd + INCR_TAPE_LOCALB(__rp, __rm, s),
                  s);
  UNLOCALIZE_DEFAULTB(c);
}
