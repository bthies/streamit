package at.dms.kjc.slicegraph;

import at.dms.util.Utils;
import java.util.ArrayList;

/** 
 * Slice class models a slice (joiner, sequence of filters, splitter).
 * Beware: slices are linked with Edges, but the back edge of an InputSliceNode and
 * the forward edge of an OutputSliceNode should be null: they are not related to the
 * InterSliceEdge's of the InputSliceNode and OutputSliceNode.
 *  
 * @author mgordon
 */
public class Slice implements at.dms.kjc.DeepCloneable {
        
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
     * @{link {@link #finish() finish} } will tack on an OutputSliceNode if missing.
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
     * After a slice has been cloned, set up the fields of the slicenodes included 
     * in it.
     */
    public void finishClone() {
        
        //set the head refs
        head.setParent(this);
        head.setNext(filterNodes[0]);
        
        //set the filternodes' prev, head, and parent
        for (int i = 0; i < filterNodes.length; i++) {
            filterNodes[i].setParent(this);
            if (i == 0)         
                filterNodes[i].setPrevious(head);
            else
                filterNodes[i].setPrevious(filterNodes[i-1]);
            
            if (i == filterNodes.length - 1) 
                filterNodes[i].setNext(tail);
            else
                filterNodes[i].setNext(filterNodes[i+1]);
        }
        
        //set the tail's structures
        tail.setParent(this);
        tail.setPrevious(filterNodes[filterNodes.length -1]);
    }
    
    /**
     * Finishes creating Slice.
     * Expects the slice to have an InputSliceNode, and 1 or more FilterSliceNodes. 
     * Creates an OutputSliceNode if necessary.
     * 
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
        //remember that setting a node's next will also set the next node's previous edge
        //so no need to set the previous edges explicitly 
        getHead().setNext(node);
        while (node.isFilterSlice()) {
            filterNodes[i++] = node.getAsFilter();
            node.setNext(node.getNext());
            node = node.getNext();
        }
        assert i == size;
        return size;
    }

    /**
     * @return The incoming Slices (Slices) in the partitioned stream graph for this slice (slice). 
     */
    public Slice[] getDependencies(SchedulingPhase phase) {
        Slice[] depends = new Slice[head.getSources(phase).length];
        
        for (int i = 0; i < depends.length; i++)
            depends[i] = head.getSources(phase)[i].getSrc().getParent();
        
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

    /**
     * Get the first FilterSliceNode of this slice.
     *   
     * @return The first FilterSliceNode of this Slice.
     */
    public FilterSliceNode getFirstFilter() {
        return head.getNextFilter();
    }
    
    /**
     * get the InputSliceNode of the Slice containing this node.
     * @return
     */
    public InputSliceNode getHead() {
        return head;
    }

    /**
     * get the OutputSliceNode of the Slice containing this node.
     * @return
     */
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
        assert ret == filterNodes.length;
        return ret;
    }


    /** 
     * @return list the filter slice nodes, in data flow order, unmodifiable.
     */
    public java.util.List<FilterSliceNode> getFilterNodes() {
        return java.util.Collections.unmodifiableList(java.util.Arrays.asList(filterNodes));
    }

    /** THE FOLLOWING SECTION IS AUTO-GENERATED CLONING CODE - DO NOT MODIFY! */

    /** Returns a deep clone of this object. */
    public Object deepClone() {
        at.dms.kjc.slicegraph.Slice other = new at.dms.kjc.slicegraph.Slice();
        at.dms.kjc.AutoCloner.register(this, other);
        deepCloneInto(other);
        return other;
    }

    /** Clones all fields of this into <pre>other</pre> */
    protected void deepCloneInto(at.dms.kjc.slicegraph.Slice other) {
        other.head = (at.dms.kjc.slicegraph.InputSliceNode)at.dms.kjc.AutoCloner.cloneToplevel(this.head);
        other.tail = (at.dms.kjc.slicegraph.OutputSliceNode)at.dms.kjc.AutoCloner.cloneToplevel(this.tail);
        other.len = this.len;
        other.filterNodes = (at.dms.kjc.slicegraph.FilterSliceNode[])at.dms.kjc.AutoCloner.cloneToplevel(this.filterNodes);
        //System.out.println(other.filterNodes[0].hashCode() + " " + filterNodes[0].hashCode());
    }

    /** THE PRECEDING SECTION IS AUTO-GENERATED CLONING CODE - DO NOT MODIFY! */
}
