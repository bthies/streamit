package at.dms.kjc.slicegraph;

import at.dms.util.Utils;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import at.dms.kjc.spacetime.IntraTraceBuffer;
import at.dms.kjc.spacetime.OffChipBuffer;
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
        if (weights.length == 1)
            this.weights = new int[]{1};
        else 
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
        
        if (weights.length == 1)
            this.weights = new int[]{1};
        else 
            this.weights = weights;
        ident = "input" + unique;
        unique++;
    }

    public InputTraceNode(int[] weights) {
        // this.parent = parent;
        sources = EMPTY_SRCS;
        if (weights.length == 1)
            this.weights = new int[]{1};
        else 
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

    /**
     * Merge any neighboring edges in the sources and weights 
     * arrays that are equal.
     *
     */
    public void canonicalize() {
        //do nothing for 0 length joiners
        if (sources.length == 0)
            return;
        
        LinkedList<Integer> newWeights = new LinkedList<Integer>();
        LinkedList<Edge> newEdges = new LinkedList<Edge>();

        //add the first edge and weight
        newWeights.add(new Integer(weights[0]));
        newEdges.add(sources[0]);
        
        for (int i = 1; i < sources.length; i++) {
            if (sources[i] == newEdges.get(newEdges.size() - 1)) {
                //this src is equal to the last one, so add the weights together!
                Integer newWeight = 
                    new Integer(newWeights.get(newWeights.size() - 1).intValue() + 
                                weights[i]);
                newWeights.remove(newWeights.size() - 1);
                newWeights.add(newWeight);
            }
            else {
                //not equal, start a new entry and weight
                newEdges.add(sources[i]);
                newWeights.add(new Integer(weights[i]));
            }
        }
        set(newWeights, newEdges);
    }
    
    public boolean isFileOutput() {
        return ((FilterTraceNode) getNext()).isFileOutput();
    }

    public String getIdent() {
        return ident;
    }

    /**
     * Return the number of unique inputs (Edges) to this join.
     * 
     * @return The width of the join.
     */
    public int getWidth() {
        return getSourceSet().size();
    }
    
    /**
     * Return the number of items that traverse this edge
     * on one iteration of this input trace node, remember
     * that a single edge can appear multiple times in the joining
     * pattern.
     * 
     * @param edge The edge to query.
     * @return The number of items passing on the edge.
     */
    public int getItems(Edge edge) {
        int items = 0;
        
        for (int i = 0; i < sources.length; i++) {
            if (sources[i] == edge) {
                items += weights[i];
            }
        }
        
        return items;
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
        if (newWeights.length == 1)
            this.weights = new int[]{1};
        else 
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

    /**
     * Return a list of the edges with each edge appearing once
     * and ordered by the order in which each edge appears in the
     * join pattern.
     * 
     * @return The list.
     */
    public LinkedList<Edge> getSourceSequence() {
        LinkedList<Edge> list = new LinkedList<Edge>();
        for (int i = 0; i < sources.length; i++) {
            if (!list.contains(sources[i]))
                list.add(sources[i]);
        }
        return list;
    }
    
    /**
     * Return a linked list of the sources pattern.
     * 
     * @return The linked list of the sources pattern.
     */
    public LinkedList<Edge> getSourceList() {
       LinkedList<Edge> list = new LinkedList<Edge>();
       for (int i = 0; i < sources.length; i++)
           list.add(sources[i]);
       return list;
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
     * In the sources array for the input, replace all instances of 
     * oldEdge with newEdge.
     * 
     * @param oldEdge The edge to replace.
     * @param newEdge The edge to install.
     */
    public void replaceEdge(Edge oldEdge, Edge newEdge) {
        for (int i = 0; i < sources.length; i++) {
            if (sources[i] == oldEdge)
                sources[i] = newEdge;
        }
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
