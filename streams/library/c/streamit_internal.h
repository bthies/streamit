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

#endif /* __STREAMIT_INTERNAL_H__ */
