#ifndef STREAMIT_H
#define STREAMIT_H

typedef enum stream_type {
  INVALID_STREAM_TYPE,
  FILTER,
  PIPELINE,
  SPLIT_JOIN,
  FEEDBACK_LOOP
} stream_type;
typedef enum splitjoin_type {
  ROUND_ROBIN,
  WEIGHTED_ROUND_ROBIN,
  DUPLICATE,
  COMBINE,
  NULL_SJ
} splitjoin_type;
typedef enum split_or_join {
  SPLITTER,
  JOINER
} split_or_join;
typedef enum in_or_out {
  INPUT,
  OUTPUT
} in_or_out;
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
  latency_list *list;
  latency_range range;
  latency_special special;
} latency;

#define LATENCY_BEST_EFFORT ((latency){ special: _BEST_EFFORT })
typedef void (*streamit_handler)(void *);
typedef void (*work_fn)(void *);
typedef void (*message_fn)(void *data, void *params);
typedef message_fn *interface_table;
typedef struct tape {
  void *data;
  int read_pos;
  int write_pos;
  int data_size;
  int tape_length;
} tape;
#define INCR_TAPE_WRITE(t) \
  ((++((t)->write_pos) >= (t)->tape_length) ? \
    (t)->write_pos = 0 : (t)->write_pos)
#define INCR_TAPE_READ(t) \
  ((++((t)->read_pos) >= (t)->tape_length) ? \
    (t)->read_pos = 0 : (t)->read_pos)
#define PUSH_TAPE(t, type, d) \
  (((type *)((t)->data))[INCR_TAPE_WRITE(t)] = (d))
#define PEEK_TAPE(t, type, n) \
  (((type *)(t)->data)[((t)->read_pos+n+1)%(t)->tape_length])
#define POP_TAPE(t, type) \
  (((type *)((t)->data))[INCR_TAPE_READ(t)])
#define COPY_TAPE_ITEM(s, d) \
  (memcpy((d)->data + (d)->write_pos * (d)->data_size, \
          (s)->data + (s)->read_pos * (s)->data_size, \
          (d)->data_size))
#define PUSH(c, type, d) PUSH_TAPE((c)->output_tape, type, d)
#define PEEK(c, type, n) PEEK_TAPE((c)->input_tape, type, n)
#define POP(c, type) POP_TAPE((c)->input_tape, type)
#define FEEDBACK_DELAY(d, c, n, t, f) { \
  int i; \
  for (i = 0; i < (n); i++) { \
    PUSH_TAPE((c)->type_data.splitjoin_data.joiner.tape[1], t, f((d), i)); \
  } \
}
typedef struct one_to_many {
  splitjoin_type type;
  int fan;
  int *ratio;
  int slots;
  tape *one_tape, **tape, **tcache;
} one_to_many;
struct stream_context;

typedef struct stream_context_list {
  struct stream_context *context;
  struct stream_context_list *next;
} stream_context_list;

typedef struct pipeline_type_data {
  stream_context_list *first_child;
  stream_context_list *last_child;
} pipeline_type_data;
typedef struct splitjoin_type_data {
  stream_context_list *first_child;
  stream_context_list *last_child;
  one_to_many splitter;
  one_to_many joiner;
} splitjoin_type_data;

typedef union stream_type_data {
  pipeline_type_data pipeline_data;
  splitjoin_type_data splitjoin_data;
} stream_type_data;
typedef struct stream_context {
  void *stream_data;
  stream_type type;
  int peek_size, pop_size, push_size;
  work_fn work_function;
  struct stream_context *parent;
  tape *input_tape;
  tape *output_tape;
  stream_type_data type_data;
} stream_context;
stream_context *create_context(void *p);
typedef struct portal_receiver {
  struct portal_receiver *next;
  stream_context *context;
  interface_table vtbl;
} portal_receiver;
typedef struct portal {
  portal_receiver *receiver;
} _portal, *portal;
void set_stream_type(stream_context *c, stream_type type);
void set_peek(stream_context *c, int peeks);
void set_pop(stream_context *c, int pops);
void set_push(stream_context *c, int pushes);
void set_work(stream_context *c, work_fn f);
void set_teardown(stream_context *c, streamit_handler f);
void register_child(stream_context *c, stream_context *child);
stream_context *get_parent(stream_context *c);
void create_tape(stream_context *a, stream_context *b,
                 int data_size, int tape_length);
void set_to_canon(stream_context *c, streamit_handler f);
void set_from_canon(stream_context *c, streamit_handler f);
void set_splitter(stream_context *c, splitjoin_type type, int n, ...);
void set_joiner(stream_context *c, splitjoin_type type, int n, ...);
void create_splitjoin_tape(stream_context *container,
                           split_or_join sj,
                           in_or_out io,
                           int slot,
                           stream_context *other,
                           int data_size, int tape_length);
void run_splitter(stream_context *c);
void run_joiner(stream_context *c);
portal create_portal(void);
void register_receiver(portal p, stream_context *receiver,
                       interface_table vtbl, latency l);
/* void register_sender(portal p, stream_context *sender, latency l); */
void send_message(portal p, int msgid, latency l, void *params);
void connect_tapes(stream_context *c);
void streamit_run(stream_context *c);

#endif /* STREAMIT_H */
