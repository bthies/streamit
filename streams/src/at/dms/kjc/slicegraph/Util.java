package at.dms.kjc.slicegraph;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;


/**
 *  A class with useful functions that span classes. 
 * 
 * 
**/
public class Util {

    /**
     * Get a traversal (linked list iterator) that includes all the slice nodes of the
     * given slice traversal.  Inserting or removing in the returned iterator will not
     * affect the list passed to sliceNodeTraversal.  Altering the individual SliceNode's
     * will alter the SliceNode's in the original list.
     * 
     * @param slices a list of Slice's.
     * @return An Iterator over SliceNode's.
     */
    public static Iterator<SliceNode> sliceNodeTraversal(List<Slice> slices) {
        LinkedList<SliceNode> trav = new LinkedList<SliceNode>();
        ListIterator it = slices.listIterator();
    
        while (it.hasNext()) {
            Slice slice = (Slice) it.next();
            SliceNode sliceNode = slice.getHead();
            while (sliceNode != null) {
                trav.add(sliceNode);
                sliceNode = sliceNode.getNext();
            }
    
        }
    
        return trav.listIterator();
    }

    /**
     * Get a traversal (linked list) that includes the head nodes of the
     * given array of slices.
     * 
     * @param slices an array of Slice's.
     * @return An Iterator over SliceNodes.
     */
    public static Iterator<SliceNode> sliceNodeTraversal(Slice[] slices) {
        LinkedList<SliceNode> trav = new LinkedList<SliceNode>();
    
        for (int i = 0; i < slices.length; i++) {
            SliceNode sliceNode = slices[i].getHead();
            while (sliceNode != null) {
                trav.add(sliceNode);
                sliceNode = sliceNode.getNext();
            }
    
        }
    
        return trav.listIterator();
    }

    /**
     * Given two slice nodes, return an edge.
     * Returns an existing edge from src to dst if one is found, else creates a new edge.
     * Only looks at src's edges to find one to return, not at dst's edges.
     * If the edge should be an InterSliceEdge, then the returned edge will be one.
     * Does not update the SliceNode's in any way.
     * @param src  Source SliceNode for edge
     * @param dst  Destination SliceNode for Edge
     * @return an InterSliceEdge or Edge from src to dst
     */
    public static Edge srcDstToEdge(SliceNode src, SliceNode dst) {
        if (src instanceof OutputSliceNode && dst instanceof InputSliceNode) {
            InterSliceEdge[][] edgesedges = ((OutputSliceNode)src).getDests();
            for (InterSliceEdge[] edges : edgesedges) {
                for (InterSliceEdge edge : edges) {
                    assert edge.src == src;
                    if (edge.dest == dst) {
                        return edge;
                    }
                }
            }
            return new InterSliceEdge((OutputSliceNode)src,(InputSliceNode)dst);
        } else {
            Edge e = src.getEdgeToNext();
            if (e == null || e.getDest() != dst) {
                e = new Edge(src,dst);
            }
            return e;
        }
    }
}
