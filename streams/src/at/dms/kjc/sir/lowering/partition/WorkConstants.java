package at.dms.kjc.sir.lowering.partition;

/**
 * This interface contains estimates of work for various operations.
 */
interface WorkConstants {
    // operations
    int PEEK = 4;
    int POP = 4;
    int PRINT = 4;
    int PUSH = 4;
    int SWITCH = 4;

    int ASSIGN = 1;

    int INT_ARITH_OP = 1;
    int FLOAT_ARITH_OP = 4;

    // treat a return like an assignment, since you might have to
    // remove something into a return register?
    int RETURN = ASSIGN;

    // how many times we assume a loop executes if we encounter one
    int LOOP_COUNT = 5;

    // TODO - I can't find the mis-predict penalty for a branch on
    // raw.  I guess it should be factored into an the costs below.
    int IF = 1;
    int CONTINUE = 1;
    int BREAK = 1;

    // NEED TO VERIFY THE FOLLOWING
    int MEMORY_OP = 3;
    int METHOD_CALL = 60;
}
