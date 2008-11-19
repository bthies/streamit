package at.dms.kjc.spacedynamic;

import at.dms.kjc.flatgraph.FlatNode;
import at.dms.kjc.flatgraph.FlatVisitor;
import java.util.HashMap;
import at.dms.util.Utils;
import at.dms.kjc.sir.lowering.partition.WorkEstimate;
import at.dms.kjc.sir.*;

public class WorkEstimatesMap implements FlatVisitor 
{
    private HashMap<FlatNode, Long> estimates;
    private SpdStreamGraph streamGraph;

    public WorkEstimatesMap(SpdStreamGraph sg) 
    {
        streamGraph = sg;
        estimates = new HashMap<FlatNode, Long>();
    }
    
    public void addEstimate(FlatNode node) 
    {
        if (node.isFilter())
            estimates.put(node,
                          new Long
                          (WorkEstimate.getWorkEstimate((SIRFilter)node.contents).
                           getWork((SIRFilter)node.contents))
                          );
        else if (node.isJoiner()) {
            //just mult by 3 to account for d-cache access
            estimates.put(node, 
                          new Long(node.getTotalIncomingWeights() * 3));
            //estimates.put(node, new Long(1));    
        }   
    }
    

    public WorkEstimatesMap(SpdStreamGraph sg, FlatNode top) 
    {
        sg = streamGraph;
        estimates = new HashMap<FlatNode, Long>();
        top.accept(this, null, true);
    }
    
    public void visitNode(FlatNode node) 
    {
        addEstimate(node);
    }

    public int getEstimate(FlatNode node) 
    {
        if (!estimates.containsKey(node))
            Utils.fail("Node " + node.contents.getName() + " not in map.");
        return estimates.get(node).intValue();
    }

    /** return the work estimate multiplied by the number of times the 
        node executes in the steady state **/
    public int getSteadyEstimate(FlatNode node) 
    {
        if (!estimates.containsKey(node))
            Utils.fail("Node " + node.contents.getName() + " not in map.");
        return estimates.get(node).intValue() * 
        ((SpdStaticStreamGraph)streamGraph.getParentSSG(node)).getMult(node, false);
    }
    
}

