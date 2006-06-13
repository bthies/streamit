package at.dms.kjc.spacetime;

import at.dms.util.Utils;
import java.util.HashSet;
import java.util.LinkedList;
import at.dms.kjc.flatgraph2.*;
import at.dms.kjc.*;

/**
 * 
 */
public class InputTraceNode extends TraceNode {
    private int[] weights;

    private Edge[] sources;

    private static int unique = 0;

    private String ident;

    private static int[] EMPTY_WEIGHTS = new int[0];

    private static Edge[] EMPTY_SRCS = new Edge[0];

    public InputTraceNode(int[] weights, Edge[] sources) {
        // this.parent = parent;
        if (weights.length != sources.length)
            Utils.fail("Add comment later");
        this.sources = sources;
        this.weights = weights;
        ident = "input" + unique;
        unique++;
    }

    public InputTraceNode(int[] weights, OutputTraceNode[] sources) {
        // this.parent = parent;
        if (weights.length != sources.length)
            Utils.fail("Add comment later");
        // this.sources = sources;
        this.sources = new Edge[sources.length];
        for (int i = 0; i < sources.length; i++)
            this.sources[i] = new Edge(sources[i], this);
        this.weights = weights;
        ident = "input" + unique;
        unique++;
    }

    public InputTraceNode(int[] weights) {
        // this.parent = parent;
        sources = EMPTY_SRCS;
        this.weights = weights;
        ident = "input" + unique;
        unique++;
    }

    public InputTraceNode() {
        // this.parent = parent;
        sources = EMPTY_SRCS;
        weights = EMPTY_WEIGHTS;
        ident = "input" + unique;
        unique++;
    }

    public boolean isFileOutput() {
        return ((FilterTraceNode) getNext()).isFileOutput();
    }

    public String getIdent() {
        return ident;
    }

    /**
     * Return the number of inputs to this join.
     * 
     * @return The width of the join.
     */
    public int getWidth() {
        assert weights.length == sources.length;
        
        return weights.length;
    }
    
    public int[] getWeights() {
        return weights;
    }

    public Edge[] getSources() {
        return sources;
    }

    /**
     * Set the weights and sources array of this input trace node
     * to the weights list and the edges list.
     * 
     * @param weights The list of weights (Integer).
     * @param edges The list of edges.
     */
    public void set(LinkedList<Integer> weights, 
            LinkedList<Edge> edges) {
        int[] intArr = new int[weights.size()]; 
        for (int i = 0; i < weights.size(); i++)
            intArr[i] = weights.get(i).intValue();
        setWeights(intArr);
        setSources(edges.toArray(new Edge[edges.size()]));
    }
    
    /**
     * Set the weights to newWeights.
     * 
     * @param newWeights
     */
    public void setWeights(int[] newWeights) {
        this.weights = newWeights;
    }
    
    public void setSources(Edge[] sources) {
        this.sources = sources;
    }

    public int totalWeights() {
        int sum = 0;
        for (int i = 0; i < weights.length; i++)
            sum += weights[i];
        return sum;
    }

    public int getWeight(Edge out) {
        int sum = 0;

        for (int i = 0; i < sources.length; i++)
            if (sources[i] == out)
                sum += weights[i];

        return sum;
    }

    public boolean oneInput() {
        return (sources.length == 1);
    }

    public Edge getSingleEdge() {
        assert oneInput() : "Calling getSingeEdge() on InputTrace with less/more than one input";
        return sources[0];
    }

    public FilterTraceNode getNextFilter() {
        return (FilterTraceNode) getNext();
    }

    public boolean noInputs() {
        return sources.length == 0;
    }

    public double ratio(Edge edge) {
        if (totalWeights() == 0)
            return 0.0;
        return ((double) getWeight(edge) / (double) totalWeights());
    }

    public HashSet<Edge> getSourceSet() {
        HashSet<Edge> set = new HashSet<Edge>();
        for (int i = 0; i < sources.length; i++)
            set.add(sources[i]);
        return set;
    }

    /**
     * Return a string that gives some information for this input trace node.
     * If escape is true, then escape the new lines "\\n".
     *  
     * @param escape Should we escape the new lines?
     * @return The string.
     */
    public String debugString(boolean escape) {
        String newLine = "\n";
        if (escape)
            newLine = "\\n";

        StringBuffer buf = new StringBuffer();
        buf.append("***** " + this.toString() + " *****" + newLine);
        for (int i = 0; i < sources.length; i++) {
            buf.append("  weight " + weights[i] + ": " + sources[i].toString()
                       + newLine);
        }
        buf.append("**********" + newLine);
        return buf.toString();
    }

    public boolean hasFileInput() {
        for (int i = 0; i < sources.length; i++) {
            if (sources[i].getSrc().isFileInput())
                return true;
        }
        return false;
    }

    /**         
     * @return true if this input trace node reads from a single source
     * and the source is a file device.
     */
    public boolean onlyFileInput() {
        // get this buffer or this first upstream non-redundant buffer
        OffChipBuffer buffer = 
            IntraTraceBuffer.getBuffer(this, getNextFilter()).getNonRedundant();
        
        if (buffer == null)
            return false;
        
        //if not a file reader, then we might have to align the dest
        if (buffer.getDest() instanceof OutputTraceNode
                && ((OutputTraceNode) buffer.getDest()).isFileInput())
            return true;
        
        return false;
    }

    public CType getType() {
        return getNextFilter().getFilter().getInputType();
    }

}
