package at.dms.kjc.spacetime;

import java.util.List;
import java.util.Iterator;
import java.util.Arrays;
import at.dms.kjc.*;
import at.dms.kjc.sir.*;
import at.dms.kjc.slicegraph.*;

import java.util.HashSet;
import java.util.Vector;

/**
 * This class will insert identity filters to reduce the width of 
 * splitting and joining, so that it

 * It modifies the stream graph in place, adding edges and identities.

 * It must add the new nodes to the init and steady trace list for 
 * other passes.
 **/
public class ReduceSJWidth
{
    private static List<Slice> steady;
    private static List<Slice> init;
    private static int DRAMs;

    public static void run(List<Slice> initList, List<Slice> steadyList, 
                           RawProcElements chip, Slice[] files) 
    {
        //keep the old steady traversal around so we can iterate over it...
        Slice[] oldSteady = steadyList.toArray(new Slice[0]);
        steady = steadyList;
        init = initList;
        DRAMs = chip.getNumDev();

        for (int i = 0; i < oldSteady.length; i++) {
            reduceIncomingEdges(oldSteady[i]);
            reduceOutgoingEdges(oldSteady[i].getTail());
        }
    
    }
    

    private static void reduceIncomingEdges(Slice slice) 
    {
        //check if there is anything to do
        if (slice.getHead().getSourceSet().size() <= DRAMs)
            return;

        //create the new trace to add with an identity
        Slice newTrace = newIdentityTrace(slice.getHead().getType());
        InputSliceNode input = slice.getHead();

        //set the connections
        //choose first DRAMs incoming filters
        HashSet<Edge> coalesce = new HashSet<Edge>();
        for (int i = 0; i < input.getSources().length; i++) {
            //if not already in the set, add it
            if (!coalesce.contains(input.getSources()[i]))
                coalesce.add(input.getSources()[i]);
            //break when we have DRAMs traces in the set
            if (coalesce.size() >= DRAMs)
                break;
        }

        //coalesce them...
        Vector newEdgesOldTrace = new Vector();
        Vector newEdgesNewTrace = new Vector();
        int i = 0;

        while (i < input.getSources().length) {
            int sum = 0;
            //      if (input.getSources[i])

        }
    
        //set the new edge vectors  UNCOMMENT
        //newTrace.getHead().getSources((Edge[])newEdgesNewTrace.toArray(new Edge[0]));
        //input.getSources((Edge[])newEdgesOldTrace.toArray(new Edge[0]));
        
        //set the multiplicities!
    
        //add the trace to the traversals
        addTraceBefore(newTrace, slice);

        //repeat on the new inputTraceNode
        //by recursively calling, 
        reduceIncomingEdges(newTrace);
    }
    
    private static void addTraceBefore(Slice addMe, Slice before) 
    {
        //add to init, this will not add it if before is not in the 
        //traversal
        for (int i = 0; i < init.size(); i++) {
            if (init.get(i) == before) {
                init.add(i, addMe);
                break;
            }
        }
        //add to steady
        assert steady.contains(before) : "Cannot add to traversal";
        for (int i = 0; i < steady.size(); i++) {
            if (steady.get(i) == before) {
                steady.add(i, addMe);
                break;
            }
        }
    }
    

    private static void reduceOutgoingEdges(OutputSliceNode output)
    {
    
    }

    //return a new trace with an identity filter and input/output
    //the state of the trace nodes are not set
    private static Slice newIdentityTrace(CType type) 
    {
        FilterContent filterC = new FilterContent(new SIRIdentity(type));
        FilterSliceNode node = new FilterSliceNode(filterC);
    
        Slice slice = new Slice(node);
        //finish creating the trace? Jasp?
        slice.finish();
    
        return slice;
    }
    
}