package at.dms.kjc.spacetime;

import java.io.FileWriter;
import java.util.Vector;

public class LayoutDot 
{
    public static void dumpLayout(RawChip rawChip, String fileName) 
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
		    for (int t = 0; t < tile.getFilters(true, false).size(); t++) {
			FilterInfo fi = 
			    FilterInfo.getFilterInfo((FilterTraceNode)tile.getFilters(true, false).get(t));
			
			fw.write(fi.filter.toString() + "(" + 
				 fi.initMult + ")\\n");
		    }
		    
		    fw.write("Prime Pump:\\n");
		    for (int t = 0; t < tile.getFilters(false, true).size(); t++) {
			FilterInfo fi = 
			    FilterInfo.getFilterInfo((FilterTraceNode)tile.getFilters(false, true).get(t));
			
			fw.write(fi.filter.toString() + "(" + 
				 fi.primePump + ")\\n");
		    }
		    
		    fw.write("Steady:\\n");
		    for (int t = 0; t < tile.getFilters(false, false).size(); t++) {
			FilterInfo fi = 
			    FilterInfo.getFilterInfo((FilterTraceNode)tile.getFilters(false, false).get(t));
			
			fw.write(fi.filter.toString() + "(" + 
				 fi.steadyMult + ")\\n");
		    }
		    fw.write("\"];\n");
		    
		    for (int c = 0; c < tile.getNeighborTiles().size(); c++) {
			fw.write("tile" + tile.getTileNumber() + " -> tile" + 
				 ((RawTile)tile.getNeighborTiles().get(c)).getTileNumber() +
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
