package at.dms.kjc.flatgraph;

import at.dms.kjc.*;
import at.dms.kjc.sir.*;
import at.dms.kjc.sir.lowering.*;
import at.dms.util.Utils;

import java.util.HashSet;
import java.util.HashMap;

/**
 * This class represents a node in the flattened graph.  It has incoming edges
 * with associated weights and outgoing edges with associated weights.  The edges 
 * point to other FlatNodes.  
 * 
 * A FlatNode is unlike a StreamIt filter; each node
 * can have multiple input and output. However, this feature is current not 
 * utilized because {@link GraphFlattener} just converts each filter/splitter/joiner
 * into a FlatNode, so that filter FlatNodes are single input/single output, splitter
 * FlatNodes are single input/multiple output.  The spacedynamic backend and the raw
 * backend rely on this translation of the StreamIt Graph. 
 * 
 * For nodes with multiple output, duplicate round-robins have 1 as the output 
 * weight for all of their output arcs.  One must use the type of the splitter in
 * contents to see what type of splitter is used.  @see #contents  The same goes for
 * the types of joiners although there are only round-robin joiners supported in StreamIt
 * currently.  
 *
 * @author mgordon 
 */
public class FlatNode {

    /** The operator this node contains (either a splitter, joiner, or filter) */
    public SIROperator contents;
    /** The outgoing arcs of this FlatNode */
    private FlatNode[] edges;
    /** The incoming edges of this FlatNode */
    public FlatNode[] incoming;
    /** The round robin weights of each incoming arc */
    public int[] incomingWeights;
    /** The weights of each outgoing arc */
    public int[] weights;
    /** The number of inputs (number of input arcs */
    public int inputs;
    /** The number of outputs (number of output arcs */
    public int ways;
    /** the current outgoing edge we are connecting, all edges before this are connected,
     * used during construction */
    public int currentEdge;
    /** the current inedges we are connecting, all edges before this are connected,
     * used during construction */
    public int currentIncoming;
    /** Used by synch removal to calcuate the new multiplicity of this node */
    public int schedMult;
    /** Used by synch removal to calcuate the new multiplicity of this node */
    public int schedDivider;
    /** Used to give each FlatNode a unique id */
    private static int uin = 0;
    /** The unique id of this FlatNode */
    private int label;
    /** Used by synch removal to remember the old sir Operator */
    public SIROperator oldContents;

    /**
     * Remove all downstream edges this node.
     */
    public void removeEdges() {
        setEdges(new FlatNode[0]);
        ways = 0;
        weights = new int[0];
        currentEdge = 0;
    }

    
    /**
     * Remove all incoming edges at this node, but this 
     * does not remove the associated edges from the source nodes. 
     */
    public void removeIncoming() {
        incoming = new FlatNode[0];
        inputs = 0;
        incomingWeights = new int[0];
        currentIncoming = 0;
    }

    /**
     * Remove the outgoing edge to the Flatnode to.  This removes 
     * both the edge and its associated weight.
     *  
     * @param to The FlatNode whose edge we want to remove.
     */
    public void removeForwardEdge(FlatNode to) {
        assert getEdges().length == ways && getEdges().length == weights.length
        && getEdges().length > 0;
        
        //the new versions of the outgoing edges and the associated weights
        FlatNode[] newEdges = new FlatNode[getEdges().length - 1];
        int[] newWeights = new int[getEdges().length - 1];

        boolean found = false;
        int j = 0;

        //iterate over all the edges and store the ones we don't want
        //to remove in the new arrays
        for (int i = 0; i < getEdges().length; i++) {
            if (to != getEdges()[i]) {
                newEdges[j] = getEdges()[i];
                newWeights[j++] = weights[i];
            } else
                found = true;
        }

        assert found : "Trying to remove an outgoing edge that does not exist.";
        
        setEdges(newEdges);
        weights = newWeights;
        ways--;
        currentEdge--;
    }

    /**
     * Return the ratio of itms that gets split to the node out.
     * 
     * @param out An outgoing node.
     * @return The ratio.
     */
    public double splittingRatio(FlatNode out) {
        return ((double)getWeight(out)) / ((double) getTotalOutgoingWeights());
    }
    
    /**
     * Return the ratio items that gets joined from the node in.
     *  
     * @param in An incoming node.
     * @return The ratio.
     */
    public double joiningRatio(FlatNode in) {
        return ((double)getIncomingWeight(in)) / ((double)getTotalIncomingWeights()); 
    }
    
    /**
     * Remove the incoming edge to this Flatnode from from.  This removes 
     * both the edge and its associated weight.
     *  
     * @param from The FlatNode whose edge we want to remove.
     */
    public void removeBackEdge(FlatNode from) {
        assert incomingWeights.length == incoming.length && incoming.length > 0 : this
            + " " + incomingWeights.length + ", " + incoming.length;

        //the new incoming edges and the new incoming weights
        FlatNode[] newIncoming = new FlatNode[incoming.length - 1];
        int[] newIncomingWeights = new int[incomingWeights.length - 1];
        boolean found = false;
        int j = 0;

        //iterate over the incoming edges and weights
        //keep all execpt if the incoming source == from
        for (int i = 0; i < incomingWeights.length; i++) {
            if (from != incoming[i]) {
                newIncoming[j] = incoming[i];
                newIncomingWeights[j] = incomingWeights[i];
                j++;
            } else
                found = true;
        }

        assert found : "Trying to remove incoming edge that does not exist!";

        incoming = newIncoming;
        incomingWeights = newIncomingWeights;
        inputs--;
        currentIncoming--;
    }

    /**
     * Create a new FlatNode with op as the underlying SIROperator.
     * The incoming and outgoing edge structures are empty  for 
     * splitters and joiners they are declared to be the correct size the weights
     * arrays (for incoming and outgoing edges) are set from the underlying splitter
     * or joiner.  The connections between nodes must be explicitedly set.
     * 
     * For filters everything is of size 0. 
     * 
     * @param op The SIROperator that this FlatNode represents.
     */
    public FlatNode(SIROperator op) {
        assert op != null : "Cannot create FlatNode with null SIROperator.";
        
        contents = op;
        currentEdge = 0;
        currentIncoming = 0;
        if (op instanceof SIRFilter) {
            //create a filter with no connections,
            //we wait to allocate the incoming and outgoing structures
            //until we create the edges at this node
            ways = 0;
            inputs = 0;
            incoming = new FlatNode[0];
            setEdges(new FlatNode[0]);
            // edges[0] = null;
        }

        if (op instanceof SIRJoiner) {
            //create a node with no outgoing edges
            //but with incoming weights equal to the underlying
            //joiner node.  
            SIRJoiner joiner = (SIRJoiner) op;
            ways = 0;
            inputs = joiner.getWays();
            incoming = new FlatNode[inputs];
            incomingWeights = joiner.getWeights();
            setEdges(new FlatNode[0]);
            // edges[0] = null;
        }
        if (op instanceof SIRSplitter) {
            //create a node with no incoming edges
            //but with outgoing weights equal to the underlying
            //splitter weights...
            SIRSplitter splitter = (SIRSplitter) op;
            ways = splitter.getWays();
            setEdges(new FlatNode[ways]);
            weights = splitter.getWeights();
            inputs = 0;
            incoming = new FlatNode[0];
        }
        //the label of the node
        label = uin++;
    }

    /**
     * Add a outgoing edge from this node to to.
     * 
     * Note: you must add to nodes in the order specified by the
     * underlying SIROperator. 
     * 
     * @param to The node to connect to.
     */
    public void addEdges(FlatNode to) {
        // do not connect to oneself
        if (!(this.equals(to))) {
            this.addEdgeTo(to);
            to.addIncomingFrom(this);
        }
    }

    /**
     * Add an edge between from to to.  So we add an
     * edge at from to to, and at to that is from from.  
     *
     * Note: you must add to nodes in the order specified by the
     * underlying SIROperator. 
     *
     * @param from The source node
     * @param to The dest node
     */
    public static void addEdges(FlatNode from, FlatNode to) {
        if (from != null) {
            from.addEdgeTo(to);
        }
        if (to != null)
            to.addIncomingFrom(from);
    }
 
    /**
     * Add an edge at this FlatNode to to. 
     *  
     * Note: you must add to nodes in the order specified by the
     * underlying SIROperator. 
     *
     * @param to The new edge's destination.
     */
    public void addEdgeTo(FlatNode to) {
        // create the edge and weight arrays only if this node is connected
        // to something
        
        //if we have not expected to create an edge, then alloc the 
        //outgoing structures
        if (ways == 0) {
            ways = 1;
            setEdges(new FlatNode[ways]);
            weights = new int[1];
            weights[0] = 1;
        }

        getEdges()[currentEdge++] = to;
    }

    /**
     * Add an incoming edge at this node from from.
     * 
     * Note: you must add to nodes in the order specified by the
     * underlying SIROperator. 
     * 
     * @param from The source of the new edge.
     */
    public void addIncomingFrom(FlatNode from) {
        if (inputs == 0) {
            inputs = 1;
            incoming = new FlatNode[1];
            incomingWeights = new int[1];
            incomingWeights[0] = 1;
        }

        incoming[currentIncoming++] = from;
    }

    /**
     * This function is called by {@link GraphFlattener} 
     * after {@link GraphFlattener#createGraph} is called. It
     * is called for each splitter of a feedback loop. {@link GraphFlattener#createGraph} 
     * connects the outgoing edges of the splitter of a feedback in the reverse order and
     * this swaps them.
     */
    public void swapSplitterEdges() {
        if (!(contents instanceof SIRSplitter)
            || !(contents.getParent() instanceof SIRFeedbackLoop))
            Utils.fail("We do not want to swap the edges on non-splitter");
        
        if (getEdges().length != 2)
            return;

        // The weights are correct and do not need to be swapped

        FlatNode temp = getEdges()[0];
        getEdges()[0] = getEdges()[1];
        getEdges()[1] = temp;
    }

    /**
     * Accept a FlatVisitor v, that will visit this node.  
     * 
     * To visit, we call v's visitNode() method, and then we 
     * call the visitor on each of our downstream (outgoing) edges.
     *   
     * Since this graph can have loops, we have to keep track
     * of what we visited already.  We keep the nodes that we 
     * have already visited in set and we will not visit a 
     * node of set.
     * 
     * @param v The FlatVisitor to call.
     * @param set The FlatNodes we have already visited.
     * @param reset If true, reset set to be empty.
     */
    public void accept(FlatVisitor v, HashSet<FlatNode> set, boolean reset) {
        if (reset)
            set = new HashSet<FlatNode>();

        set.add(this);
        v.visitNode(this);
        for (int i = 0; i < ways; i++) {
            if (getEdges()[i] == null)
                continue;
            if (!set.contains(getEdges()[i]))
                getEdges()[i].accept(v, set, false);
        }
    }

    /**
     * Now uses {@link FlatNode#uin} to implement deterministic hashcode system.
     * It has the added
     * benefit that a FlatNode's identity isn't tied to it's inputs, ways, etc
     * not changing ie now hashcode is synched with equals() and equality
     * doesn't change just because ones ways, etc changes
     */
    public int hashCode() {
        return label;
    }

    /**
     * Return the name of the underlying SIROperator with the
     * FlatNode's unique ID appended.
     * 
     * @return The name of the underlying SIROperator with the
     * FlatNode's unique ID appended.
     */
    public String getName() {
        // if((contents instanceof SIRIdentity)||(contents instanceof
        // SIRJoiner)) {
        /*
         * String out=(String)identMap.get(contents); if(out==null) {
         * out=contents.getName()+"_"+(nameInt++); identMap.put(contents,out);
         * return out; } else return out;
         */
        return contents.getName() + "_" + label;
        // } else
        // return contents.getName();
    }

    /**
     * Return True if the the underlying SIROperator is a 
     * SIRFilter.
     * 
     * @return True if the the underlying SIROperator is a 
     * SIRFilter.
     */
    public boolean isFilter() {
        if (contents instanceof SIRFilter)
            return true;
        return false;
    }
    
    /**
     * Return True if the the underlying SIROperator is a 
     * SIRFilter and could be at the top of a program
     * 
     * @return True if the the underlying SIROperator is a 
     * SIRFilter and the filter neither peeks nor pops.
     */
    public boolean isTopFilter() {
        if (! ( contents instanceof SIRFilter)) {
            return false;
        }
        SIRFilter filter = (SIRFilter)contents;
        return filter.getPopInt() == 0 && filter.getPeekInt() == 0;
    }

    /**
     * Return True if the underlying SIROperator is an SIRJoiner 
     * 
     * @return True if the underlying SIROperator is an SIRJoiner 
     */
    public boolean isJoiner() {
        if (contents instanceof SIRJoiner)
            return true;
        return false;
    }

    /**
     * Return True if the underlying SIROperator is an SIRSplitter 
     * @return True if the underlying SIROperator is an SIRSplitter 
     */
    public boolean isSplitter() {
        if (contents instanceof SIRSplitter)
            return true;
        return false;
    }

    /**
     * Return True if the underlying SIROperator is an SIRSplitter that 
     * is a duplicate splitter.
     * 
     * @return True if the underlying SIROperator is an SIRSplitter that 
     * is a duplicate splitter.
     */
    public boolean isDuplicateSplitter() {
        if (contents instanceof SIRSplitter
            && ((SIRSplitter) contents).getType() == SIRSplitType.DUPLICATE)
            return true;
        return false;
    }

    /**
     * Return true if this flat node is a null splitter.
     * 
     * @return true if this flat node is a null splitter.
     */
    public boolean isNullSplitter() {
        if (isSplitter() && getTotalOutgoingWeights() == 0) {
            assert getTotalIncomingWeights() == 0;
            return true;
        }
        return false;
    }
    
    public boolean isNullJoiner() {
        if (isJoiner() && getTotalIncomingWeights() == 0) {
            assert getTotalOutgoingWeights() == 0;
            return true;
        }
        return false;
    }
    
    /**
     * Return True if the underlying SIROperator is a joiner and that joiner
     * is directly contained in an SIRFeedbackLoop.
     * 
     * @return True if the underlying SIROperator is a joiner and that joiner
     * is directly contained in an SIRFeedbackLoop.
     */
    public boolean isFeedbackJoiner() {
        if (contents instanceof SIRJoiner
            && contents.getParent() instanceof SIRFeedbackLoop) {
            // oddly enough, a feedback joiner has a with no incoming edge from
            // the outside has incoming[0] = null, rather than having only one
            // incoming edge.
            assert inputs == 2 : "Feedback Joiner without 2 inputs in flat graph";
            return true;
        }

        return false;
    }

    /**
     * Return True if the underlying SIROperator is a joiner and that joiner
     * is directly contained in an SIRFeedbackLoop and the incoming edge at i is the
     * feed-back path.
     * 
     * @param int i: the edge number.
     * @return True if the underlying SIROperator is a joiner and that joiner
     * is directly contained in an SIRFeedbackLoop and the incoming edge at i is the
     * feed-back path.  
     */
    public boolean isFeedbackIncomingEdge(int i) {
        // oddly enough, a feedback joiner has a with no incoming edge from
        // the outside has incoming[0] = null, rather than having only one
        // incoming edge.
        return isFeedbackJoiner() && i == 1;
    }

    /**
     * @see FlatNode#getName
     */
    public String toString() {
        return "FlatNode:" + getName();
    }
 
    /**
     * Return the sum of the weights on the incoming edges.
     * @return The sum of the weights on the incoming edges.
     */
    public int getTotalIncomingWeights() {
        int sum = 0;

        for (int i = 0; i < inputs; i++)
            sum += incomingWeights[i];
        return sum;
    }

    /**
     * Return the sum of the weights of the outgoing edges.
     * @return The sum of the weights of the outgoing edges.
     */
    public int getTotalOutgoingWeights() {
        int sum = 0;

        for (int i = 0; i < ways; i++)
            sum += weights[i];

        return sum;
    }

    /**
     * Get partial sum of outgoing weights 0 thru i - 1.
     * @return partial sum of outgoing weights 0 thru i - 1.
     */
    public int getPartialOutgoingSum(int i) {
        assert i >= 0 && i < ways;

        int sum = 0;

        for (int q = 0; q < i; q++)
            sum += weights[q];

        return sum;
    }

    /**
     * Get the partial sum of incoming weight 0 thru i - 1.
     * @return the partial sum of incoming weight 0 thru i - 1.
     */
    public int getPartialIncomingSum(int i) {
        assert i >= 0 && i < inputs;

        int sum = 0;

        for (int j = 0; j < i; j++)
            sum += incomingWeights[j];

        return sum;
    }
 
    /**
     * return The incoming weight that is associated with the incoming
     * edge that connects from prev.
     * 
     * 
     * @param prev The source we are interested it. 
     * 
     * @return The incoming weight that is associated with the incoming
     * edge that connects from prev.
     */
    public int getIncomingWeight(FlatNode prev) {
        for (int i = 0; i < inputs; i++) {
            if (incoming[i] == prev)
                return incomingWeights[i];
        }
        assert false : "Node " + prev + " not connected to " + this;
        return -1;
    }

    /**
     * Return the outgoing weight that is associated with 
     * the outgoing edge that points to to.
     * 
     * @param to
     * @return The outgoing weight that is associated with 
     * the outgoing edge that points to to.
     */
    public int getWeight(FlatNode to) {
        for (int i = 0; i < ways; i++) {
            if (getEdges()[i] == to)
                return weights[i];
        }
        assert false : "Node " + this + " not connected to " + to;
        return -1;
    }

    /**
     * Return the index into the outgoing weights and edges structures
     * that holds the edge to to.
     * 
     * @param to The dest of the edge.
     * @return the index into the outgoing weights and edges structure
     * that holds the edge to to.
     */
    public int getWay(FlatNode to) {
        for (int i = 0; i < ways; i++) {
            if (getEdges()[i] == to)
                return i;
        }
        assert false : "Node " + this + " not connected to " + to;
        return -1;
    }

    /**
     * Return the index into the incoming weights and edges structures
     * that holds the edge with source prev.
     * 
     * @param prev The source of the edge.
     * @return the index into the incoming weights and edges structures
     * that holds the edge with source prev.
     */
    public int getIncomingWay(FlatNode prev) {
        for (int i = 0; i < inputs; i++) {
            if (incoming[i] == prev)
                return i;
        }
        assert false : "Node " + prev + " not connected to " + this;
        return -1;
    }

    /**
     * If underlying SIROperator is an SIRFilter, return the SIRFilter, 
     * otherwise throw an exception.
     *  
     * @return The underlying SIRFilter.
     */
    public SIRFilter getFilter() {
        assert isFilter();
        return (SIRFilter) contents;
    }
    
    /**
     * Return the number of items pushed from *from* to *to*
     * on each iteration of *from*.
     * If *from* is a splitter take this into account.
     * @param from : the node pushing the items
     * @param to : the node receiving the pushed items
     * @return the number of items pushed.
     */
    public static int getItemsPushed(FlatNode from, FlatNode to) {
        if (from.isFilter())
            return ((SIRFilter) from.contents).getPushInt();
        else if (from.isJoiner())
            return from.getTotalIncomingWeights();
        else if (from.isSplitter())
            return from.getWeight(to);
        assert false : "Invalid FlatNode type" + from.toString();
        return -1;
    }


    public void setEdges(FlatNode[] edges) {
        this.edges = edges;
    }


    public FlatNode[] getEdges() {
        return edges;
    }

}
