

package at.dms.kjc.cluster;

import at.dms.kjc.flatgraph.*;
import at.dms.kjc.sir.*;
import at.dms.kjc.CType;
import at.dms.kjc.CStdType;
import at.dms.kjc.common.CommonUtils;
import java.util.*;

/**
 * Constructs a list of input and output tapes for each stream operator.
 * Stores this information in static fields and provides static access methods.
 * 
 * Needs to be run after NodeEnumerator, 
 * after ClusterFusion.fusedWith data is set up,
 * and after StaticSubgraph data is set up.
 * 
 * TODO: would be nice to create a Tape only once rather than twice.
 * @author Janis
 *
 */
public class RegisterStreams {

    // input tapes coresponding to an operator
    static HashMap<SIROperator,List<Tape>> filterInStreams = null;

    // output tapes coresponding to an operator
    static HashMap<SIROperator,List<Tape>> filterOutStreams = null;

    /**
     * Clean up static data structures (also before using, to allocate)
     */
    
    public static void reset() {
        filterInStreams = new HashMap<SIROperator,List<Tape>>();
        filterOutStreams = new HashMap<SIROperator,List<Tape>>();
    }
    
    /**
     * Set up Tape objects for all non-0-weight edges and set up vectors of
     * edges (including nulls for 0-weight edges) for all incoming and outgoing edges
     * for all filters, splitters, and joiners in graph.
     *  
     * @param first node in a flatgraph
     */
    public static void init(FlatNode top) {
        top.accept(new FlatVisitor() {

            public void visitNode(FlatNode node) {

                CType input_t = null, output_t = null;

                SIROperator operator = node.contents;

            // get the input and output types of the operator

                try {
                    if (operator instanceof SIRJoiner) {
                        input_t = CommonUtils.getBaseType(CommonUtils.getJoinerType(node));
                        output_t = input_t;
                    }
                } catch (Exception ex) {}

                try {
                    if (operator instanceof SIRSplitter) {
                        input_t = CommonUtils.getBaseType(CommonUtils.getOutputType(node));
                        output_t = input_t;
                    }
                } catch (Exception ex) {}

                if (operator instanceof SIRStream) {
                    SIRStream stream = (SIRStream)operator;
                    input_t = stream.getInputType();
                    output_t = stream.getOutputType();
                }

            // create a vector of input tapes

                int dest = NodeEnumerator.getFlatNodeId(node);
                if (node.incoming != null && node.incoming.length > 0
                        && dest != -1) {
                    Tape[] incomings = new Tape[node.incoming.length];
                    for (int i = 0; i < node.incoming.length; i++) {
                        // don't track edges with no tapes
                        if (node.incoming[i] == null
                                || node.incomingWeights[i] == 0
                                || input_t == CStdType.Void) {
                            //System.err.print(node.contents.toString() +  " given null edge [" + i + "] because ");
                            //if (node.incoming[i] == null) System.err.println("incoming == null");
                            //else if (node.incomingWeights[i] == 0) System.err.println("weight == 0");
                            //else if (input_t == CStdType.Void) System.err.println("void");
                            incomings[i] = null;
                        } else {
                            int source = NodeEnumerator
                                    .getFlatNodeId(node.incoming[i]);
                            assert source >= 0; // if have incoming edge, it
                                                // should have a number.
                            incomings[i] = TapeBase.newTape(source, dest, input_t);
                        }
                    }
                    filterInStreams
                            .put(node.contents, Arrays.asList(incomings));
                } else {
                    List<Tape> l = Collections.emptyList();
                    filterInStreams.put(node.contents, l);
                }

            // create a vector of output tapes

                int source = NodeEnumerator.getFlatNodeId(node);
                if (node.edges != null && node.edges.length > 0 && source != -1) {

                    Tape[] outgoings = new Tape[node.edges.length];
                    for (int i = 0; i < node.edges.length; i++) {
                        // don't track edges with no tapes.
                        if (node.edges[i] == null || node.weights[i] == 0
                                || output_t == CStdType.Void) {
                            outgoings[i] = null;
                        } else {
                            int ndest = NodeEnumerator.getFlatNodeId(node.edges[i]);
                            assert ndest >= 0; // if have outgoing edge, it
                                                // should have a number.
                            outgoings[i] = TapeBase.newTape(source, ndest, output_t);
                        }
                    }
                    filterOutStreams.put(node.contents, Arrays
                            .asList(outgoings));
                } else {
                    List<Tape> l = Collections.emptyList();
                    filterOutStreams.put(node.contents, l);
                }
            } } , new HashSet(), true);
    }

    /**
     * Return a Tape that represents input tape for a filter
     * 
     * no side effects.
     * 
     * @param filter a SIROperator with at most one input tape
     * @return The FlatGraph input to <filter> in Tape format, or null if no input
     * @see Tape 
     */
    public static Tape getFilterInStream(SIROperator filter) {
        List<Tape> v = filterInStreams.get(filter);
        assert v.size() <= 1 ;
        if (v.size() == 0) return null; 
        return v.get(0);
    }

    /**
     * Return a Tape that represents output tape for a filter
     * 
     * no side effects
     * 
     * @param filter a SIROperator with at most onr output tape
     * @return The FlatGraph output of <filter> in Tape format, or null if no output
     * @see Tape 
     */
     public static Tape getFilterOutStream(SIROperator filter) {
        List<Tape> v = filterOutStreams.get(filter);
        assert v.size() <= 1;
        if (v.size() == 0) return null; 
        return v.get(0);
    }


    /**
     * Return a Vector containing input tapes as Tape objects.
     * <br/>0-weight (joiner) edges are represented as null.
     * @param op a SIROperator
     * @return List of inputs to <op> in Tape format, may return empty list but never null.
     * @see Tape 
     */
    public static List<Tape> getNodeInStreams(SIROperator op) {
    
        return filterInStreams.get(op);
    }

    /**
     * Return a Vector containing output tapes as Tape objects
     * <br/>0-weight (splitter) edges are represented as null.
     * 
     * @param op a SIROperator
     * @return List of outputs from <op> in Tape format, may return empty list but never null.
     * @see Tape 
     */
    public static List<Tape> getNodeOutStreams(SIROperator op) {
    
        return filterOutStreams.get(op);
    }
}


