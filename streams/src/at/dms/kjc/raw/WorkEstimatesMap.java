package at.dms.kjc.raw;

import at.dms.kjc.flatgraph.FlatNode;
import at.dms.kjc.flatgraph.FlatVisitor;
import java.util.HashMap;
import at.dms.util.Utils;
import at.dms.kjc.sir.lowering.partition.WorkEstimate;
import at.dms.kjc.sir.*;

public class WorkEstimatesMap implements FlatVisitor 
{
    HashMap estimates;

    public WorkEstimatesMap (FlatNode top) 
    {
	estimates = new HashMap();
	top.accept(this, null, true);
    }
    
    public void visitNode(FlatNode node) 
    {
	
	if (node.isFilter())
	    estimates.put(node,
			  new Integer
			  (WorkEstimate.getWorkEstimate((SIRFilter)node.contents).
			  getWork((SIRFilter)node.contents)));
	else if (node.isJoiner())
	    estimates.put(node, new Integer(1));
    }

    public int getEstimate(FlatNode node) 
    {
	if (!estimates.containsKey(node))
	    Utils.fail("Node " + node.contents.getName() + " not in map.");
	return ((Integer)estimates.get(node)).intValue();
    }
    
}

