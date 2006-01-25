package at.dms.kjc.lir;

import at.dms.kjc.*;
import at.dms.compiler.*;

/**
 * This sets the type of a stream.
 */
public class LIRStreamType {

    public static final LIRStreamType LIR_PIPELINE = 
        new LIRStreamType("PIPELINE");
    public static final LIRStreamType LIR_SPLIT_JOIN = 
        new LIRStreamType("SPLIT_JOIN");
    public static final LIRStreamType LIR_FEEDBACK_LOOP = 
        new LIRStreamType("FEEDBACK_LOOP");
    public static final LIRStreamType LIR_FILTER = 
        new LIRStreamType("FILTER");

    private final String name;

    private LIRStreamType(String name) {
        this.name = name;
    }

    public String toString() {
        return name;
    }
}
