package at.dms.kjc.raw;

import at.dms.kjc.*;
import at.dms.kjc.sir.*;
import at.dms.util.Utils;
import java.util.List;
import at.dms.kjc.sir.lowering.*;
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
    //Hash set of tiles mapped to filters or joiners
    //all other tiles are routing tiles
    public static HashSet realTiles;
    public static HashSet tiles;

    public static void generateCode(FlatNode topLevel) 
    {
	//create a set containing all the coordinates of all
	//the nodes in the FlatGraph plus all the tiles involved
	//in switching
	//generate the code for all tiles containing filters and joiners
	realTiles = new HashSet();
	topLevel.accept(new TileCode(), new HashSet(), true);
	tiles = new HashSet();
	tiles.addAll(Simulator.initSchedules.keySet());
	tiles.addAll(Simulator.steadySchedules.keySet());
	
	Iterator tileIterator = tiles.iterator();
	while(tileIterator.hasNext()) {
	    Coordinate tile = (Coordinate)tileIterator.next();
	    //do not generate code for this tile
	    //if it contains a filter or a joiner
	    //we have already generated the code in the visitor
	    if (realTiles.contains(tile))
		continue;
	    noFilterCode(tile);
	}
    }
    
    private static void joinerCode(FlatNode joiner) 
    {
	try {
	    FileWriter fw = 
		new FileWriter("tile" + Layout.getTileNumber(Layout.getTile(joiner)) 
			       + ".c");
	    
	    fw.write("#include <raw.h>\n");
	    fw.write("#include <math.h>\n\n");
	    fw.write(createJoinerWork(joiner));
	    if (joiner.contents.getParent() instanceof SIRFeedbackLoop)
		fw.write(createInitPath(joiner) + "\n");	    
	    //write the extern for the function to init the 
	    //switch
	    fw.write("void raw_init();\n\n");
	    fw.write("void begin(void) {\n");
	    if (joiner.contents.getParent() instanceof SIRFeedbackLoop)
		fw.write("  int i;\n\n");
	    fw.write("  raw_init();\n");
	    if (joiner.contents.getParent() instanceof SIRFeedbackLoop)
		fw.write(initPathCallCode(joiner));
	    fw.write("  work();\n");
	    fw.write("}\n");
	    fw.close();
	    System.out.println("Code for " + Namer.getName(joiner.contents) +
			       " written to tile" + Layout.getTileNumber(Layout.getTile(joiner)) +
			       ".c");
	}
	catch (Exception e) {
	    e.printStackTrace();
	    
	    Utils.fail("Error writing switch code for tile " +
		       Layout.getTileNumber(Layout.getTile(joiner)));
	}
    }
    
    private static String createInitPath(FlatNode joiner) {
	if (!(joiner.contents.getParent() instanceof SIRFeedbackLoop))
	    return "";
	
	FlatIRToC toC = new FlatIRToC();
	toC.declOnly = false;
	
	JMethodDeclaration initPath = ((SIRFeedbackLoop)joiner.contents.getParent()).getInitPath();
	initPath.accept(toC);
	return toC.getString();
    }

    private static String initPathCallCode(FlatNode joiner) {
	if (!(joiner.contents.getParent() instanceof SIRFeedbackLoop))
	    return "";
	
	StringBuffer buf = new StringBuffer();
	int delay = ((SIRFeedbackLoop)joiner.contents.getParent()).getDelay();
	JMethodDeclaration initPath = ((SIRFeedbackLoop)joiner.contents.getParent()).getInitPath();
	buf.append("\n  for (i = 0; i < " + delay + "; i++) \n");
	buf.append("    static_send(" + initPath.getName() + "(i));\n");
	return buf.toString();
    }

    private static String createJoinerWork(FlatNode joiner) 
    {
	boolean fp = false;
	StringBuffer ret = new StringBuffer();
	int buffersize = nextPow2((Integer)SimulationCounter.maxJoinerBufferSize.get(joiner),
				  joiner);
	//get the type, since this joiner is guaranteed to be connected to a filter
	CType ctype = getJoinerType(joiner);  //??
	if (ctype.equals(CStdType.Float))
	    fp = true;
	String type = ctype.toString(); 	
	ret.append("#define BUFSIZE " + buffersize + "\n\n");
	
	ret.append("void work() { \n");
		
	HashSet buffers = (HashSet)JoinerSimulator.buffers.get(joiner);
	Iterator bufIt = buffers.iterator();
	//print all the var definitions
	while (bufIt.hasNext()) {
	    String current = (String)bufIt.next();
	    ret.append("int first" + current + " = 0;\n");
	    ret.append("int last" + current + " = 0;\n");
	    ret.append(type + " buffer" + current + "[BUFSIZE];\n");
	}
	//print the init schedule
		
	JoinerScheduleNode init = (JoinerScheduleNode)Simulator.initJoinerCode.get(joiner);
	while (init != null) {
	    ret.append(init.getC(fp));
	    init = init.next;
	}
	//print the steady state schedule
	JoinerScheduleNode steady = (JoinerScheduleNode)Simulator.steadyJoinerCode.get(joiner);
	ret.append("while(1) {\n");
	while (steady != null) {
	    ret.append("  " + steady.getC(fp));
	    steady = steady.next;
	}
	ret.append("}}\n");
	return ret.toString();
	
    }
    
    private static CType getJoinerType(FlatNode joiner) 
    {
	boolean found;
	//search backward until we find the first filter
	while (!(joiner == null || joiner.contents instanceof SIRFilter)) {
	    found = false;
	    for (int i = 0; i < joiner.inputs; i++) {
		if (joiner.incomingWeights[i] > 0) {
		    joiner = joiner.incoming[i];
		    found = true;
		}
	    }
	    if (!found)
		Utils.fail("cannot find any upstream filter from " + Namer.getName(joiner.contents));
	}
	if (joiner != null) 
	    return ((SIRFilter)joiner.contents).getOutputType();
	else 
	    return CStdType.Null;
    }
    

    private static int nextPow2(Integer i, FlatNode node) 
    {
	System.out.println(Namer.getName(node.contents) + " BufferSize = " + i);
	
	//	return 1024;
	
	String str = Integer.toBinaryString(i.intValue());
	if  (str.indexOf('1') == -1)
	    return 0;
	int bit = str.length() - str.indexOf('1');
	return (int)Math.pow(2, bit);
    }
        
    private static void noFilterCode(Coordinate tile) 
    {
	try {
	    FileWriter fw = 
		new FileWriter("tile" + Layout.getTileNumber(tile) 
			       + ".c");
	    
	    fw.write("#include <raw.h>\n\n");
	    
	    //write the extern for the function to init the 
	    //switch
	    fw.write("void raw_init();\n\n");
	    fw.write("void begin(void) {\n");
	    fw.write("  raw_init();\n");
	    fw.write("}\n");
	    fw.close();
	    System.out.println("Code " +
			       " written to tile" + Layout.getTileNumber(tile) +
			       ".c");


	}
	catch (Exception e) {
	    Utils.fail("Error writing switch code for tile " +
		       Layout.getTileNumber(tile));
	}
    }
    
        
    //generate the code for the tiles containing filters and joiners
    //remember which tiles we have generated code for
    public void visitNode(FlatNode node) 
    {
	//this is a mapped joiner
	if (Layout.joiners.contains(node)) {
	    realTiles.add(Layout.getTile(node.contents));
	    joinerCode(node);
	}
	if (node.contents instanceof SIRFilter) {
	    realTiles.add(Layout.getTile(node.contents));
	    FlatIRToC.generateCode(node);
	}
    }
}

