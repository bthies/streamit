package at.dms.kjc.slicegraph;


/**
 * 
 */
abstract public class SliceNode {
    private SliceNode next;

    private SliceNode previous;

    private Slice parent;

    public SliceNode getNext() {
        return next;
    }

    public SliceNode getPrevious() {
        return previous;
    }

    public void setPrevious(SliceNode prev) {
        previous = prev;
    }

    public void setNext(SliceNode next) {
        this.next = next;
    }

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
    
    public void setParent(Slice par) {
        parent = par;
    }

    public Slice getParent() {
        assert parent != null : "parent not set for slice node";
        return parent;
    }
}
