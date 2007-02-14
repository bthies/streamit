package at.dms.kjc.slicegraph;


/**
 * SliceNode's are a doubly-linked list with a parent pointer to a Slice.
 * They can be specialized into {@link InputSliceNode}, {@link FilterSliceNode}, or {@link OutputSliceNode}. 
 */
public class SliceNode {
    private Edge toNext = null;  // internal to slice: remains null for OutputSliceNode
    private Edge toPrev = null;  // internal to slice: remains null for InputSliceNode

    private Slice parent;

    public SliceNode getNext() {
        return (toNext == null)? null : toNext.getDest();
    }

    public SliceNode getPrevious() {
        return (toPrev == null)? null : toPrev.getSrc();
    }

    public void setPrevious(SliceNode prev) {
        assert ! (this instanceof InputSliceNode);
        if (toPrev == null) { 
            toPrev = new Edge(prev,this); 
        } else {
            toPrev.setSrc(prev);
        }
    }

    public void setNext(SliceNode next) {
        assert ! (this instanceof OutputSliceNode);
        if (toNext == null) {
            toNext = new Edge(this,next);
        } else {
            toNext.setDest(next);
        }
    }

    public Edge getEdgeToNext() {
        return toNext;
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
