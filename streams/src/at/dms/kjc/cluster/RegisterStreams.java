

package at.dms.kjc.cluster;

import at.dms.kjc.flatgraph.*;
import at.dms.kjc.sir.*;
import at.dms.kjc.raw.*;
import at.dms.kjc.CType;
//import java.lang.*;
import java.util.*;

/**
 * Constructs a list of input and output tapes for each stream operator.
 * Stores this information in static fields and provides static access methods.
 * 
 * @author Janis
 *
 */
public class RegisterStreams implements FlatVisitor {

    static HashMap<SIROperator,Vector<NetStream>> filterInStreams = 
        new HashMap<SIROperator,Vector<NetStream>>(); 
    // input tapes coresponding to an operator

    static HashMap<SIROperator,Vector<NetStream>> filterOutStreams = 
        new HashMap<SIROperator,Vector<NetStream>>();
    // output tapes coresponding to an operator

    /**
     * Sets up some internal data static structures for use by static methods in this class.
     * Must create instance of RegisterStreams and pass it to the top level stream.
     *
     */
    public void visitNode(FlatNode node) {

	// visit a flat node that coresponds to an operator

        /*
          System.out.print("RegisterStreams: Visiting node name:" + 
          node.getName() + 
          " getNodeid:"+
          NodeEnumerator.getNodeId(node)+
          "\n");
        */
    
        CType input_t = null, output_t = null;

        SIROperator operator = node.contents;

	// get the input and output types of the operator

        try {
            if (operator instanceof SIRJoiner) {
                input_t = Util.getBaseType(Util.getJoinerType(node));
                output_t = Util.getBaseType(Util.getJoinerType(node));
            }
        } catch (Exception ex) {}

        try {
            if (operator instanceof SIRSplitter) {
                input_t = Util.getBaseType(Util.getOutputType(node));
                output_t = Util.getBaseType(Util.getOutputType(node));
            }
        } catch (Exception ex) {}

        if (operator instanceof SIRStream) {
            SIRStream stream = (SIRStream)operator;
            input_t = stream.getInputType();
            output_t = stream.getOutputType();
        }

	// create a vector of input tapes

        Vector<NetStream> v = new Vector<NetStream>();
        int i;

        if (node.incoming != null) {

            int dest = NodeEnumerator.getNodeId(node);
            for (i = 0; i < node.incoming.length; i++) {
        
                if (node.incoming[i] != null) {
            
                    int source = NodeEnumerator.getNodeId(node.incoming[i]);
            
                    if (source != -1 && dest != -1) {
                        v.add(new NetStream(source, dest, input_t));
            
                    }   
                }
            }
        }

        filterInStreams.put(node.contents, v);

	// create a vector of output tapes

        v = new Vector<NetStream>();

        if (node.edges != null) {

            int source = NodeEnumerator.getNodeId(node);
            for (i = 0; i < node.edges.length; i++) {

                if (node.edges[i] != null) {

                    int dest = NodeEnumerator.getNodeId(node.edges[i]);

                    if (source != -1 && dest != -1) {
                        v.add(new NetStream(source, dest, output_t));
                    }
                }
            }
        }

        filterOutStreams.put(node.contents, v);
    }

    /**
     * Return a NetStream that represents input tape for a filter
     * 
     * @param filter a SIRFilter
     * @return The FlatGraph input to <filter> in NetStream format, or null if no input
     * @see NetStream 
     */
    public static NetStream getFilterInStream(SIRFilter filter) {
        Vector<NetStream> v = filterInStreams.get(filter);
        if (v.size() == 0) return null; else return v.elementAt(0);
    }

    /**
     * Return a NetStream that represents output tape for a filter
     * 
     * @param filter a SIRFilter
     * @return The FlatGraph output of <filter> in NetStream format, or null if no output
     * @see NetStream 
     */
     public static NetStream getFilterOutStream(SIRFilter filter) {
        Vector<NetStream> v = filterOutStreams.get(filter);
        if (v.size() == 0) return null; else return v.elementAt(0);
    }


    /**
     * Return a Vector containing input tapes as NetStream objects
     * 
     * @param op a SIROperator
     * @return Vector of FlatGraph inputs to <op> in NetStream format
     * @see NetStream 
     */
    public static Vector<NetStream> getNodeInStreams(SIROperator op) {
    
        return filterInStreams.get(op);
    }

    /**
     * Return a Vector containing output tapes as NetStream objects
     * 
     * @param op a SIROperator
     * @return Vector of FlatGraph outputs from <op> in NetStream format
     * @see NetStream 
     */
    public static Vector<NetStream> getNodeOutStreams(SIROperator op) {
    
        return filterOutStreams.get(op);
    }
}


