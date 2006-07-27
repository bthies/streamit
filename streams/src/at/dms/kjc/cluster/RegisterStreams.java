

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
 * TODO: would be nice to create a NetStream only once rather than twice.
 * @author Janis
 *
 */
public class RegisterStreams {

    // input tapes coresponding to an operator
    static HashMap<SIROperator,List<NetStream>> filterInStreams = null;

    // output tapes coresponding to an operator
    static HashMap<SIROperator,List<NetStream>> filterOutStreams = null;

    /**
     * Clean up static data structures (also before using, to allocae)
     */
    
    public static void reset() {
        filterInStreams = new HashMap<SIROperator,List<NetStream>>();
        filterOutStreams = new HashMap<SIROperator,List<NetStream>>();
    }
    
    /**
     * Set up netStream objects for all non-0-weight edges and set up vectors of
     * edges (including nulls for 0-weight edges) for all incoming and outgoing edges
     * for all filters, splitters, and joiners in graph.
     *  
     * @param graphFlattener a GraphFlattener which, when visited will give us all nodes. 
     */
    public static void init(GraphFlattener graphFlattener) {
        graphFlattener.top.accept(new FlatVisitor() {

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

                if (node.incoming != null) {
                    int dest = NodeEnumerator.getNodeId(node);
                    if (dest != -1) {
                        NetStream[] incomings = new NetStream[node.incoming.length];
                        for (int i = 0; i < node.incoming.length; i++) {
                            if (node.incoming[i] == null) {
                                incomings[i] = null;
                            } else if (node.incomingWeights[i] == 0 || input_t == CStdType.Void) {
                                incomings[i] = null;  // don't track edges with no tapes.
                            } else {
                                int source = NodeEnumerator.getNodeId(node.incoming[i]);
                                assert source >= 0; // if have incoming edge, it should have a number.
                                incomings[i] = new NetStream(source, dest, input_t);
                            }
                        }
                        filterInStreams.put(node.contents,Arrays.asList(incomings));
                    }
                }

            // create a vector of output tapes

                if (node.edges != null) {
                    int source = NodeEnumerator.getNodeId(node);
                    if (source != -1) {
                        NetStream[] outgoings = new NetStream[node.edges.length];
                        for (int i = 0; i < node.edges.length; i++) {
                            if (node.edges[i] == null) {
                                outgoings[i] = null;
                            } else if (node.weights[i] == 0 || output_t == CStdType.Void) {
                                outgoings[i] = null;  // don't track edges with no tapes.
                            } else {
                                int dest = NodeEnumerator.getNodeId(node.edges[i]);
                                assert dest >= 0; // if have outgoing edge, it should have a number.
                                outgoings[i] = new NetStream(source, dest, output_t);
                            }
                        }
                        filterOutStreams.put(node.contents, Arrays.asList(outgoings));
                    }
                } 
            } } , new HashSet(), true);
    }

    /**
     * Return a NetStream that represents input tape for a filter
     * 
     * no side effects.
     * 
     * @param filter a SIRFilter
     * @return The FlatGraph input to <filter> in NetStream format, or null if no input
     * @see NetStream 
     */
    public static NetStream getFilterInStream(SIRFilter filter) {
        List<NetStream> v = filterInStreams.get(filter);
        if (v.size() == 0) return null; else return v.get(0);
    }

    /**
     * Return a NetStream that represents output tape for a filter
     * 
     * no side effects
     * 
     * @param filter a SIRFilter
     * @return The FlatGraph output of <filter> in NetStream format, or null if no output
     * @see NetStream 
     */
     public static NetStream getFilterOutStream(SIRFilter filter) {
        List<NetStream> v = filterOutStreams.get(filter);
        if (v.size() == 0) return null; else return v.get(0);
    }


    /**
     * Return a Vector containing input tapes as NetStream objects.
     * <br/>0-weight (joiner) edges are represented as null.
     * @param op a SIROperator
     * @return Vector of FlatGraph inputs to <op> in NetStream format
     * @see NetStream 
     */
    public static List<NetStream> getNodeInStreams(SIROperator op) {
    
        return filterInStreams.get(op);
    }

    /**
     * Return a Vector containing output tapes as NetStream objects
     * <br/>0-weight (splitter) edges are represented as null.
     * 
     * @param op a SIROperator
     * @return Vector of FlatGraph outputs from <op> in NetStream format
     * @see NetStream 
     */
    public static List<NetStream> getNodeOutStreams(SIROperator op) {
    
        return filterOutStreams.get(op);
    }
}


