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
    
    public int totalWeights() 
    {
	int sum = 0;
	for (int i = 0; i < weights.length; i++)
	    sum += weights[i];
	return sum;
    }
    
    /**
     * return the number of items sent to this inputtracenode
     * for on iteration of the weights..
     **/
    public int getWeight(InputTraceNode in) 
    {
	int sum = 0;
	
	for (int i = 0; i < dests.length; i++) {
	    for (int j = 0; i < dests[i].length; j++)
		if (dests[i][j] == in) {
		    sum += weights[i];
		    break;
		}
	}
	return sum;
    }
}
