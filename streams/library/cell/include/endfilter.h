/*-----------------------------------------------------------------------------
 * endfilter.h
 *
 * Undefines macros defined by beginfilter.h so code for another filter can be
 * written in the same file.
 *---------------------------------------------------------------------------*/

#ifndef _SPULIB_BEGINFILTER_H_
#error "endfilter.h without matching beginfilter.h."
#else
#undef _SPULIB_BEGINFILTER_H_

#undef FILTER_NAME
#undef USE_PARAM
#undef HAS_STATE
#undef NUM_INPUT_TAPES
#undef NUM_OUTPUT_TAPES
#undef ITEM_TYPE
#undef INPUT_ITEM_TYPE
#undef OUTPUT_ITEM_TYPE

#undef INPUT_ITEM_SIZE
#undef OUTPUT_ITEM_SIZE

#undef DECORATE_FUNC_NAME
#undef DECORATE_TYPE_NAME
#undef DECORATE_NAME
#undef _DECORATE_NAME
#undef __DECORATE_NAME

#undef PARAM_ARG
#undef STATE_ARG_SEP
#undef STATE_ARG
#undef INPUT_ARG_SEP
#undef INPUT_ARG
#undef OUTPUT_ARG_SEP
#undef OUTPUT_ARG
#undef COMMON_DECLARATION_ARGS
#undef COMMON_CALL_ARGS
#undef IF_SINGLE_INPUT
#undef IF_SINGLE_OUTPUT

#undef DECLARE_FUNC
#undef BEGIN_FUNC
#undef END_FUNC
#undef CALL_FUNC

#undef BEGIN_WORK_FUNC
#undef END_WORK_FUNC

#undef state
#undef peek
#undef pop
#undef push
#undef get_input
#undef get_output
#undef advance_input
#undef advance_output

#endif
