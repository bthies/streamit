#ifndef __STREAMIT_INTERNAL_H__
#define __STREAMIT_INTERNAL_H__

#include "streamit.h"

void connect_tapes (stream_context *c);
tape *create_tape_internal(int data_size, int tape_length);

#endif // __STREAMIT_INTERNAL_H__
