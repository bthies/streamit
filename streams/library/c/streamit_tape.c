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
    
    // and tell the source and sink that they have a new tape
    src->output_tape = new_tape;
    dst->input_tape = new_tape;
}

tape *create_tape_internal(int data_size, int tape_length)
{
    tape *new_tape;
    int check, nmask;
  
    new_tape = (tape*) malloc (sizeof (tape));
    assert (new_tape);

    // initialize the fields in the new tape
    new_tape->read_pos = 0;
    new_tape->write_pos = 0;
    new_tape->tape_length = tape_length;
    new_tape->data_size = data_size;
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

  
