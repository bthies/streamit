#ifndef STREAMIT_H
#define STREAMIT_H

typedef struct stream_context {
} stream_context;
stream_context *create_context(void *p);
typedef struct portal {
} portal;
typedef struct tape {
  void *data;
  int read_pos;
  int write_pos;
  int len;
  int size;
} tape;
#define PUSH(t, type, d) { if (++(t)->write_pos >= (t)->len ) \
                             (t)->write_pos = 0; \
                           ((type *)(t)->data)[(t)->write_pos] = (d); }
#define PEEK(t, type, n) (((type *)t->data)[(t->read_pos+n)%t->len])
#define POP(t, type) ((((++t->read_pos) >= t->len) ? (t->read_pos = 0) : 0), \
                      ((type *)t->data)[t->read_pos])
typedef enum stream_type {
  FILTER,
  PIPELINE,
  SPLIT_JOIN,
  FEEDBACK_LOOP
} stream_type;
typedef enum splitjoin_type {
  ROUND_ROBIN,
  DUPLICATE,
  COMBINE,
  NULL_SJ
} splitjoin_type;
typedef struct latency_list {
  int val;
  struct latency_list *next;
} latency_list;

typedef struct latency_range {
  int min_val;
  int max_val;
} latency_range;

typedef enum latency_special {
  _BEST_EFFORT
} latency_special;

typedef union latency {
  latency_list list;
  latency_range range;
  latency_special special;
} latency;

/* TODO: define LATENCY_BEST_EFFORT as a special case */
typedef void (*streamit_handler)(void *);
typedef void (*work_fn)(void *, tape *in, tape *out);
typedef streamit_handler *interface_table;
void set_stream_type(stream_context *c, stream_type type);
void set_peek(stream_context *c, int peeks);
void set_pop(stream_context *c, int pops);
void set_push(stream_context *c, int pushes);
void set_work(stream_context *c, work_fn f);
void set_teardown(stream_context *c, streamit_handler f);
void register_child(stream_context *c, stream_context *child);
stream_context *get_parent(stream_context *c);
void set_to_canon(stream_context *c, streamit_handler f);
void set_from_canon(stream_context *c, streamit_handler f);
portal *create_portal(void);
void register_receiver(portal *p, stream_context *receiver,
                       interface_table *vtbl, latency *l);
void register_sender(portal *p, stream_context *sender, latency *l);
void send_message(portal *p, int msgid, latency *l, ...);

#endif /* STREAMIT_H */
