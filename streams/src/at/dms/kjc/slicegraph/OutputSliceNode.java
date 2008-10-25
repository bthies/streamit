package at.dms.kjc.slicegraph;

import at.dms.kjc.*;
import at.dms.kjc.spacetime.Util;
import at.dms.kjc.backendSupport.*;
import java.util.Set;
import java.util.HashSet;
import java.util.Vector;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

/**
 * Each slice is terminated by an OutputSliceNode that has single input (the last filter) 
 * and multiple outputs (to downstream slices through edges). There is a possibility that 
 * an output slice node could have a different schedule for the initialization stage. 
 * This is not the common case, so most methods assume there is not a separate schedule and use the 
 * single (steady and init) weights/dests.
 * 
 * @author mgordon
 */
public class OutputSliceNode extends SliceNode implements at.dms.kjc.DeepCloneable {
    public static final String[] DO_NOT_CLONE_THESE_FIELDS = 
        { "weights", "dests", "initWeights", "initDests" };
    
    /** the (round-robin) weight for each edge used for the steady and for init if this
     * node does not have a separate init pattern.
     */
    private int[] weights;
    /** Ordered array of sets of edges
     * The order in the outer array corresponds to the order of weights.
     * The inner array is just a set: the elements correspond to 
     * elements of duplicate splitters fused with the top-level
     * round-robin splitter (by synch removal).
     * A round-robin splitter of size n would be an Edge[n][1]
     * A duplicate splitter of size n would be an Edge[1][n]
     */
    private InterSliceEdge[][] dests;
    /** the weights for init if this node requires a different init splitting pattern */
    private int[] initWeights;
    /** the dest array for init if this node requires a differnt init splitting pattern */
    private InterSliceEdge[][] initDests; 
        /** unique identifier for this node */
    private String ident;
    /** used to generate unique id */
    private static int unique = 0;
    /** used to initialize the weights array */
    private static int[] EMPTY_WEIGHTS = new int[0];
    /** used to initialize the weights array */
    private static InterSliceEdge[][] EMPTY_DESTS = new InterSliceEdge[0][0];

    
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
        set(weights, dests, SchedulingPhase.STEADY);
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

    /**
     * Return true if this output node has a different schedule for the initialization 
     * stage.  This means initWeights and initDests are not null.  Otherwise, return false
     * meaning the init stages is the same as the steady.
     * 
     * @return 
     */
    public boolean hasInitPattern() {
        assert (initWeights == null && initDests == null) || 
            (initWeights != null && initDests != null) :
                "Something wrong with init distribution in  " + getParent() + 
                "   weights: " + initWeights + ", dests: " + initDests;
        
        return (initWeights != null);
    }
    
    
    /** Set the weights for the steady state (and for init if this
     * node does not require a different pattern for init) */
    public void setWeights(int[] newW) {
        this.weights = newW;
    }

    /** Set the weights for the init stage, this means that init will 
     * have a splitting pattern that is different from steady 
     * */
    public void setInitWeights(int[] newW) {
        this.initWeights = newW;
    }
    
    /**
     * Set the steady weights and dests of this input slice node to 
     * weights and dests.
     * 
     * @param weights List of integer weights.
     * @param dests List of Lists of Edge for splitting pattern.
     */
    public void set(LinkedList<Integer> weights, 
            LinkedList<LinkedList<InterSliceEdge>> dests, SchedulingPhase phase) {
        int[] newWeights;
        InterSliceEdge[][] newDests;
        
        if (weights.size() == 1) 
            newWeights = new int[]{1};
        else {
            newWeights = new int[weights.size()];
            for (int i = 0; i < weights.size(); i++)
                newWeights[i] = weights.get(i).intValue();
        }
        //convert the dests list
        newDests = new InterSliceEdge[dests.size()][];
        for (int i = 0; i < dests.size(); i++) 
            newDests[i] = dests.get(i).toArray(new InterSliceEdge[0]);
        
        if (SchedulingPhase.INIT == phase) {
            setInitWeights(newWeights);
            setInitDests(newDests);
        } else {
            setWeights(newWeights);
            setDests(newDests);
        }
    }
        
    /** @return the weights */
    public int[] getWeights(SchedulingPhase phase) {
        if (phase == SchedulingPhase.INIT && hasInitPattern())
            return initWeights;
        return weights;
    }

    /** 
     * @return the weights for the initialization stage, note that this may be null
     * if the splitting pattern for init is the same as steady. 
     */
    public int[] getInitWeights() {
        return initWeights;
    }
    
    /** @return whether previous filter was FileInput */
    public boolean isFileInput() {
        return ((FilterSliceNode) getPrevious()).isFileInput();
    }

    /** @return dests */
    public InterSliceEdge[][] getDests(SchedulingPhase phase) {
        if (phase == SchedulingPhase.INIT && hasInitPattern())
            return initDests;
        
        return dests;
    }

    /** Set dests */
    public void setDests(InterSliceEdge[][] dests) {
        this.dests = dests;
    }

    /** 
     * Return the initialization pattern for splitting.  Note that this may be null
     * if the pattern is the same as the steady pattern. 
     * @return dests 
     */
    public InterSliceEdge[][] getInitDests() {
        return initDests;
    }

    /** 
     * Set the initialization pattern for splitting.
     */
    public void setInitDests(InterSliceEdge[][] dests) {
        this.initDests = dests;
    }

    /** @return unique string */
    public String getIdent() {
        return ident;
    }

    /** @return total of weights */
    public int totalWeights(SchedulingPhase phase) {
        int sum = 0;
        for (int i = 0; i < getWeights(phase).length; i++)
            sum += getWeights(phase)[i];
        return sum;
    }

    /**
     * Combine the weights of adjacent outputs that have equal 
     * destinations.
     * This operation exists as a cleanup operation for synch removal.
     * Code generation for Edges may rely on {@link InputSliceNode#canonicalize()}
     * being run on all input nodes whose edges are combined by canonicalize.
     */
    public void canonicalize(SchedulingPhase phase) {
        if (getWeights(phase).length == 0)
            return;
        LinkedList<LinkedList<InterSliceEdge>> edges = new LinkedList<LinkedList<InterSliceEdge>>();
        LinkedList<Integer> newWeights = new LinkedList<Integer>();
        //add the first port to the new edges and weights
        LinkedList<InterSliceEdge> port = new LinkedList<InterSliceEdge>();
        Util.add(port, getDests(phase)[0]);
        edges.add(port);
        newWeights.add(new Integer(getWeights(phase)[0]));
        
        for (int i = 1; i < getDests(phase).length; i++) {
            if (Util.setCompare(edges.get(edges.size() - 1), getDests(phase)[i])) {
                Integer newWeight = 
                    newWeights.get(newWeights.size() - 1).intValue() + 
                    getWeights(phase)[i];
                newWeights.remove(newWeights.size() - 1);
                newWeights.add(newWeight);
            }
            else {
                //not equal, so create a new port and add it and the weight
                port = new LinkedList<InterSliceEdge>();
                Util.add(port, getDests(phase)[i]);
                edges.add(port);
                newWeights.add(new Integer(getWeights(phase)[i]));
            }
        }
        //set the new weights and the dests
        set(newWeights, edges, phase);
    }
    
    /**
     * Return the width of this splitter meaning the number
     * of connections it has to downstream slices, including 
     * all the edges of a duplicated item, counting each unique 
     * edge once.
     * 
     * @return The width of this splitter.
     */
    public int getWidth(SchedulingPhase phase) {
        return getDestSet(phase).size();
    }
    
    /**
     * Return true if the weight duplicates to edge during the scheduling phase.
     */
    public boolean weightDuplicatesTo(int weight, InterSliceEdge edge, SchedulingPhase phase) {
        InterSliceEdge[][] dests = getDests(phase);
        
        for (int d = 0; d < dests[weight].length; d++) {
            if (dests[weight][d] == edge)
                return true;
        }
        return false;
    }
    
    /**
     * Return a list of the edges with each edge appearing once
     * and ordered by the order in which each edge appears in the
     * split pattern.
     * 
     * @return The list.
     */
    public LinkedList<InterSliceEdge> getDestSequence(SchedulingPhase phase) {
        
        LinkedList<InterSliceEdge> list = new LinkedList<InterSliceEdge>();
        for (int i = 0; i < getDests(phase).length; i++) {
            for (int j = 0; j < getDests(phase)[i].length; j++) 
                if (!list.contains(getDests(phase)[i][j]))
                    list.add(getDests(phase)[i][j]);
        }
        return list;
    }
    
    /**
     * return the number of items sent by this output slice node on all instances of a particular edge.
     */
    public int getWeight(InterSliceEdge in, SchedulingPhase phase) {
        int sum = 0;

        for (int i = 0; i < getDests(phase).length; i++) {
            for (int j = 0; j < getDests(phase)[i].length; j++) {
                if (getDests(phase)[i][j] == in) {
                    sum += getWeights(phase)[i];
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
    public Set<Slice> getDestSlices(SchedulingPhase phase) {
        HashSet<Slice> dests = new HashSet<Slice>();
        
        for (InterSliceEdge edge : getDestSet(phase)) {
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
    public InterSliceEdge[] getDestList(SchedulingPhase phase) {
        
        LinkedList<InterSliceEdge> edges = new LinkedList<InterSliceEdge>();
        for (int i = 0; i < getDests(phase).length; i++) {
            for (int j = 0; j < getDests(phase)[i].length; j++)
                edges.add(getDests(phase)[i][j]);
        }
        return edges.toArray(new InterSliceEdge[edges.size()]);
    }
    
    /**
     * Return the set of the outgoing edges of this OutputSliceNode.
     * 
     * @return The set of the outgoing edges of this OutputSliceNode.
     */
    public Set<InterSliceEdge> getDestSet(SchedulingPhase phase) {
        HashSet<InterSliceEdge> set = new HashSet<InterSliceEdge>();
        for (int i = 0; i < getDests(phase).length; i++) {
            for (int j = 0; j < getDests(phase)[i].length; j++)
                set.add(getDests(phase)[i][j]);
        }
        return set;
    }

    /**
     * @return true if each output edge appears only once in the schedule of splitting
     */
    public boolean singleAppearance() {
        if (hasInitPattern() && 
                getDestSet(SchedulingPhase.INIT).size() != getDestList(SchedulingPhase.INIT).length)
            return false;
        
        return getDestSet(SchedulingPhase.STEADY).size() == getDestList(SchedulingPhase.STEADY).length;
    }
    
    
    public boolean oneOutput() {
        if (hasInitPattern() && 
                ((initWeights.length != 1 || (initDests[0].length != 1))))
            return false;
        
        return (weights.length == 1 && dests[0].length == 1);
    }

    public InterSliceEdge getSingleEdge(SchedulingPhase phase) {
        assert oneOutput() : "Calling getSingleEdge() on OutputSlice with less/more than one output";
        //System.out.println(getParent() + " " + phase);
        return getDests(phase)[0][0];
    }

    public boolean noOutputs() {
        if (hasInitPattern() && initWeights.length > 0)
            return false;
            
        return weights.length == 0;
    }
    
    public boolean isDuplicateSplitter(SchedulingPhase phase) {
        return (getWeights(phase).length == 1 && getDests(phase).length == 1 && getDests(phase)[0].length >= 2);
    }
    
    public boolean isRRSplitter(SchedulingPhase phase) {
        return (getWeights(phase).length >=2 && getDests(phase).length >= 2);
    }
    
    public boolean isSplitter(SchedulingPhase phase) {
        return (isDuplicateSplitter(phase) || isRRSplitter(phase));
    }

    /**
     * return an iterator that iterates over the inputslicenodes in descending
     * order of the number of items sent to the inputslicenode
     */
    public List<InterSliceEdge> getSortedOutputs(SchedulingPhase phase) {
        LinkedList<InterSliceEdge>sortedOutputs = new LinkedList<InterSliceEdge>();  
        // if there are no dest just return an empty iterator
        if (weights.length == 0) {
            return sortedOutputs;
        }
        // just do a simple linear insert over the dests
        // only has to be done once
        Vector<InterSliceEdge> sorted = new Vector<InterSliceEdge>();
        Iterator<InterSliceEdge> destsIt = getDestSet(phase).iterator();
        // add one element
        sorted.add(destsIt.next());
        while (destsIt.hasNext()) {
            InterSliceEdge current = destsIt.next();
            // add to end if it is less then everything
            if (getWeight(current, phase) <= getWeight(sorted.get(sorted
                    .size() - 1), phase))
                sorted.add(current);
            else { // otherwise find the correct place to add it
                for (int i = 0; i < sorted.size(); i++) {
                    // if this is the correct place to insert it,
                    // add it and break
                    if (getWeight(current, phase) > getWeight(sorted.get(i), phase)) {
                        sorted.add(i, current);
                        break;
                    }
                }
            }
        }
        assert sorted.size() == getDestSet(phase).size() : "error "
            + sorted.size() + "!= " + getDestSet(phase).size();
        sortedOutputs = (LinkedList<InterSliceEdge>)sorted.subList(0, sorted.size());

        return sortedOutputs;
    }

    public FilterSliceNode getPrevFilter() {
        return (FilterSliceNode) getPrevious();
    }

    /**
     * Return the number of items that are sent along the <edge> in <phase>.
     */
    public int itemsSentOn(InterSliceEdge edge, SchedulingPhase phase) {
        int totalItems = FilterInfo.getFilterInfo(getPrevFilter()).totalItemsSent(phase);
        
        double items = totalItems * ratio(edge, phase);
        assert items == Math.floor(items);
        return (int)(items);
    }
    
    public double ratio(InterSliceEdge edge, SchedulingPhase phase) {
        if (totalWeights(phase) == 0)
            return 0.0;
        return ((double) getWeight(edge, phase) / (double) totalWeights(phase));
    }

    public String  debugString(boolean escape) {
        return debugString(escape, SchedulingPhase.STEADY);
    }
    
    public String debugString(boolean escape, SchedulingPhase phase) {
        String newLine = "\n";
        StringBuffer buf = new StringBuffer();
        if (escape)
            newLine = "\\n";
        buf.append("***** " + this.toString() + " "+ phase + " *****" + newLine);
        for (int i = 0; i < getWeights(phase).length; i++) {
            buf.append("* Weight = " + getWeights(phase)[i] + newLine);
            
            for (int j = 0; j < getDests(phase)[i].length; j++)
                buf.append("  " + getDests(phase)[i][j] + newLine);
        }
        buf.append("**********" + newLine);
        return buf.toString();
    }


    public boolean hasFileOutput() {
        Iterator dests = getDestSet(SchedulingPhase.STEADY).iterator();
        while (dests.hasNext()) {
            if (((InterSliceEdge) dests.next()).getDest().isFileOutput())
                return true;
        }
        return false;
    }

    public Set<InputSliceNode> fileOutputs() {
        HashSet<InputSliceNode> fileOutputs = new HashSet<InputSliceNode>();
        Iterator dests = getDestSet(SchedulingPhase.STEADY).iterator();
        while (dests.hasNext()) {
            InterSliceEdge edge = (InterSliceEdge) dests.next();
            if (edge.getDest().isFileOutput())
                fileOutputs.add(edge.getDest());
        }
        return fileOutputs;
    }
    
    /**
     * Return the sum of the weights before index of the weights array.
     */
    public int weightBefore(int index, SchedulingPhase phase) {
        assert index < weights.length;
        int total = 0;
                
        for (int i = 0; i < index; i++)
            total += weights[i];
        
        return total;
    }
    
    /**
     * Return the sum of weights for edges before this edge appears in the splitting schedule.
     * This output slice node must be single appearance.
     * 
     * @param edge The edge in question
     * @return The total weights before edge
     */
    public int weightBefore(InterSliceEdge edge, SchedulingPhase phase) {
        assert singleAppearance();
        int total = 0;
        
        for (int w = 0; w < getWeights(phase).length; w++ ) {
            boolean found = false;
            //see if the edge is in this dest list
            for (int d = 0; d < getDests(phase)[w].length; d++) {
                if (getDests(phase)[w][d] == edge) {
                    found = true;
                    break;
                }
            }
            if (found) {
                return total;
            }
            total += getWeights(phase)[w];
        }
        assert false;
        return 0;
    }
    
    /*
     * public int itemsReceived(boolean init, boolean primepump) { return
     * FilterInfo.getFilterInfo(getPrevFilter()).totalItemsSent(init,
     * primepump); }
     * 
     * public int itemsSent(boolean init, boolean primepump) {
     *  }
     */

    /** THE FOLLOWING SECTION IS AUTO-GENERATED CLONING CODE - DO NOT MODIFY! */

    /** Returns a deep clone of this object. */
    public Object deepClone() {
        at.dms.kjc.slicegraph.OutputSliceNode other = new at.dms.kjc.slicegraph.OutputSliceNode();
        at.dms.kjc.AutoCloner.register(this, other);
        deepCloneInto(other);
        return other;
    }

    /** Clones all fields of this into <pre>other</pre> */
    protected void deepCloneInto(at.dms.kjc.slicegraph.OutputSliceNode other) {
        super.deepCloneInto(other);
        other.weights = this.weights;
        other.dests = this.dests;
        other.initWeights = this.initWeights;
        other.initDests = this.initDests;
        other.ident = (java.lang.String)at.dms.kjc.AutoCloner.cloneToplevel(this.ident);
    }

    /** THE PRECEDING SECTION IS AUTO-GENERATED CLONING CODE - DO NOT MODIFY! */
}
