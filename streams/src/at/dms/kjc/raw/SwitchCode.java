package at.dms.kjc.raw;

import at.dms.kjc.*;
import at.dms.kjc.sir.*;
import at.dms.kjc.sir.lowering.*;
import at.dms.util.Utils;
import java.util.HashSet;
import java.util.Set;
import java.util.HashMap;
import java.util.Iterator;
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
		FileWriter fw =
		    new FileWriter("sw" + Layout.getTileNumber(tile) 
				   + ".s");
		fw.write("#  Switch code\n");
		fw.write(getHeader());
		//print the init switch code
		if (Simulator.initSchedules.get(tile) != null)
		    fw.write(((StringBuffer)Simulator.
			      initSchedules.get(tile)).toString());
		//loop label
		fw.write("sw_loop:\n");
		//print the steady state switch code
		if (Simulator.steadySchedules.get(tile) != null)
		    fw.write(((StringBuffer)Simulator.
			      steadySchedules.get(tile)).toString());
		//print the jump ins
		fw.write("\tj\tsw_loop\n\n");
		fw.write(getTrailer());
		fw.close();
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
    
    private static String getTrailer() 
    {
	StringBuffer buf = new StringBuffer();
	
	buf.append(".text\n\n");
	buf.append("raw_init:\n");
	buf.append("\tmtsri	SW_PC, %lo(sw_begin)\n");
	buf.append("\tmtsri	SW_FREEZE, 0\n");
	buf.append("\tjr $31\n");
	return buf.toString();
    }
}

//old switch schedule production
//
//     private static void generalSchedule(FlatNode node) throws Exception
//     {
// 	FileWriter fw = new FileWriter("sw" + Layout.getTile(node.contents) + 
// 				       ".s");
// 	System.out.println("Inside the general Schedule");
	
// 	SIRFilter filter = (SIRFilter)node.contents;
// 	int peek, pop, push, receive;
// 	//must get peek items from the upstream node on the first invocation
// 	boolean first = true;
	
// 	peek = filter.getPeekInt();
// 	pop = filter.getPopInt();
// 	push = filter.getPushInt();
	
// 	fw.write("#  Switch code for a filter with pushes and pops\n");
// 	fw.write(getHeader());
	
// 	SwitchScheduleNode currentReceive, firstReceive; 
// 	currentReceive = firstReceive = 
// 	    (SwitchScheduleNode)switchReceiveCode.get(node);
// 	SwitchScheduleNode currentSend, firstSend;
// 	currentSend = firstSend = 
// 	    (SwitchScheduleNode)switchSendCode.get(node);
	
// 	//The first schedule with peek receives as the 
// 	//first quantum for receives
// 	while (currentReceive != null && currentSend != null) {
// 	    //receives first, for the first receive burst
// 	    //the first must receive peek items
// 	    //all others are pop items
// 	    if (first) {
// 		first  = false;
// 		receive = peek;
// 	    }
// 	    else 
// 		receive = pop;
// 	    for (int i = 0; i < receive; i++) {
// 		if (currentReceive == null)
// 		    currentReceive = firstReceive;
// 		fw.write(currentReceive.toAssembly(node, false));
// 		currentReceive = currentReceive.next;
// 	    }
// 	    //sends, just send in push intervals
// 	    for (int i = 0; i < push; i++) {
// 		if (currentSend == null)
// 		    currentSend = firstSend;
// 		fw.write(currentSend.toAssembly(node, true));
// 		currentSend = currentSend.next;
// 	    }
// 	}
	
// 	fw.write("sw_loop:\n");
// 	//now the steady state schedule
// 	//pop receives followed by peek sends
// 	//finish when they both end at that same time...
// 	currentReceive = firstReceive;
// 	currentSend = firstSend;
// 	while (currentReceive != null && currentSend != null) {
// 	    for (int i = 0; i < pop; i++) {
// 		if (currentReceive == null)
// 		    currentReceive = firstReceive;
// 		fw.write(currentReceive.toAssembly(node, false));
// 		currentReceive = currentReceive.next;
// 	    }
// 	    //sends, just send in push intervals
// 	    for (int i = 0; i < push; i++) {
// 		if (currentSend == null)
// 		    currentSend = firstSend;
// 		fw.write(currentSend.toAssembly(node, true));
// 		currentSend = currentSend.next;
// 	    }
// 	}
// 	//loop to the steady state schedule
// 	fw.write("\tj\tsw_loop\n\n");
// 	fw.write(getTrailer());
// 	fw.close();
	
//     }
    
