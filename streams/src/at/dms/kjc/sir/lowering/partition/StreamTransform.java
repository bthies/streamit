package at.dms.kjc.sir.lowering.partition;

import java.util.*;

import at.dms.kjc.*;
import at.dms.util.*;
import at.dms.kjc.sir.*;
import at.dms.kjc.sir.lowering.*;

/**
 * This represents a symbolic transformation on a stream graph.
 */

abstract class StreamTransform {
    /**
     * List of child transforms.
     */
    protected List children;

    protected StreamTransform() {
	this.children = new LinkedList();
    }

    /**
     * Perform the transform on <str> and return new stream.
     */
    abstract SIRStream doTransform(SIRStream str);

    /**
     * Adds a child transform to this.
     */
    public void add(StreamTransform st) {
	children.add(st);
    }
    
    /**
     * Gets the <i'th> child transform from this.
     */
    public StreamTransform get(int i) {
	return (StreamTransform)children.get(i);
    }

    /**************************************************************/

}
