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
    private static int[] EMPTY_WEIGHTS=new int[0];
    private static OutputTraceNode[] EMPTY_SRCS=new OutputTraceNode[0];

    public InputTraceNode(int[] weights,
			  OutputTraceNode[] sources) {
	if (weights.length != sources.length)
	    Utils.fail("Add comment later");
	this.sources = sources;
	this.weights=weights;
	ident = "input" + unique;
	unique++;
    }

    public InputTraceNode(int[] weights) {
	sources=EMPTY_SRCS;
	this.weights=weights;
	ident = "input" + unique;
	unique++;
    }

    public InputTraceNode() {
	sources=EMPTY_SRCS;
	weights=EMPTY_WEIGHTS;
	ident = "input" + unique;
	unique++;
    }

    public boolean isFileOutput() 
    {
	return ((FilterTraceNode)getNext()).isFileOutput();
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

    public int getWeight(OutputTraceNode out) 
    {
	for (int i = 0; i < sources.length; i++)
	    if (sources[i] == out)
		return weights[i];
	Utils.fail("Cannot find weight for OutputTraceNode");
	return -1;
    }
}
