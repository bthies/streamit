package at.dms.kjc.raw;

import at.dms.kjc.*;
import at.dms.kjc.sir.*;
import at.dms.util.Utils;
import java.util.List;
import java.util.ListIterator;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.TreeSet;
import java.util.HashSet;
import java.io.*;

/**
 * This class dumps the tile code for each filter into a file based 
 * on the tile number assigned 
 */
public class TileCode extends at.dms.util.Utils implements FlatVisitor {
    public static HashSet filters;
    public static HashSet tiles;
    
    public static void generateCode(FlatNode topLevel) 
    {
	//create a set containing all the coordinates of all
	//the nodes in the FlatGraph plus all the tiles involved
	//in switching
	//generate the code for all tiles containing filters
	filters = new HashSet();
	topLevel.accept(new TileCode(), new HashSet(), true);
	tiles = new HashSet();
	tiles.addAll(Simulator.initSchedules.keySet());
	tiles.addAll(Simulator.steadySchedules.keySet());
	
	Iterator tileIterator = tiles.iterator();
	while(tileIterator.hasNext()) {
	    Coordinate tile = (Coordinate)tileIterator.next();
	    //do not generate code for this tile
	    //if it contains a filter
	    //we have already generated the code in the visitor
	    if (filters.contains(tile))
		continue;
	    noFilterCode(tile);
	}
    }
    
    private static void noFilterCode(Coordinate tile) 
    {
	try {
	    FileWriter fw = 
		new FileWriter("tile" + Layout.getTileNumber(tile) 
			       + ".c");
	    
	    //Entry point of the visitor
	    fw.write("#include <raw.h>\n\n");
	    
	    //write the extern for the function to init the 
	    //switch
	    fw.write("void raw_init();\n\n");
	    fw.write("void begin(void) {\n");
	    fw.write("  raw_init();\n");
	    fw.write("}\n");
	    fw.close();
	}
	catch (Exception e) {
	    Utils.fail("Error writing switch code for tile " +
		       Layout.getTileNumber(tile));
	}
    }
    
        
    //generate the code for the tiles containing filters
    //remember which tiles we have generated code for
    public void visitNode(FlatNode node) 
    {
	if (node.contents instanceof SIRFilter) {
	    filters.add(Layout.getTile(node.contents));
	    FlatIRToC.generateCode(node);
	}
    }
}

