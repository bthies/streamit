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
 * @file streamit.h
 * External interface to the StreamIt C library.  C implementations of
 * StreamIt programs should include this header file and link against
 * -lstreamit.
 */

#ifndef STREAMIT_H
#define STREAMIT_H

/**
 * @name Fundamental Definitions
 * @{
 */

/** External definition of a stream context. */
typedef struct stream_context stream_context;

/** Enumeration indicating possible types of a stream object. */
typedef enum stream_type {
  INVALID_STREAM_TYPE,
  FILTER,
  PIPELINE,
  SPLIT_JOIN,
  FEEDBACK_LOOP
} stream_type;

/** Enumeration indicating possible types of a splitter or joiner.
 * This alone is not enough to describe a complete splitter or joiner.
 * @see struct one_to_many
 */
typedef enum splitjoin_type {
  ROUND_ROBIN,          /**< A fixed-weight round-robin. */
  WEIGHTED_ROUND_ROBIN, /**< A variable-weight round robin. */
  DUPLICATE,            /**< A duplicating splitter. */
  COMBINE,              /**< Reserved for future use. */
  NULL_SJ               /**< A splitter or joiner that moves no data. */
} splitjoin_type;

/** Enumeration indicating either a splitter or joiner for function
 * parameters. */
typedef enum split_or_join {
  SPLITTER,
  JOINER
} split_or_join;

/** Enumeration indicating either an input or output tape for function
 * parameters. */
typedef enum in_or_out {
  INPUT,
  OUTPUT
} in_or_out;

/** Singly linked list of fixed message latencies.  This list is used
 * to allow specification of a sparse set of message latencies. */
typedef struct latency_list {
  int val;           /**< Allowed latency, in steady-state iterations */
  struct latency_list *next; /**< Pointer to the next latency or NULL */
} latency_list;

/** A single dense range of allowed message latencies. */
typedef struct latency_range {
  int min_val; /**< Minimum allowed latency, in steady-state iterations */
  int max_val; /**< Maximum allowed latency, in steady-state iterations */
} latency_range;

/** Message latencies besides ranges and lists of discrete values. */
typedef enum latency_special {
  _BEST_EFFORT /**< Request best-effort delivery with no constraints */
} latency_special;

/** Union specifying a latency of any sort. */
typedef union latency {
  latency_list *list;
  latency_range range;
  latency_special special;
} latency;

/** Macro to allow user specification of best-effort latency. */
#define LATENCY_BEST_EFFORT ((latency){ special: _BEST_EFFORT })

/** Type of generic handler functions. */
typedef void (*streamit_handler)(void *);
/** Type of work functions. */
typedef void (*work_fn)(void *);
/** Type of message handlers. */
typedef void (*message_fn)(void *data, void *params);
/** Type of message interface tables.  These are always arrays of
 * message handlers. */
typedef message_fn *interface_table;

/** Data object for internal handlers.  File readers and writers
 * use this as their data objects.  They contain context members to
 * be useful with macros that depend on finding the context from a
 * data item. */
typedef struct ContextContainer {
  stream_context *_context;
} _ContextContainer, *ContextContainer;

/** A tape between two stream objects.  This is a FIFO circular queue of
 * homogeneous items of length data_size.  The read position and write
 * position are specified in number of items.
 * The length of the queue is always a power of two items; mask is a
 * bitmask such that any valid tape position and not mask yields zero.
 * Conversely, it means that the item n beyond a specified position is
 * yielded by adding n and doing a bitwise and with mask. */
typedef struct tape {
  void *data;    /**< Pointer to the start of the data area */
  int read_pos;  /**< Next item to be read, in items */
  int write_pos; /**< Next item to be written, in items */
  int data_size; /**< Size of an item, in bytes */
  int mask;      /**< Bitmask for valid item positions */
} tape;

/**
 * @}
 *
 * @name Tape Macros
 *
 * @{
 */

#define INCR_TAPE_LOCALB(p, m, n) ((p) = ((p)+n) & (m))
#define PUSH_TAPE_LOCALB(d, p, m, type, v) \
  (*((type *)((char *)(d)+INCR_TAPE_LOCALB(p, m, sizeof(type))))=(v))
#define POP_TAPE_LOCALB(d, p, m, type) \
  (*((type *)((char *)(d)+INCR_TAPE_LOCALB(p, m, sizeof(type)))))
#define PEEK_TAPE_LOCALB(d, p, m, type, n) \
  (*((type *)((char *)(d)+(((p)+((n)+1)*sizeof(type))&(m)))))
#define LOCALIZE_TAPE(rt, rd, rp, rm, wt, wd, wp, wm)\
  ((rd=rt?rt->data:NULL), (rp=rt?rt->read_pos:0), (rm=rt?rt->mask:0), \
   (wd=wt?wt->data:NULL), (wp=wt?wt->write_pos:0), (wm=wt?wt->mask:0))
#define UNLOCALIZE_TAPE(rt, rp, wt, wp) \
  ((rt ? (rt->read_pos=rp) : 0), (wt ? (wt->write_pos=wp) : 0))
#define PUSH_DEFAULTB(type, v) PUSH_TAPE_LOCALB(__wd, __wp, __wm, type, v)
#define POP_DEFAULTB(type) POP_TAPE_LOCALB(__rd, __rp, __rm, type)
#define POP_DEFAULTB_N(type, _n) INCR_TAPE_LOCALB(__rp, __rm, (_n*sizeof(type)))
#define PEEK_DEFAULTB(type, n) PEEK_TAPE_LOCALB(__rd, __rp, __rm, type, n)
#define VARS_DEFAULTB() void *__rd, *__wd; int __rp, __rm, __wp, __wm
#define LOCALIZE_DEFAULTB(c) \
  int __localize_defaultb_dummy = \
    (LOCALIZE_TAPE((c)->input_tape, __rd, __rp, __rm, \
                (c)->output_tape, __wd, __wp, __wm))
#define UNLOCALIZE_DEFAULTB(c) \
  UNLOCALIZE_TAPE((c)->input_tape, __rp, (c)->output_tape, __wp)
#define INCR_TAPE_POS(t, v, n) INCR_TAPE_LOCALB((t)->v, (t)->mask, (n))
#define PEEK_TAPE(t, type, n) \
  (*((type *)((char *)((t)->data)+(((n+1)*sizeof(type))&(t)->mask))))
#define INCR_TAPE_WRITE(t, size) INCR_TAPE_POS(t, write_pos, size)
#define INCR_TAPE_READ(t, size) INCR_TAPE_POS(t, read_pos, size)
#define PUSH_TAPE(t, type, d) \
  (*((type *)((char *)((t)->data)+INCR_TAPE_POS(t, write_pos, sizeof(type))))=(d))
#define POP_TAPE(t, type) \
  (*((type *)((char *)((t)->data)+INCR_TAPE_POS(t, read_pos, sizeof(type)))))
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
#define READ_ADDR(t) ((char *)((t)->data) + (t)->read_pos)
#define WRITE_ADDR(t) ((char *)((t)->data) + (t)->write_pos)
#define COPY_TAPE_ITEM(s, d) \
  (streamit_memcpy(WRITE_ADDR(d), READ_ADDR(s), (d)->data_size))
#define FEEDBACK_DELAY(d, c, n, t, f) { \
  int i; \
  for (i = 0; i < (n); i++) { \
    PUSH_TAPE((c)->type_data.splitjoin_data.joiner.tape[1], t, f((d), i)); \
  } \
}

/**
 * @}
 */

/**
 * @name Stream Graph Components
 *
 * @{
 */

/** Full description of a splitter or joiner.  Splitters and joiners both
 * have a "one" side and a "many" side, hence the name of the object. */
typedef struct one_to_many {
  splitjoin_type type; /**< Type (e.g. duplicate, roundrobin) of the object */
  int fan;             /**< Number of streams connected to many side */
  int *ratio;          /**< I/O rates for weighted round-robin */
  int slots;           /**< Total number of items per iteration */
  tape *one_tape;      /**< Tape connected to the one side */
  tape **tape;         /**< Array of tapes connected to the many side */
  tape **tcache;       /**< Array of tapes, with one tape per item pushed */
} one_to_many;

struct stream_context;

/** A singly linked list of stream contexts. */
typedef struct stream_context_list {
  struct stream_context *context;   /**< The current context */
  struct stream_context_list *next; /**< Pointer to the next node, or NULL */
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

/** Creates a new context for a stream object with the specified data. */
stream_context *create_context(void *p);

/** Sets the stream type for a given context to the specified type. */
void set_stream_type(stream_context *c, stream_type type);
/** Sets the peek rate for a stream context. */
void set_peek(stream_context *c, int peeks);
/** Sets the pop rate for a stream context. */
void set_pop(stream_context *c, int pops);
/** Sets the push rate for a stream context. */
void set_push(stream_context *c, int pushes);
/** Sets the work function for a stream context. */
void set_work(stream_context *c, work_fn f);

void set_teardown(stream_context *c, streamit_handler f);
/** Add a context as a child stream of the specified composite stream. */
void register_child(stream_context *c, stream_context *child);
/** Returns the parent of the specified stream, or NULL if the stream
 * is a top-level stream. */
stream_context *get_parent(stream_context *c);
/** Create a new tape connecting two stream contexts. */
void create_tape(stream_context *a, stream_context *b,
                 int data_size, int tape_length);
void set_to_canon(stream_context *c, streamit_handler f);
void set_from_canon(stream_context *c, streamit_handler f);
void set_splitter(stream_context *c, splitjoin_type type, int n, ...);
void set_joiner(stream_context *c, splitjoin_type type, int n, ...);

/** Create a tape connecting a splitter or joiner and another object.
 * Every tape to a splitter or joiner is created using this function,
 * even tapes on the "one" side.  It cannot be used to directly connect
 * one splitter or joiner to another.  For feedback loops, slot 0
 * is the external input or output, and slot 1 is connected to the
 * loop object.
 *
 * @param container   Context for the splitjoin or feedbackloop object
 * @param sj          Indicates whether the splitter or joiner is affected
 * @param io          Indicates whether an input or output is affected
 * @param slot        Which tape to connect to on the many side
 * @param other       Object being connected to
 * @param data_size   Size of individual items on the tape
 * @param tape_length Number of items on the tape
 */
void create_splitjoin_tape(stream_context *container,
                           split_or_join sj,
                           in_or_out io,
                           int slot,
                           stream_context *other,
                           int data_size, int tape_length);

/** Runs the splitter object in a composite stream.  There must be
 * enough input to the splitter to cause data to be distributed to
 * all of its outputs. */
void run_splitter(stream_context *c);

/** Runs the joiner object in a composite stream.  There must be
 * enough input on the joiner's many side to cause data to be
 * distributed from all of its inputs. */
void run_joiner(stream_context *c);

/**
 * @}
 *
 * @name Messaging
 *
 * @{
 */

typedef struct portal_receiver {
  struct portal_receiver *next;
  stream_context *context;
  interface_table vtbl;
} portal_receiver;
typedef struct portal {
  portal_receiver *receiver;
} _portal, *portal;

portal create_portal(void);
void register_receiver(portal p, stream_context *receiver,
                       interface_table vtbl, latency l);
/* void register_sender(portal p, stream_context *sender, latency l); */
void send_message(portal p, int msgid, latency l, void *params);

/**
 * @}
 *
 * @name Built-in Objects
 *
 * @{
 */

/** Create a new file reader object for a specified filename. */
stream_context *streamit_filereader_create(char *filename);
/** Work function for a file reader object. */
void streamit_filereader_work(ContextContainer c);
/** Create a new file writer object for a specified filename. */
stream_context *streamit_filewriter_create(char *filename);
/** Work function for a file reader object. */
void streamit_filewriter_work(ContextContainer c);
/** Create a new identity filter. */
stream_context *streamit_identity_create(void);
/** Work function for an identity filter. */
void streamit_identity_work(ContextContainer c);

/**
 * @}
 *
 * @name System Initialization
 *
 * @{
 */

/** Recursively connect tapes in child objects.  This causes tapes that
 * aren't explicitly connected to become connected.  For example, the
 * input tape to a pipeline is also connected to the first child;
 * the input tape to a split/join is connected to the input of the
 * splitter.  This then recursively descends into children and performs
 * tape connection as needed.
 *
 * @param c  The context of the stream to connect tapes for
 */
void connect_tapes(stream_context *c);

/** Run the StreamIt system.  This takes the argc and argv parameters
 * from your program's main() function.  Depending on the command-line
 * parameters, it may return after a specified number of iterations are
 * run, or not at all.
 *
 * @param c     Context of the top-level stream.
 * @param argc  Number of command-line arguments, from main().
 * @param argv  Actual command-line arguments, from main(). */
void streamit_run(stream_context *c, int argc, char **argv);

void ERROR (void *data, char *error_msg);

/**
 * @}
 */



/* Multiplies an FFTW halfcomplex array by a known set of
 * complex constants, also in halfcomplex format.
 * output: Y (float array of size size)
 * input1: X (float array of size size)
 * input2: H (float array of size size)
 *
 * All arrays are  Hermitian, meaning that for all
 * i 0<=i<n, x[i] = conj(x[n-i]).  FFTW then stores this in a single
 * array, where for 0<=i<=n/2, x[i] is the real part of X[i] (and also
 * the real part of X[n-i]), and for 0<i<n/2, x[n-i] is the complex part
 * of X[i] (and the negated complex part of X[n-i]).  It appears to
 * follow from the documentation that X[0] is strictly real (which is
 * due to the math of the FFT.
 *
 * The output can be safely set to be one of the inputs if desired.
 */
void do_halfcomplex_multiply(void* thisptr, float *Y, float *X, float *H, int size);

/**
 * Replaces the contents of input_buff with the value of its FFT.
 * input_buff: input (real format)/output (halfcomplex format)
 *
 * Since buff is a assumed completly real, the corresponding complex
 * valued FFT(input_buff) is stored in the "half complex array" format of
 * fftw (see http://www.fftw.org/doc/fftw_2.html#SEC5)
 **/
void convert_to_freq(void* thisptr, float* input_buff, int size);

/** 
 * Scales each element of the passed array by 1/size. 
 *
 * buffer: input/output
 * Since FFTW does not perform the 1/N scaling of the inverse
 * DFT, the N point IFFT(FFT(x)) will result in x scaled by N.
 * This function is used to pre-scale the coefficients of H
 * by 1/N so we don't have to do it on each filter invocation.
 **/
void scale_by_size(void* thisptr, float* buffer, int size);


/**
 * Converts the contents of input_buff from the frequency domain
 * to the time domain, omitting the 1/N factor.
 *
 * input_buff: input in half complex array format.
 * output_buff: output of real values (because the IFFT of a 
 *              halfcomplex (eg symmetric) sequency is purely real.
 * 
 * Since this function uses FFTW to compute the inverse FFT,
 * the result is not scaled by the 1/N factor that it should be.
 * In our implementation, the impulse response of the filter
 * is prescaled scaled by 1/N so we get the correct answer.
 *
 * Note that this function trashes the values in input_buff.
 **/
void convert_from_freq(void* thisptr, float* input_buff, float* output_buff, int size);


#endif /* STREAMIT_H */
