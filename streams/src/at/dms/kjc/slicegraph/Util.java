package at.dms.kjc.slicegraph;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;

import at.dms.kjc.spacetime.Trace;

/**
 *  A class with useful functions that span classes. 
 * 
 * 
**/
public class Util {

    /**
     * Get a traversal (linked list iterator) that includes all the trace nodes of the
     * given trace traversal.  Inserting or removing in the returned iterator will not
     * affect the list passed to traceNodeTraversal.  Altering the individual TreceNode's
     * will alter the TraceNode's in the original list.
     * 
     * @param traces a list of TraceNodes.
     * @return An Iterator of TraceNodes.
     */
    public static Iterator<TraceNode> traceNodeTraversal(List traces) {
        LinkedList<TraceNode> trav = new LinkedList<TraceNode>();
        ListIterator it = traces.listIterator();
    
        while (it.hasNext()) {
            Trace trace = (Trace) it.next();
            TraceNode traceNode = trace.getHead();
            while (traceNode != null) {
                trav.add(traceNode);
                traceNode = traceNode.getNext();
            }
    
        }
    
        return trav.listIterator();
    }

}
