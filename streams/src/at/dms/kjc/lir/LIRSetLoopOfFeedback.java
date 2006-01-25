package at.dms.kjc.lir;

import at.dms.kjc.*;
import at.dms.compiler.*;

/**
 * This represents a the loop stream in a feedback loop.  At the
 * runtime system level, it should take care of connecting the inputs
 * and outputs of the stream with the splitter and joiner.  It should
 * not allocate the structure or call init functions for the parallel
 * stream--this will be done separately with an LIRSetChild construct
 * and an explicit call to the child's init function.  
 */
public class LIRSetLoopOfFeedback extends LIRNode {

    /**
     * The child context to be used as the feedback loop loop part.
     */
    private JExpression childContext;

    /**
     * The input type of the loop stream.
     */
    private CType inputType;
    
    /**
     * The output type of the loop stream.
     */
    private CType outputType;
    
    /**
     * The number of items appearing on the input tape of the loop stream.
     */
    private int inputSize;

    /**
     * The number of items appearing on the output tape of the loop stream.
     */
    private int outputSize;

    // ----------------------------------------------------------------------
    // CONSTRUCTORS
    // ----------------------------------------------------------------------

    /**
     * Construct a node.
     */
    public LIRSetLoopOfFeedback(JExpression streamContext,
                                JExpression childContext,
                                CType inputType,
                                CType outputType,
                                int inputSize,
                                int outputSize) {
        super(streamContext);
        this.childContext = childContext;
        this.inputType = inputType;
        this.outputType = outputType;
        this.inputSize = inputSize;
        this.outputSize = outputSize;
    }

    public void accept(SLIRVisitor v)
    {
        v.visitSetLoopOfFeedback(this,
                                 this.getStreamContext(), 
                                 this.childContext,
                                 this.inputType,
                                 this.outputType,
                                 this.inputSize,
                                 this.outputSize);
    }
}
