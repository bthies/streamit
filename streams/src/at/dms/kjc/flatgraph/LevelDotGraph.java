package at.dms.kjc.flatgraph;

import java.io.FileWriter;
import java.util.HashMap;
import java.util.Set;
import java.util.Iterator;

import at.dms.kjc.sir.SIRFilter;
import at.dms.kjc.sir.SIRJoiner;
import at.dms.kjc.sir.SIRTwoStageFilter;

public class LevelDotGraph implements FlatVisitor {
    /** The dotty code as we create it */
    private StringBuffer buf;
    private LevelMap levelMap;    
    
    /**
     * Creates the dot file representing the flattened graph and stores it in
     * filename.  
     *  
     * @param toplevel The starting node of the FlatNode graph.
     * @param filename The file to write the dot graph to.
     * @param levelMap The object with the colors for the levels of the graph 
     */
    public void dumpGraph(FlatNode toplevel, String filename) 
    {
        buf = new StringBuffer();
        buf.append("digraph Flattend {\n");
        buf.append("size = \"7.5, 10\";\n");
        buf.append("ratio=compress;\n");
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
        
        assert buf!=null;
        if (node.contents instanceof SIRFilter) {
            //print the name and multiplicities and some other crap
            buf.append(node.getName() + "[style=filled,label=\"\",fillcolor="  + LevelMap.getLevelColor(node));
            buf.append("];\n");

            //create the arcs for the outgoing edges
          
            Iterator<FlatNode> dsFilters = node.downStreamFilters().iterator();
            while (dsFilters.hasNext()) {
                FlatNode ds = dsFilters.next();
          
                buf.append(node.getName() + " -> " 
                       + ds.getName() + "\n");
            }
        }
        /*
        else if (node.isSplitter()) {
            buf.append(node.getName() + "[style=filled,shape=triangle,label=\"\",color=grey");
            buf.append("];\n");
        }
        else {
            //joiner
            buf.append(node.getName() + "[style=filled,shape=invtriangle,label=\"\",color=gray");
            buf.append("];\n");
        }
        //      create the arcs for the outgoing edges
        for (int i = 0; i < node.ways; i++) {
            if (node.getEdges()[i] == null)
                continue;
            buf.append(node.getName() + " -> " 
                       + node.getEdges()[i].getName() + ";\n");
        }
        */
    }
}
