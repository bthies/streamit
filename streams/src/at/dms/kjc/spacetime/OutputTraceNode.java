package at.dms.kjc.spacetime;

import at.dms.util.Utils;

/** 
 *
 **/
public class OutputTraceNode extends TraceNode
{
    private int[] weights;
    private Trace[][] dests;
    
    public OutputTraceNode(int[] weight,
			  Trace[][] dests) {
	this.weights = weights;
	this.dests = dests;
	if (weight.length != dests.length)
	    Utils.fail("Add comment later");
    }

    public int[] getWeight() {
	return weights;
    }

    public Trace[][] getDests() {
	return dests;
    } 
}
