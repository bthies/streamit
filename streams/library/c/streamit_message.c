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

#include <malloc.h>
#include <stdio.h>

#include "streamit.h"
#include "streamit_internal.h"

/* Keep a list of messages that have been sent, but not yet received.
 * Right now we only have best-effort delivery, with the result that
 * messages get sent between steady-state executions.  For consistency,
 * record the targets at the time of message dispatch; we might need
 * this information, along with a better data structure, when we do
 * latency. */
struct queued_message
{
  struct queued_message *next;
  stream_context *context;
  message_fn fn;
  void *params;
};
static struct queued_message *msg_queue = NULL;

portal create_portal(void)
{
  portal p = malloc(sizeof(_portal));
  p->receiver = NULL;
  return p;
}

void register_receiver(portal p, stream_context *receiver,
                       interface_table vtbl, latency l)
{
  portal_receiver *rec;
  
  /* malloc a new portal_receiver object. */
  rec = malloc(sizeof(portal_receiver));
  rec->next = p->receiver;
  rec->context = receiver;
  rec->vtbl = vtbl;
  /* Ignore latency for now. */
  p->receiver = rec;
}

void register_sender(portal p, stream_context *sender, latency l)
{
  /* Ignore; our current model doesn't deal. */
}

void send_message(portal p, int msgid, latency l, void *params)
{
  /* Again, ignore the latency. */
  /* Go through every receiver in the portal, and push an appropriate
   * object into the message queue. */
  portal_receiver *rec = p->receiver;
  while (rec)
  {
    struct queued_message *q = malloc(sizeof(struct queued_message));
    q->next = msg_queue;
    q->fn = (rec->vtbl)[msgid];
    q->context = rec->context;
    q->params = params;
    msg_queue = q;
    rec = rec->next;
  }
}

void dispatch_messages(void)
{
  /* Dispatch every message in the queue. */
  while (msg_queue)
  {
    struct queued_message *q = msg_queue;
    (q->fn)(q->context->stream_data, q->params);
    msg_queue = q->next;
    free(q->params);
    free(q);
  }
}
