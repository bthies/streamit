/*-----------------------------------------------------------------------------
 * depend.h
 *
 * Public declarations for tracking commands and dependencies.
 *---------------------------------------------------------------------------*/

#ifndef _DEPEND_H_
#define _DEPEND_H_

#include "spucommand.h"

// ID indicating no command.
#define INVALID_COMMAND_ID  255

// Array mapping command IDs to data.
extern CMD_HEADER *commands[];

/*
 * dep_cur_id and dep_cur_ptr may be invalidated by any function that requires
 * the command handler to return afterwards. dep_next_ptr must always be valid.
 */

// ID of the current command.
extern uint8_t dep_cur_id;
// Pointer to .next field (in header of a queued command) that stores ID of
// current command.
extern uint8_t *dep_cur_ptr;
// Pointer to .next field that stores ID of next command.
extern uint8_t *dep_next_ptr;

#if DEBUG
// Whether the current command has been suspended.
extern bool_t dep_dequeued;
#endif

// Indicates that all processing for the current command has completed. Command
// handler must return afterwards.
void dep_complete_command();

#endif
