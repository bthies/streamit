

package at.dms.kjc.cluster;

import at.dms.kjc.flatgraph.*;
import at.dms.kjc.sir.*;
import at.dms.kjc.raw.*;
import at.dms.kjc.CType;
//import java.lang.*;
import java.util.*;

/**
 * WTF?
 * 
 * Only static data structures / methods, so better not try it on more than one
 * program (fragment) at a time.
 * 
 * Since no constructor, probably need to have this visit a FlatGraph to set up data.
 * 
 * @author Janis
 *
 */
public class RegisterStreams implements FlatVisitor {

    static HashMap<SIROperator,Vector<NetStream>> filterInStreams = 
        new HashMap<SIROperator,Vector<NetStream>>(); 
    // SIROperator -> Vector of NetStream

    static HashMap<SIROperator,Vector<NetStream>> filterOutStreams = 
        new HashMap<SIROperator,Vector<NetStream>>();
    // SIROperator -> Vector of NetStream


    /**
     * Sets up some internal data static structures for use by static methods in this class.
     *
     * (Would normally not call visitnode from outside, but no other way to set up.)
     */
    public void visitNode(FlatNode node) {

        /*
          System.out.print("RegisterStreams: Visiting node name:" + 
          node.getName() + 
          " getNodeid:"+
          NodeEnumerator.getNodeId(node)+
          "\n");
        */
    
        CType input_t = null, output_t = null;

        SIROperator operator = node.contents;

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
     * WTF?
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
     * WTF?
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
     * WTF?
     * 
     * @param op a SIROperator
     * @return Vector of FlatGraph inputs to <op> in NetStream format
     * @see NetStream 
     */
    public static Vector<NetStream> getNodeInStreams(SIROperator op) {
    
        return filterInStreams.get(op);
    }

    /**
     * WTF?
     * 
     * @param op a SIROperator
     * @return Vector of FlatGraph outputs from <op> in NetStream format
     * @see NetStream 
     */
    public static Vector<NetStream> getNodeOutStreams(SIROperator op) {
    
        return filterOutStreams.get(op);
    }
}


