package at.dms.kjc.spacetime;

import at.dms.util.Utils;

/** 
 *
 **/
public class OutputTraceNode extends TraceNode
{
    private int[] weights;
    private InputTraceNode[][] dests;
    private String ident;
    private static int unique = 0;

    public OutputTraceNode(int[] weights,
			   InputTraceNode[][] dests) {
	this(weights);
	this.dests = dests;
	if (weights.length != dests.length)
	    Utils.fail("Add comment later");
    }
    
    public OutputTraceNode(int[] weights) {
	ident = "output" + unique;
	unique++;
	this.weights=weights;
    }

    public int[] getWeights() {
	return weights;
    }
    
    public InputTraceNode[][] getDests() {
	return dests;
    }
    
    public void setDests(InputTraceNode[][] dests) {
	this.dests=dests;
    }

    public String getIdent() 
    {
	return ident;
    }
}
