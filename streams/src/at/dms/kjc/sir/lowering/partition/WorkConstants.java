package at.dms.kjc.sir.lowering.partition;

/**
 * This interface contains estimates of work for various operations.
 */
interface WorkConstants {
    // measured delay between consecutive constant print statements
    int PRINT = 17;
    // tape operations -- have some overhead for index adjustment,
    // plus memory ops usually
    int PEEK = 3;
    int POP = 3;
    int PUSH = 3;

    int INT_ARITH_OP = 1;
    // the latency of a float op is 4 on Raw, but occupancy is only 1
    // -- so the effective work required depends on how soon we use
    // the result after issuing this instruction.  It seems to be the
    // case that 2 gives the best estimate (e.g. on fm, beamformer).
    int FLOAT_ARITH_OP = 2;

    // how many times we assume a loop executes if we encounter one
    int LOOP_COUNT = 5;

    // TODO - I can't find the mis-predict penalty for a branch on
    // raw.  I guess it should be factored into an the costs below.
    int SWITCH = 1;
    int IF = 1;
    int CONTINUE = 1;
    int BREAK = 1;

    // NEED TO VERIFY THE FOLLOWING

    // occupancy is 1, but latency is 3
    int MEMORY_OP = 2;
    // overhead for any method call
    int METHOD_CALL_OVERHEAD = 10;
    // the amount of work estimated for a method that we can't find
    // the source for
    int UNKNOWN_METHOD_CALL = 60;
}
