package at.dms.kjc.raw;

import at.dms.kjc.flatgraph.FlatNode;
import at.dms.kjc.flatgraph.FlatVisitor;
import at.dms.kjc.*;
import at.dms.kjc.sir.*;
import at.dms.kjc.sir.lowering.*;
import at.dms.util.Utils;
import java.util.HashSet; 
import java.util.HashMap;

public class BlockExecutionCounts implements FlatVisitor 
{
    private static HashMap blockCounts;

    public static int getBlockCount(FlatNode node) 
    {
	if (blockCounts == null)
	    Utils.fail("Block Execution Count not calculated");
	return ((Integer)blockCounts.get(node)).intValue();
    }

    public static void calcBlockCounts(FlatNode top) 
    {
	BlockExecutionCounts bec = new BlockExecutionCounts();
	top.accept(bec, null, true);
    }
    
    public BlockExecutionCounts () 
    {
	blockCounts = new HashMap();
    }
    
    public void visitNode(FlatNode node) 
    {
	if (node.contents instanceof SIRJoiner ||
	    node.contents instanceof SIRSplitter) {
	    blockCounts.put(node, new Integer(1));
	    return;
	} 
	if (((SIRStream)node.contents).insideFeedbackLoop()) {
	    blockCounts.put(node, new Integer(1));
	    return;
	}
	
	SIRFilter filter = (SIRFilter)node.contents;

	//block count of sources are 1
	
	//must be a multiple!!!!!!
	if (filter.getPopInt() == 0) {
	    blockCounts.put(node, new Integer(1));
	}
	else {
	    //blockCounts.put(node, new Integer(1));
	    blockCounts.put(node, 
			    new Integer(((Integer)RawBackend.
					 steadyExecutionCounts.get(node)).intValue()));
	}
	return;
    }
}

