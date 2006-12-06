package at.dms.kjc.spacetime;

import at.dms.kjc.slicegraph.FilterTraceNode;
import at.dms.kjc.slicegraph.InputTraceNode;
import at.dms.kjc.slicegraph.OutputTraceNode;
import at.dms.kjc.slicegraph.TraceNode;
import at.dms.util.Utils;
import java.util.ArrayList;

/** 
 * Trace class models a trace (slice).
 *  
 * @author mgordon
 */
public class Trace {
    //The head of the slice.
    private InputTraceNode head;
    //the Tail of the slice.
    private OutputTraceNode tail;
    //The length of the slice.
    private int len;
    //This should be deleted, not used anymore!
    private int primePump;
    private FilterTraceNode[] filterNodes;
    
    /*
     * public Trace (Trace[] edges, Trace[] depends, InputTraceNode head) { if
     * (edges == null) this.edges = new Trace[0]; else this.edges = edges;
     * 
     * this.head = head; head.setParent(this);
     * 
     * if (depends == null) this.depends = new Trace[0]; else this.depends =
     * depends; len=-1; }
     */

    public Trace(InputTraceNode head) {
        this.head = head;
        head.setParent(this);
        // depends = new Trace[0];
        // edges = new Trace[0];
        len = -1;
    }

    public Trace(TraceNode node) {
        if (!(node instanceof FilterTraceNode))
            Utils.fail("FilterTraceNode expected: " + node);
        head = new InputTraceNode();
        head.setParent(this);
        head.setNext(node);
        node.setPrevious(head);
        // depends = new Trace[0];
        // edges = new Trace[0];
        len = -1;
    }

    /**
     * Finishes creating Trace.
     *  
     * @return The number of FilterTraceNodes.
     */
    public int finish() {
        int size = 0;
        TraceNode node = head;
        if (node instanceof InputTraceNode)
            node = node.getNext();
        TraceNode end = node;
        while (node != null && node instanceof FilterTraceNode) {
            node.setParent(this);
            size++;
            end = node;
            node = node.getNext();
        }
        if (node != null)
            end = node;
        len = size;
        if (end instanceof OutputTraceNode)
            tail = (OutputTraceNode) end;
        else {
            tail = new OutputTraceNode();
            end.setNext(tail);
            tail.setPrevious(end);
        }
        tail.setParent(this);
        //set the filterNodes array
        filterNodes = new FilterTraceNode[size];
        int i = 0;
        node = getHead().getNext();
        while (node.isFilterTrace()) {
            filterNodes[i++] = node.getAsFilter();
            node = node.getNext();
        }
        assert i == size;
        return size;
    }

    /**
     * @return The incoming Traces (Slices) in the partitioned stream graph for this slice (trace). 
     */
    public Trace[] getDependencies() {
        Trace[] depends = new Trace[head.getSources().length];
        
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
     * Set the tail of this trace to out.  This method
     * does not fix the intra-trace connections of the trace nodes, but 
     * it does set the parent of the new output trace.
     * 
     * @param out The new output trace node.
     */
    public void setTail(OutputTraceNode out) {
        tail = out;
        out.setParent(this);
    }
    
    /**
     * Set the head of this trace to node.  This method
     * does not fix the intra-trace connections of the trace nodes, but 
     * it does set the parent of the new input trace node.
     * 
     * @param node The new input trace node.
     */
    public void setHead(InputTraceNode node) {
        head = node;
        node.setParent(this);
    }

    public InputTraceNode getHead() {
        return head;
    }

    // finish() must have been called
    public OutputTraceNode getTail() {
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
        return "Trace: " + head + "->" + head.getNext() + "->...";
    }

    public void setPrimePump(int pp) {
        primePump = pp;
        /*
         * TraceNode cur=head.getNext(); while(cur instanceof FilterTraceNode) {
         * ((FilterTraceNode)cur).getFilter().setPrimePump(pp);
         * cur=cur.getNext(); }
         */
    }

    public int getPrimePump() {
        return primePump;
    }

    // return the number of filters in the trace
    public int getNumFilters() {
        TraceNode node = getHead().getNext();
        int ret = 0;
        while (node instanceof FilterTraceNode) {
            node = node.getNext();
            ret++;
        }
        return ret;
    }

    /** 
     * @return The array of just the filter trace nodes, in data flow order.
     */
    public FilterTraceNode[] getFilterNodes() {
        return filterNodes;
    }
}