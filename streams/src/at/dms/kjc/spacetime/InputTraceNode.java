package at.dms.kjc.spacetime;

import at.dms.util.Utils;

/** 
 *
 **/
public class InputTraceNode extends TraceNode
{
    private int[] weights;
    private OutputTraceNode[] sources;

    public InputTraceNode(int[] weight,
			  OutputTraceNode[] sources) {
	this.weights = weights;
	this.sources = sources;
	if (weight.length != sources.length)
	    Utils.fail("Add comment later");
    }

    public InputTraceNode(int[] weight) {
	this.weights=weights;
    }

    public int[] getWeight() {
	return weights;
    }

    public OutputTraceNode[] getSources() {
	return sources;
    }

    public void setSources(OutputTraceNode[] sources) {
	this.sources=sources;
    }
}
