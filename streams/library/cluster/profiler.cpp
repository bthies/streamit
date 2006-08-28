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

#include "profiler.h"

#include <stdlib.h>
#include <stdio.h>
#include <assert.h>

// counts of all operations
int profiler::float_total = 0;
int profiler::int_total = 0;

// counts of specific operations
int* profiler::float_ops = (int*)calloc(NUM_OPCODES(), sizeof(int));
int* profiler::int_ops = (int*)calloc(NUM_OPCODES(), sizeof(int));

// names of operations
char** profiler::OP_TO_NAME = (char**)calloc(NUM_OPCODES(), sizeof(char*));

// for constructing operations
int profiler::curr_opcode = 0;
int profiler::register_op(char* name) {
  assert(curr_opcode < NUM_OPCODES()); // increase NUM_OPCODES if fails
  int result = curr_opcode++;
  OP_TO_NAME[result] = name;
  return result;
}

// initialized by set_num_ids
int* profiler::id_counts;
int profiler::num_ids;

// the names of these fields should match those targetted from the
// compiler
int profiler::BINOP_ADD =       register_op("add");
int profiler::BINOP_SUB =       register_op("sub");
int profiler::BINOP_MUL =       register_op("mul");
int profiler::BINOP_DIV =       register_op("div");
int profiler::BINOP_MOD =       register_op("mod");
int profiler::BINOP_AND =       register_op("and");
int profiler::BINOP_OR =        register_op("or");
int profiler::BINOP_EQ =        register_op("eq");
int profiler::BINOP_NEQ =       register_op("neq");
int profiler::BINOP_LT =        register_op("lt");
int profiler::BINOP_LE =        register_op("le");
int profiler::BINOP_GT =        register_op("gt");
int profiler::BINOP_GE =        register_op("ge");
// These are bitwise AND/OR/XOR:       
int profiler::BINOP_BAND =      register_op("band");
int profiler::BINOP_BOR =       register_op("bor");
int profiler::BINOP_BXOR =      register_op("bxor");
int profiler::BINOP_LSHIFT =    register_op("lshift");
int profiler::BINOP_RSHIFT =    register_op("rshift");
// unary ops
int profiler::UNOP_NOT =        register_op("unary_not");
// not sure what a unary positive really is, but kopi defines it, so
// put it here just in case
int profiler::UNOP_POS =        register_op("unary_pos");
int profiler::UNOP_NEG =        register_op("unary_neg");
int profiler::UNOP_PREINC =     register_op("preinc");
int profiler::UNOP_POSTINC =    register_op("postinc");
int profiler::UNOP_PREDEC =     register_op("predec");
int profiler::UNOP_POSTDEC =    register_op("postdec");
int profiler::UNOP_COMPLEMENT = register_op("complement");
// these are math functions:           
int profiler::FUNC_ABS =        register_op("abs");
int profiler::FUNC_ACOS =       register_op("cos");
int profiler::FUNC_ASIN =       register_op("sin");
int profiler::FUNC_ATAN =       register_op("tan");
int profiler::FUNC_ATAN2 =      register_op("tan2");
int profiler::FUNC_CEIL =       register_op("ceil");
int profiler::FUNC_COS =        register_op("cos");
int profiler::FUNC_SIN =        register_op("sin");
int profiler::FUNC_COSH =       register_op("cosh");
int profiler::FUNC_SINH =       register_op("sinh");
int profiler::FUNC_EXP =        register_op("exp");
int profiler::FUNC_FABS =       register_op("abs");
int profiler::FUNC_MODF =       register_op("modf");
int profiler::FUNC_FMOD =       register_op("mod");
int profiler::FUNC_FREXP =      register_op("frexp");
int profiler::FUNC_FLOOR =      register_op("floor");
int profiler::FUNC_LOG =        register_op("log");
int profiler::FUNC_LOG10 =      register_op("log10");
int profiler::FUNC_POW =        register_op("pow");
int profiler::FUNC_ROUND =      register_op("round");
int profiler::FUNC_RINT =       register_op("int");
int profiler::FUNC_SQRT =       register_op("sqrt");
int profiler::FUNC_TANH =       register_op("tanh");
int profiler::FUNC_TAN =        register_op("tan");
// make sure you set this to be the number of operations
int profiler::NUM_OPCODES() { return 50; }

/**
 * One of the main callbacks from the instrumented StreamIt code.
 * This version is for int values.
 *
 * @param op      The operation being performed.
 * @param id      A static identifier for the line of code calling this.
 * @param val     One of the values taking part in the operation.
 *
 * @return The value of the parameter <val>.
 */
int profiler::register_op(int op, int id, int val) {
  id_counts[id]++;
  int_ops[op]++;
  int_total++;
  return val;
}

/**
 * One of the main callbacks from the instrumented StreamIt code.
 * This version is for float values.
 *
 * @param op      The operation being performed.
 * @param id      A static identifier for the line of code calling this.
 * @param val     One of the values taking part in the operation.
 *
 * @return The value of the parameter <val>.
 */
float profiler::register_op(int op, int id, float val) {
  id_counts[id]++;
  float_ops[op]++;
  float_total++;
  return val;
}

/**
 * Called before execution to tell the profiler how many operation
 * ID's there will be.
 */
void profiler::set_num_ids(int _num_ids) {
  id_counts = (int*)calloc(_num_ids, sizeof(int));
  num_ids = _num_ids;
}

/**
 * Called when the program has finished, to print results, etc.
 */
void profiler::summarize() {
  FILE *f = fopen("./countops.c.log", "w");

  // total ops 
  int ops_total = float_total + int_total;
  fprintf(f, "Total ops:   %d\n", ops_total);
  fprintf(f, "  float ops: %d\n", float_total);
  fprintf(f, "  int ops:   %d\n", int_total);

  /* NOT IMPLEMENTED YET

  // communication
  fprintf(f, "\n");
  fprintf(f, "Items pushed: %d\n", push_total);

  // communication-to-computation ratio
  fprintf(f, "\n");
  fprintf(f, "Ops / push:   %f\n", (ops_total / (float)push_total));

  */

  // float ops summary
  fprintf(f, "\nFloat ops breakdown:\n");
  for (int i=0; i<NUM_OPCODES(); i++) {
    fprintf(f, "  %s: %d\n", OP_TO_NAME[i], float_ops[i]);
  }
  // int ops summary
  fprintf(f, "\nInt ops breakdown:\n");
  for (int i=0; i<NUM_OPCODES(); i++) {
    fprintf(f, "  %s: %d\n", OP_TO_NAME[i], int_ops[i]);
  }

  // the count by actual statement in the code
  fprintf(f, "\nCount for each static operation ID (see .cpp files for ID's):\n");
  for (int i=0; i<num_ids; i++) {
    float percent = 100*id_counts[i]/(float)ops_total;
    fprintf(f, "  %d: %d (%2.2f\%)\n", i, id_counts[i], percent);
  }

  // the sorted count by actual statement in the code
  fprintf(f, "\nSorted count for each static operation ID (see .cpp files for ID's):\n");
  // just do selection sort because I don't know C++ sort libraries
  for (int i=0; i<num_ids; i++) {
    int max = 0;
    // find greatest
    for (int j=0; j<num_ids; j++) {
      if (id_counts[j] > id_counts[max]) {
	max = j;
      }
    }
    // print greatest
    float percent = 100*id_counts[max]/(float)ops_total;
    fprintf(f, "  %d: %d (%2.2f\%)\n", max, id_counts[max], percent);
    // zero-out greatest
    id_counts[max] = -1;
  }

  fclose(f);

  printf("Profile information written to countops.c.log.\n");
}
