package at.dms.kjc.raw;

import at.dms.kjc.flatgraph.FlatNode;
import at.dms.kjc.flatgraph.FlatVisitor;
import java.util.HashSet;
import at.dms.kjc.*;
import at.dms.kjc.sir.*;
import at.dms.kjc.sir.lowering.*;

public class JoinerRemoval implements FlatVisitor 
{
    public static HashSet unnecessary;
    
    public static void run(FlatNode top) 
    {
	unnecessary = new HashSet();
	if (!KjcOptions.ratematch)
	    return;

	System.out.println("Looking for Joiners to remove...");
	
	//calculation the block exe for each node
	BlockExecutionCounts.calcBlockCounts(top);
	
	top.accept(new JoinerRemoval(), null, true);
    }

    protected JoinerRemoval() 
    {
    }

    public void visitNode(FlatNode node) 
    {

	
	if (node.contents instanceof SIRJoiner) {
	    //make sure that this joiner is not connected to any other
	    //joiners
	    if (node.edges[0] != null && (node.edges[0].contents instanceof SIRJoiner))
		return;
	    
	    for (int i = 0; i < node.inputs; i++) {
		if (node.incoming[i] == null || 
		    !(node.incoming[i].contents instanceof SIRFilter))
		    return;
	    }
	    
	    //now check if there is no buffering
	    if (KjcOptions.ratematch) {
		if (node.inputs < 1)
		    return;

		for (int i = 0; i < node.inputs; i++) {
		    //make sure it does not execute in the initialization schedule
		    if (RawBackend.initExecutionCounts.get(node.incoming[i]) != null)
			return;
		    SIRFilter filter = (SIRFilter)node.incoming[i].contents;
		    System.out.println(node.incoming[i]+"->"+node);
		    int exe = ((Integer)RawBackend.steadyExecutionCounts.get(node.incoming[i])).intValue();
		    int prod = exe * filter.getPushInt();
		    if (exe != BlockExecutionCounts.getBlockCount(node.incoming[i]))
			return;
		    if (prod != node.incomingWeights[i])
			return;
		}
		//if we get here then all the tests pass
		//add it to the list of joiners to get removed
		System.out.println("Removing Unnessary Joiner " + node.contents.getName());
		unnecessary.add(node);
	    }
	}
	
    }
}
