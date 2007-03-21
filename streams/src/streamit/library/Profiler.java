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

package streamit.library;

import java.io.FileOutputStream;
import java.io.PrintStream;
import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.HashSet;
import java.util.LinkedList;

/**
 * This class can tabulate profiling statistics for execution of
 * StreamIt programs in the Java library.  It is notified of
 * arithmetic operations by instrumentation code that is inserted by
 * the frontend (when run with strc -countops -library).
 */
public class Profiler {
    // Temporary list for constructing names of operations.
    private static final List<String> tempOpToName = new LinkedList<String>();
    // for constructing operations
    private static final int registerOp(String name) {
        tempOpToName.add(name);
        return tempOpToName.size()-1;
    }

    // the names of these fields should match those targetted from
    // streamit.frontend.tojava.NodesToJava.  
    public static final int BINOP_ADD =       registerOp("add");
    public static final int BINOP_SUB =       registerOp("sub");
    public static final int BINOP_MUL =       registerOp("mul");
    public static final int BINOP_DIV =       registerOp("div");
    public static final int BINOP_MOD =       registerOp("mod");
    public static final int BINOP_AND =       registerOp("and");
    public static final int BINOP_OR =        registerOp("or");
    public static final int BINOP_EQ =        registerOp("eq");
    public static final int BINOP_NEQ =       registerOp("neq");
    public static final int BINOP_LT =        registerOp("lt");
    public static final int BINOP_LE =        registerOp("le");
    public static final int BINOP_GT =        registerOp("gt");
    public static final int BINOP_GE =        registerOp("ge");
    // These are bitwise AND/OR/XOR:       
    public static final int BINOP_BAND =      registerOp("band");
    public static final int BINOP_BOR =       registerOp("bor");
    public static final int BINOP_BXOR =      registerOp("bxor");
    public static final int BINOP_LSHIFT =    registerOp("lshift");
    public static final int BINOP_RSHIFT =    registerOp("rshift");
    // unary ops
    public static final int UNOP_NOT =        registerOp("unary_not");
    // not sure what a unary positive really is, but kopi defines it,
    // so put it here for consistency
    public static final int UNOP_POS =        registerOp("unary_pos");
    public static final int UNOP_NEG =        registerOp("unary_neg");
    public static final int UNOP_PREINC =     registerOp("preinc");
    public static final int UNOP_POSTINC =    registerOp("postinc");
    public static final int UNOP_PREDEC =     registerOp("predec");
    public static final int UNOP_POSTDEC =    registerOp("postdec");
    public static final int UNOP_COMPLEMENT = registerOp("complement");
    // these are math functions:           
    public static final int FUNC_ABS =        registerOp("abs");
    public static final int FUNC_ACOS =       registerOp("cos");
    public static final int FUNC_ASIN =       registerOp("sin");
    public static final int FUNC_ATAN =       registerOp("tan");
    public static final int FUNC_ATAN2 =      registerOp("tan2");
    public static final int FUNC_CEIL =       registerOp("ceil");
    public static final int FUNC_COS =        registerOp("cos");
    public static final int FUNC_SIN =        registerOp("sin");
    public static final int FUNC_COSH =       registerOp("cosh");
    public static final int FUNC_SINH =       registerOp("sinh");
    public static final int FUNC_EXP =        registerOp("exp");
    public static final int FUNC_FABS =       registerOp("abs");
    public static final int FUNC_MODF =       registerOp("modf");
    public static final int FUNC_FMOD =       registerOp("mod");
    public static final int FUNC_FREXP =      registerOp("frexp");
    public static final int FUNC_FLOOR =      registerOp("floor");
    public static final int FUNC_LOG =        registerOp("log");
    public static final int FUNC_LOG10 =      registerOp("log10");
    public static final int FUNC_POW =        registerOp("pow");
    public static final int FUNC_RANDOM =     registerOp("random");
    public static final int FUNC_ROUND =      registerOp("round");
    public static final int FUNC_RINT =       registerOp("int");
    public static final int FUNC_SQRT =       registerOp("sqrt");
    public static final int FUNC_TANH =       registerOp("tanh");
    public static final int FUNC_TAN =        registerOp("tan");
    public static final int FUNC_MIN =        registerOp("minf");
    public static final int FUNC_MAX =        registerOp("maxf");

    // construct array of names
    public static final String[] OP_TO_NAME = tempOpToName.toArray(new String[0]);
    // number of operations
    public static final int NUM_OPS = OP_TO_NAME.length;

    // counts of all operations
    private static long floatTotal = 0;
    private static long intTotal = 0;
    private static long boolTotal = 0;

    // counts of all communication
    private static long pushTotal = 0;
    private static long popTotal = 0;

    // counts of specific operations
    private static long[] floatOps = new long[NUM_OPS];
    private static long[] intOps = new long[NUM_OPS];
    private static long[] boolOps = new long[NUM_OPS];

    // counts of how many times each operation ID executed.  There is
    // an ID assigned to each static arithmetic operation by the
    // frontend.  This is similar to a line number, except that there
    // could be multiple operation ID's per line.
    static long[] idCounts;
    // the actual line of code that is being computed at a given id
    static String[] idCode;

    /**
     * One of the main callbacks from the instrumented StreamIt code.
     * This version is for int values.
     *
     * @param op      The operation being performed.
     * @param id      A static identifier for the line of code calling this.
     * @param val     One of the values taking part in the operation.
     * @param code    The code that computes this op.
     *
     * @return The value of the parameter <pre>val</pre>.
     */
    public static int registerOp(int op, int id, String code, int val) {
        idCode[id] = code;
        idCounts[id]++;
        intOps[op]++;
        intTotal++;
        return val;
    }

    /**
     * One of the main callbacks from the instrumented StreamIt code.
     * This version is for float values.
     *
     * @param op      The operation being performed.
     * @param id      A static identifier for the line of code calling this.
     * @param val     One of the values taking part in the operation.
     * @param code    The code that computes this op.
     *
     * @return The value of the parameter <pre>val</pre>.
     */
    public static float registerOp(int op, int id, String code, float val) {
        idCode[id] = code;
        idCounts[id]++;
        floatOps[op]++;
        floatTotal++;
        return val;
    }

    /**
     * One of the main callbacks from the instrumented StreamIt code.
     * This version is for boolean values.
     *
     * @param op      The operation being performed.
     * @param id      A static identifier for the line of code calling this.
     * @param val     One of the values taking part in the operation.
     * @param code    The code that computes this op.
     *
     * @return The value of the parameter <pre>val</pre>.
     */
    public static boolean registerOp(int op, int id, String code, boolean val) {
        idCode[id] = code;
        idCounts[id]++;
        boolOps[op]++;
        boolTotal++;
        return val;
    }

    /**
     * Called before execution to tell the profiler how many operation
     * ID's there will be.
     */
    public static void setNumIds(int numIds) {
        // initialize arrays
        idCounts = new long[numIds];
        idCode = new String[numIds];
    }

    /**
     * One of the main callbacks from the instrumented StreamIt code.
     * This version is for string values.
     *
     * @param op      The operation being performed.
     * @param id      A static identifier for the line of code calling this.
     * @param val     One of the values taking part in the operation.
     *
     * @return The value of the parameter <pre>val</pre>.
     */
    public static String registerOp(int op, int id, String code, String val) {
        // don't profile string ops, should only be used for debugging
        return val;
    }

    /**
     * Registers a push operation.  Note that this is for a primitive
     * datatype -- will only be called once when pushing arrays.
     */
    public static void registerPop() {
        popTotal++;
    }
    
    /**
     * Registers a push operation.  Note that this is for a primitive
     * datatype -- will only be called once when pushing arrays.
     */
    public static void registerPush() {
        pushTotal++;
    }
    
    /**
     * Called when the program has finished, to print results, etc.
     */
    public static void summarize() {
        try {
            PrintStream out = new PrintStream(new FileOutputStream("countops.java.log"));
            summarize(out);
            out.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        System.out.println("Operation counts written to countops.java.log.");
    }

    /**
     * Write profile information to <pre>out</pre>.
     */
    private static void summarize(PrintStream out) throws IOException {
        // total ops 
        long opsTotal = floatTotal + intTotal + boolTotal;
        out.println("Total ops:    " + opsTotal);
        out.println("  float ops:  " + floatTotal);
        out.println("  int ops:    " + intTotal);
        out.println("  bool ops:   " + boolTotal);

        // communication
        out.println();
        out.println("Items pushed: " + pushTotal);

        // communication-to-computation ratio
        out.println();
        out.println("Ops / push:   " + (opsTotal / (float)pushTotal));
        out.println("  float+int:  " + ((floatTotal + intTotal) / (float)pushTotal));
        out.println("  bool:       " + (boolTotal / (float)pushTotal));

        // float ops summary
        out.println("\nFloat ops breakdown:");
        for (int i=0; i<NUM_OPS; i++) {
            out.println("  " + OP_TO_NAME[i] + ": " + floatOps[i]);
        }
        // int ops summary
        out.println("\nInt ops breakdown:");
        for (int i=0; i<NUM_OPS; i++) {
            out.println("  " + OP_TO_NAME[i] + ": " + intOps[i]);
        }
        // float ops summary
        out.println("\nBool ops breakdown:");
        for (int i=0; i<NUM_OPS; i++) {
            out.println("  " + OP_TO_NAME[i] + ": " + boolOps[i]);
        }

        // the count by actual statement in the code
        out.println("\nCount for each static operation ID (see .java file for ID's):");
        out.println("  ID        COUNT          % OF TOTAL    CODE (if no arith op, then on RHS of +=, *=, etc) ");
        out.println("------------------------------------------------------------------------------------------");
        for (int i=0; i<idCounts.length; i++) {
            String code = (idCode[i] == null ? "" : idCode[i]);
            out.println("  " + format(i, 10) + format(idCounts[i], 15) + format(100*idCounts[i]/(float)opsTotal, 4) + "          " + code);
        }

        // the sorted count by actual statement in the code
        out.println("\nSorted count for each static operation ID (see .java file for ID's):");
        out.println("  ID        COUNT          % OF TOTAL    CODE (if no arith op, then on RHS of +=, *=, etc) ");
        out.println("------------------------------------------------------------------------------------------");
        // just do selection sort because I had it handy from C++ code
        for (int i=0; i<idCounts.length; i++) {
            int max = 0;
            // find greatest
            for (int j=0; j<idCounts.length; j++) {
                if (idCounts[j] > idCounts[max]) {
                    max = j;
                }
            }
            // print greatest
            String code = (idCode[max] == null ? "" : idCode[max]);
            out.println("  " + format(max, 10) + format(idCounts[max], 15) + format(100*idCounts[max]/(float)opsTotal, 4) + "          " + code);
            // zero-out greatest
            idCounts[max] = -1;
        }
    }

    /**
     * Formats <pre>f</pre> into an <pre>n</pre>-character string.
     */
    private static String format(float f, int n) {
        String str = ""+f;
        int length = str.length();
        for (int j=length; j<n; j++) {
            str += " ";
        }
        return str.substring(0, n);
    }

    /**
     * Formats <pre>i</pre> into an <pre>n</pre>-character string.
     */
    private static String format(int i, int n) {
        String str = ""+i;
        int length = str.length();
        for (int j=length; j<n; j++) {
            str += " ";
        }
        return str;
    }
}
