package at.dms.kjc.slicegraph;

import at.dms.util.Utils;
import java.util.ArrayList;

/** 
 * Slice class models a slice (slice).
 *  
 * @author mgordon
 */
public class Slice {
    //The head of the slice.
    protected InputSliceNode head;
    //the Tail of the slice.
    protected OutputSliceNode tail;
    //The length of the slice.
    protected int len;
    protected FilterSliceNode[] filterNodes;
    
    /*
     * public Slice (Slice[] edges, Slice[] depends, InputSliceNode head) { if
     * (edges == null) this.edges = new Slice[0]; else this.edges = edges;
     * 
     * this.head = head; head.setParent(this);
     * 
     * if (depends == null) this.depends = new Slice[0]; else this.depends =
     * depends; len=-1; }
     */

    /**
     * Create slice with an InputSliceNode.
     * "head" is expected to be linked to a FilterSliceNode by the time finish is called.
     * @{link {@link #finish() finish} will tack on an OutputSliceNode if missing.
     * @param head  the InputSliceNode
     */
    public Slice(InputSliceNode head) {
        this.head = head;
        head.setParent(this);
        len = -1;
    }

    /**
     * Create slice with a FilterSliceNode.
     * Creates an InputSliceNode automatically and links it with the FilterSliceNode.
     * @param node
     */
    public Slice(SliceNode node) {
        if (!(node instanceof FilterSliceNode))
            Utils.fail("FilterSliceNode expected: " + node);
        head = new InputSliceNode();
        head.setParent(this);
        head.setNext(node);
        node.setPrevious(head);
        len = -1;
    }

    protected Slice() {
    }
    
    /**
     * Finishes creating Slice.
     * Expects the slice to have an InputSliceNode, and 1 or more FilterliceNodes. 
     * Creates an OutputSliceNode if necessary.
     * @return The number of FilterSliceNodes.
     */
    public int finish() {
        int size = 0;
        SliceNode node = head.getNext();
        SliceNode end = node;
        while (node != null && node instanceof FilterSliceNode) {
            node.setParent(this);
            size++;
            end = node;
            node = node.getNext();
        }
        if (node != null)
            end = node;
        len = size;
        if (end instanceof OutputSliceNode)
            tail = (OutputSliceNode) end;
        else {
            tail = new OutputSliceNode();
            end.setNext(tail);
            tail.setPrevious(end);
        }
        tail.setParent(this);
        //set the filterNodes array
        filterNodes = new FilterSliceNode[size];
        int i = 0;
        node = getHead().getNext();
        while (node.isFilterSlice()) {
            filterNodes[i++] = node.getAsFilter();
            node = node.getNext();
        }
        assert i == size;
        return size;
    }

    /**
     * @return The incoming Slices (Slices) in the partitioned stream graph for this slice (slice). 
     */
    public Slice[] getDependencies() {
        Slice[] depends = new Slice[head.getSources().length];
        
        for (int i = 0; i < depends.length; i++)
            depends[i] = head.getSources()[i].getSrc().getParent();
        
        return depends;
    }
    
    // finish() must have been called
    public int size() {
        assert len > -1 : "finish() was not called";
        return len;
    }

    /**
     * Set the tail of this slice to out.  This method
     * does not fix the intra-slice connections of the slice nodes, but 
     * it does set the parent of the new output slice.
     * 
     * @param out The new output slice node.
     */
    public void setTail(OutputSliceNode out) {
        tail = out;
        out.setParent(this);
    }
    
    /**
     * Set the head of this slice to node.  This method
     * does not fix the intra-slice connections of the slice nodes, but 
     * it does set the parent of the new input slice node.
     * 
     * @param node The new input slice node.
     */
    public void setHead(InputSliceNode node) {
        head = node;
        node.setParent(this);
    }

    public InputSliceNode getHead() {
        return head;
    }

    // finish() must have been called
    public OutputSliceNode getTail() {
        return tail;
    }

    /**
     * Return a brief string description of this slice.
     * 
     * @return a brief string description of this slice.
     */
    public String getIdent() {
        return head.toString() + tail.toString();
    }
    
    public String toString() {
        return "Slice: " + head + "->" + head.getNext() + "->...";
    }


    // return the number of filters in the slice
    public int getNumFilters() {
        SliceNode node = getHead().getNext();
        int ret = 0;
        while (node instanceof FilterSliceNode) {
            node = node.getNext();
            ret++;
        }
        return ret;
    }

    /** 
     * @return The array of just the filter slice nodes, in data flow order.
     */
    public FilterSliceNode[] getFilterNodes() {
        return filterNodes;
    }
}
