package at.dms.kjc.spacetime;

import at.dms.util.Utils;
import at.dms.kjc.*;
import java.util.Set;
import java.util.HashSet;


/** 
 *
 **/
public class OutputTraceNode extends TraceNode
{
    private int[] weights;
    private InputTraceNode[][] dests;
    private String ident;
    private static int unique = 0;
    private static int[] EMPTY_WEIGHTS=new int[0];
    private static InputTraceNode[][] EMPTY_DESTS=new InputTraceNode[0][0];
    private Trace parent;

    public OutputTraceNode(int[] weights,
			   InputTraceNode[][] dests) {
	this.parent = parent;
	assert weights.length == dests.length : 
	    "weights must equal sources";
	ident = "output" + unique;
	unique++;
	this.weights=weights;
	this.dests = dests;
    }
    
    public OutputTraceNode(int[] weights) {
	this.parent = parent;
	ident = "output" + unique;
	unique++;
	this.weights=weights;
	dests=EMPTY_DESTS;
    }

    public OutputTraceNode() {
	this.parent = parent;
	ident = "output" + unique;
	unique++;
	weights=EMPTY_WEIGHTS;
	dests=EMPTY_DESTS;
    }

    public void setParent(Trace parent) 
    {
	this.parent = parent;
    }
    
    
    public Trace getParent() 
    {
	assert parent != null : "parent not set for output trace node";
	return parent;
    }
    public int[] getWeights() {
	return weights;
    }

    public boolean isFileInput() 
    {
	return ((FilterTraceNode)getPrevious()).isFileInput();
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

    public CType getType() 
    {
	//keep search backwards until you find a filtertrace node
	//and return its type
	TraceNode current = this;
	while (!current.isFilterTrace()){
	    //check this
	    if (current.isInputTrace())
		Utils.fail("previous of outputnode is inputnode, where is the filter");
	    current = current.getPrevious();
	}
	return ((FilterTraceNode)current).getFilter().getOutputType();
    }
    
    //return a set containing the destinations for this input trace node
    public Set getDestSet() 
    {
	HashSet set = new HashSet();
	for (int i = 0; i < dests.length; i++) {
	    for (int j = 0; i < dests[i].length; j++)
		set.add(dests[i][j]);
	}
	return set;
    }
    
    public boolean oneOutput() 
    {
	return (weights.length == 1 &&
		dests[0].length == 1);
    }
}
