package at.dms.kjc.lir;

import at.dms.kjc.*;
import at.dms.compiler.*;

/**
 * This represents a parallel stream in a split-join construct.  At
 * the runtime system level, it should take care of connecting the
 * inputs and outputs of the parallel stream with the splitter and
 * joiner.  It should not allocate the structure or call init
 * functions for the parallel stream--this will be done separately
 * with an LIRSetChild construct and an explicit call to the child's
 * init function.
 */
public class LIRSetParallelStream extends LIRNode {

    /**
     * The child context to be used as the split/join body.
     */
    private JExpression childContext;

    /**
     * The position of this stream in its parent's SplitJoin
     * construct.  That is, the 0'th position is on the far left, then
     * the 1'st position, etc.
     */
    private int position;

    /**
     * The input type of the parallel stream.
     */
    private CType inputType;
    
    /**
     * The output type of the parallel stream.
     */
    private CType outputType;
    
    /**
     * The number of items appearing on the input tape of the parallel stream.
     */
    private int inputSize;

    /**
     * The number of items appearing on the output tape of the parallel stream.
     */
    private int outputSize;

    // ----------------------------------------------------------------------
    // CONSTRUCTORS
    // ----------------------------------------------------------------------

    /**
     * Construct a node.
     */
    public LIRSetParallelStream(JExpression streamContext,
                                JExpression childContext,
                                int position,
                                CType inputType,
                                CType outputType,
                                int inputSize,
                                int outputSize) {
        super(streamContext);
        this.childContext = childContext;
        this.position = position;
        this.inputType = inputType;
        this.outputType = outputType;
        this.inputSize = inputSize;
        this.outputSize = outputSize;
    }

    public void accept(SLIRVisitor v)
    {
        v.visitSetParallelStream(this,
                                 this.getStreamContext(), 
                                 this.childContext,
                                 this.position,
                                 this.inputType,
                                 this.outputType,
                                 this.inputSize,
                                 this.outputSize);
    }
}
