package at.dms.kjc.spacetime;

import at.dms.util.Utils;
import at.dms.kjc.*;
import java.util.Set;
import java.util.HashSet;
import java.util.Vector;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

/** 
 *
 **/
public class OutputTraceNode extends TraceNode
{
    private int[] weights;
    private Edge[][] dests;
    private String ident;
    private static int unique = 0;
    private static int[] EMPTY_WEIGHTS=new int[0];
    private static Edge[][] EMPTY_DESTS=new Edge[0][0];
    private List sortedOutputs;
    private Set destSet;

    public OutputTraceNode(int[] weights,
			   Edge[][] dests) {
	//this.parent = parent;
	assert weights.length == dests.length : 
	    "weights must equal sources";
	ident = "output" + unique;
	unique++;
	this.weights=weights;
	this.dests = dests;
    }
    
    public OutputTraceNode(int[] weights) {
	//this.parent = parent;
	ident = "output" + unique;
	unique++;
	this.weights=weights;
	dests=EMPTY_DESTS;
    }

    public OutputTraceNode() {
	//this.parent = parent;
	ident = "output" + unique;
	unique++;
	weights=EMPTY_WEIGHTS;
	dests=EMPTY_DESTS;
    }

    public int[] getWeights() {
	return weights;
    }

    public boolean isFileInput() 
    {
	return ((FilterTraceNode)getPrevious()).isFileInput();
    }
    
    
    public Edge[][] getDests() {
	return dests;
    }
    
    public void setDests(Edge[][] dests) {
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
    public int getWeight(Edge in) 
    {
	int sum = 0;
	
	for (int i = 0; i < dests.length; i++) {
	    for (int j = 0; j < dests[i].length; j++)
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
	    for (int j = 0; j < dests[i].length; j++)
		set.add(dests[i][j]);
	}
	return set;
    }
    
    public boolean oneOutput() 
    {
	return (weights.length == 1 &&
		dests[0].length == 1);
    }
    
    public Edge getSingleEdge() 
    {
	assert oneOutput() :
	    "Calling getSingleEdge() on OutputTrace with less/more than one output";
	return dests[0][0];
    }
    


    public boolean noOutputs() 
    {
	return weights.length == 0;
    }
    
    
    /** return an iterator that iterates over the 
     * inputtracenodes in descending order of the number
     *  of items sent to the inputtracenode
     **/
    public List getSortedOutputs() 
    {
	if (sortedOutputs == null) {
	    //if there are no dest just return an empty iterator
	    if (weights.length == 0) {
		sortedOutputs = new LinkedList();
		return sortedOutputs;
	    }
	    //just do a simple linear insert over the dests
	    //only has to be done once
	    Vector sorted = new Vector();
	    Iterator dests = getDestSet().iterator();
	    //add one element
	    sorted.add(dests.next());
	    while (dests.hasNext()) {
		Edge current = (Edge)dests.next();
		//add to end if it is less then everything
		if (getWeight(current) <= 
		    getWeight((Edge)sorted.get(sorted.size() - 1))) 		  
		    sorted.add(current);
		else {  //otherwise find the correct place to add it
		    for (int i = 0; i < sorted.size(); i++) {
			//if this is the correct place to insert it, 
			//add it and break
			if (getWeight(current) > 
			    getWeight((Edge)sorted.get(i))) {
			    sorted.add(i, current);
			    break;
			}
		    }
		}
	    }
	    assert sorted.size() == getDestSet().size() :
		"error " + sorted.size() + "!= " + getDestSet().size();
	    sortedOutputs = sorted.subList(0, sorted.size());
	}
	return sortedOutputs;
    }
    
    public FilterTraceNode getPrevFilter() 
    {
	return (FilterTraceNode)getPrevious();
    }
    
}
