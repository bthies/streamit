/**
 * @file streamit_internal.h
 * Definitions of functions only used within the StreamIt library.
 * This header file should never be used be end programs.
 */

#ifndef __STREAMIT_INTERNAL_H__
#define __STREAMIT_INTERNAL_H__

#include "streamit.h"

/** Create and initialize a new tape, not necessarily connected to anything.
 *
 * @param data_size    Size of an individual tape item
 * @param tape_length  Number of items on the tape
 */
tape *create_tape_internal(int data_size, int tape_length);

void dispatch_messages(void);

#endif // __STREAMIT_INTERNAL_H__
