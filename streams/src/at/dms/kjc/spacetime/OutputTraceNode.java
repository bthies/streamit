package at.dms.kjc.spacetime;

import at.dms.util.Utils;

/** 
 *
 **/
public class OutputTraceNode extends TraceNode
{
    private int[] weights;
    private InputTraceNode[][] dests;
    
    public OutputTraceNode(int[] weights,
			   InputTraceNode[][] dests) {
	this.weights = weights;
	this.dests = dests;
	if (weights.length != dests.length)
	    Utils.fail("Add comment later");
    }
    
    public OutputTraceNode(int[] weights) {
	this.weights=weights;
    }

    public int[] getWeight() {
	return weights;
    }
    
    public InputTraceNode[][] getDests() {
	return dests;
    }
    
    public void setDests(InputTraceNode[][] dests) {
	this.dests=dests;
    }
}
