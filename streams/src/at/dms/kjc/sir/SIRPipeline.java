package at.dms.kjc.sir;

import at.dms.kjc.*;
import java.util.LinkedList;

/**
 * This represents a pipeline of stream structures, as would be
 * declared with a Stream construct in StreaMIT.
 */
public class SIRPipeline extends SIRStream {
    /**
     * The elements of the pipeline.  Each element should be an SIRStream.
     */
    private LinkedList elements;

    /**
     * Construct a new SIRPipeline with the given fields and methods.
     */
    public SIRPipeline(SIRStream parent,
		       JFieldDeclaration[] fields,
		       JMethodDeclaration[] methods) {
	super(parent, fields, methods);
	// initialize elements array
	this.elements = new LinkedList();
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
    public SIRStream get(int i) {
	return (SIRStream)elements.get(i);
    }

    /**
     * Returns the index of <str> in this pipeline, or -1 if <str>
     * does not appear in this.
     */
    public int indexOf(SIRStream str) {
	return elements.indexOf(str);
    }

    /**
     * Returns the number of substreams in this.
     */
    public int size() {
	return elements.size();
    }

    /**
     * Accepts visitor <v> at this node.
     */
    public void accept(SIRVisitor v) {
	v.preVisitPipeline(this,
			   parent,
			   fields,
			   methods,
			   init,
			   elements);
	/* visit components */
	for (int i=0; i<elements.size(); i++) {
	    ((SIRStream)elements.get(i)).accept(v);
	}
	v.postVisitPipeline(this,
			    parent,
			    fields,
			    methods,
			    init,
			    elements);
    }
}

