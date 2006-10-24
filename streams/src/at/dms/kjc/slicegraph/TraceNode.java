package at.dms.kjc.slicegraph;

import at.dms.kjc.spacetime.Trace;

/**
 * 
 */
abstract public class TraceNode {
    private TraceNode next;

    private TraceNode previous;

    private Trace parent;

    public TraceNode getNext() {
        return next;
    }

    public TraceNode getPrevious() {
        return previous;
    }

    public void setPrevious(TraceNode prev) {
        previous = prev;
    }

    public void setNext(TraceNode next) {
        this.next = next;
    }

    public boolean isInputTrace() {
        return this instanceof InputTraceNode;
    }

    public boolean isFilterTrace() {
        return this instanceof FilterTraceNode;
    }

    public boolean isOutputTrace() {
        return this instanceof OutputTraceNode;
    }

    public InputTraceNode getAsInput() {
        return (InputTraceNode)this;
    }
    
    public OutputTraceNode getAsOutput() {
        return (OutputTraceNode)this;
    }
    
    public FilterTraceNode getAsFilter() {
        return (FilterTraceNode) this;
    }
    
    public void setParent(Trace par) {
        parent = par;
    }

    public Trace getParent() {
        assert parent != null : "parent not set for trace node";
        return parent;
    }
}
