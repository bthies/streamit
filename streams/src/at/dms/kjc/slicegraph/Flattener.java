package at.dms.kjc.slicegraph;

import at.dms.kjc.sir.SIRFeedbackLoop;
import at.dms.kjc.sir.SIRFilter;
import at.dms.kjc.sir.SIRJoiner;
import at.dms.kjc.sir.SIROperator;
import at.dms.kjc.sir.SIRPipeline;
import at.dms.kjc.sir.SIRSplitJoin;
import at.dms.kjc.sir.SIRSplitter;
import at.dms.kjc.sir.SIRStream;

public class Flattener {

    // The filter at the top of the stream graph
    private FlatFilter top;
    
    public Flattener() {
        top = null;
    }
    
    /**
     * Recursively transforms from the SIR to a Flat representation
     * 
     * @param op The current SIROperator to be flattened
     * @param parent The parent of the current SIROperator (which has already
     * been flattened). Must be null for the first operator.
     * @return The last SIROperator to be transformed
     */
    public FlatFilter flatten(SIROperator op, FlatFilter parent) {
       if (op instanceof SIRFilter) {
           return handleFilter((SIRFilter) op, parent);
       } else if (op instanceof SIRPipeline) {
           return handlePipeline((SIRPipeline) op, parent);
       } else if (op instanceof SIRFeedbackLoop) {
           return handleFeedbackLoop((SIRFeedbackLoop) op, parent);
       } else if (op instanceof SIRSplitJoin) {
           return handleSplitJoin((SIRSplitJoin) op, parent);
       } else return null;
    }
    
    /**
     * Creates new FlatFilter and connects it to the parent. Sets <tt>top</tt>
     * if this is the first filter.
     * 
     * @param f
     * @param parent
     * @return The new FlatFilter
     */
    private FlatFilter handleFilter(SIRFilter f, FlatFilter parent) {
        FlatFilter flatf = new FlatFilter(f);
        if (parent == null) {
            top = flatf;
        } else {
            addEdgeBetween(parent, flatf);
        }
        return flatf;
    }
    
    /**
     * Create FlatFilters for the entire pipeline by calling <tt>flatten</tt>
     * on each successive filter in the pipeline. Connects each filter to its 
     * parent in the pipeline.
     * 
     * @param p
     * @param parent
     * @return The last filter in the pipeline
     */
    private FlatFilter handlePipeline(SIRPipeline p, FlatFilter parent) {
        FlatFilter previous = parent;
        for (SIROperator op : p.getChildren()) {
            previous = flatten(op, previous);
        }
        return previous;
    }
    
    private FlatFilter handleFeedbackLoop(SIRFeedbackLoop fl, FlatFilter parent) {
        return null;
    }
    
    /**
     * Creates FlatFilters out of the splitter, joiner, and parallel intermediate
     * streams by calling <tt>flatten</tt> on each stream. Connects the splitter
     * to the top of each stream, and the joiner to the bottom of each stream.
     *  
     * @param sj
     * @param parent
     * @return The FlatFilter representation of the joiner
     */
    private FlatFilter handleSplitJoin(SIRSplitJoin sj, FlatFilter parent) {
        SIRSplitter splitter = sj.getSplitter();
        SIRJoiner joiner = sj.getJoiner();
        
        FlatFilter flatSplitter = new FlatFilter(splitter);
        if (parent == null) {
            top = flatSplitter;
        } else {
            addEdgeBetween(parent, flatSplitter);
        }
        
        FlatFilter flatJoiner= new FlatFilter(joiner);
        
        for (int i=0; i<sj.size(); i++) {
            SIRStream child = sj.get(i);
            FlatFilter flatChild = flatten(child, flatSplitter);
            addEdgeBetween(flatChild, flatJoiner);
        }
        
        return flatJoiner;
    }
    
    private void addEdgeBetween(FlatFilter parent, FlatFilter child) {
        if (parent.isSplitter()) {
            parent.addOutgoing(child);
        } else {
            parent.addOutgoing(child, 1);
        }
        
        if (child.isJoiner()) {
            child.addIncoming(parent);
        } else {
            child.addIncoming(parent, 1);
        }
    }
    
    public FlatFilter getTop() {
        return top;
    }
}
