package at.dms.kjc.sir.lowering.partition;

import java.util.*;

import at.dms.kjc.*;
import at.dms.util.*;
import at.dms.kjc.sir.*;
import at.dms.kjc.sir.lowering.*;
import at.dms.kjc.sir.lowering.fusion.*;

/**
 * Identity transform on a stream graph.
 */

class IdentityTransform extends StreamTransform {

    public IdentityTransform() {
	super();
    }

    /**
     * Perform the transform on <str> and return new stream.
     */
    public SIRStream doTransform(SIRStream str) {
	if (str instanceof SIRContainer) {
	    transformChildren((SIRContainer)str);
	}
	return str;
    }

    /**
     * Transforms the children of <str> according to child transforms,
     * replacing each child in <str> with the new stream.
     */
    private void transformChildren(SIRContainer str) {
	SIRContainer cont = (SIRContainer)str;
	// make sure we have the same number of transforms to apply as
	// we have children
	Utils.assert(this.children.size() == cont.size());
	
	// visit children
	for (int i=0; i<cont.size(); i++) {
	    SIRStream child = (SIRStream)cont.get(i);
	    SIRStream newChild = ((StreamTransform)children.get(i)).doTransform(child);
	    cont.set(i, newChild);
	    // if we got a pipeline, try lifting it.  note that this
	    // will mutate the children array and the init function of
	    // <self>
	    if (newChild instanceof SIRPipeline) {
		Lifter.eliminatePipe((SIRPipeline)newChild);
	    }
	}
    }
}
