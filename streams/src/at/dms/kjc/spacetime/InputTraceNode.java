package at.dms.kjc.spacetime;

import at.dms.util.Utils;

/** 
 *
 **/
public class InputTraceNode extends TraceNode
{
    private int[] weights;
    private Trace[] sources;

    public InputTraceNode(int[] weight,
			  Trace[] sources) {
	this.weights = weights;
	this.sources = sources;
	if (weight.length != sources.length)
	    Utils.fail("Add comment later");
    }

    public int[] getWeight() {
	return weights;
    }

    public Trace[] getSources() {
	return sources;
    }
}
