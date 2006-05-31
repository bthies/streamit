package at.dms.kjc.spacedynamic;

import at.dms.kjc.flatgraph.FlatNode;
import at.dms.kjc.flatgraph.FlatVisitor;
import java.util.HashMap;
import at.dms.util.Utils;
import at.dms.kjc.sir.lowering.partition.WorkEstimate;
import at.dms.kjc.sir.*;

public class WorkEstimatesMap implements FlatVisitor 
{
    private HashMap estimates;
    private StreamGraph streamGraph;

    public WorkEstimatesMap(StreamGraph sg) 
    {
        streamGraph = sg;
        estimates = new HashMap();
    }
    
    public void addEstimate(FlatNode node) 
    {
        if (node.isFilter())
            estimates.put(node,
                          new Integer
                          (WorkEstimate.getWorkEstimate((SIRFilter)node.contents).
                           getWork((SIRFilter)node.contents))
                          );
        else if (node.isJoiner()) {
            //just mult by 3 to account for d-cache access
            estimates.put(node, 
                          new Integer(node.getTotalIncomingWeights() * 3));
            //estimates.put(node, new Integer(1));    
        }   
    }
    

    public WorkEstimatesMap(StreamGraph sg, FlatNode top) 
    {
        sg = streamGraph;
        estimates = new HashMap();
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
        return ((Integer)estimates.get(node)).intValue();
    }

    /** return the work estimate multiplied by the number of times the 
        node executes in the steady state **/
    public int getSteadyEstimate(FlatNode node) 
    {
        if (!estimates.containsKey(node))
            Utils.fail("Node " + node.contents.getName() + " not in map.");
        return ((Integer)estimates.get(node)).intValue() * 
        ((StaticStreamGraph)streamGraph.getParentSSG(node)).getMult(node, false);
    }
    
}

