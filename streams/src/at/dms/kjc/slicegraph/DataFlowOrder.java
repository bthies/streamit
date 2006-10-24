package at.dms.kjc.slicegraph;

import java.util.LinkedList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Vector;

import at.dms.kjc.spacetime.Trace;
import at.dms.util.Utils;

/**
 * This class generates a data flow schedule of the trace graph.
 * More specifically, in the traversal, all ancestors of a node are 
 * guaranteed to appear before the node.
 * 
 * @author mgordon 
 */
public class DataFlowOrder {
    
    /**
     * Generate a list of traces in data-flow order (don't add I/O traces to the traversal).
     * 
     * @param topTraces The trace forrest.
     * 
     * @return A LinkedList of traces in data-flow order
     */
    public static LinkedList<Trace> getTraversal(Trace[] topTraces) {
        LinkedList<Trace> schedule = new LinkedList<Trace>();
        HashSet<Trace> visited = new HashSet<Trace>();
        LinkedList<Trace> queue = new LinkedList<Trace>();
        for (int i = 0; i < topTraces.length; i++) {
            queue.add(topTraces[i]);
            while (!queue.isEmpty()) {
                Trace trace = queue.removeFirst();
                if (!visited.contains(trace)) {
                    visited.add(trace);
                    Iterator dests = trace.getTail().getDestSet().iterator();
                    while (dests.hasNext()) {
                        Trace current = ((Edge) dests.next()).getDest()
                            .getParent();
                        if (!visited.contains(current)) {
                            // only add if all sources has been visited
                            Iterator sources = current.getHead().getSourceSet()
                                .iterator();
                            boolean addMe = true;
                            while (sources.hasNext()) {
                                if (!visited.contains(((Edge) sources.next())
                                                      .getSrc().getParent())) {
                                    addMe = false;
                                    break;
                                }
                            }
                            if (addMe)
                                queue.add(current);
                        }
                    }
                    //if (!trace.getHead().getNextFilter().isPredefined()) {
                    schedule.add(trace);
                    //}
                }
            }
        }

        return schedule;
    }
}
