package at.dms.kjc.sir.lowering.partition;

import java.util.*;

import at.dms.kjc.*;
import at.dms.util.*;
import at.dms.kjc.sir.*;
import at.dms.kjc.sir.lowering.*;
import at.dms.kjc.sir.lowering.fusion.*;

/**
 * This represents a symbolic transformation on a stream graph.
 */

public abstract class StreamTransform {
    /**
     * List of predecessor child transforms.  These children operate
     * on the input to this transform, BEFORE it is performed.
     */
    private List pred;
    /**
     * List of successor child transforms.  These children operate on
     * the result of this transform, AFTER it is performed.
     */
    private List succ;


    protected StreamTransform() {
	this.pred = new LinkedList();
	this.succ = new LinkedList();
    }

    protected abstract SIRStream doMyTransform(SIRStream str);

    /**
     * Perform the transform on <str> and return new stream.
     */
    public final SIRStream doTransform(SIRStream str) {
	// do preds
	if (str instanceof SIRContainer) {
	    doPredTransforms((SIRContainer)str);
	}
	// do this transform
	SIRStream result = doMyTransform(str);
	// do succ's
	if (str instanceof SIRContainer) {
	    doSuccTransforms((SIRContainer)result);
	}
	return result;
    }

    /**
     * Adds a predecessor child transform to this.
     */
    public void addPred(StreamTransform st) {
	pred.add(st);
    }
    
    /**
     * Gets the <i'th> predecessor child transform from this.
     */
    public StreamTransform getPred(int i) {
	return (StreamTransform)pred.get(i);
    }

    /**
     * Adds a successor child transform to this.
     */
    public void addSucc(StreamTransform st) {
	succ.add(st);
    }
    
    /**
     * Gets the <i'th> successor child transform from this.
     */
    public StreamTransform getSucc(int i) {
	return (StreamTransform)succ.get(i);
    }
    
    /*****************************************************************/

    /**
     * Do all the predecessor transformations on <cont>.
     */
    private void doPredTransforms(SIRContainer str) {
	doChildTransforms(str, pred);
    }

    /**
     * Do all the successor transformations on <cont>.
     */
    private void doSuccTransforms(SIRContainer str) {
	doChildTransforms(str, succ);
    }

    /**
     * Transforms the children of <cont> according to child
     * <transforms>, replacing each child in <str> with the new
     * stream.
     */
    private void doChildTransforms(SIRContainer cont, List transforms) {
	// make sure we have the same number of transforms to apply as
	// we have children
	Utils.assert(transforms.size() == cont.size());
	
	// visit transforms
	for (int i=0; i<cont.size(); i++) {
	    SIRStream child = (SIRStream)cont.get(i);
	    SIRStream newChild = ((StreamTransform)transforms.get(i)).doTransform(child);
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
