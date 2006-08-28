/*
 * Copyright 2006 by the Massachusetts Institute of Technology.
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

#ifndef __DATA_PRODUCER_H
#define __DATA_PRODUCER_H

#include <socket_holder.h>
#include <serializable.h>

class data_producer : public socket_holder, public serializable {

  char *data_buffer;
  int buf_offset;

  int items_sent;

 public:

  data_producer();

  virtual void write_object(object_write_buffer *);
  virtual void read_object(object_write_buffer *);

  void write_chunk(void *buf, int size, int nitems);
  void write_item(void *buf, int size);

  void write_int(int);
  void write_float(float);

  void flush();

};


#endif
