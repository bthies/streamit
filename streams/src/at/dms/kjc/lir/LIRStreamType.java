package at.dms.kjc.lir;

import at.dms.kjc.*;
import at.dms.compiler.*;

/**
 * This sets the type of a stream.
 */
public class LIRStreamType {

    public static final LIRStreamType LIR_PIPELINE = new LIRStreamType();
    public static final LIRStreamType LIR_SPLIT_JOIN = new LIRStreamType();
    public static final LIRStreamType LIR_FEEDBACK_LOOP = new LIRStreamType();
    public static final LIRStreamType LIR_FILTER = new LIRStreamType();

    private LIRStreamType() {}
}
