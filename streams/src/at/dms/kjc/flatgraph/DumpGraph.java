package at.dms.kjc.flatgraph;

import at.dms.kjc.*;
import at.dms.kjc.raw.*;
import at.dms.kjc.sir.*;
import at.dms.kjc.sir.lowering.*;
import at.dms.util.Utils;
import java.io.*;
import java.util.*;

/**
 * Dump the flat graph to a dot file.
 */

public class DumpGraph implements FlatVisitor
{
    private StringBuffer buf;
    private HashMap initMults,
	steadyMults;
    
    /** 
     * creates the dot file representing the flattened graph 
     * must be called after createExecutionCounts in 
     */
    public void dumpGraph(FlatNode toplevel, String filename, HashMap initExeCounts,
			  HashMap steadyExeCounts) 
    {
	buf = new StringBuffer();
	this.initMults = initExeCounts;
	this.steadyMults = steadyExeCounts;	
	buf.append("digraph Flattend {\n");
	buf.append("size = \"8, 10.5\";");
	toplevel.accept(this, null, true);
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
	    assert buf!=null;
	    
	    buf.append(node.getName() + "[ label = \"" +
		       node.getName() + "\\n");
	    buf.append("init Mult: " + GraphFlattener.getMult(node, true,
							      initMults, steadyMults) + 
		       " steady Mult: " + GraphFlattener.getMult(node, false, initMults, steadyMults));
	    buf.append("\\n");
	    buf.append(" peek: " + filter.getPeek() + 
		       " pop: " + filter.getPop() + 
		       " push: " + filter.getPush());
	    buf.append("\\n");
	    if (node.contents instanceof SIRTwoStageFilter) {
		SIRTwoStageFilter two = (SIRTwoStageFilter)node.contents;
		buf.append(" initPeek: " + two.getInitPeek() + 
			   " initPop: " + two.getInitPop() + 
			   " initPush: " + two.getInitPush());
		buf.append("\\n");
	    }
	    if (node.inputs != node.incoming.length) {
		buf.append("node.inputs (" + node.inputs + ") != node.incoming.length (" + 
			   node.incoming.length + ")");
		buf.append("\\n");
	    }
	    if (node.ways != node.edges.length) {
		buf.append("node.ways (" + node.ways + ") != node.edges.length (" + 
			   node.edges.length + ")");
	    }
		
	    buf.append("\"];");
	}
	    
	if (node.contents instanceof SIRJoiner) {
	    for (int i = 0; i < node.inputs; i++) {
		//joiners may have null upstream neighbors
		if (node.incoming[i] == null)
		    continue;
		buf.append(node.incoming[i].getName() + " -> " 
			   + node.getName());
		buf.append("[label=\"" + node.incomingWeights[i] + "\"];\n");
	    }
		
	}
	for (int i = 0; i < node.ways; i++) {
	    if (node.edges[i] == null)
		continue;
	    if (node.edges[i].contents instanceof SIRJoiner)
		continue;
	    buf.append(node.getName() + " -> " 
		       + node.edges[i].getName());
	    buf.append("[label=\"" + node.weights[i] + "\"];\n");
	}
    }
}

