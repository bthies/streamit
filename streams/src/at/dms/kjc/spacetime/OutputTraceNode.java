package at.dms.kjc.spacetime;

import at.dms.util.Utils;

/** 
 *
 **/
public class OutputTraceNode extends TraceNode
{
    private int[] weights;
    private InputTraceNode[][] dests;
    
    public OutputTraceNode(int[] weight,
			   InputTraceNode[][] dests,
			   Trace parent) {
	super(parent);
	this.weights = weights;
	this.dests = dests;
	if (weight.length != dests.length)
	    Utils.fail("Add comment later");
    }

    public int[] getWeight() {
	return weights;
    }

    public InputTraceNode[][] getDests() {
	return dests;
    } 
}
