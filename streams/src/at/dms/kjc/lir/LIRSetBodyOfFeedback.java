package at.dms.kjc.lir;

import at.dms.kjc.*;
import at.dms.compiler.*;

/**
 * This represents a the body stream in a feedback loop.  At the
 * runtime system level, it should take care of connecting the inputs
 * and outputs of the stream with the splitter and joiner.  It should
 * not allocate the structure or call init functions for the parallel
 * stream--this will be done separately with an LIRSetChild construct
 * and an explicit call to the child's init function.  
 */
public class LIRSetBodyOfFeedback extends LIRNode {

    /**
     * The input type of the body stream.
     */
    private CType inputType;
    
    /**
     * The output type of the body stream.
     */
    private CType outputType;
    
    /**
     * The number of items appearing on the input tape of the body stream.
     */
    private int inputSize;

    /**
     * The number of items appearing on the output tape of the body stream.
     */
    private int outputSize;

    // ----------------------------------------------------------------------
    // CONSTRUCTORS
    // ----------------------------------------------------------------------

    /**
     * Construct a node.
     */
    public LIRSetBodyOfFeedback(JExpression streamContext,
				CType inputType,
				CType outputType,
				int inputSize,
				int outputSize) {
	super(streamContext);
	this.inputType = inputType;
	this.outputType = outputType;
	this.inputSize = inputSize;
	this.outputSize = outputSize;
    }

    public void accept(SLIRVisitor v)
    {
        v.visitSetBodyOfFeedback(this,
				 this.getStreamContext(), 
				 this.inputType,
				 this.outputType,
				 this.inputSize,
				 this.outputSize);
    }
}
