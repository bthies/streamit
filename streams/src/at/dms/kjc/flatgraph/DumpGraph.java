package at.dms.kjc.flatgraph;

import at.dms.kjc.sir.*;
import java.io.*;
import java.util.*;

/**
 * Dump a representation of the flat graph to a dot file to be 
 * used with dot (or dotty).
 * 
 * @author mgordon
 */
public class DumpGraph implements FlatVisitor
{
    /** The dotty code as we create it */
    private StringBuffer buf;
    /** HashMaps from SIROperator -> Integer for multiplicities */
    private HashMap<FlatNode, Integer> initMults,
        steadyMults;
    
    /**
     * Creates the dot file representing the flattened graph and stores it in
     * filename.  
     *  
     * This function must be called after 
     * {@link RawBackend#createExecutionCounts} because execution multiplicities 
     * need to be set.
     * 
     * @param toplevel The starting node of the FlatNode graph.
     * @param filename The file to write the dot graph to.
     * @param initExeCounts The multiplicities in the init stage.
     * @param steadyExeCounts The multiplicities in the steady-state stage.
     */
    public void dumpGraph(FlatNode toplevel, String filename, HashMap<FlatNode, Integer> initExeCounts,
                          HashMap<FlatNode, Integer> steadyExeCounts) 
    {
        buf = new StringBuffer();
        this.initMults = initExeCounts;
        this.steadyMults = steadyExeCounts; 
        buf.append("digraph Flattend {\n");
        buf.append("size = \"8, 10.5\";");
        toplevel.accept(this, null, true);
        buf.append("}\n");  
        try {
            FileWriter fw = new FileWriter(filename);
            fw.write(buf.toString());
            fw.close();
        }
        catch (Exception e) {
            System.err.println("Could not print flattened graph");
        }
    
    }

    /**
     * This function should not be called by the outside world.  It is used
     * by this class to visit each node of the FlatNode graph and create the dot
     * code for connectivity and for the node label. 
     * 
     * @param node The current node we are visiting.
     */
    public void visitNode(FlatNode node) 
    {
        if (node.contents instanceof SIRFilter) {
            //we are visiting a filter
            SIRFilter filter = (SIRFilter)node.contents;
            assert buf!=null;
        
            //print the name and multiplicities and some other crap
            buf.append(node.getName() + "[ label = \"" +
                       node.getName() + "\\n");
            buf.append("init Mult: " + GraphFlattener.getMult(node, true,
                                                              initMults, steadyMults) + 
                       " steady Mult: " + GraphFlattener.getMult(node, false, initMults, steadyMults));
            buf.append("\\n");
            buf.append(" peek: " + filter.getPeek() + 
                       " pop: " + filter.getPop() + 
                       " push: " + filter.getPush());
            buf.append("\\n");
            if (node.contents instanceof SIRTwoStageFilter) {
                SIRTwoStageFilter two = (SIRTwoStageFilter)node.contents;
                buf.append(" initPeek: " + two.getInitPeek() + 
                           " initPop: " + two.getInitPop() + 
                           " initPush: " + two.getInitPush());
                buf.append("\\n");
            }
            if (node.inputs != node.incoming.length) {
                buf.append("node.inputs (" + node.inputs + ") != node.incoming.length (" + 
                           node.incoming.length + ")");
                buf.append("\\n");
            }
            if (node.ways != node.getEdges().length) {
                buf.append("node.ways (" + node.ways + ") != node.edges.length (" + 
                           node.getEdges().length + ")");
            }
        
            buf.append("\"];");
        }
        
        //if we have a joiner, print the weights on the incoming edges
        if (node.contents instanceof SIRJoiner) {
            for (int i = 0; i < node.inputs; i++) {
                //joiners may have null upstream neighbors
                if (node.incoming[i] == null)
                    continue;
                buf.append(node.incoming[i].getName() + " -> " 
                           + node.getName());
                buf.append("[label=\"" + node.incomingWeights[i] + "\"];\n");
            }
        
        }
        //create the arcs for the outgoing edges
        for (int i = 0; i < node.ways; i++) {
            if (node.getEdges()[i] == null)
                continue;
            if (node.getEdges()[i].contents instanceof SIRJoiner)
                continue;
            buf.append(node.getName() + " -> " 
                       + node.getEdges()[i].getName());
            buf.append("[label=\"" + node.weights[i] + "\"];\n");
        }
    }
}

