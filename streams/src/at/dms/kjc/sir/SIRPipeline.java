package at.dms.kjc.sir;

import at.dms.kjc.*;
import java.util.Vector;

/**
 * This represents a pipeline of stream structures, as would be
 * declared with a Stream construct in StreaMIT.
 */
public class SIRPipeline extends SIRStream {
    /**
     * The elements of the pipeline.  Each element should be an SIRStream.
     */
    private Vector elements;

    /**
     * Construct a new SIRPipeline with the given fields and methods.
     */
    public SIRPipeline(JFieldDeclaration[] fields,
			JMethodDeclaration[] methods) {
	super(fields, methods);
	// initialize elements array
	this.elements = new Vector();
    }

    /**
     * Add a stream to the pipeline.
     */
    public void add(SIRStream str) {
	elements.add(str);
    }

    /**
     * Return i'th stream in this pipeline.
     */
    public SIRStream elementAt(int i) {
	return (SIRStream)elements.elementAt(i);
    }
}

