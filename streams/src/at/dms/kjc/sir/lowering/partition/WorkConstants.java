package at.dms.kjc.sir.lowering.partition;

/**
 * This interface contains estimates of work for various operations.
 */
interface WorkConstants {
    // measured delay between consecutive constant print statements
    int PRINT = 17;
    // operations
    int PEEK = 4;
    int POP = 4;
    int PUSH = 4;
    int SWITCH = 4;

    int INT_ARITH_OP = 1;
    int FLOAT_ARITH_OP = 4;

    // how many times we assume a loop executes if we encounter one
    int LOOP_COUNT = 5;

    // TODO - I can't find the mis-predict penalty for a branch on
    // raw.  I guess it should be factored into an the costs below.
    int IF = 1;
    int CONTINUE = 1;
    int BREAK = 1;

    // NEED TO VERIFY THE FOLLOWING
    int MEMORY_OP = 3;
    // overhead for any method call
    int METHOD_CALL_OVERHEAD = 10;
    // the amount of work estimated for a method that we can't find
    // the source for
    int UNKNOWN_METHOD_CALL = 60;
}
