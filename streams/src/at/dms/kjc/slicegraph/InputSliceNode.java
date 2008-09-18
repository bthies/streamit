package at.dms.kjc.slicegraph;

import at.dms.util.Utils;
import java.util.HashSet;
import java.util.Set;
import java.util.LinkedList;
import at.dms.kjc.*;

/**
 * Each Slice is started by an InputSlice Node that is either a joiner connecting several other slices, 
 * or a connection to a single other slice.  There is a possibility that an input slice node could have 
 * a different schedule for the initialization stage. This is not the common case, so most methods 
 * assume there is not a separate schedule and use the single (steady and init) weights/sources.
 * 
 * Has an array of weights and corresponding {@link InterSliceEdge}s.
 */
public class InputSliceNode extends SliceNode implements at.dms.kjc.DeepCloneable {
    public static final String[] DO_NOT_CLONE_THESE_FIELDS = 
        { "weights", "sources", "initWeights", "initSources" };
    
    /** the incoming round robin weights for this input slice node for the steady and for init 
     * if the initWeights are null.
     */
    private int[] weights;
    /** the sources that correspond to the weights for the steady and for init if initWeights/initSources
     * are null
     */
    private InterSliceEdge[] sources;
    /** if this inputslicenode requires a different joiner pattern for init, this will encode the weights */
    private int[] initWeights;
    /** if this inputslicenode requires a different joiner patter for init, this will encode the sources */
    private InterSliceEdge[] initSources;
    /** used to construct a unique identifier */
    private static int unique = 0;
    /** unique identifier */
    private String ident;
    /** used if no joining is performed* */
    private static int[] EMPTY_WEIGHTS = new int[0];
    /** used if no joining is performed */
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
     * Return true if this input node has a different schedule for the initialization 
     * stage.  This mean initWeights and initSources are not null.  Otherwise, return false
     * meaning the init stages is the same as the steady.
     * 
     * @return 
     */
    public boolean hasInitPattern() {
        assert (initWeights == null && initSources == null) || 
            (initWeights != null && initSources != null); 
        return (initWeights != null);
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
     * @return true if each input edge appears only once in the schedule of joining
     */
    public boolean singleAppearance() {
        if (hasInitPattern() && 
                getSourceSet(SchedulingPhase.INIT).size() != getSourceList(SchedulingPhase.INIT).size())
            return false;
        
        return getSourceSet(SchedulingPhase.STEADY).size() == getSourceList(SchedulingPhase.STEADY).size();
    }
    
    /**
     * return the sum of the weights that appear before index in the joining schedule
     */
    public int weightBefore(int index, SchedulingPhase phase) {
        assert index < weights.length;
        int total = 0;
        
        for (int i = 0; i < index; i++) {
            total += getWeights(phase)[i];
        }
 
        return total;
    }
    
    /**
     * return the sum of the weights that appear before this edge in the joining schedule
     * 
     * @param edge the edge in question
     * 
     * @return the sum of the weights before edge
     */
    public int weightBefore(InterSliceEdge edge, SchedulingPhase phase) {
        assert singleAppearance();
        
        int total = 0;
        for (int i = 0; i < getWeights(phase).length; i++) {
            if (getSources(phase)[i] == edge) 
                return total;
            
            total += getWeights(phase)[i];
        }
        assert false;
        return 0;
    }
    
    /**
     * Return the number of unique inputs (Edges) to this join.
     * 
     * @return The width of the join.
     */
    public int getWidth(SchedulingPhase phase) {
        return getSourceSet(phase).size();
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
    public int getItems(InterSliceEdge edge, SchedulingPhase phase) {
        int items = 0;
        
        for (int i = 0; i < getSources(phase).length; i++) {
            if (getSources(phase)[i] == edge) {
                items += getWeights(phase)[i];
            }
        }
        
        return items;
    }

    /** @return array of edge weights */
    public int[] getWeights(SchedulingPhase phase) {
        if (phase == SchedulingPhase.INIT && hasInitPattern())
            return initWeights;
        
        return weights;
    }

    /** @return array of edges */
    public InterSliceEdge[] getSources(SchedulingPhase phase) {
        if (phase == SchedulingPhase.INIT && hasInitPattern())
            return initSources;
        
        return sources;
    }


    /** @return array of edge weights for the init schedule */
    public int[] getInitWeights() {
        return initWeights;
    }

    /** @return array of edges for the init schedule*/
    public InterSliceEdge[] getInitSources() {
        return initSources;
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
     * Set the initialization weights.
     * 
     * @param newWeights The new weights
     */
    public void setInitWeights(int[] newWeights) {
        if (newWeights != null && newWeights.length == 1)
            this.initWeights = new int[]{1};
        else 
            this.initWeights = newWeights;
    }
    
    /**
     * If the initialization pattern needs to be different from steady,
     * set the weights to newWeights.
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

    /**
     * If the initialization pattern needs to be different from steady,
     * set the sources to newSrcs.  (shares, does not copy)
     * 
     * @param newSrcs The new sources.
     */
    public void setInitSources(InterSliceEdge[] newSrcs) {
        this.initSources = newSrcs;
    }
    
    /** @return total weight of all edges */
    public int totalWeights(SchedulingPhase phase) {
        int sum = 0;
        for (int i = 0; i < getWeights(phase).length; i++)
            sum += getWeights(phase)[i];
        return sum;
    }

    /** @return total weight on all connections to a single Edge. 
     * @param out The Edge that we are interested in*/
    public int getWeight(Edge out, SchedulingPhase phase) {
        int sum = 0;

        for (int i = 0; i < getSources(phase).length; i++)
            if (getSources(phase)[i] == out)
                sum += getWeights(phase)[i];

        return sum;
    }

    /**
     * Does sources have a single element.
     *  
     * @return true if there is a single element in sources. */
    public boolean oneInput() {
        if (getSources(SchedulingPhase.INIT).length != 1)
            return false;
        
        return (sources.length == 1);
    }
    
    /**
     * Does sources have a single element in phase
     *  
     * @return true if there is a single element in sources. */
    public boolean oneInput(SchedulingPhase phase) {
        return getSources(phase).length == 1;
    }
    
    /** 
     * Is a joiner if there are at least 2 sources (even if same Edge object).
     * @return is a joiner.
     */
    public boolean isJoiner(SchedulingPhase phase) {
        return getSources(phase).length >= 2;
    }

    /** Get the singleton edge. 
     * Must have only one input in sources.
     * @return the edge, or throw AssertionError
     */
    public InterSliceEdge getSingleEdge(SchedulingPhase phase) {
        assert oneInput(phase) : "Calling getSingeEdge() on InputSlice with less/more than one input";
        return getSources(phase)[0];
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
        if (getSources(SchedulingPhase.INIT).length != 0)
            return false;
        
        return sources.length == 0;
    }

    /** return ratio of weight of edge to totalWeights().
     * 
     * @param edge
     * @return  0.0 if totalWeights() == 0, else ratio.
     */
    public double ratio(Edge edge, SchedulingPhase phase) {
        if (totalWeights(phase) == 0)
            return 0.0;
        return ((double) getWeight(edge, phase) / (double) totalWeights(phase));
    }

    /**
     * Return a list of the edges with each edge appearing once
     * and ordered by the order in which each edge appears in the
     * join pattern.
     * 
     * @return The list.
     */
    public LinkedList<InterSliceEdge> getSourceSequence(SchedulingPhase phase) {
        LinkedList<InterSliceEdge> list = new LinkedList<InterSliceEdge>();
        for (int i = 0; i < getSources(phase).length; i++) {
            if (!list.contains(getSources(phase)[i]))
                list.add(getSources(phase)[i]);
        }
        return list;
    }
    
    /**
     * Return a set of all the slices that are inputs to this slice.
     * 
     * @return a set of all the slices that are inputs to this slice.
     */
    public Set<Slice> getSourceSlices(SchedulingPhase phase) {
        HashSet<Slice> slices = new HashSet<Slice>();
        for (InterSliceEdge edge : getSourceList(phase)) {
            slices.add(edge.getSrc().getParent());
        }
        return slices;
    }
    
    /**
     * Return a linked list of the sources pattern.
     * 
     * @return The linked list of the sources pattern.
     */
    public LinkedList<InterSliceEdge> getSourceList(SchedulingPhase phase) {
       LinkedList<InterSliceEdge> list = new LinkedList<InterSliceEdge>();
       for (int i = 0; i < getSources(phase).length; i++)
           list.add(getSources(phase)[i]);
       return list;
    }
    
    public Set<InterSliceEdge> getSourceSet(SchedulingPhase phase) {
        HashSet<InterSliceEdge> set = new HashSet<InterSliceEdge>();
        for (int i = 0; i < getSources(phase).length; i++)
            set.add(getSources(phase)[i]);
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
    public void replaceEdge(InterSliceEdge oldEdge, InterSliceEdge newEdge, SchedulingPhase phase) {
        if (phase == SchedulingPhase.INIT && hasInitPattern()) {
            for (int i = 0; i < initSources.length; i++) {
                if (initSources[i] == oldEdge)
                    initSources[i] = newEdge;
            }

        }
        else {
            for (int i = 0; i < sources.length; i++) {
                if (sources[i] == oldEdge)
                    sources[i] = newEdge;
            }
        }
    }

    public CType getType() {
        return getNextFilter().getFilter().getInputType();
    }


    /** THE FOLLOWING SECTION IS AUTO-GENERATED CLONING CODE - DO NOT MODIFY! */

    /** Returns a deep clone of this object. */
    public Object deepClone() {
        at.dms.kjc.slicegraph.InputSliceNode other = new at.dms.kjc.slicegraph.InputSliceNode();
        at.dms.kjc.AutoCloner.register(this, other);
        deepCloneInto(other);
        return other;
    }

    /** Clones all fields of this into <pre>other</pre> */
    protected void deepCloneInto(at.dms.kjc.slicegraph.InputSliceNode other) {
        super.deepCloneInto(other);
        other.weights = this.weights;
        other.sources = this.sources;
        other.initWeights = this.initWeights;
        other.initSources = this.initSources;
        other.ident = (java.lang.String)at.dms.kjc.AutoCloner.cloneToplevel(this.ident);
    }

    /** THE PRECEDING SECTION IS AUTO-GENERATED CLONING CODE - DO NOT MODIFY! */
}
