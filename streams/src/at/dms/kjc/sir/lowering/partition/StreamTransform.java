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
    /**
     * Identifier for this transform.
     */
    protected int id;    
    /**
     * Max identifier;
     */
    private static int maxId = 0;

    protected StreamTransform() {
	this.pred = new LinkedList();
	this.succ = new LinkedList();
	this.id = maxId++;
    }

    protected abstract SIRStream doMyTransform(SIRStream str);

    /**
     * Perform the transform on <str> and return new stream.
     */
    public final SIRStream doTransform(SIRStream str) {
	//System.err.println("performing " + this + " on " + str.getName());
	//printHierarchy();
	// do preds (if we have children and there are any transforms)
	if (str instanceof SIRContainer && pred.size()>0) {
	    doPredTransforms((SIRContainer)str);
	}
	// do this transform
	SIRStream result = doMyTransform(str);

	// do succ's (if we have children and there are any transforms)
	if (result instanceof SIRContainer && succ.size()>0) {
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
     * Returns the number of predecessors.
     */
    public int getPredSize() {
	return pred.size();
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

    /**
     * Returns the number of successors.
     */
    public int getSuccSize() {
	return succ.size();
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
	assert transforms.size() == cont.size():
            "Have " + transforms.size() + " transforms but " +
            cont.size() + " children for " + cont.getName() +
            ".  Transforms are children of " + this;
	
	// visit transforms
	for (int i=0; i<cont.size(); i++) {
	    SIRStream child = (SIRStream)cont.get(i);
	    SIRStream newChild = ((StreamTransform)transforms.get(i)).doTransform(child);
	    // some people did their own replacing, returning the orig
	    // stream or null, so only do it if it's not done
	    if (newChild!=null && child!=newChild && cont.get(i)!=newChild) {
		cont.replace(child, newChild);
	    }
	}

	// try lifting pipelines as post-pass so as not to mess up
	// counters above note that this will mutate the children
	// array and the init function of <self>
	for (int i=0; i<cont.size(); i++) {
	    if (cont.get(i) instanceof SIRPipeline) {
		int size = ((SIRPipeline)cont.get(i)).size();
		Lifter.eliminatePipe((SIRPipeline)cont.get(i));
		i+=size-1;
	    }
	}
    }

    /**
     * Prints hierarchy of stream transforms rooted at <st>.
     */
    public void printHierarchy() {
	printHierarchy(0);
    }

    private void printHierarchy(int tabs) {
	if (pred.size()>0) {
	    for (int i=0; i<tabs; i++) {
		System.err.print("  ");
	    }
	    System.err.println((tabs+"").charAt(0) + " - Preds: (" + pred.size() + ")");
	    for (int i=0; i<pred.size(); i++) {
		((StreamTransform)pred.get(i)).printHierarchy(tabs+1);
	    }
	}
	for (int i=0; i<tabs; i++) {
	    System.err.print("  ");
	}
	System.err.println((tabs+"").charAt(0) + " - NODE: " + this);
	if (succ.size()>0) {
	    for (int i=0; i<tabs; i++) {
		System.err.print("  ");
	    }
	    System.err.println((tabs+"") + " - Succs: (" + succ.size() + ")");
	    for (int i=0; i<succ.size(); i++) {
		((StreamTransform)succ.get(i)).printHierarchy(tabs+1);
	    }	
	}
    }

    /**
     * If this is an idempotent transform, then returns an identity
     * transform with no predecessors or sucessors.  Otherwise returns
     * this.
     */
    public final StreamTransform reduce() {
	if (isIdempotent()) {
	    return new IdentityTransform();
	} else {
	    return this;
	}
    }

    /**
     * Returns whether or not the deep set of transforms starting at
     * this performs any fusion or fission on the child streams.  If
     * it is just re-arranging synchronization, then it is idempotent
     * (and can be replaced by an identity if desired.)
     */
    protected boolean isIdempotent() {
	return false;
    }

}
