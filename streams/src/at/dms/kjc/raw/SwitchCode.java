package at.dms.kjc.raw;

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

    public static void generate(FlatNode top) 
    {
	//Use the simulator to create the switch schedules
	Simulator.simulate(top);
	//now print the schedules
	dumpSchedules();
    }
    
    public static void dumpSchedules() 
    {
	//get all the nodes that have either init switch code
	//or steady state switch code
	HashSet tiles = new HashSet(Simulator.initSchedules.
	    keySet());
	
	RawBackend.addAll(tiles, Simulator.steadySchedules.keySet());
	
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
		if (Simulator.initSchedules.get(tile) != null)
		    initCode = ((StringBuffer)Simulator.initSchedules.get(tile)).toString();
		if (Simulator.steadySchedules.get(tile) != null)
		    steadyCode = ((StringBuffer)Simulator.steadySchedules.get(tile)).toString();
		
		//the sequences we are going to compress if compression is needed
		Repetition[] threeBiggest = null;
		
		int codeSize = getCodeLength(steadyCode + initCode);
		if (codeSize > 5000) {
		    System.out.println("Compression needed.  Code size = " + codeSize);
		    compression = true;
		    threeBiggest = threeBiggestRepetitions(steadyCode);
		}
		
		FileWriter fw =
		    new FileWriter("sw" + Layout.getTileNumber(tile) 
				   + ".s");
		fw.write("#  Switch code\n");
		fw.write(getHeader());
		//print the code to get the repetition counts from the processor
		if (threeBiggest != null)
		    getRepetitionCounts(threeBiggest, fw);
		//print the init switch code
		toASM(initCode, null, fw);
		//loop label
		fw.write("sw_loop:\n");
		//print the steady state switch code
		if (Simulator.steadySchedules.get(tile) != null)
		    toASM(steadyCode, threeBiggest, fw);
		//print the jump ins
		fw.write("\tj\tsw_loop\n\n");
		fw.write(getTrailer(threeBiggest));
		fw.close();
		if (threeBiggest != null) {
		    System.out.print("Found Seqeunces of: " +
				     threeBiggest[0].repetitions + " " + 
				     threeBiggest[0].repetitions + " " + 
				     threeBiggest[0].repetitions + "\n");
		} 

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
    private static void toASM(String ins, Repetition[] compressMe, FileWriter fw) throws Exception
    {
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
		    if (counter == compressMe[i].line) {
			repetitions = compressMe[i].repetitions;
			fw.write("\tmove $3, $" + i + "\n");
			fw.write("seq_start" + i + ":\n");
			//fw.write("\tnop\t" + current + "\n");
			//fw.write("\tbnezd $3, $3, seq_start" + i + "\n");
			fw.write("\tbnezd $3, $3, seq_start" + i + "\t" + current + "\n");
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
	public int line;
	public int repetitions;
	public Repetition(int l, int r) 
	{
	    line = l;
	    repetitions = r;
	}
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
		for (int i = 0; i < 3; i++) {
		    if (repetitions > threeBiggest[i].repetitions) {
			threeBiggest[i] = new Repetition(line, repetitions); 
			break;
		    }
		}
		repetitions = 1;
		last = current;
		line = counter;
	    }
	}
	//see if the repetition count is larger for the last sequence
	for (int i = 0; i < 3; i++) {
	    if (repetitions > threeBiggest[i].repetitions) {
		threeBiggest[i] = new Repetition(line, repetitions); 
		break;
	    }
	}
	return threeBiggest;
    }
    
    private static String getHeader() 
    {
	StringBuffer buf = new StringBuffer();
	
	buf.append("#include \"module_test.h\"\n\n");
	buf.append(".swtext\n");
	buf.append(".global sw_begin\n");
	buf.append(".global raw_init\n\n");
	buf.append("sw_begin:\n");
	
	return buf.toString();
    }
    
    private static String getTrailer(Repetition[] compressMe) 
    {
	StringBuffer buf = new StringBuffer();
	
	buf.append(".text\n\n");
	buf.append("raw_init:\n");
	buf.append("\tmtsri	SW_PC, %lo(sw_begin)\n");
	buf.append("\tmtsri	SW_FREEZE, 0\n");
	if (compressMe != null) {
	    
	    for (int i = 0; i < compressMe.length; i++) {
		//System.out.println("line: " + compressMe[i].line + " reps: " + compressMe[i].repetitions);
		
		//need to subtract 1 because we are adding the route instruction to the
		//branch and it will execute regardless of whether we branch
		buf.append("\tori! $0, $0, " + (compressMe[i].repetitions  - 1) + "\n");
	    }
	}
	buf.append("\tjr $31\n");
	return buf.toString();
    }
}
