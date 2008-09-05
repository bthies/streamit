/**
 * 
 */
package at.dms.kjc.slicegraph;

/**
 * A SimpleSlice is a Slice with exactly one {@link FilterSliceNode}.
 * 
 * @author dimock
 *
 */
public class SimpleSlice extends Slice implements at.dms.kjc.DeepCloneable {

    protected FilterSliceNode body;
    
    /** Constructor: creates a slice with one filter and sets
     * previous parent and next links the supplied
     * InputSliceNode, FilterSliceNode, and OutputSliceNode.
     * <br/>
     * One of head, body, tail must be non-null.
     * If body is null, then a FilterSliceNode for the body
     * must be reachable from the head or the tail.
     * If head is null and no InputSliceNode is connected to body,
     * then a default InputSliceNode is generated.
     * If tail is null and no OutputSliceNode is connected to body,
     * then a default OutputSliceNode is created.
     * @param head InputSliceNode at head of slice
     * @param body FilterSliceNode in simple slice.
     * @param tail OutputSliceNode at tail of slice.
     */
    public SimpleSlice(InputSliceNode head, 
       FilterSliceNode body, OutputSliceNode tail) {
            if (body == null && head != null) {
                body = (FilterSliceNode)head.getNext();
            }
            if (tail == null && body != null) {
                tail = (OutputSliceNode)body.getNext();
            }
            if (body == null && tail != null) {
                body = (FilterSliceNode)tail.getPrevious();
            }
            if (head == null && body != null) {
                head = (InputSliceNode)body.getPrevious();
            }
            if (head == null) {
                head = new InputSliceNode();
            }
            if (tail == null) {
                tail = new OutputSliceNode();
            }
            assert body != null : "SimpleSlice must be created with a non-null body.";
            
            this.head = head;
            this.body = body; 
            this.tail = tail;
            head.setParent(this);
            body.setParent(this);
            tail.setParent(this);
            len = 1;
            head.setNext(body);
            body.setPrevious(head);
            body.setNext(tail);
            tail.setPrevious(body);
    }
    
    protected SimpleSlice(){};
    
    /**
     * @param head
     */
    public SimpleSlice(InputSliceNode head) {
        this(head,null,null);
    }

    /**
     * @param node
     */
    public SimpleSlice(SliceNode node) {
        this(null,(FilterSliceNode)node,null);
    }

    /**
     * Not needed for SimpleSlice, kept as a sanity check.
     */
    public int finish() {
        super.finish();
        
        assert head.getNext() == body : head.getNext() + " " + body.getPrevious();
        assert tail.getPrevious() == body;
        assert head.getParent() == this; 
        assert body.getParent() == this;
        assert tail.getParent() == this;
        return 1;
    }
    
    /**
     * Should not need to be called for SimpleSlice: always 1.
     */
    public int getNumFilters() {
        return 1;
    }
    
    
    /**
     * For SimpleSlice: call {@link #getBody()} instead.
     * For compatability with Slice, returns a one-element list of
     * FilterSliceNode.
      * @return (singleton) list the filter slice nodes, in data flow order, unmodifiable.
    */
    public java.util.List<FilterSliceNode> getFilterNodes() {
        return java.util.Collections.singletonList(body);
    }

    /**
     * Preferred way to access body of a SimpleSlice.
     * @return the FilterSliceNode
     */
    public FilterSliceNode getBody() {
        return body;
    }
    
    /**
     * Set the body.
     * Updates parent pointer in the body, but not the previous
     * or next pointers.
     * @param body a FilterSliceNode.
     */
    public void setBody(FilterSliceNode body) {
        this.body = body; 
        body.setParent(this);
    }
    
    /** THE FOLLOWING SECTION IS AUTO-GENERATED CLONING CODE - DO NOT MODIFY! */    
    
    /** Returns a deep clone of this object. */
    public Object deepClone() {
        at.dms.kjc.slicegraph.SimpleSlice other = new at.dms.kjc.slicegraph.SimpleSlice();
        at.dms.kjc.AutoCloner.register(this, other);
        deepCloneInto(other);
        return other;
    }

    /** Clones all fields of this into <pre>other</pre> */
    protected void deepCloneInto(at.dms.kjc.slicegraph.SimpleSlice other) {
        super.deepCloneInto(other);
        other.body = (at.dms.kjc.slicegraph.FilterSliceNode)at.dms.kjc.AutoCloner.cloneToplevel(this.body);
        System.out.println(other.body.hashCode() + " " + body.hashCode());
    }

    /** THE PRECEDING SECTION IS AUTO-GENERATED CLONING CODE - DO NOT MODIFY! */
}
