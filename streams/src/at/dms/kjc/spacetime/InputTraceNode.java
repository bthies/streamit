package at.dms.kjc.spacetime;

import at.dms.util.Utils;

/** 
 *
 **/
public class InputTraceNode extends TraceNode
{
    private int[] weights;
    private OutputTraceNode[] sources;
    private static int unique = 0;
    private String ident;

    public InputTraceNode(int[] weights,
			  OutputTraceNode[] sources) {
	this(weights);
	this.sources = sources;
	if (weights.length != sources.length)
	    Utils.fail("Add comment later");

    }

    public InputTraceNode(int[] weights) {
	this.weights=weights;
	ident = "input" + unique;
	unique++;
    }

    public String getIdent() 
    {
	return ident;
    }
    
    public int[] getWeights() {
	return weights;
    }

    public OutputTraceNode[] getSources() {
	return sources;
    }

    public void setSources(OutputTraceNode[] sources) {
	this.sources=sources;
    }

    public int totalWeights() 
    {
	int sum = 0;
	for (int i = 0; i < weights.length; i++) 
	    sum += weights[i];
	return sum;
    }
    
}
