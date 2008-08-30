package at.dms.kjc.slicegraph;


/**
 * SliceNode's are a doubly-linked list with a parent pointer to a Slice.
 * They can be specialized into {@link InputSliceNode}, {@link FilterSliceNode}, or {@link OutputSliceNode}. 
 */
public class SliceNode {
    public static final String[] DO_NOT_CLONE_THESE_FIELDS = { "toNext", "toPrev" };

    private IntraSliceEdge toNext = null;  // internal to slice: remains null for OutputSliceNode
    private IntraSliceEdge toPrev = null;  // internal to slice: remains null for InputSliceNode

    private Slice parent;

    public SliceNode getNext() {
        return (toNext == null)? null : toNext.getDest();
    }

    public SliceNode getPrevious() {
        return (toPrev == null)? null : toPrev.getSrc();
    }

    public IntraSliceEdge getEdgeToNext() {
        return toNext;
    }
    
    public IntraSliceEdge getEdgeToPrev() {
        return toPrev;
    }

    private void setNextEdge(IntraSliceEdge edge) {
        toNext = edge;
    }
    

    private void setPrevEdge(IntraSliceEdge edge) {
        toPrev = edge;
    }
    
    /**
     * Set the IntraSliceEdge pointing to previous to prev, by creating a new edge.  
     * Also, set prev's next edge to the newly created edge. 
     * 
     * @param prev The new previous node
     */
    public void setPrevious(SliceNode prev) {
        assert ! (this instanceof InputSliceNode); 
        toPrev = new IntraSliceEdge(prev,this);
        prev.setNextEdge(toPrev);
    }

    /**
     * Set the intraslicenedge pointing to the next node to next by creating 
     * a new edge.  Also, set next's edge to the newly created edge.
     * 
     * @param next The new next node
     */
    public void setNext(SliceNode next) {
        assert ! (this instanceof OutputSliceNode);
        toNext = new IntraSliceEdge(this, next);
        next.setPrevEdge(toNext);
    }

//    public Edge getEdgeToPrev() {
//        return toPrev;
//    }
    
    public boolean isInputSlice() {
        return this instanceof InputSliceNode;
    }

    public boolean isFilterSlice() {
        return this instanceof FilterSliceNode;
    }

    public boolean isOutputSlice() {
        return this instanceof OutputSliceNode;
    }

    public InputSliceNode getAsInput() {
        return (InputSliceNode)this;
    }
    
    public OutputSliceNode getAsOutput() {
        return (OutputSliceNode)this;
    }
    
    public FilterSliceNode getAsFilter() {
        return (FilterSliceNode) this;
    }
    
    /**
     * Had been some assertion checking: removed.
     * Now does nothing.
     *
     */
    protected SliceNode() {
    }
    
    public void setParent(Slice par) {
        parent = par;
    }

    public Slice getParent() {
        assert parent != null : "parent not set for slice node";
        return parent;
    }
}
