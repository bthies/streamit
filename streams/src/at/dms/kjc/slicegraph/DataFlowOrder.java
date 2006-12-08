package at.dms.kjc.slicegraph;

import java.util.LinkedList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Vector;

import at.dms.util.Utils;

/**
 * This class generates a data flow schedule of the slice graph.
 * More specifically, in the traversal, all ancestors of a node are 
 * guaranteed to appear before the node.
 * 
 * @author mgordon 
 */
public class DataFlowOrder {
    
    /**
     * Generate a list of slices in data-flow order (don't add I/O slices to the traversal).
     * 
     * @param topSlices The slice forest.
     * 
     * @return A LinkedList of slices in data-flow order
     */
    public static LinkedList<Slice> getTraversal(Slice[] topSlices) {
        LinkedList<Slice> schedule = new LinkedList<Slice>();
        HashSet<Slice> visited = new HashSet<Slice>();
        LinkedList<Slice> queue = new LinkedList<Slice>();
        for (int i = 0; i < topSlices.length; i++) {
            queue.add(topSlices[i]);
            while (!queue.isEmpty()) {
                Slice slice = queue.removeFirst();
                if (!visited.contains(slice)) {
                    visited.add(slice);
                    Iterator dests = slice.getTail().getDestSet().iterator();
                    while (dests.hasNext()) {
                        Slice current = ((Edge) dests.next()).getDest()
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
                    //if (!slice.getHead().getNextFilter().isPredefined()) {
                    schedule.add(slice);
                    //}
                }
            }
        }

        return schedule;
    }
}
