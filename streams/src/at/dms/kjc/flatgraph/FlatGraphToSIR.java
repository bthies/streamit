package at.dms.kjc.flatgraph;

import at.dms.kjc.*;
import at.dms.kjc.raw.*;
import at.dms.kjc.spacedynamic.*;
import at.dms.kjc.sir.*;
import at.dms.kjc.sir.lowering.*;
import at.dms.util.Utils;
import java.io.*;
import java.util.*;


/**
 * Convert the FlatGraph back to an SIR representation
 */

public class FlatGraphToSIR extends at.dms.util.Utils
{
    private FlatNode toplevel;
    private SIRPipeline toplevelSIR;
    private int id;
    private static int globalID;

    public FlatGraphToSIR(FlatNode top) 
    {
	toplevel = top;
	id = 0;
	toplevelSIR = new SIRPipeline("TopLevel" + globalID++);
	reSIR(toplevelSIR, toplevel, new HashSet());
    }

    public SIRPipeline getTopLevelSIR() 
    {
	assert toplevelSIR != null;
	return toplevelSIR;
    }
    //this does not work for feedbackloops!!!
    private void reSIR(SIRContainer parent, FlatNode current, HashSet visited) 
    {
	if (current.isFilter()) {
	    SIRPipeline pipeline;

	    if (!(parent instanceof SIRPipeline)) {
		pipeline = new SIRPipeline(parent, "Pipeline" + id++);
		pipeline.setInit(SIRStream.makeEmptyInit());
		parent.add(pipeline);
	    }
	    else 
		pipeline = (SIRPipeline)parent;
	    
	    //keep adding filters to this pipeline
	    while (current.isFilter()) {
		//make sure we have not added this filter before
		assert !(visited.contains(current)) : "FlatGraph -> SIR does not support Feedbackloops";
		//record that we have added this filter
		visited.add(current);
		pipeline.add((SIRFilter)current.contents, ((SIRFilter)current.contents).getParams());
		((SIRFilter)current.contents).setParent(parent);
		//nothing more to do here so just return
		if (current.ways == 0)
		    return;
		//assert that there is exactly one outgoing edge per filter if there is none
		assert current.ways == 1 && current.edges.length == 1;
		current = current.edges[0];
	    }

	}
	
	if (current.isSplitter()) {
	    SIRSplitJoin splitJoin = new SIRSplitJoin(parent, "SplitJoin" + id++);
	    splitJoin.setInit(SIRStream.makeEmptyInit());
	    //add this splitjoin to the parent!
	    parent.add(splitJoin);
	    //make sure we have not seen this splitter
	    assert !(visited.contains(current)) : "FlatGraph -> SIR does not support Feedbackloops";
	    //record that we have added this splitter
	    visited.add(current);
	    splitJoin.setSplitter((SIRSplitter)current.contents);
	    //create a dummy joiner for this split join just in case it does not have one
	    SIRJoiner joiner = SIRJoiner.create(splitJoin, SIRJoinType.NULL, 
						((SIRSplitter)current.contents).getWays());
	    splitJoin.setJoiner(joiner);

	    for (int i = 0; i < current.edges.length; i++) {
		if (current.edges[i] != null)
		    reSIR(splitJoin, current.edges[i], visited);
	    }
	    return;
	}
	
	if (current.isJoiner()){
	    assert parent instanceof SIRSplitJoin;
	    SIRSplitJoin splitJoin = (SIRSplitJoin)parent;

	    if (!visited.contains(current)) {
		//make sure we have not seen this Joiner
		assert !(visited.contains(current)) : "FlatGraph -> SIR does not support Feedbackloops";
		//record that we have added this Joiner
		visited.add(current);
		//first time we are seeing this joiner so set it as the joiner
		//and visit the remainder of the graph
		splitJoin.setJoiner((SIRJoiner)current.contents);
		if (current.ways > 0) {
		    assert current.edges.length == 1 && current.ways == 1 && current.edges[0] != null;
		    reSIR(splitJoin.getParent(), current.edges[0], visited);
		}
	    }
	}    
    }
}

