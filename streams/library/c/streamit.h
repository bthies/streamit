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

#define LATENCY_BEST_EFFORT ((latency){ special: _BEST_EFFORT })
typedef void (*streamit_handler)(void *);
typedef void (*work_fn)(void *);
typedef streamit_handler *interface_table;
typedef struct tape {
  void *data;
  int read_pos;
  int write_pos;
  int data_size;
  int tape_length;
} tape;
#define PUSH(c, type, d) \
  { if (++((c)->output_tape->write_pos) >= (c)->output_tape->tape_length ) \
       (c)->output_tape->write_pos = 0; \
    ((type *)((c)->output_tape->data))[(c)->output_tape->write_pos] = (d); }
#define PEEK(c, type, n) \
  (((type *)(c)->input_tape->data) \
   [((c)->input_tape->read_pos+n)%(c)->input_tape->tape_length])
#define POP(c, type) \
  ((((++((c)->input_tape->read_pos)) >= (c)->input_tape->tape_length) \
    ? ((c)->input_tape->read_pos = 0) : 0), \
   ((type *)((c)->input_tape->data))[(c)->input_tape->read_pos])
struct stream_context;

typedef struct stream_context_list {
  struct stream_context *context;
  struct stream_context_list *next;
} stream_context_list;

typedef struct pipeline_type_data {
  stream_context_list *first_child;
  stream_context_list *last_child;
} pipeline_type_data;

typedef union stream_type_data {
  pipeline_type_data pipeline_data;
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
  int num_splits, num_joins;
  splitjoin_type split_type, join_type;
  int *split_ratio, *join_ratio;
  tape **split_tape, **join_tape;
} stream_context;
stream_context *create_context(void *p);
typedef struct portal {
  stream_context_list *destinations;
} portal;
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
void create_split_tape(stream_context *container, int slot,
                       stream_context *dst,
                       int data_size, int tape_length);
void create_join_tape(stream_context *src,
                      stream_context *container, int slot,
                      int data_size, int tape_length);
portal *create_portal(void);
void register_receiver(portal *p, stream_context *receiver,
                       interface_table *vtbl, latency *l);
void register_sender(portal *p, stream_context *sender, latency *l);
void send_message(portal *p, int msgid, latency *l, ...);
void streamit_run(stream_context *c);

#endif /* STREAMIT_H */
