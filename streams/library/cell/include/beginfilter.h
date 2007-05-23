/*-----------------------------------------------------------------------------
 * beginfilter.h
 *
 * Simple interface for filter code.
 *
 * Enclose all code for a filter between:
 *   #include "beginfilter.h"
 * and:
 *   #include "endfilter.h"
 *
 * endfilter.h does not need to be included if the file only contains one
 * filter.
 *
 * Before including beginfilter.h, do the following:
 * - Define FILTER_NAME as name of filter (must be a C identifier).
 * - Define HAS_STATE if filter is stateful. If undefined, defaults to
 *   stateless.
 * - Define NUM_INPUT_TAPES as the number of input tapes. If undefined,
 *   defaults to a single input tape. Define INPUT_ITEM_TYPE as the C type of
 *   items on the input tapes. If INPUT_ITEM_TYPE is undefined, ITEM_TYPE is
 *   checked for the type.
 * - (Similarly for output tapes.)
 *
 * The work function is defined using:
 *   BEGIN_WORK_FUNC
 *   <code>
 *   END_WORK_FUNC
 *
 * The actual name of the work function is filter_<filter_name>_wf.
 *
 * If filter is stateful, state variables can be accessed as fields of the
 * "state" pseudo-variable. FILTER_<filter_name>_STATE must be a structure that
 * defines the state fields for this to work.
 *
 * Tapes can be accessed using the push/pop/peek macros. When the filter has
 * multiple input/output tapes, the first parameter specifies the tape index.
 *
 * Additional functions can be defined using:
 *   BEGIN_FUNC(<name>, <return_type>[, <args>])
 *   <code>
 *   END_FUNC
 *
 * These functions are called using:
 *   CALL_FUNC(<name>[, <args>])
 *
 * They can be declared beforehand using:
 *   DECLARE_FUNC(<name>, <return_type>[, <args>]);
 *
 * State/tape access is the same in these functions. Function names are
 * decorated to filter_<filter_name>_<name>. Alternatively, functions can be
 * defined/called normally, but state/tape access won't automatically work.
 * Names for these functions should be decorated similarly (can use
 * DECORATE_FUNC_NAME).
 *
 * Some tokens cannot be used as identifiers or macros because they are already
 * macros. See endfilter.h for a list (in particular, "state" is a macro iff
 * the filter is stateful and push/pop/peek are macros iff the operation makes
 * sense for the filter). Filter code must not define macros named "filter" or
 * "FILTER". Filter code cannot use identifiers or macros named: _state,
 * _input, _inputs, _output, _outputs.
 *---------------------------------------------------------------------------*/

#ifdef _SPULIB_BEGINFILTER_H_
#error "beginfilter.h already included."
#else
#define _SPULIB_BEGINFILTER_H_

#include "filterdefs.h"

#ifndef FILTER_NAME
#error "FILTER_NAME must be defined."
#endif

#ifndef NUM_INPUT_TAPES
#define NUM_INPUT_TAPES 1
#endif

#ifndef NUM_OUTPUT_TAPES
#define NUM_OUTPUT_TAPES 1
#endif

#if (NUM_INPUT_TAPES != 0)

#ifndef INPUT_ITEM_TYPE
#ifdef ITEM_TYPE
#define INPUT_ITEM_TYPE ITEM_TYPE
#else
#error "INPUT_ITEM_TYPE (or ITEM_TYPE) must be defined."
#endif
#endif

#define INPUT_ITEM_SIZE sizeof(INPUT_ITEM_TYPE)

#endif // NUM_INPUT_TAPES != 0

#if (NUM_OUTPUT_TAPES != 0)

#ifndef OUTPUT_ITEM_TYPE
#ifdef ITEM_TYPE
#define OUTPUT_ITEM_TYPE ITEM_TYPE
#else
#error "OUTPUT_ITEM_TYPE (or ITEM_TYPE) must be defined."
#endif
#endif

#define OUTPUT_ITEM_SIZE sizeof(OUTPUT_ITEM_TYPE)

#endif // NUM_OUTPUT_TAPES != 0

/*-----------------------------------------------------------------------------
 * Name decoration macros.
 *---------------------------------------------------------------------------*/

#define DECORATE_FUNC_NAME(name) DECORATE_NAME(filter, name)
#define DECORATE_TYPE_NAME(name) DECORATE_NAME(FILTER, name)

#define DECORATE_NAME(prefix, name) _DECORATE_NAME(prefix, FILTER_NAME, name)
#define _DECORATE_NAME(prefix, filter_name, name) \
  __DECORATE_NAME(prefix, filter_name, name)
#define __DECORATE_NAME(prefix, filter_name, name) \
  prefix##_##filter_name##_##name

/*-----------------------------------------------------------------------------
 * Function declarations and calls.
 *---------------------------------------------------------------------------*/

#ifdef USE_PARAM
#define PARAM_ARG(exp) exp,
#else
#define PARAM_ARG(exp)
#endif

#ifdef USE_PARAM
#define STATE_ARG_SEP ,
#else
#define STATE_ARG_SEP
#endif

#ifdef HAS_STATE
#define STATE_ARG(exp) STATE_ARG_SEP exp
#else
#define STATE_ARG(exp)
#endif

#if (defined(USE_PARAM) || defined(HAS_STATE))
#define INPUT_ARG_SEP ,
#else
#define INPUT_ARG_SEP
#endif

#if (NUM_INPUT_TAPES == 0)
#define INPUT_ARG(single_exp, multiple_exp)
#define IF_SINGLE_INPUT(exp)
#elif (NUM_INPUT_TAPES == 1)
#define INPUT_ARG(single_exp, multiple_exp) INPUT_ARG_SEP single_exp
#define IF_SINGLE_INPUT(exp)                exp
#else
#define INPUT_ARG(single_exp, multiple_exp) INPUT_ARG_SEP multiple_exp
#define IF_SINGLE_INPUT(exp)
#endif

#if (defined(USE_PARAM) || defined(HAS_STATE) || (NUM_INPUT_TAPES != 0))
#define OUTPUT_ARG_SEP ,
#else
#define OUTPUT_ARG_SEP
#endif

#if (NUM_OUTPUT_TAPES == 0)
#define OUTPUT_ARG(single_exp, multiple_exp)
#define IF_SINGLE_OUTPUT(exp)
#elif (NUM_OUTPUT_TAPES == 1)
#define OUTPUT_ARG(single_exp, multiple_exp) OUTPUT_ARG_SEP single_exp
#define IF_SINGLE_OUTPUT(exp)                exp
#else
#define OUTPUT_ARG(single_exp, multiple_exp) OUTPUT_ARG_SEP multiple_exp
#define IF_SINGLE_OUTPUT(exp)
#endif

// Common argument list for function declarations.
#define COMMON_DECLARATION_ARGS \
  PARAM_ARG(void *param) STATE_ARG(void *const _state)                        \
  INPUT_ARG(void *const _input, void *const *const _inputs)                   \
  OUTPUT_ARG(void *const _output, void *const *const _outputs)

// Common argument list for function calls.
#define COMMON_CALL_ARGS \
  PARAM_ARG(param) STATE_ARG(_state) INPUT_ARG(_input, _inputs)               \
  OUTPUT_ARG(_output, _outputs)

#define DECLARE_FUNC(name, return_type, ...) \
  static return_type DECORATE_FUNC_NAME(name)(COMMON_DECLARATION_ARGS,        \
                                              ##__VA_ARGS__)

#define BEGIN_FUNC(name, return_type, ...) \
  DECLARE_FUNC(name, return_type, ##__VA_ARGS__)                              \
  {

#define END_FUNC \
  }

#define CALL_FUNC(name, ...) \
  DECORATE_FUNC_NAME(name)(COMMON_CALL_ARGS, ##__VA_ARGS__)

/*-----------------------------------------------------------------------------
 * Work function.
 *---------------------------------------------------------------------------*/

#define BEGIN_WORK_FUNC \
  void                                                                        \
  DECORATE_FUNC_NAME(wf)(void *param, void *const _state,                     \
                         void *const *const _inputs,                          \
                         void *const *const _outputs)                         \
  {                                                                           \
    IF_SINGLE_INPUT(void *const _input = _inputs[0]);                         \
    IF_SINGLE_OUTPUT(void *const _output = _outputs[0]);                      \
    UNUSED_PARAM(param);                                                      \
    UNUSED_PARAM(_state);                                                     \
    UNUSED_PARAM(_inputs);                                                    \
    UNUSED_PARAM(_outputs);                                                   \
    IF_SINGLE_INPUT(UNUSED_PARAM(_input));                                    \
    IF_SINGLE_OUTPUT(UNUSED_PARAM(_output));

#define END_WORK_FUNC \
  }

/*-----------------------------------------------------------------------------
 * State/tape access.
 *---------------------------------------------------------------------------*/

// Access to state fields.
#ifdef HAS_STATE
#define state (*(DECORATE_TYPE_NAME(STATE) *)_state)
#endif

// peek/pop functions and wrapper macros.
#if (NUM_INPUT_TAPES != 0)

static INLINE INPUT_ITEM_TYPE
DECORATE_FUNC_NAME(peek)(void *buf_data, uint32_t n)
{
  BUFFER_CB *buf = buf_get_cb(buf_data);
  check(((buf->tail - buf->head) & buf->mask) >= (n + 1) * INPUT_ITEM_SIZE);
  return *(INPUT_ITEM_TYPE *)
    (buf_data + ((buf->head + n * INPUT_ITEM_SIZE) & buf->mask));
}

static INLINE INPUT_ITEM_TYPE
DECORATE_FUNC_NAME(pop)(void *buf_data)
{
  BUFFER_CB *buf = buf_get_cb(buf_data);
  INPUT_ITEM_TYPE item;
  check(((buf->tail - buf->head) & buf->mask) >= INPUT_ITEM_SIZE);
  item = *(INPUT_ITEM_TYPE *)(buf_data + buf->head);
  buf->head = (buf->head + INPUT_ITEM_SIZE) & buf->mask;
  return item;
}

#if (NUM_INPUT_TAPES == 1)
#define peek(n)             DECORATE_FUNC_NAME(peek)(_input, n)
#define pop()               DECORATE_FUNC_NAME(pop)(_input)
#define get_input()         \
  ((INPUT_ITEM_TYPE *)(_input + buf_get_cb(_input)->head))
#define advance_input(n)    buf_advance_head(_input, (n) * INPUT_ITEM_SIZE)
#else
#define peek(t, n)          DECORATE_FUNC_NAME(peek)(_inputs[t], n)
#define pop(t)              DECORATE_FUNC_NAME(pop)(_inputs[t])
#define get_input(t)        \
  ((INPUT_ITEM_TYPE *)(_inputs[t] + buf_get_cb(_inputs[t])->head))
#define advance_input(t, n) \
  buf_advance_head(_inputs[t], (n) * INPUT_ITEM_SIZE)
#endif

#endif // NUM_INPUT_TAPES != 0

// push function and wrapper macro.
#if (NUM_OUTPUT_TAPES != 0)

static INLINE void
DECORATE_FUNC_NAME(push)(void *buf_data, OUTPUT_ITEM_TYPE item)
{
  BUFFER_CB *buf = buf_get_cb(buf_data);
  check(((buf->head - buf->tail - 1) & buf->mask) >= OUTPUT_ITEM_SIZE);
  *(OUTPUT_ITEM_TYPE *)(buf_data + buf->tail) = item;
  buf->tail = (buf->tail + OUTPUT_ITEM_SIZE) & buf->mask;
}

#if (NUM_OUTPUT_TAPES == 1)
#define push(item)            DECORATE_FUNC_NAME(push)(_output, item)
#define get_output()          \
  ((OUTPUT_ITEM_TYPE *)(_output + buf_get_cb(_output)->tail))
#define advance_output(n)     buf_advance_tail(_output, (n) * OUTPUT_ITEM_SIZE)
#else
#define push(t, item)         DECORATE_FUNC_NAME(push)(_outputs[t], item)
#define get_output(t)         \
  ((OUTPUT_ITEM_TYPE *)(_outputs[t] + buf_get_cb(_outputs[t])->tail))
#define advance_output(t, n)  \
  buf_advance_tail(_outputs[t], (n) * OUTPUT_ITEM_SIZE)
#endif

#endif // NUM_OUTPUT_TAPES != 0

#endif
