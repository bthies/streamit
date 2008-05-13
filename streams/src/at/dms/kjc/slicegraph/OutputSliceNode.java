package at.dms.kjc.slicegraph;

import at.dms.kjc.*;
import at.dms.kjc.spacetime.Util;

import java.util.Set;
import java.util.HashSet;
import java.util.Vector;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

/**
 * Each slice is terminated by an OutputSliceNode that has single input (the last filter) 
 * and multiple outputs (to downstream slices through edges).
 * 
 * @author mgordon
 */
public class OutputSliceNode extends SliceNode {
    // the (round-robin) weight for each edge.
    private int[] weights;

    // Ordered array of sets of edges.
    // The order in the outer array corresponds to the order of weights.
    // The inner array is just a set: the elements correspond to 
    // elements of dulicate splitters fused with the top-level
    // round-robin splitter (by synch removal).
    // A round-robin splitter of size n would be an Edge[n][1]
    // A duplicate splitter of size n would be an Edge[1][n]
    private InterSliceEdge[][] dests;

    private String ident;

    private static int unique = 0;

    private static int[] EMPTY_WEIGHTS = new int[0];

    private static InterSliceEdge[][] EMPTY_DESTS = new InterSliceEdge[0][0];

    private List sortedOutputs;

    
    /**
     * Construct a new output slice node based on the arrays weights
     * and dests.
     * 
     * @param weights The array of weights
     * @param dests The array of dests.
     */

    public OutputSliceNode(int[] weights, InterSliceEdge[][] dests) {
        // this.parent = parent;
        assert weights.length == dests.length : "weights must equal sources";
        ident = "output" + unique;
        unique++;
        if (weights.length == 1)
            this.weights = new int[]{1};
        else 
            this.weights = weights;
        this.dests = dests;
    }

    /**
     * Construct a new output slice node based on the lists weights
     * and dests.
     * 
     * @param weights The list of weights
     * @param dests The list of dests.
     */
    public OutputSliceNode(LinkedList<Integer> weights, 
            LinkedList<LinkedList<InterSliceEdge>> dests) {
        assert weights.size() == dests.size();
        ident = "output" + unique++;
        //convert the weights list
        set(weights, dests);
    }
    
    
    /**
     * Construct a new output slice node based on the array weights.
     * Dests is to be set later.
     * 
     * @param weights The array of weights
     */
    public OutputSliceNode(int[] weights) {
        ident = "output" + unique;
        unique++;
        if (weights.length == 1)
            this.weights = new int[]{1};
        else 
            this.weights = weights;
        dests = EMPTY_DESTS;
    }

    /**
     * Construct a nre output slice node.
     * Weights and dests to be st later.
     *
     */
    public OutputSliceNode() {
        ident = "output" + unique;
        unique++;
        weights = EMPTY_WEIGHTS;
        dests = EMPTY_DESTS;
    }

    /** Set the weights */
    public void setWeights(int[] newW) {
        this.weights = newW;
    }
    
    /**
     * Set the weights and dests of this input slice node to 
     * weights and dests.
     * 
     * @param weights List of integer weights.
     * @param dests List of Lists of Edge for splitting pattern.
     */
    public void set(LinkedList<Integer> weights, 
            LinkedList<LinkedList<InterSliceEdge>> dests) {
        if (weights.size() == 1) 
            this.weights = new int[]{1};
        else {
            this.weights = new int[weights.size()];
            for (int i = 0; i < weights.size(); i++)
                this.weights[i] = weights.get(i).intValue();
        }
        //convert the dests list
        this.dests = new InterSliceEdge[dests.size()][];
        for (int i = 0; i < dests.size(); i++) 
            this.dests[i] = dests.get(i).toArray(new InterSliceEdge[0]);
    }
        
    /** @return the weights */
    public int[] getWeights() {
        return weights;
    }

    /** @return whether previous filter was FileInput */
    public boolean isFileInput() {
        return ((FilterSliceNode) getPrevious()).isFileInput();
    }

    /** @return dests */
    public InterSliceEdge[][] getDests() {
        return dests;
    }

    /** Set dests */
    public void setDests(InterSliceEdge[][] dests) {
        this.dests = dests;
    }

    /** @return unique string */
    public String getIdent() {
        return ident;
    }

    /** @return total of weights */
    public int totalWeights() {
        int sum = 0;
        for (int i = 0; i < weights.length; i++)
            sum += weights[i];
        return sum;
    }

    /**
     * Combine the weights of adajacent outputs that have equal 
     * destinations.
     * This operation exists as a cleanup operation for synch removal.
     * Code generation for Edges may rely on {@link InputSliceNode#canonicalize()}
     * being run on all input nodes whose edges are combined by canonicalize.
     */
    public void canonicalize() {
        if (weights.length == 0)
            return;
        LinkedList<LinkedList<InterSliceEdge>> edges = new LinkedList<LinkedList<InterSliceEdge>>();
        LinkedList<Integer> newWeights = new LinkedList<Integer>();
        //add the first port to the new edges and weights
        LinkedList<InterSliceEdge> port = new LinkedList<InterSliceEdge>();
        Util.add(port, dests[0]);
        edges.add(port);
        newWeights.add(new Integer(weights[0]));
        
        for (int i = 1; i < dests.length; i++) {
            if (Util.setCompare(edges.get(edges.size() - 1), dests[i])) {
                Integer newWeight = 
                    newWeights.get(newWeights.size() - 1).intValue() + 
                    weights[i];
                newWeights.remove(newWeights.size() - 1);
                newWeights.add(newWeight);
            }
            else {
                //not equal, so create a new port and add it and the weight
                port = new LinkedList<InterSliceEdge>();
                Util.add(port, dests[i]);
                edges.add(port);
                newWeights.add(new Integer(weights[i]));
            }
        }
        //set the new weights and the dests
        set(newWeights, edges);
    }
    
    /**
     * Return the width of this splitter meaning the number
     * of connections it has to downstream slices, including 
     * all the edges of a duplicated item, counting each unique 
     * edge once.
     * 
     * @return The width of this splitter.
     */
    public int getWidth() {
        return getDestSet().size();
    }
    
    /**
     * Return a list of the edges with each edge appearing once
     * and ordered by the order in which each edge appears in the
     * split pattern.
     * 
     * @return The list.
     */
    public LinkedList<InterSliceEdge> getDestSequence() {
        LinkedList<InterSliceEdge> list = new LinkedList<InterSliceEdge>();
        for (int i = 0; i < dests.length; i++) {
            for (int j = 0; j < dests[i].length; j++) 
                if (!list.contains(dests[i][j]))
                    list.add(dests[i][j]);
        }
        return list;
    }
    
    /**
     * return the number of items sent by this output slice node on all instances of a particular edge.
     */
    public int getWeight(InterSliceEdge in) {
        int sum = 0;

        for (int i = 0; i < dests.length; i++) {
            for (int j = 0; j < dests[i].length; j++) {
                // System.out.println("Checking dest(" + i + ", " + j + ")");
                // System.out.println(dests[i][j] + " ?= " + in);
                if (dests[i][j] == in) {
                    // System.out.println("Found edge");
                    sum += weights[i];
                    break;
                }
            }
        }
        return sum;
    }

    /** type is output type of previous filter */
    public CType getType() {
        return getPrevFilter().getFilter().getOutputType();
    }

    /**
     * Return a set of all the destination slices of this output slice node.
     * 
     * @return a set of all the destination slices of this output slice node.
     */
    public Set<Slice> getDestSlices() {
        HashSet<Slice> dests = new HashSet<Slice>();
        for (InterSliceEdge edge : getDestSet()) {
            dests.add(edge.getDest().getParent());
        }
        return dests;
    }
    
    /**
     * Return a list of the dests in round-robin order flattening
     * the duplicates.  
     * 
     * @return A list of the dests in round-robin order flattening
     * the duplicates.  
     */ 
    public InterSliceEdge[] getDestList() {
        LinkedList<InterSliceEdge> edges = new LinkedList<InterSliceEdge>();
        for (int i = 0; i < dests.length; i++) {
            for (int j = 0; j < dests[i].length; j++)
                edges.add(dests[i][j]);
        }
        return edges.toArray(new InterSliceEdge[edges.size()]);
    }
    
    /**
     * Return the set of the outgoing edges of this OutputSliceNode.
     * 
     * @return The set of the outgoing edges of this OutputSliceNode.
     */
    public Set<InterSliceEdge> getDestSet() {
        HashSet<InterSliceEdge> set = new HashSet<InterSliceEdge>();
        for (int i = 0; i < dests.length; i++) {
            for (int j = 0; j < dests[i].length; j++)
                set.add(dests[i][j]);
        }
        return set;
    }

    public boolean oneOutput() {
        return (weights.length == 1 && dests[0].length == 1);
    }

    public InterSliceEdge getSingleEdge() {
        assert oneOutput() : "Calling getSingleEdge() on OutputSlice with less/more than one output";
        return dests[0][0];
    }

    public boolean noOutputs() {
        return weights.length == 0;
    }
    
    public boolean isDuplicateSplitter() {
        return (weights.length == 1 && dests.length == 1 && dests[0].length >= 2);
    }
    
    public boolean isRRSplitter() {
        return (weights.length >=2 && dests.length >= 2);
    }
    
    public boolean isSplitter() {
        return (isDuplicateSplitter() || isRRSplitter());
    }

    /**
     * return an iterator that iterates over the inputslicenodes in descending
     * order of the number of items sent to the inputslicenode
     */
    public List<InterSliceEdge> getSortedOutputs() {
        if (sortedOutputs == null) {
            // if there are no dest just return an empty iterator
            if (weights.length == 0) {
                sortedOutputs = new LinkedList();
                return sortedOutputs;
            }
            // just do a simple linear insert over the dests
            // only has to be done once
            Vector<InterSliceEdge> sorted = new Vector();
            Iterator<InterSliceEdge> dests = getDestSet().iterator();
            // add one element
            sorted.add(dests.next());
            while (dests.hasNext()) {
                InterSliceEdge current = (InterSliceEdge) dests.next();
                // add to end if it is less then everything
                if (getWeight(current) <= getWeight((InterSliceEdge) sorted.get(sorted
                                                                      .size() - 1)))
                    sorted.add(current);
                else { // otherwise find the correct place to add it
                    for (int i = 0; i < sorted.size(); i++) {
                        // if this is the correct place to insert it,
                        // add it and break
                        if (getWeight(current) > getWeight((InterSliceEdge) sorted.get(i))) {
                            sorted.add(i, current);
                            break;
                        }
                    }
                }
            }
            assert sorted.size() == getDestSet().size() : "error "
                + sorted.size() + "!= " + getDestSet().size();
            sortedOutputs = sorted.subList(0, sorted.size());
        }
        return sortedOutputs;
    }

    public FilterSliceNode getPrevFilter() {
        return (FilterSliceNode) getPrevious();
    }

    public double ratio(InterSliceEdge edge) {
        if (totalWeights() == 0)
            return 0.0;
        return ((double) getWeight(edge) / (double) totalWeights());
    }

    public String debugString(boolean escape) {
        String newLine = "\n";
        StringBuffer buf = new StringBuffer();
        if (escape)
            newLine = "\\n";

        buf.append("***** " + this.toString() + " *****" + newLine);
        for (int i = 0; i < weights.length; i++) {
            buf.append("* Weight = " + weights[i] + newLine);
            for (int j = 0; j < dests[i].length; j++)
                buf.append("  " + dests[i][j] + newLine);
        }
        buf.append("**********" + newLine);
        return buf.toString();
    }


    public boolean hasFileOutput() {
        Iterator dests = getDestSet().iterator();
        while (dests.hasNext()) {
            if (((InterSliceEdge) dests.next()).getDest().isFileOutput())
                return true;
        }
        return false;
    }

    public Set<InputSliceNode> fileOutputs() {
        HashSet<InputSliceNode> fileOutputs = new HashSet<InputSliceNode>();
        Iterator dests = getDestSet().iterator();
        while (dests.hasNext()) {
            InterSliceEdge edge = (InterSliceEdge) dests.next();
            if (edge.getDest().isFileOutput())
                fileOutputs.add(edge.getDest());
        }
        return fileOutputs;
    }
    
    /*
     * public int itemsReceived(boolean init, boolean primepump) { return
     * FilterInfo.getFilterInfo(getPrevFilter()).totalItemsSent(init,
     * primepump); }
     * 
     * public int itemsSent(boolean init, boolean primepump) {
     *  }
     */
}