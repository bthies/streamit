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
    // the max-ahead is the maximum number of lines that this will
    // recognize as a pattern for folding into a loop
    private static final int MAX_LOOKAHEAD = 20;

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
	    System.out.println("Code for " + joiner.contents.getName() +
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
	int delay = ((SIRFeedbackLoop)joiner.contents.getParent()).getDelayInt();
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
	ret.append("#define __BUFSIZE__ " + buffersize + "\n");
	ret.append("#define __MINUSONE__ " + (buffersize - 1) + "\n\n");
	
	ret.append("void work() { \n");
	//print the temp for the for loop
	ret.append("  int rep;\n");
	HashSet buffers = (HashSet)JoinerSimulator.buffers.get(joiner);
	Iterator bufIt = buffers.iterator();
	//print all the var definitions
	while (bufIt.hasNext()) {
	    String current = (String)bufIt.next();
	    ret.append("int __first" + current + " = 0;\n");
	    ret.append("int __last" + current + " = 0;\n");
	    ret.append(type + " __buffer" + current + "[__BUFSIZE__];\n");
	}

	printSchedule((JoinerScheduleNode)Simulator.initJoinerCode.get(joiner), ret, fp);
	ret.append("while(1) {\n");
	printSchedule((JoinerScheduleNode)Simulator.steadyJoinerCode.get(joiner), ret, fp);
	ret.append("}}\n");

	return ret.toString();
    }

    /**
     * Prints the schedule to <ret> for node list starting at <first>.
     */
    private static void printSchedule(JoinerScheduleNode first, StringBuffer ret, boolean fp) {
	// get the array of the schedule
	JoinerScheduleNode[] nodes = JoinerScheduleNode.toArray(first);
	// pos is our location in <nodes>
	int pos = 0;
	// keep going 'til we've printed all the nodes
	while (pos<nodes.length) {
	    // ahead is our repetition-looking device
	    int ahead=1;
	    do {
		while (ahead <= MAX_LOOKAHEAD &&
		       pos+ahead < nodes.length &&
		       !nodes[pos].equals(nodes[pos+ahead])) {
		    ahead++;
		}
		// if we found a match, try to build on it.  <reps> denotes
		// how many iterations of a loop we have.
		int reps = 0;
		if (ahead <= MAX_LOOKAHEAD &&
		    pos+ahead < nodes.length &&
		    nodes[pos].equals(nodes[pos+ahead])) {
		    // see how many repetitions of the loop we can make...
		    do {
			int i;
			for (i=pos+reps*ahead; i<pos+(reps+1)*ahead; i++) {
			    // quit if we reach the end of the array
			    if (i+ahead >= nodes.length) { break; }
			    // quit if there's something non-matching
			    if (!nodes[i].equals(nodes[i+ahead])) { break; }
			}
			// if we finished loop, increment <reps>; otherwise break
			if (i==pos+(reps+1)*ahead) {
			    reps++;
			} else {
			    break;
			}
		    } while (true);
		}
		// if reps is <= 1, it's not worth the loop, so just
		// add the statement (or keep looking for loops) and
		// continue
		if (reps<=1) {
		    // if we've't exhausted the possibility of finding
		    // loops, then make a single statement
		    if (ahead >= MAX_LOOKAHEAD) {
			ret.append(nodes[pos].getC(fp));
			pos++;
		    }
		} else {
		    /*
		    System.err.println("!!! Making a loop of reps=" + reps + " with " + ahead + 
				       " instructions");
		    */
		    // otherwise, add a loop with the right number of elements
		    ret.append("for (rep = 0; rep < " + reps + "; rep++) {\n");
		    // add the component code
		    for (int i=0; i<ahead; i++) {
			ret.append(nodes[pos+i].getC(fp));
		    }
		    ret.append("}\n");
		    // increment the position
		    pos += reps*ahead;
		    // quit looking for loops
		    break;
		}
		// increment ahead so that we have a chance the next time through
		ahead++;
	    } while (ahead<=MAX_LOOKAHEAD);
	} 
    }

    /**
     * Appends schedule for <node> to <ret>, only compressing single lines that are repeated.
     */
    private static void oldPrintSchedule(JoinerScheduleNode node, StringBuffer ret, boolean fp) {
	while (node != null) {
	    int repeat = 1;
	    String code = node.getC(fp);
	    node = node.next;
	    //look for repeats
	    while (true) {
		if (node == null)
		    break;
		if (node.getC(fp).equals(code)) {
		    node = node.next;
		    repeat++;
		}
		else 
		    break;
	    }
	    if (repeat > 1) 
		ret.append("for (rep = 0; rep < " + repeat + "; rep++) {\n");
	    ret.append(code);
	    if (repeat > 1)
		ret.append("}\n");
	    
	}
    }
    
    private static CType getJoinerType(FlatNode joiner) 
    {
	boolean found;
	//search backward until we find the first filter
	while (!(joiner == null || joiner.contents instanceof SIRFilter)) {
	    found = false;
	    for (int i = 0; i < joiner.inputs; i++) {
		//	if (joiner.incomingWeights[i] > 0) {
		    joiner = joiner.incoming[i];
		    found = true;
		    //}
	    }
	    if (!found)
		Utils.fail("cannot find any upstream filter from " + joiner.contents.getName());
	}
	if (joiner != null) 
	    return ((SIRFilter)joiner.contents).getOutputType();
	else 
	    return CStdType.Null;
    }
    

    private static int nextPow2(Integer i, FlatNode node) 
    {
	if (i == null) return 0;

	System.out.println(node.contents.getName() + " BufferSize = " + i);
	
	//	return 1024;
	
	String str = Integer.toBinaryString(i.intValue());
	if  (str.indexOf('1') == -1)
	    return 0;
	int bit = str.length() - str.indexOf('1');
	return (int)Math.pow(2, bit);
    }
        
    private static void noFilterCode(Coordinate tile) 
    {
	//do not generate code for file manipulators
	if (FileVisitor.fileNodes.contains(Layout.getNode(tile)))
	    return;
	
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
	    //do not generate code for the file manipulators
	    if (FileVisitor.fileNodes.contains(node))
		return;
	    realTiles.add(Layout.getTile(node.contents));
	    FlatIRToC.generateCode(node);
	}
    }
}

