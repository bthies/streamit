package at.dms.kjc.slicegraph;

import at.dms.util.Utils;
import java.util.HashSet;
import java.util.Set;
import java.util.LinkedList;
import at.dms.kjc.*;

/**
 * Each Slice is started by an InputSlice Node that is either a joiner connecting several other slices, 
 * or a connection to a single other slice.
 * Has an array of weights and corresponding {@link InterSliceEdge}s.
 */
public class InputSliceNode extends SliceNode {
    private int[] weights;

    private InterSliceEdge[] sources;

    private static int unique = 0;

    private String ident;

    private static int[] EMPTY_WEIGHTS = new int[0];

    private static InterSliceEdge[] EMPTY_SRCS = new InterSliceEdge[0];

    /** Creator */
    public InputSliceNode(int[] weights, InterSliceEdge[] sources) {
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

    /** Constructor */
    public InputSliceNode(int[] weights, OutputSliceNode[] sources) {
        // this.parent = parent;
        if (weights.length != sources.length)
            Utils.fail("Add comment later");
        // this.sources = sources;
        this.sources = new InterSliceEdge[sources.length];
        for (int i = 0; i < sources.length; i++)
            this.sources[i] = new InterSliceEdge(sources[i], this);
        
        if (weights.length == 1)
            this.weights = new int[]{1};
        else 
            this.weights = weights;
        ident = "input" + unique;
        unique++;
    }

    /** Constructor: weights, edges to be set later. */
    public InputSliceNode(int[] weights) {
        // this.parent = parent;
        sources = EMPTY_SRCS;
        if (weights.length == 1)
            this.weights = new int[]{1};
        else 
            this.weights = weights;
        ident = "input" + unique;
        unique++;
    }

    /** Constructor: no edges, no weights */
    public InputSliceNode() {
        // this.parent = parent;
        sources = EMPTY_SRCS;
        weights = EMPTY_WEIGHTS;
        ident = "input" + unique;
        unique++;
    }

    /**
     * Merge neighboring edges and weights if the neighboring edges
     * are actually the same Edge object. 
     * This operation exists as a cleanup operation for synch removal.
     * Code generation for Edges may rely on {@link OutputSliceNode#canonicalize()}
     * being run on all output nodes whose edges are combined by canonicalize.
     */
    public void canonicalize() {
        //do nothing for 0 length joiners
        if (sources.length == 0)
            return;
        
        LinkedList<Integer> newWeights = new LinkedList<Integer>();
        LinkedList<InterSliceEdge> newEdges = new LinkedList<InterSliceEdge>();

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
    
    /** InputSliceNode is FileOutput if FilterSliceNode is FileOutput.*/
    public boolean isFileOutput() {
        return ((FilterSliceNode) getNext()).isFileOutput();
    }

    /** Returns unique string identifying slice. */
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
     * on one iteration of this input slice node, remember
     * that a single edge can appear multiple times in the joining
     * pattern.
     * 
     * @param edge The edge to query.
     * @return The number of items passing on the edge.
     */
    public int getItems(InterSliceEdge edge) {
        int items = 0;
        
        for (int i = 0; i < sources.length; i++) {
            if (sources[i] == edge) {
                items += weights[i];
            }
        }
        
        return items;
    }
  
    /** @return array of edge weights */
    public int[] getWeights() {
        return weights;
    }

    /** @return array of edges */
    public InterSliceEdge[] getSources() {
        return sources;
    }

    /**
     * Set the weights and sources array of this input slice node
     * to the weights list and the edges list.
     * 
     * @param weights The list of weights (Integer).
     * @param edges The list of edges.
     */
    public void set(LinkedList<Integer> weights, 
            LinkedList<InterSliceEdge> edges) {
        int[] intArr = new int[weights.size()]; 
        
        for (int i = 0; i < weights.size(); i++)
            intArr[i] = weights.get(i).intValue();
        setWeights(intArr);
        
        setSources(edges.toArray(new InterSliceEdge[edges.size()]));
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
    
    /** Set the source edges. (shares, does not copy.) */
    public void setSources(InterSliceEdge[] sources) {
        this.sources = sources;
    }

    /** @return total weight of all edges */
    public int totalWeights() {
        int sum = 0;
        for (int i = 0; i < weights.length; i++)
            sum += weights[i];
        return sum;
    }

    /** @return total weight on all connections to a single Edge. 
     * @param out The Edge that we are interested in*/
    public int getWeight(Edge out) {
        int sum = 0;

        for (int i = 0; i < sources.length; i++)
            if (sources[i] == out)
                sum += weights[i];

        return sum;
    }

    /**
     * Does sources have a single element. 
     * @return true if there is a single element in sources. */
    public boolean oneInput() {
        return (sources.length == 1);
    }
    
    /** 
     * Is a joiner if there are at least 2 sources (even if same Edge object).
     * @return is a joiner.
     */
    public boolean isJoiner() {
        return sources.length >= 2;
    }

    /** Get the singleton edge. 
     * Must have only one input in sources.
     * @return the edge, or throw AssertionError
     */
    public InterSliceEdge getSingleEdge() {
        assert oneInput() : "Calling getSingeEdge() on InputSlice with less/more than one input";
        return sources[0];
    }

    /** 
     * Get the following FilterSliceNode.
     * @return
     */
    public FilterSliceNode getNextFilter() {
        return (FilterSliceNode) getNext();
    }

    /** Are there no inputs?
     * 
     * @return
     */
    public boolean noInputs() {
        return sources.length == 0;
    }

    /** return ratio of weight of edge to totalWeights().
     * 
     * @param edge
     * @return  0.0 if totalWeights() == 0, else ratio.
     */
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
    public LinkedList<InterSliceEdge> getSourceSequence() {
        LinkedList<InterSliceEdge> list = new LinkedList<InterSliceEdge>();
        for (int i = 0; i < sources.length; i++) {
            if (!list.contains(sources[i]))
                list.add(sources[i]);
        }
        return list;
    }
    
    /**
     * Return a set of all the slices that are inputs to this slice.
     * 
     * @return a set of all the slices that are inputs to this slice.
     */
    public Set<Slice> getSourceSlices() {
        HashSet<Slice> slices = new HashSet<Slice>();
        for (InterSliceEdge edge : getSourceList()) {
            slices.add(edge.getSrc().getParent());
        }
        return slices;
    }
    
    /**
     * Return a linked list of the sources pattern.
     * 
     * @return The linked list of the sources pattern.
     */
    public LinkedList<InterSliceEdge> getSourceList() {
       LinkedList<InterSliceEdge> list = new LinkedList<InterSliceEdge>();
       for (int i = 0; i < sources.length; i++)
           list.add(sources[i]);
       return list;
    }
    
    public Set<InterSliceEdge> getSourceSet() {
        HashSet<InterSliceEdge> set = new HashSet<InterSliceEdge>();
        for (int i = 0; i < sources.length; i++)
            set.add(sources[i]);
        return set;
    }

    /**
     * Return a string that gives some information for this input slice node.
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
    public void replaceEdge(InterSliceEdge oldEdge, InterSliceEdge newEdge) {
        for (int i = 0; i < sources.length; i++) {
            if (sources[i] == oldEdge)
                sources[i] = newEdge;
        }
    }
    
    public CType getType() {
        return getNextFilter().getFilter().getInputType();
    }

}
