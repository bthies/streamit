package at.dms.kjc.raw;

import at.dms.kjc.flatgraph.FlatNode;
import at.dms.kjc.*;
import at.dms.kjc.sir.*;
import at.dms.kjc.sir.lowering.*;
import at.dms.util.Utils;
import java.util.HashSet;
import java.util.Set;
import java.util.HashMap;
import java.util.Iterator;
import java.util.StringTokenizer;
import java.io.*;

/**
 * This class generates the switch code for each tile and writes it to a file 
 */

public class SwitchCode extends at.dms.util.Utils 
{
 // the max-ahead is the maximum number of lines that this will
    // recognize as a pattern for folding into a loop
    private static final int MAX_LOOKAHEAD = 10000;


    public static void generate(FlatNode top) 
    {
	//Use the simulator to create the switch schedules
	
	RawBackend.simulator.simulate(top);
	//now print the schedules
	dumpSchedules();
    }
    
    public static void dumpSchedules() 
    {
	//get all the nodes that have either init switch code
	//or steady state switch code
	HashSet tiles = new HashSet(RawBackend.simulator.initSchedules.
	    keySet());
	
	RawBackend.addAll(tiles, RawBackend.simulator.steadySchedules.keySet());
	RawBackend.addAll(tiles, Layout.getTiles());

	//do not generate switchcode for Tiles assigned to file readers/writers
	//they are just dummy tiles
	Iterator fs = FileVisitor.fileNodes.iterator();
	while (fs.hasNext()) {
	    tiles.remove(Layout.getTile((FlatNode)fs.next()));
	}
       
	
	Iterator tileIterator = tiles.iterator();
			
	//for each tiles dump the code
	while(tileIterator.hasNext()) {
	    Coordinate tile = (Coordinate) tileIterator.next();
	    try {
		//true if we are compressing the switch code
		boolean compression = false;
		
		//get the code
		String steadyCode = "";
		String initCode = "";
		if (RawBackend.simulator.initSchedules.get(tile) != null)
		    initCode = ((StringBuffer)RawBackend.simulator.initSchedules.get(tile)).toString();
		if (RawBackend.simulator.steadySchedules.get(tile) != null)
		    steadyCode = ((StringBuffer)RawBackend.simulator.steadySchedules.get(tile)).toString();
		
		//the sequences we are going to compress if compression is needed
		Repetition[] big3init = null;
                Repetition[] big3work = null;
		
		int codeSize = getCodeLength(steadyCode + initCode);
		if (codeSize > 5000) {
		    System.out.println("Compression needed.  Code size = " + codeSize);
		    compression = true;
                    big3init = threeBiggestRepetitions(initCode);
		    big3work = threeBiggestRepetitions(steadyCode);
		}
		
		FileWriter fw =
		    new FileWriter("sw" + Layout.getTileNumber(tile) 
				   + ".s");
		fw.write("#  Switch code\n");
		fw.write(getHeader());
		//if this tile is the north neighbor of a bc file i/o device
		//we need to send a data word to it
		printIOStartUp(tile, fw);
		//print the code to get the repetition counts from the processor
		//print the init switch code
                if (big3init != null)
                    getRepetitionCounts(big3init, fw);
		toASM(initCode, "i", big3init, fw);
		//loop label
		if (big3work != null)
		    getRepetitionCounts(big3work, fw);
		fw.write("sw_loop:\n");
		//print the steady state switch code
		if (RawBackend.simulator.steadySchedules.get(tile) != null)
		    toASM(steadyCode, "w", big3work, fw);
		//print the jump ins
		fw.write("\tj\tsw_loop\n\n");
		fw.write(getTrailer(tile, big3init, big3work));
		fw.close();
		/*if (threeBiggest != null) {
		    		    System.out.print("Found Seqeunces of: " +
				     threeBiggest[0].repetitions + " " + t" " + 
				     threeBiggest[1].repetitions + " " + threeBiggest[1].length + " " + 
				     threeBiggest[2].repetitions + " " + threeBiggest[2].length + "\n");
				     } */

		System.out.println("sw" + Layout.getTileNumber(tile) 
				   + ".s written");
	    }
	    catch (Exception e) {
		e.printStackTrace();
		
		Utils.fail("Error creating switch code file for tile " + 
			   Layout.getTileNumber(tile));
	    }
	}
    }

    //this this tile is the north neightbor of a bc file i/o device, we must send a
    //dummy 
    private static void printIOStartUp(Coordinate tile, FileWriter fw) throws Exception 
    {
	if (FileVisitor.connectedToFR(tile))
	    fw.write("\tnop\troute $csto->$cEo\n");
    }

    //receives the constants from the tile processor
    private static void getRepetitionCounts(Repetition[] compressMe, FileWriter fw) throws Exception
    {
	if (compressMe != null) {
	    //print the code to get the immediates from the 
	    for (int i = 0; i < compressMe.length; i++) {
		fw.write("\tmove $" + i +", $csto\n");
	    }
	}
    }
    
    //print the assemble for the tile routing instructions.  if init is true
    //then we are printing the init schedule and if we have repetitions
    //we must get the constants from the processor
    private static void toASM(String ins, String side, Repetition[] compressMe, FileWriter fw) throws Exception
    {
        int seq = 0;
	StringTokenizer t = new StringTokenizer(ins, "\n");

	if (compressMe == null) {
	    //no compression --- just dump the routes with nops as the instructions
	     while (t.hasMoreTokens()) {
		fw.write("\tnop\t" + t.nextToken() + "\n");
	     }
	}
	else{
	    //print the switch instructions compressing according to compressMe
	    int counter = 0;
	    String current;
	    while (t.hasMoreTokens()) {
		//get the next route instruction
		current = t.nextToken();
		counter++;
		int repetitions = 1;
		for (int i = 0; i < compressMe.length; i++) {
		    if (compressMe[i].hasLine(counter)) {
			repetitions = compressMe[i].repetitions;
			fw.write("\tmove $3, $" + i + "\n");
			fw.write("seq_start" + side + seq + ":\n");
			//fw.write("\tnop\t" + current + "\n");
			//fw.write("\tbnezd $3, $3, seq_start" + i + "\n");
			fw.write("\tbnezd $3, $3, seq_start" + side + seq + "\t" + current + "\n");
                        seq++;
			break;
		    }
		}
		if (repetitions == 1)
		    fw.write("\tnop\t" + current + "\n");
		else {
		    for (int i = 0; i < repetitions - 1; i++) {
			//skip over remainders
			if (t.hasMoreTokens())
			    t.nextToken();
			counter++;
		    }
		}
	    }
	}
    }
    
    //class used to encapsulate a sequence: the starting line and the repetition count
    static class Repetition 
    {
	public Set lines;
	public int repetitions;
	public int length;
	public Repetition(int l, int r) 
	{
            lines = new HashSet();
            lines.add(new Integer(l));
	    repetitions = r;
	    // length = len;
	}
        public void addLine(int l) { lines.add(new Integer(l)); }
        public boolean hasLine(int l) { return lines.contains(new Integer(l)); }
    }
    
    private static int getCodeLength(String str) 
    {
	StringTokenizer t = new StringTokenizer(str, "\n");
	return t.countTokens();
    }
    
    private static Repetition[] threeBiggestRepetitions(String str) {
	StringTokenizer t = new StringTokenizer(str, "\n");
	Repetition[] threeBiggest = new Repetition[3];

	//force the repetition count to be > 1
	for (int i = 0; i < 3; i++) 
	    threeBiggest[i] = new Repetition(-1, 1);
	
	String last = "";
	//the current number of repetitions for the sequence
	int repetitions = 1;
	//the total lines seen so far
	int counter = 0;
	//the starting line of the sequence we are currently at
	int line = 1;
	String current;
	
	while (t.hasMoreTokens()) {
	    //get the next route instruction
	    current = t.nextToken();
	    counter++;
	    if (last.equals(current)) {
		repetitions++;
	    }
	    else {
		//see if the repetition count is larger than any of the previous
                addToThreeBiggest(threeBiggest, line, repetitions);
		repetitions = 1;
		last = current;
		line = counter;
	    }
	}
	//see if the repetition count is larger for the last sequence
        addToThreeBiggest(threeBiggest, line, repetitions);
	return threeBiggest;
    }

    private static void addToThreeBiggest(Repetition[] threeBiggest,
                                          int line,
                                          int repetitions)
    {
	for (int i = 0; i < 3; i++) {
            if (repetitions == threeBiggest[i].repetitions)
            {
                threeBiggest[i].addLine(line);
                break;
            }
	    if (repetitions > threeBiggest[i].repetitions) {
                // shuffle the remainder down:
                for (int j = i + 1; j < 3; j++)
                    threeBiggest[j] = threeBiggest[j-1];
                // add the new one:
		threeBiggest[i] = new Repetition(line, repetitions);
		break;
	    }
	}
    }
      
   private static String[] getStringArray(StringTokenizer st) {
	String[] ret = new String[st.countTokens()];
	for (int i = 0; i < ret.length; i++)
	    ret[i] = st.nextToken();
	return ret;
    }

    /**
     * Prints the schedule to <ret> for node list starting at <first>.
     */
    /*
  private static Repetition[] threeBiggestRepetitions(String str) {
      //(JoinerScheduleNode first, StringBuffer ret, boolean fp) {
	String[] nodes = getStringArray(new  StringTokenizer(str, "\n"));
	Repetition[] threeBiggest = new Repetition[3];
	//force the repetition count to be > 1
	for (int i = 0; i < 3; i++) 
	    threeBiggest[i] = new Repetition(-1, 1, 1);

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
			pos++;
		    }
		} else {
		    //see if the repetition count is larger for the last sequence
		    for (int i = 0; i < 3; i++) {
			if (reps > threeBiggest[i].repetitions) {
			    threeBiggest[i] = new Repetition(pos, ahead, reps); 
			    break;
			}
		    }
		    // increment the position
		    pos += reps*ahead;
		    // quit looking for loops
		    break;
		}
		// increment ahead so that we have a chance the next time through
		ahead++;
	    } while (ahead<=MAX_LOOKAHEAD);
	} 
	return threeBiggest;
    }
    */
    
    private static String getHeader() 
    {
	StringBuffer buf = new StringBuffer();
	
	buf.append("#include \"module_test.h\"\n\n");
	buf.append(".swtext\n");
	buf.append(".global sw_begin\n");
	buf.append(".global raw_init\n");
        buf.append(".global raw_init2\n\n");
	buf.append("sw_begin:\n");
	
	return buf.toString();
    }
    
    private static String getTrailer(Coordinate tile,
                                     Repetition[] compressInit,
                                     Repetition[] compressWork) 
    {
	StringBuffer buf = new StringBuffer();
	
	buf.append(".text\n\n");
	buf.append("raw_init:\n");
	buf.append("\tmtsri	SW_PC, %lo(sw_begin)\n");
	buf.append("\tmtsri	SW_FREEZE, 0\n");
	if (FileVisitor.connectedToFR(tile))
	    buf.append("\tori! $0, $0, 1\n");

	if (compressInit != null) {
	    for (int i = 0; i < compressInit.length; i++) {
		//System.out.println("line: " + compressMe[i].line + " reps: " + compressMe[i].repetitions);
		
		//need to subtract 1 because we are adding the route instruction to the
		//branch and it will execute regardless of whether we branch
		buf.append("\tori! $0, $0, " + (compressInit[i].repetitions  - 1) + "\n");
	    }
	}
	buf.append("\tjr $31\n");

        buf.append("raw_init2:\n");
	if (compressWork != null) {
	    for (int i = 0; i < compressWork.length; i++) {
		
		//need to subtract 1 because we are adding the route instruction to the
		//branch and it will execute regardless of whether we branch
		buf.append("\tori! $0, $0, " + (compressWork[i].repetitions  - 1) + "\n");
	    }
	}
        buf.append("\tjr $31\n");

	return buf.toString();
    }
}
