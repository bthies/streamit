package at.dms.kjc.raw;

import at.dms.kjc.*;
import at.dms.kjc.sir.*;
import at.dms.kjc.sir.lowering.*;
import at.dms.util.Utils;

import java.util.HashSet;


/**
 * This class represents a node in the flattened graph
 */
public class FlatNode {
    
    /* The operator this node contains (either a splitter, joiner, or filter) */
    public SIROperator contents;
    public FlatNode[] edges;
    public int[] weights;
    public int ways;
        /* the current edges we are connecting, all edges before this are connected */
    private int currentEdge;
    
    
    /* create a new node with <op> */
    public FlatNode(SIROperator op) 
    {
	contents = op;
	currentEdge = 0;
	if (op instanceof SIRFilter)
	    ways = 0;
		   
	if (op instanceof SIRJoiner)
	    ways = 0;
	
	if (op instanceof SIRSplitter) {
	    SIRSplitter splitter = (SIRSplitter)op;
	    int num = splitter.getWays();
	    ways = num;
	    edges = new FlatNode[num];
	    weights = splitter.getWeights();
	}
    }
    
    public void addEdgeTo(FlatNode to) 
    {
	//create the edge and weight arrays only if this node is connected
	//to something
	if ( ways == 0) {
	    ways = 1;
	    edges = new FlatNode[ways];
	    weights = new int[1];
	    weights[0] = 1;
	}
	
	// do not connect to oneself
	if (!(to.equals(this)))
	    edges[currentEdge++] = to;
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
	for (int i = 0; i < ways; i++)
	    if (!set.contains(edges[i]))
		edges[i].accept(v, set, false);
    }
}

