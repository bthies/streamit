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
     * affect the list passed to sliceNodeTraversal.  Altering the individual TreceNode's
     * will alter the SliceNode's in the original list.
     * 
     * @param slices a list of SliceNodes.
     * @return An Iterator of SliceNodes.
     */
    public static Iterator<SliceNode> sliceNodeTraversal(List slices) {
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
     * Get a traversal (linked list) that includes all the trace nodes of the
     * given trace traversal.
     * 
     * @param slices
     * @return A LinkedList of SliceNodes.
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

}
