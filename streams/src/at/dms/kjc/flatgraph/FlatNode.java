package at.dms.kjc.flatgraph;

import at.dms.kjc.*;
import at.dms.kjc.sir.*;
import at.dms.kjc.sir.lowering.*;
import at.dms.util.Utils;

import java.util.HashSet;
import java.util.HashMap;



/**
 * This class represents a node in the flattened graph
 */
public class FlatNode {
    
    /* The operator this node contains (either a splitter, joiner, or filter) */
    public SIROperator contents;
    public FlatNode[] edges;
    public FlatNode[] incoming;
    public int[] incomingWeights;
    public int[] weights;
    public int inputs;
    public int ways;
        /* the current edges we are connecting, all edges before this are connected */
    public int currentEdge;
    public int currentIncoming;

    private HashMap identMap;
    private static int nameInt=0;
    
    public int schedMult;
    public int schedDivider;

    private static int uin=0;
    private int label;

    public SIROperator oldContents;

    /* create a new node with <op> */
    public FlatNode(SIROperator op) 
    {
	if (op == null) {
	    Utils.fail("");
	}
	contents = op;
	currentEdge = 0;
	currentIncoming = 0;
	if (op instanceof SIRFilter) {
	    ways = 0;
	    inputs = 0;
	    edges = new FlatNode[1];
	    edges[0] = null;
	}
		   
	if (op instanceof SIRJoiner) {
	    SIRJoiner joiner = (SIRJoiner)op;
	    ways = 0;
	    inputs = joiner.getWays();
	    incoming = new FlatNode[inputs];
	    incomingWeights = joiner.getWeights();
	    edges = new FlatNode[1];
	    edges[0] = null;
	}
	if (op instanceof SIRSplitter) {
	    SIRSplitter splitter = (SIRSplitter)op;
	    ways = splitter.getWays();
	    edges = new FlatNode[ways];
	    weights = splitter.getWeights();
	    inputs = 0;
	}
	identMap=new HashMap();
	label=uin++;
    }

    public void addEdges(FlatNode to) {
	//do not connect to oneself
	if (!(this.equals(to))) {
	    this.addEdgeTo(to);
	    to.addIncomingFrom(this);
	}
    }
    
    public static void addEdges(FlatNode from, FlatNode to) {
	if (from != null) {
	    from.addEdgeTo(to);
	}
	if (to != null)
	    to.addIncomingFrom(from);
    }
    

    public void addEdgeTo(FlatNode to) 
    {
	//create the edge and weight arrays only if this node is connected
	//to something
	if (ways == 0) {
	    ways = 1;
	    edges = new FlatNode[ways];
	    weights = new int[1];
	    weights[0] = 1;
	}
	
	
	edges[currentEdge++] = to;
    }

    public void addIncomingFrom(FlatNode from) {
	if (inputs == 0) {
	    inputs = 1;
	    incoming = new FlatNode[1];
	    incomingWeights = new int[1];
	    incomingWeights[0] = 1;
	}
	
	incoming[currentIncoming++] = from;
    }
	
    /*
      This function is called by rawFlattener after createGraph is called.
      It is called for each splitter of a feedback loop.  
      createGraph connects the outgoing edges of the splitter of a feedback
      in the reverse order and this swaps them
    */
    public void swapSplitterEdges() 
    {
	if (!(contents instanceof SIRSplitter) ||
	    !(contents.getParent() instanceof SIRFeedbackLoop))
	    Utils.fail("We do not want to swap the edges on non-splitter");
	if(edges.length != 2)
	    return;
    
	//The weights are correct and do not need to be swapped
	
	FlatNode temp = edges[0];
	edges[0] = edges[1];
	edges[1] = temp;
    }
    
	
    /** 
     * accept a visitor, since this graph can have loops, 
     * we have to keep track of what he have visited.  
     * If true <reset> resets the given hashset
     */
    public void accept(FlatVisitor v, HashSet set, boolean reset) 
    {
	if (reset)
	    set = new HashSet();
	
	set.add(this);
	v.visitNode(this);
	for (int i = 0; i < ways; i++) {
	    if (edges[i] == null)
		continue;
	    if (!set.contains(edges[i]))
		edges[i].accept(v, set, false);
	}
    }
    
    /**
     * Override the hashcode so that it is deterministic with each
     * run, so that layouts can be deterministic.
     */
    public int hashCode() {
	return inputs * ways * 
	    (edges==null ? 1 : edges.length) * 
	    (incoming==null ? 1 : incoming.length);
    }

    /**
     * Now uses uin to implement deterministic hashcode system
     * It has the added benefit that a FlatNode's identity
     * isn't tied to it's inputs, ways, etc not changing
     * ie now hashcode is synched with equals() and equality
     * doesn't change just because ones ways, etc changes
     */
    /*public int hashCode() {
      return hashCode;
      }*/
    
    public String getName() {
	//if((contents instanceof SIRIdentity)||(contents instanceof SIRJoiner)) {
	    /*String out=(String)identMap.get(contents);
	      if(out==null) {
	      out=contents.getName()+"_"+(nameInt++);
	      identMap.put(contents,out);
	      return out;
	      } else
	      return out;*/
	    return contents.getName()+"_"+label;
	    //} else
	    //return contents.getName();
    }

    public boolean isFilter() {
	if (contents instanceof SIRFilter) 
	    return true;
	return false;
    }
    
    public boolean isJoiner() {
	if (contents instanceof SIRJoiner) 
	    return true;
	return false;
    }

    public boolean isSplitter() {
	if (contents instanceof SIRSplitter) 
	    return true;
	return false;
    }

    public boolean isDuplicateSplitter() 
    {
	if (contents instanceof SIRSplitter &&
	    ((SIRSplitter)contents).getType() == SIRSplitType.DUPLICATE)
	    return true;
	return false;
    }
    
    
    public String toString() {
	return "FlatNode:"+getName();
    }
    
    public int getTotalIncomingWeights() 
    {
	int sum = 0;
	
	for (int i= 0; i < inputs; i++)
	    sum += incomingWeights[i];
	return sum;
    }
    
    public int getTotalOutgoingWeights() 
    {
	int sum = 0;
	
	for (int i = 0; i < ways; i++) 
	    sum += weights[i];
	
	return sum;
    }
    

    /**
     *  get partial sum of weights 0 thru i - 1 
    **/
    public int getPartialOutgoingSum(int i) 
    {
	assert i >= 0 && i < ways;
	    
	int sum = 0;

	for (int q = 0; q < i; q++)
	    sum += weights[q];
	
	return sum;
    }
    
    /**
     * get the partial sum of incoming weight 0 thru i - 1
     **/
    public int getPartialIncomingSum(int i) 
    {
	assert i >= 0 && i < inputs;
	
	int sum = 0; 
	
	for (int j = 0; j < i; j++)
	    sum += incomingWeights[j];
	
	return sum;
    }
    

    public int getWeight(FlatNode to) 
    {
	for (int i = 0; i < ways; i++) {
	    if (edges[i] == to)
		return weights[i];
	}
	assert false : "Node " + this + " not connected to " + to;
	return -1;
    }
}

