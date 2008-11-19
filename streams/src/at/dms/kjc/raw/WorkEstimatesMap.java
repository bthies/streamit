package at.dms.kjc.raw;

import at.dms.kjc.flatgraph.FlatNode;
import at.dms.kjc.flatgraph.FlatVisitor;
import java.util.HashMap;
import at.dms.util.Utils;
import at.dms.kjc.sir.lowering.partition.WorkEstimate;
import at.dms.kjc.sir.*;

public class WorkEstimatesMap implements FlatVisitor 
{
    HashMap<FlatNode, Long> estimates;

    public WorkEstimatesMap (FlatNode top) 
    {
        estimates = new HashMap<FlatNode, Long>();
        top.accept(this, null, true);
    }
    
    public void visitNode(FlatNode node) 
    {
    
        if (node.isFilter())
            estimates.put(node,
                          new Long
                          (WorkEstimate.getWorkEstimate((SIRFilter)node.contents).
                           getWork((SIRFilter)node.contents)));
        else if (node.isJoiner())
            estimates.put(node, new Long(1));
    }

    public long getEstimate(FlatNode node) 
    {
        if (!estimates.containsKey(node))
            Utils.fail("Node " + node.contents.getName() + " not in map.");
        return estimates.get(node).longValue();
    }
    
}

