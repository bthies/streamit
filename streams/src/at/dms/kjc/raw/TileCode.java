package at.dms.kjc.raw;

import at.dms.kjc.flatgraph.FlatNode;
import at.dms.kjc.flatgraph.FlatVisitor;
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

    public static final String ARRAY_INDEX = "__ARRAY_INDEX__";

    public static void generateCode(FlatNode topLevel) 
    {
	//create a set containing all the coordinates of all
	//the nodes in the FlatGraph plus all the tiles involved
	//in switching
	//generate the code for all tiles containing filters and joiners
	realTiles = new HashSet();
	topLevel.accept(new TileCode(), new HashSet(), true);
	tiles = new HashSet();
	//for decoupled execution the scheduler does not run
	if (!KjcOptions.decoupled) {
	    tiles.addAll(RawBackend.simulator.initSchedules.keySet());
	    tiles.addAll(RawBackend.simulator.steadySchedules.keySet());
	}
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
	    fw.write("/* " + joiner.contents.getName() + "*/\n");
	    fw.write("#include <raw.h>\n");
	    fw.write("#include <math.h>\n\n");
  
	    if(KjcOptions.altcodegen) {
		fw.write("register float " + Util.CSTOFPVAR + " asm(\"$csto\");\n");
		fw.write("register float " + Util.CSTIFPVAR + " asm(\"$csti\");\n");
		fw.write("register int " + Util.CSTOINTVAR + " asm(\"$csto\");\n");
		fw.write("register int " + Util.CSTIINTVAR + " asm(\"$csti\");\n");
	    }

	    if (RawBackend.FILTER_DEBUG_MODE) {
		fw.write("void static_send_print(");
		fw.write(Util.getJoinerType(joiner) + " f) {\n");
		if (Util.getJoinerType(joiner).isFloatingPoint()) 
		    fw.write("print_float(f);\n");
		else 
		    fw.write("print_int(f);\n");
		fw.write("static_send(f);\n");
		fw.write("}\n\n");
	    }
	
	    
	    //if there are structures in the code, include
	    //the structure definition header files
	    //this must be included after the above declarations 
	    //(of CSTO*, CSTI*)

	    if (RawBackend.structures.length > 0) 
		fw.write("#include \"structs.h\"\n");
	    
	    if (KjcOptions.decoupled) {
		fw.write("float " + Util.CSTOFPVAR + ";\n");
		fw.write("float " + Util.CSTIFPVAR + ";\n");
		fw.write("int " + Util.CSTOINTVAR + ";\n");
		fw.write("int " + Util.CSTIINTVAR + ";\n");
	    }
	    
	    if (joiner.contents.getParent() instanceof SIRFeedbackLoop)
		fw.write(createInitPath(joiner) + "\n");	    
	    fw.write(createJoinerWork(joiner));
	    //write the extern for the function to init the 
	    //switch, but there is no switch for the magic network
	    if (!KjcOptions.magic_net && !KjcOptions.decoupled) {
		fw.write("void raw_init();\n\n");
		fw.write("void raw_init2();\n\n");
	    }
	    fw.write("void begin(void) {\n");
	    if (!KjcOptions.magic_net) {
		fw.write("  raw_init();\n");
		fw.write("  raw_init2();\n");
	    }
	    else
		fw.write("  __asm__ volatile (\"magc $0, $0, 1\");\n");
	    
	    //initialize the dummy network receive value
	    if (KjcOptions.decoupled) {
		if (Util.getJoinerType(joiner).isFloatingPoint()) 
		    fw.write("  " + Util.CSTIFPVAR + " = 1.0;\n");
		else 
		    fw.write("  " + Util.CSTIINTVAR + " = 1;\n");
	    }
	    
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

    private static String createJoinerWork(FlatNode joiner) 
    {
	StringBuffer ret = new StringBuffer();
	int buffersize = nextPow2((Integer)SimulationCounter.maxJoinerBufferSize.get(joiner),
				  joiner);
	//get the type, since this joiner is guaranteed to be connected to a filter
	CType type = Util.getBaseType(Util.getJoinerType(joiner));  //??
	
	ret.append("#define __BUFSIZE__ " + buffersize + "\n");
	ret.append("#define __MINUSONE__ " + (buffersize - 1) + "\n\n");
	
	ret.append("void work() { \n");
	//print the temp for the for loop
	ret.append("  int rep;\n");
	//print the index vars if the type is an array type
	//and the duplication var for duplicate splitjoins with identities inside 
	if (Util.getJoinerType(joiner).isArrayType()) {
	    String dims[] = 
		Util.makeString(((CArrayType)Util.getJoinerType(joiner)).getDims());
	    ret.append("  int ");
	    for (int i = 0; i < dims.length -1; i++)
		ret.append(ARRAY_INDEX + i + ", ");
	    ret.append(ARRAY_INDEX + (dims.length -1) + ";\n");

	    ret.append(type + " " + JoinerScheduleNode.DUPVAR);
	    for (int i = 0; i < dims.length; i++)
		ret.append("[" + dims[i] + "]");
	    ret.append(";\n");
		       
	}
	else {
	    //print the duplication var if not array type
	    ret.append(type + " " + JoinerScheduleNode.DUPVAR);
	    ret.append(";\n");
	}
	    
	HashSet buffers = (HashSet)JoinerSimulator.buffers.get(joiner);
	Iterator bufIt = buffers.iterator();
	//print all the var definitions
	while (bufIt.hasNext()) {
	    String current = (String)bufIt.next();
	    ret.append("int __first" + current + " = 0;\n");
	    ret.append("int __last" + current + " = 0;\n");
	    ret.append(type + " __buffer" + current + "[__BUFSIZE__]");
	    if (Util.getJoinerType(joiner).isArrayType()) {
		String dims[] = 
		    Util.makeString(((CArrayType)Util.getJoinerType(joiner)).getDims());
		for (int i = 0; i < dims.length; i++)
		    ret.append("[" + dims[i] + "]");
	    }
	    ret.append(";\n");
	}

	printSchedule(joiner, (JoinerScheduleNode)RawBackend.simulator.initJoinerCode.get(joiner), ret);
	ret.append("while(1) {\n");
	printSchedule(joiner, (JoinerScheduleNode)RawBackend.simulator.steadyJoinerCode.get(joiner), ret);
	ret.append("}}\n");

	return ret.toString();
    }

    /**
     * Prints the schedule to <ret> for node list starting at <first>.
     */
    private static void printSchedule(FlatNode joiner, JoinerScheduleNode first, StringBuffer ret) {
	// get the array of the schedule
	JoinerScheduleNode[] nodes = JoinerScheduleNode.toArray(first);
	//	System.out.println("Joiner sched size " + nodes.length);
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
			ret.append(nodes[pos].getC(Util.getJoinerType(joiner)));
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
			ret.append(nodes[pos+i].getC(Util.getJoinerType(joiner)));
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
    private static void oldPrintSchedule(FlatNode joiner, JoinerScheduleNode node, StringBuffer ret, boolean fp) {
	while (node != null) {
	    int repeat = 1;
	    String code = node.getC(Util.getJoinerType(joiner));
	    node = node.next;
	    //look for repeats
	    while (true) {
		if (node == null)
		    break;
		if (node.getC(Util.getJoinerType(joiner)).equals(code)) {
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
    
   

    private static int nextPow2(Integer i, FlatNode node) 
    {
	if (i == null) return 0;

	//	System.out.println(node.contents.getName() + " BufferSize = " + i);
	
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
	    if (!KjcOptions.raw_uni) 
		fw.write("#include <raw.h>\n\n");
	    
	    //write the extern for the function to init the 
	    //switch
	    fw.write("void raw_init();\n\n");
            fw.write("void raw_init2();\n\n");
	    fw.write("void begin(void) {\n");
	    fw.write("  raw_init();\n");
            fw.write("  raw_init2();\n");
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
	//this is a mapped joiner, we do not want to generate code for
	//joiners in the decoupled case
	if (Layout.getJoiners().contains(node) && !KjcOptions.decoupled) {
	    realTiles.add(Layout.getTile(node.contents));
	    joinerCode(node);
	    //After done with node drops its contents for garbage collection
	    //node.contents=null;
	    //node.edges=null;
	    //for(int i=0;i<node.incoming.length;i++) {
	    //node.incoming[i].contents=null;
	    //}
	    //node.incoming=null;
	}
	if (node.contents instanceof SIRFilter) {
	    //do not generate code for the file manipulators
	    if (FileVisitor.fileNodes.contains(node))
		return;
	    if (!Layout.isAssigned(node))
		return;
	    realTiles.add(Layout.getTile(node.contents));
	    FlatIRToC.generateCode(node);
	    //After done with node drops its contents for garbage collection
	    //Need to keep contents for filter type checking but dropping methods
	    ((SIRFilter)node.contents).setMethods(JMethodDeclaration.EMPTY());
	}
    }
}

