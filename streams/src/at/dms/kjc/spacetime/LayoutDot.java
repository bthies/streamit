package at.dms.kjc.spacetime;

import java.io.FileWriter;
import java.util.Iterator;
import java.util.HashMap;

import at.dms.kjc.backendSupport.ComputeNode;
import at.dms.kjc.backendSupport.FilterInfo;
import at.dms.kjc.backendSupport.Layout;
import at.dms.kjc.slicegraph.FilterSliceNode;
import at.dms.kjc.slicegraph.SliceNode;
import at.dms.kjc.slicegraph.Util;

/**
 * This class generates a dot graph of the layout and for each tile the
 * schedule of filters that the tile runs.
 * 
 * @author mgordon
 *
 */
public class LayoutDot 
{
    /**
     * Print the final cost of the layout according to the cost function
     * of <pre>anneal</pre>.
     *  
     * @param spaceTime
     * @param layout
     */
    public static void printLayoutCost(SpaceTimeSchedule spaceTime, 
            Layout layout) {
        AnnealedLayout anneal = new AnnealedLayout(spaceTime);
        anneal.initialize();
        Iterator<SliceNode> nodes  = 
            Util.sliceNodeTraversal(spaceTime.getSlicer().getSliceGraph());
        HashMap assignment = new HashMap();
        while (nodes.hasNext()) {
            SliceNode node = nodes.next();
            if (node.isFilterSlice()) {
                ComputeNode tile = 
                    layout.getComputeNode(node.getAsFilter());
                assignment.put(node, tile);
            }
        }
        anneal.setAssignment(assignment);
        anneal.printLayoutStats();
    }
    
    /**
     * Generate the dot graph of the layout.
     * 
     * @param rawChip
     * @param fileName
     */
    public static void dumpLayout(SpaceTimeSchedule spaceTime, RawChip rawChip, String fileName) 
    {
        try {
            FileWriter fw = new FileWriter(fileName);
            fw.write("digraph LayoutDotGraph {\n");
            fw.write("size = \"8, 10.5\";\n");
            fw.write("node [shape=box];\n");
            fw.write("nodesep=.5;\nranksep=\"2.0 equally\";\n");
            for (int i = 0; i < rawChip.getYSize(); i++) {
                fw.write("{rank = same;\n");
                for (int j = 0; j < rawChip.getXSize(); j++) {
                    fw.write("tile" + rawChip.getTile(j, i).getTileNumber() + ";\n");
                }
                fw.write("}\n");
            }

            for (int i = 0; i < rawChip.getXSize(); i++) {
                for (int j = 0; j < rawChip.getYSize(); j++) {
                    RawTile tile = rawChip.getTile(i, j);
                    fw.write("tile" + tile.getTileNumber() + "[ label = \"");
                    fw.write("TILE " + tile.getTileNumber() + "(" + tile.getX() +
                             ", " + tile.getY() + ")\\n");
                    fw.write("Init:\\n");
                    for (SliceNode fs : tile.getSliceNodes(true, false)) {
                        FilterInfo fi = FilterInfo.getFilterInfo((FilterSliceNode)fs);
            
                        fw.write(fi.filter.toString() + "(" + 
                                 fi.initMult + ")\\n");
                    }
            
                    fw.write("Prime Pump:\\n");
                    for (SliceNode fs : tile.getSliceNodes(false, true)) {
                        FilterInfo fi = FilterInfo.getFilterInfo((FilterSliceNode)fs);
            
                        fw.write(fi.filter.toString() + "(" + 
                                 spaceTime.getPrimePumpTotalMult(fi) + ")\\n");
                    }
            
                    fw.write("Steady:\\n");
                    for (SliceNode fs : tile.getSliceNodes(false, false)) {
                        FilterInfo fi = FilterInfo.getFilterInfo((FilterSliceNode)fs);
            
                        fw.write(fi.filter.toString() + "(" + 
                                 fi.steadyMult + ")\\n");
                    }
                    fw.write("\"];\n");
            
                    for (int c = 0; c < tile.getNeighborTiles().size(); c++) {
                        fw.write("tile" + tile.getTileNumber() + " -> tile" + 
                                 tile.getNeighborTiles().get(c).getTileNumber() +
                                 ";\n");
                    }
            
                }
            }
        
        
            fw.write("}\n");
            fw.close();
        }
        catch (Exception e) {
        
        }
    
    }
}