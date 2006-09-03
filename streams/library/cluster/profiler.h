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

#ifndef __PROFILER_H
#define __PROFILER_H

class profiler {
  
  // counts of all operations
  static int float_total;
  static int int_total;
  
  // counts of specific operations
  static int* float_ops;
  static int* int_ops; 

  // names of operations
  static char** OP_TO_NAME;

  // for constructing operations
  static int curr_opcode;
  static int register_op(char* name);

  // counts of how many times each operation ID executed.  There is
  // an ID assigned to each static arithmetic operation by the
  // compiler.  This is similar to a line number, except that there
  // could be multiple operation ID's per line.
  static int* id_counts;
  static int num_ids; // length of id_counts array

 public:

  // the names of these fields should match those targetted from the
  // compiler
  static int BINOP_ADD;
  static int BINOP_SUB;
  static int BINOP_MUL;
  static int BINOP_DIV;
  static int BINOP_MOD;
  static int BINOP_AND;
  static int BINOP_OR;
  static int BINOP_EQ;
  static int BINOP_NEQ;
  static int BINOP_LT;
  static int BINOP_LE;
  static int BINOP_GT;
  static int BINOP_GE;
  // These are bitwise AND/OR/XOR:       
  static int BINOP_BAND;
  static int BINOP_BOR;
  static int BINOP_BXOR;
  static int BINOP_LSHIFT;
  static int BINOP_RSHIFT;
  // unary ops
  static int UNOP_NOT;
  static int UNOP_POS;
  static int UNOP_NEG;
  static int UNOP_PREINC;
  static int UNOP_POSTINC;
  static int UNOP_PREDEC;
  static int UNOP_POSTDEC;
  static int UNOP_COMPLEMENT;
  // these are math functions:           
  static int FUNC_ABS;
  static int FUNC_ACOS;
  static int FUNC_ASIN;
  static int FUNC_ATAN;
  static int FUNC_ATAN2;
  static int FUNC_CEIL;
  static int FUNC_COS;
  static int FUNC_SIN;
  static int FUNC_COSH;
  static int FUNC_SINH;
  static int FUNC_EXP;
  static int FUNC_FABS;
  static int FUNC_MODF;
  static int FUNC_FMOD;
  static int FUNC_FREXP;
  static int FUNC_FLOOR;
  static int FUNC_LOG;
  static int FUNC_LOG10;
  static int FUNC_POW;
  static int FUNC_ROUND;
  static int FUNC_RINT;
  static int FUNC_SQRT;
  static int FUNC_TANH;
  static int FUNC_TAN;
  // make sure you set this to be the maximum number of operations
  static int NUM_OPCODES();

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
  static int register_op(int op, int id, int val);

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
  static float register_op(int op, int id, float val);

  /**
   * Called before execution to tell the profiler how many operation
   * ID's there will be.
   */
  static void set_num_ids(int numIds);

  /**
   * Called when the program has finished, to print results, etc.
   */
  static void summarize();

};

#endif
