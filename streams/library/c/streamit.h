#ifndef STREAMIT_H
#define STREAMIT_H

typedef struct stream_context stream_context;
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
typedef struct ContextContainer {
  stream_context *context;
} _ContextContainer, *ContextContainer;
typedef struct tape {
  void *data;
  int read_pos;
  int write_pos;
  int data_size;
  int mask;
} tape;
#define INCR_TAPE_LOCALB(p, m, n) ((p) = ((p)+n) & (m))
#define PUSH_TAPE_LOCALB(d, p, m, type, v) \
  (*((type *)((d)+INCR_TAPE_LOCALB(p, m, sizeof(type))))=(v))
#define POP_TAPE_LOCALB(d, p, m, type) \
  (*((type *)((d)+INCR_TAPE_LOCALB(p, m, sizeof(type)))))
#define PEEK_TAPE_LOCALB(d, p, m, type, n) \
  (*((type *)((d)+(((p)+((n)+1)*sizeof(type))&(m)))))
#define LOCALIZE_TAPE(rt, rd, rp, rm, wt, wd, wp, wm)\
  ((rd=rt->data), (rp=rt->read_pos), (rm=rt->mask), \
   (wd=wt->data), (wp=rt->write_pos), (wm=wt->mask))
#define UNLOCALIZE_TAPE(rt, rp, wt, wp) \
  ((rt->read_pos=rp), (wt->write_pos=wp))
#define PUSH_DEFAULTB(type, v) PUSH_TAPE_LOCALB(__wd, __wp, __wm, type, v)
#define POP_DEFAULTB(type) POP_TAPE_LOCALB(__rd, __rp, __rm, type)
#define PEEK_DEFAULTB(type, n) PEEK_TAPE_LOCALB(__rd, __rp, __rm, type, n)
#define VARS_DEFAULTB() void *__rd, *__wd; int __rp, __rm, __wp, __wm;
#define LOCALIZE_DEFAULTB(c) \
  LOCALIZE_TAPE((c)->input_tape, __rd, __rp, __rm, \
                (c)->output_tape, __wd, __wp, __wm)
#define UNLOCALIZE_DEFAULTB(c) \
  UNLOCALIZE_TAPE((c)->input_tape, __rp, (c)->output_tape, __wp)
#define INCR_TAPE_POS(t, v, n) INCR_TAPE_LOCALB((t)->v, (t)->mask, (n))
#define PEEK_TAPE(t, type, n) \
  (*((type *)((t)->data+(((n+1)*sizeof(type))&(t)->mask))))
#define INCR_TAPE_WRITE(t, size) INCR_TAPE_POS(t, write_pos, size)
#define INCR_TAPE_READ(t, size) INCR_TAPE_POS(t, read_pos, size)
#define PUSH_TAPE(t, type, d) \
  (*((type *)((t)->data+INCR_TAPE_POS(t, write_pos, sizeof(type))))=(d))
#define POP_TAPE(t, type) \
  (*((type *)((t)->data+INCR_TAPE_POS(t, read_pos, sizeof(type)))))
#define PUSH(c, type, d) PUSH_TAPE((c)->output_tape, type, d)
#define PEEK(c, type, n) PEEK_TAPE((c)->input_tape, type, n)
#define POP(c, type) POP_TAPE((c)->input_tape, type)
#define streamit_memcpy(d, s, l) \
  (((l) == 0) ? memcpy((d), (s), 0) : \
   ((l) == 1) ? memcpy((d), (s), 1) : \
   ((l) == 2) ? memcpy((d), (s), 2) : \
   ((l) == 3) ? memcpy((d), (s), 3) : \
   ((l) == 4) ? memcpy((d), (s), 4) : \
   ((l) == 6) ? memcpy((d), (s), 6) : \
   ((l) == 8) ? memcpy((d), (s), 8) : \
   ((l) == 12) ? memcpy((d), (s), 12) : \
   ((l) == 16) ? memcpy((d), (s), 16) : \
   ((l) == 20) ? memcpy((d), (s), 20) : \
   memcpy((d), (s), (l)))
#define READ_ADDR(t) ((t)->data + (t)->read_pos)
#define WRITE_ADDR(t) ((t)->data + (t)->write_pos)
#define COPY_TAPE_ITEM(s, d) \
  (streamit_memcpy(WRITE_ADDR(d), READ_ADDR(s), (d)->data_size))
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
struct stream_context {
  void *stream_data;
  stream_type type;
  int peek_size, pop_size, push_size;
  work_fn work_function;
  struct stream_context *parent;
  tape *input_tape;
  tape *output_tape;
  stream_type_data type_data;
};
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
stream_context *streamit_filereader_create(char *filename);
void streamit_filereader_work(ContextContainer c);
stream_context *streamit_filewriter_create(char *filename);
void streamit_filewriter_work(ContextContainer c);
void connect_tapes(stream_context *c);
void streamit_run(stream_context *c, int argc, char **argv);

#endif /* STREAMIT_H */
