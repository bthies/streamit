package at.dms.kjc.slicegraph;

import at.dms.kjc.CType;

/**
 * An Edge connects two {@link SliceNode}s.
 * Edges can be differentiated into {@link InterSliceEdge}s that connect the OutputSliceNode of a slice
 * and the InputSliceNode of a slice, and <pre>Intra-Slice Edges</pre> that connect two SliceNodes in
 * the same slice.
 * @author mgordon
 *
 */

public class Edge {

    /**
     * Source of directed edge in Slice graph
     */
    protected SliceNode src;

    /**
     * Destination of directed edge in Slice graph
     */
    protected SliceNode dest;

    /**
     * Caches type for {@link #getType()} calls
     */
    private CType type;

    /**
     * Full constructor, (type will be inferred from src / dest).
     * @param src   Source assumed to be an OutputSliceNode or a FilterSliceNode.
     * @param dest  Dest assumed to be an InputSliceNode or a FilterSliceNode.
     */
    public Edge(SliceNode src, SliceNode dest) {
        assert src != null;
        assert dest != null;
        this.src = src;
        this.dest = dest;
        type = null;
    }

    /**
     * Partial constructor, for subclasses.
     *
     */
    protected Edge() { }
    
    
    /**
     * @return source SliceNode
     */
    public SliceNode getSrc() {
        return src;
    }

    public Edge(OutputSliceNode src) {
        this.src = src;
    }

    public Edge(InputSliceNode dest) {
        this.dest = dest;
    }

    public CType getType() {
        if (type != null) {
            return type;
        }
        // inter-slice edge
        if (src instanceof OutputSliceNode && dest instanceof InputSliceNode) {
            FilterContent srcContent;
            FilterContent dstContent;
            CType srcType;
            CType dstType;
            srcContent = ((OutputSliceNode)src).getPrevFilter().getFilter();
            dstContent = ((InputSliceNode)dest).getNextFilter().getFilter();
            srcType = srcContent.getOutputType();
            dstType = dstContent.getInputType();
            type = dstType;
            assert srcType.equals(dstType) : "Error calculating type: " + 
            srcContent + " -> " + dstContent;
            return type;
        }
        
        // intra-slice edges:
        if (src instanceof InputSliceNode && dest instanceof FilterSliceNode) {
            type = ((FilterSliceNode)dest).getFilter().getInputType();
            return type;
        }
        if (src instanceof FilterSliceNode && dest instanceof OutputSliceNode) {
            type = ((FilterSliceNode)src).getFilter().getOutputType();
            return type;
        }
        // only for general slices...
        if (src instanceof FilterSliceNode
                && dest instanceof FilterSliceNode) {
            type = ((FilterSliceNode)src).getFilter().getOutputType();
            assert type == ((FilterSliceNode)dest).getFilter().getInputType() 
            : "Error calculating type: " + 
            ((FilterSliceNode)src).getFilter() + " -> " + ((FilterSliceNode)dest).getFilter();
            return type;
        }
        throw new AssertionError ("Unexpected SliceNode connection " + src + " -> " + dest);
    }

    /**
     * @return dest SliceNode
     */
    public SliceNode getDest() {
        return dest;
    }

    /**
     * Set the source SliceNode
     * @param src
     */
    public void setSrc(SliceNode src) {
        this.src = src;
    }

    /**
     * Set the destination SliceNode
     * @param dest
     */
    public void setDest(SliceNode dest) {
        this.dest = dest;
    }

    public String toString() {
        return src + "->" + dest + "(" + hashCode() + ")";
    }
    /**
     * Return a FilterSliceNode that is either the passed node, or the next node if an InputSliceNode.
     * Error if passed an OutputSliceNode.
     * @param node a FilterSliceNode or InputSliceNode
     * @return a FilterSliceNode
     */
    private static FilterSliceNode nextFilter(SliceNode node) {
        if (node instanceof FilterSliceNode) {
            return (FilterSliceNode)node;
        } else {
            return ((InputSliceNode)node).getNextFilter();
        }
    }
    
    /**
     * Return a FilterSliceNode that is either the passed node, or the previous node if an OutputSliceNode.
     * Error if passed an InputSliceNode.
     * @param node a FilterSliceNode or OutputSliceNode
     * @return a FilterSliceNode
     */
   
    private static FilterSliceNode prevFilter(SliceNode node) {
        if (node instanceof FilterSliceNode) {
            return (FilterSliceNode)node;
        } else {
            return ((OutputSliceNode)node).getPrevFilter();
        }
    }


}
