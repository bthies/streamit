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

#include "streamit.h"
#include "streamit_internal.h"

void create_tape (stream_context *src, stream_context *dst, int data_size, int tape_length)
{
    tape *new_tape;
    assert (src);
    assert (dst);
    assert (data_size > 0);
    assert (tape_length > 0);

    new_tape = create_tape_internal(data_size, tape_length);
    
    /* and tell the source and sink that they have a new tape */
    src->output_tape = new_tape;
    dst->input_tape = new_tape;
}

tape *create_tape_internal(int data_size, int tape_length)
{
    tape *new_tape;
    int check, nmask;
  
    new_tape = (tape*) malloc (sizeof (tape));
    assert (new_tape);

    /* initialize the fields in the new tape */
    new_tape->read_pos = 0;
    new_tape->write_pos = 0;
    new_tape->data_size = data_size;

    /* allocate the buffer and compute the mask */
	{
		int totalSize = data_size * tape_length;

		/* make sure that totalSize is a pow of 2 */
		assert (totalSize > 0 && (totalSize & (totalSize - 1)) == 0);

		new_tape->mask = totalSize - 1;
		new_tape->data = (char*)malloc (totalSize);
		return new_tape;
	}

    new_tape->data = malloc (data_size * tape_length);
    new_tape->mask = 0xd00d;

    check = 1;
    nmask = ~0;
    while (check != 0)
    {
      if (check == tape_length)
      {
        new_tape->mask = ~nmask;
        break;
      }
      check <<= 1;
      nmask <<= 1;
    }
    
    assert(new_tape->mask != 0xd00d);

    return new_tape;
}

  
