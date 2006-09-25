package at.dms.kjc.spacetime;

import at.dms.util.Utils;
import at.dms.kjc.*;
import at.dms.kjc.flatgraph2.*;
import java.util.Set;
import java.util.HashSet;
import java.util.Vector;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

/**
 * Each slice is terminated by an OutputTraceNode that is single input (the last filter) 
 * and multiple output (to downstream slices through edges).
 * 
 * @author mgordon
 */
public class OutputTraceNode extends TraceNode {
    private int[] weights;

    private Edge[][] dests;

    private String ident;

    private static int unique = 0;

    private static int[] EMPTY_WEIGHTS = new int[0];

    private static Edge[][] EMPTY_DESTS = new Edge[0][0];

    private List sortedOutputs;

    

    public OutputTraceNode(int[] weights, Edge[][] dests) {
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
     * Create a new output trace node based on the lists weights
     * and dests.
     * 
     * @param weights The list of weights
     * @param dests The list of dests.
     */
    public OutputTraceNode(LinkedList<Integer> weights, 
            LinkedList<LinkedList<Edge>> dests) {
        assert weights.size() == dests.size();
        ident = "output" + unique++;
        //convert the weights list
        set(weights, dests);
    }
    
    
    public OutputTraceNode(int[] weights) {
        // this.parent = parent;
        ident = "output" + unique;
        unique++;
        if (weights.length == 1)
            this.weights = new int[]{1};
        else 
            this.weights = weights;
        dests = EMPTY_DESTS;
    }

    public OutputTraceNode() {
        // this.parent = parent;
        ident = "output" + unique;
        unique++;
        weights = EMPTY_WEIGHTS;
        dests = EMPTY_DESTS;
    }

    public void setWeights(int[] newW) {
        this.weights = newW;
    }
    
    /**
     * Set the weights and dests of this input trace node to 
     * weights and dests.
     * 
     * @param weights List of integer weights.
     * @param dests List of Lists of Edge for splitting pattern.
     */
    public void set(LinkedList<Integer> weights, 
            LinkedList<LinkedList<Edge>> dests) {
        if (weights.size() == 1) 
            this.weights = new int[]{1};
        else {
            this.weights = new int[weights.size()];
            for (int i = 0; i < weights.size(); i++)
                this.weights[i] = weights.get(i).intValue();
        }
        //convert the dests list
        this.dests = new Edge[dests.size()][];
        for (int i = 0; i < dests.size(); i++) 
            this.dests[i] = dests.get(i).toArray(new Edge[0]);
    }
        
    public int[] getWeights() {
        return weights;
    }

    public boolean isFileInput() {
        return ((FilterTraceNode) getPrevious()).isFileInput();
    }

    public Edge[][] getDests() {
        return dests;
    }

    public void setDests(Edge[][] dests) {
        this.dests = dests;
    }

    public String getIdent() {
        return ident;
    }

    public int totalWeights() {
        int sum = 0;
        for (int i = 0; i < weights.length; i++)
            sum += weights[i];
        return sum;
    }

    /**
     * Combine the weights of adajacent outputs that have equal 
     * destinations.
     *
     */
    public void canonicalize() {
        if (weights.length == 0)
            return;
        LinkedList<LinkedList<Edge>> edges = new LinkedList<LinkedList<Edge>>();
        LinkedList<Integer> newWeights = new LinkedList<Integer>();
        //add the first port to the new edges and weights
        LinkedList<Edge> port = new LinkedList<Edge>();
        Util.add(port, dests[0]);
        edges.add(port);
        newWeights.add(new Integer(weights[0]));
        
        for (int i = 1; i < dests.length; i++) {
            if (Util.setCompare(edges.get(edges.size() - 1), dests[i])) {
                Integer newWeight = new Integer(
                    newWeights.get(newWeights.size() - 1).intValue() + 
                    weights[i]);
                newWeights.remove(newWeights.size() - 1);
                newWeights.add(newWeight);
            }
            else {
                //not equal, so create a new port and add it and the weight
                port = new LinkedList<Edge>();
                Util.add(port, dests[i]);
                edges.add(port);
                newWeights.add(new Integer(weights[i]));
            }
        }
        //set the new weights and the destsx
        set(newWeights, edges);
    }
    
    /**
     * Return the width of this splitter meaning the number
     * of connections it has to downstream traces, including 
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
    public LinkedList<Edge> getDestSequence() {
        LinkedList<Edge> list = new LinkedList<Edge>();
        for (int i = 0; i < dests.length; i++) {
            for (int j = 0; j < dests[i].length; j++) 
                if (!list.contains(dests[i][j]))
                    list.add(dests[i][j]);
        }
        return list;
    }
    
    /**
     * return the number of items sent to this inputtracenode for on iteration
     * of the weights..
     */
    public int getWeight(Edge in) {
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

    public CType getType() {
        return getPrevFilter().getFilter().getOutputType();
    }

    /**
     * Return a list of the dests in round-robin order flattening
     * the duplicates.  
     * 
     * @return A list of the dests in round-robin order flattening
     * the duplicates.  
     */ 
    public Edge[] getDestList() {
        LinkedList<Edge> edges = new LinkedList<Edge>();
        for (int i = 0; i < dests.length; i++) {
            for (int j = 0; j < dests[i].length; j++)
                edges.add(dests[i][j]);
        }
        return edges.toArray(new Edge[0]);
    }
    
    /**
     * Return the set of the outgoing edges of this OutputTraceNode.
     * 
     * @return The set of the outgoing edges of this OutputTraceNode.
     */
    public Set<Edge> getDestSet() {
        HashSet<Edge> set = new HashSet<Edge>();
        for (int i = 0; i < dests.length; i++) {
            for (int j = 0; j < dests[i].length; j++)
                set.add(dests[i][j]);
        }
        return set;
    }

    public boolean oneOutput() {
        return (weights.length == 1 && dests[0].length == 1);
    }

    public Edge getSingleEdge() {
        assert oneOutput() : "Calling getSingleEdge() on OutputTrace with less/more than one output";
        return dests[0][0];
    }

    public boolean noOutputs() {
        return weights.length == 0;
    }

    /**
     * return an iterator that iterates over the inputtracenodes in descending
     * order of the number of items sent to the inputtracenode
     */
    public List<Edge> getSortedOutputs() {
        if (sortedOutputs == null) {
            // if there are no dest just return an empty iterator
            if (weights.length == 0) {
                sortedOutputs = new LinkedList();
                return sortedOutputs;
            }
            // just do a simple linear insert over the dests
            // only has to be done once
            Vector<Edge> sorted = new Vector();
            Iterator<Edge> dests = getDestSet().iterator();
            // add one element
            sorted.add(dests.next());
            while (dests.hasNext()) {
                Edge current = (Edge) dests.next();
                // add to end if it is less then everything
                if (getWeight(current) <= getWeight((Edge) sorted.get(sorted
                                                                      .size() - 1)))
                    sorted.add(current);
                else { // otherwise find the correct place to add it
                    for (int i = 0; i < sorted.size(); i++) {
                        // if this is the correct place to insert it,
                        // add it and break
                        if (getWeight(current) > getWeight((Edge) sorted.get(i))) {
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

    public FilterTraceNode getPrevFilter() {
        return (FilterTraceNode) getPrevious();
    }

    public double ratio(Edge edge) {
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
            if (((Edge) dests.next()).getDest().isFileOutput())
                return true;
        }
        return false;
    }

    public Set<InputTraceNode> fileOutputs() {
        HashSet<InputTraceNode> fileOutputs = new HashSet<InputTraceNode>();
        Iterator dests = getDestSet().iterator();
        while (dests.hasNext()) {
            Edge edge = (Edge) dests.next();
            if (edge.getDest().isFileOutput())
                fileOutputs.add(edge.getDest());
        }
        return fileOutputs;
    }

    /**
     * @return True if this output trace node has only one output and
     * that output is directly writing to a file reader with no non-redundant
     * buffers in between.
     */
    public boolean onlyWritingToAFile() {
        if (oneOutput()
                && OffChipBuffer.unnecessary(this)
                && getSingleEdge().getDest().isFileOutput()
                && OffChipBuffer.unnecessary(getSingleEdge()
                                     .getDest()))
            return true;
        return false;
                                   
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
