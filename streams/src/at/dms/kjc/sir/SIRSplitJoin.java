package at.dms.kjc.sir;

import at.dms.kjc.*;

/**
 * This represents a SplitJoin construct.
 */
public class SIRSplitJoin extends SIRStream {
    /**
     * The splitter at the top of this.
     */
    private SIRSplitter splitter;
    /**
     * The joiner at the bottom of this.
     */
    private SIRJoiner joiner;
    /**
     * The stream components in this.  The i'th element of this array
     * corresponds to the i'th tape in the splitter and joiner.  
     */
    private SIRStream elements[];

    /**
     * Accepts visitor <v> at this node.
     */
    public void accept(SIRVisitor v) {
	v.visitSplitJoin(this,
			 fields,
			 methods,
			 init);
	/* visit components */
	splitter.accept(v);
	for (int i=0; i<elements.length; i++) {
	    elements[i].accept(v);
	}
	joiner.accept(v);
    }

    /**
     * Construct a new SIRPipeline with the given fields and methods.
     */
    public SIRSplitJoin(JFieldDeclaration[] fields,
			JMethodDeclaration[] methods) {
	super(fields, methods);
    }
}
