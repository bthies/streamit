package at.dms.kjc.raw;

import at.dms.kjc.*;
import at.dms.kjc.sir.*;
import at.dms.kjc.sir.lowering.*;
import at.dms.util.Utils;
import java.io.*;
import java.util.*;

/**
 * This class flattens the stream graph
 */

public class RawFlattener extends at.dms.util.Utils implements FlatVisitor
{
    private FlatNode currentNode;
    private StringBuffer buf;
    
    public FlatNode top;

    //maps sir operators to their corresponding flatnode
    private HashMap SIRMap;

    /**
     * Creates a new flattener based on <toplevel>
     */
    public RawFlattener(SIROperator toplevel) 
    {
	this.SIRMap = new HashMap();
	createGraph(toplevel);
    }

    /**
     * Returns the number of tiles that would be needed to execute
     * this graph.  That is, just counts the filters, plus any joiners
     * whose output is not connected to another joiner.
     */
    public int getNumTiles() {
	dumpGraph("crash.dot");
	int count = 0;
	for (Iterator it = SIRMap.entrySet().iterator(); it.hasNext(); ) {
	    Map.Entry entry = (Map.Entry)it.next();
	    if (entry.getKey() instanceof SIRFilter) {
		// always count filter
		count++;
	    } else if (entry.getKey() instanceof SIRJoiner) {
		// count a joiner if none of its outgoing edges is to
		// another joiner
		FlatNode[] edges = ((FlatNode)entry.getValue()).edges;
		int increment = 1;
		for (int i=0; i<edges.length; i++) {
		    if (edges[i]!=null &&
			edges[i].contents instanceof SIRJoiner) {
			increment = 0;
		    }
		}
		count += increment;
	    } 
	}
	return count;
    }
    
    
    private void createGraph(SIROperator current) 
    {
	if (current instanceof SIRFilter) {
	    FlatNode node = addFlatNode(current);
	    if (top == null) {
		currentNode = node;
		top = node;
	    }
	    
	    currentNode.addEdges(node);
	    currentNode = node;
	}
	if (current instanceof SIRPipeline){
	    SIRPipeline pipeline = (SIRPipeline) current;
	    
	    for (int i=0; i<pipeline.size(); i++) {
		createGraph(pipeline.get(i));
	    }
	}
	if (current instanceof SIRSplitJoin) {
	    SIRSplitJoin sj = (SIRSplitJoin) current;
	    FlatNode splitterNode = addFlatNode (sj.getSplitter());
	    if (top == null) {
		currentNode = splitterNode;
		top = splitterNode;
	    }
	    
	    FlatNode joinerNode = addFlatNode (sj.getJoiner());
	    	    
	    currentNode.addEdges(splitterNode);
	    for (int i = 0; i < sj.size(); i++) {
		currentNode = splitterNode;
		createGraph(sj.get(i));
		currentNode.addEdges(joinerNode);
	    }
	    currentNode = joinerNode;
	    
	}
	if (current instanceof SIRFeedbackLoop) {
	    SIRFeedbackLoop loop = (SIRFeedbackLoop)current;
	    FlatNode joinerNode = addFlatNode (loop.getJoiner());
	    if (top == null) {
		//currentNode = joinerNode;
		top = joinerNode;
		}
	    FlatNode splitterNode = addFlatNode (loop.getSplitter());

	    FlatNode.addEdges(currentNode, joinerNode);
	    	    
	    currentNode = joinerNode;
	    createGraph(loop.getBody());
	    FlatNode.addEdges(currentNode, splitterNode);
	    
	    currentNode = splitterNode;
	    createGraph(loop.getLoop());
	    FlatNode.addEdges(currentNode, joinerNode);
	    currentNode = splitterNode;
	}
    }

    /**
     * Adds a flat node for the given SIROperator, and return it.
     */
    private FlatNode addFlatNode(SIROperator op) {
	FlatNode node = new FlatNode(op);
	SIRMap.put(op, node);
	return node;
    }

    public FlatNode getFlatNode(SIROperator key) {
	FlatNode node = (FlatNode)SIRMap.get(key);
	if (node == null)
	    Utils.fail("Cannot Find FlatNode for SIROperator: " + key);
	return node;
    }

    /* creates the dot file representing the flattened graph */
    public void dumpGraph(String filename) 
    {
	buf = new StringBuffer();
	
	buf.append("digraph Flattend {\n");
	buf.append("size = \"8, 10.5\";");
	top.accept(this, new HashSet(), true);
	buf.append("}\n");
	try {
	    FileWriter fw = new FileWriter(filename);
	    fw.write(buf.toString());
	    fw.close();
	}
	catch (Exception e) {
	    System.err.println("Could not print flattened graph");
	}
	
    }
    
    /* appends the dot file code representing the given node */
    public void visitNode(FlatNode node) 
    {
	if (node.contents instanceof SIRFilter) {
	    SIRFilter filter = (SIRFilter)node.contents;
	    Utils.assert(buf!=null);
	    buf.append(Namer.getName(node.contents) + "[ label = \"" +
		       Namer.getName(node.contents) + 
		       " peek: " + filter.getPeekInt() + 
		       " pop: " + filter.getPopInt() + 
		       " push: " + filter.getPushInt() +
		       "\"];");
	}
	
	if (node.contents instanceof SIRJoiner) {
	    for (int i = 0; i < node.inputs; i++) {
		buf.append(Namer.getName(node.incoming[i].contents) + " -> " 
			   + Namer.getName(node.contents));
		buf.append("[label=\"" + node.incomingWeights[i] + "\"];\n");
	    }
      
	}
	for (int i = 0; i < node.ways; i++) {
	    if (node.edges[i].contents instanceof SIRJoiner)
		continue;
	    buf.append(Namer.getName(node.contents) + " -> " 
		       + Namer.getName(node.edges[i].contents));
	    buf.append("[label=\"" + node.weights[i] + "\"];\n");
	}
    }
}


