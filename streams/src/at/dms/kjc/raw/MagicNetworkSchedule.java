package at.dms.kjc.raw;

import at.dms.kjc.flatgraph.FlatNode;
import java.util.HashMap;
import java.util.*;
import at.dms.util.Utils;
import java.io.*;


/** 
 * This class generates bC code that describes the destinations
 * for each send instruction and the sources for each receive instruction
 * for each tile.
 **/
public class MagicNetworkSchedule
{
    // the format of the schedules:
    // index the hash map by coordinate of the tile

    // the send schedules have LinkedLists of LinkedLists of Coordinates
    // because we can send to multiple destinations

    // the receive schedules have LinkedList of Coordinates

    public static HashMap steadyReceiveSchedules;
    public static HashMap initReceiveSchedules;
    public static HashMap steadySendSchedules;
    public static HashMap initSendSchedules;
    
    private static FileWriter topFile;

    public static void generateSchedules(FlatNode top) 
    {
	try {
	    steadyReceiveSchedules = new HashMap();
	    initReceiveSchedules = new HashMap();
	    steadySendSchedules = new HashMap();
	    initSendSchedules = new HashMap();
	    
	    topFile = new FileWriter("magic_schedules.bc");
	    
	    //now the above hashmaps should be filled
	    RawBackend.simulator.simulate(top);
	    
	    //declare any vars needed
	    createTopHeader(topFile);
	    
	    dumpSchedules("send", initSendSchedules, steadySendSchedules);
	    dumpSchedules("receive", initReceiveSchedules, steadyReceiveSchedules);
	    
	    //create the list to hold the schedules
	    createList(topFile);
	    topFile.close();
	}
	catch (Exception e) {
	    e.printStackTrace();
	    Utils.fail("Error creating the magic schedule file.");
	}
    }
    
    private static void createTopHeader(FileWriter fw) throws Exception
    {
	fw.write("global SendSchedules;\n");
	fw.write("global ReceiveSchedules;\n");
	fw.write("printf(\"\\nCreating magic schedules\\n\");\n");
// 	create the locals to store the schedules
	Iterator sendIt = steadySendSchedules.keySet().iterator();
 	while(sendIt.hasNext()) {
 	    fw.write("global tile" + 
 		     Layout.getTileNumber((Coordinate)sendIt.next()) +
 		     "send;\n");
	}
 	Iterator receiveIt = steadyReceiveSchedules.keySet().iterator();
 	while(receiveIt.hasNext()) {
 	    fw.write("global tile" + 
 		     Layout.getTileNumber((Coordinate)receiveIt.next()) +
 		     "receive;\n");
	}
	fw.write("fn create_schedules() {\n //Creating List...\n");
	sendIt = steadySendSchedules.keySet().iterator();
 	while(sendIt.hasNext()) {
	    Coordinate current = (Coordinate)sendIt.next();
	    fw.write("include(\"magicsend" + Layout.getTileNumber(current) +
		     ".bc\");\n");
	    fw.write("create_schedule_send_" +
 		     Layout.getTileNumber(current) +
 		     "();\n");
	}

	receiveIt = steadyReceiveSchedules.keySet().iterator();
 	while(receiveIt.hasNext()) {
	    Coordinate current = (Coordinate)receiveIt.next();
	    fw.write("include(\"magicreceive" + Layout.getTileNumber(current) +
		     ".bc\");\n");
	    fw.write("create_schedule_receive_" +
		     Layout.getTileNumber(current) +
 		     "();\n");
	}
    }
    
    private static void createHeader(FileWriter fw, String tileNum,
				     String op) throws Exception 
    {
	fw.write("global tile" + tileNum + op + ";\n");
	fw.write("fn create_schedule_" + op + "_" + tileNum + "() {\n");
	fw.write("local temp, i, rep;\n");
	fw.write("local start_of_steady;\n");

    }

    private static void createList(FileWriter fw) throws Exception
    {

	//we can only simulate 16 or 64 tile configurations at this
	//moment, if run everything else on top of these two
	int realRows = 4, realColumns = 4;
	int row, column;

	if (RawBackend.rawRows > 4)
	    realRows = 8;
	if (RawBackend.rawColumns > 4)
	    realColumns = 8;
	

	fw.write("SendSchedules = listi_new();\n");

	for (row = 0; row < realRows; row++) {
	    for (column = 0; column < realColumns; column++) {
		fw.write("listi_add(SendSchedules, ");
		//we have to create entries for all tiles in the real configuration
		//so we have to create 0 entries for the tiles not used if we
		//are simulating a layout different from 4x4 or 8x8
		if (row < RawBackend.rawRows && column < RawBackend.rawColumns) {
		    Coordinate current = Layout.getTile(row, column);
		    
		    if (steadySendSchedules.containsKey(current))
			fw.write("tile" + Layout.getTileNumber(current) + "send);\n");
		    else 
			fw.write("0);\n");
		}
		else 
		    fw.write("0);\n");
	    }
	}

	fw.write("ReceiveSchedules = listi_new();\n");
	for (row = 0; row < realRows; row++) {
	    for (column = 0; column < realColumns; column++) {
		fw.write("listi_add(ReceiveSchedules, ");
		//we have to create entries for all tiles in the real configuration
		//so we have to create 0 entries for the tiles not used if we
		//are simulating a layout different from 4x4 or 8x8
		if (row < RawBackend.rawRows && column < RawBackend.rawColumns) {
		    Coordinate current = Layout.getTile(row, column);
		    
		    if (steadyReceiveSchedules.containsKey(current))
			fw.write("tile" + Layout.getTileNumber(current) + "receive);\n");
		    else 
			fw.write("0);\n");
		}
		else 
		    fw.write("0);\n");
	    }
	}
	
	fw.write("}\n");
    }

    private static void dumpSchedules(String op,
				     HashMap init, HashMap steady) throws Exception
    {
	//check that all tiles with an init sched have a steady-state sched.
	Iterator it = init.keySet().iterator();
	Set steadyKeys = steady.keySet();
	
	while (it.hasNext())
	    if (!steadyKeys.contains(it.next()))
		Utils.fail(op + " schedule includes a tile with an init schedule " +
			   "and no steady schedule");
	
	//now create the bC code to create the schedule
	Iterator steadyIt = steadyKeys.iterator();
	while (steadyIt.hasNext()) {
	    Coordinate current = (Coordinate)steadyIt.next();
	    dumpSchedule(op, current, (LinkedList)init.get(current), (LinkedList)steady.get(current));
	} 
    }
    
    private static void dumpSchedule(String op, Coordinate tile, 
				     LinkedList init, LinkedList steady) 
	throws Exception
    {
	String tileNumber = 
	    (new Integer(Layout.getTileNumber(tile))).toString();
	
	FileWriter fw = new FileWriter("magic" + op + tileNumber + ".bc");

	createHeader(fw, tileNumber, op);

	fw.write("tile" + tileNumber + op + " = hms_new();\n");
	fw.write("temp = tile" + tileNumber + op + ";\n");

	if (init != null) {
	    //there exists an init schedule
	    Iterator it = init.listIterator(0);
	   
	    while (it.hasNext()) {
		generateMagicRoute(it.next(), fw);
		//fw.write("temp.magic = " + routesString(it.next()) + ";\n");
		if (it.hasNext()) {
		    fw.write("temp.next = hms_new();\n");
		    fw.write("temp = temp.next;\n");
		}
	    }
	    fw.write("start_of_steady = hms_new();\n");
	    fw.write("temp.next = start_of_steady;\n");
	    fw.write("temp = start_of_steady;\n");
	}
	else {
	    //no init schedule
	    fw.write("start_of_steady = temp;\n");
	}
	
	//steady schedule
	int index = 0, reps;
	while(index < steady.size()) {
	    reps = getReps(index, steady);
	    if (reps > 1) {
		fw.write ("for (rep = 0; rep < " + reps + "; rep++) {\n");
	    }
	    generateMagicRoute(steady.get(index), fw);	    
	    //don't create a new sched object if we are 
	    //at the end of the schedule
	    if (index < steady.size() - 1) {
		fw.write("temp.next = hms_new();\n");
		fw.write("temp = temp.next;\n");
	    }
	    if (reps > 1)
		fw.write ("}\n");
	    //increment the index
	    index += reps;
	}
	fw.write("temp.next = start_of_steady;\n");
	fw.write("}\n");
	fw.close();
    }
    
    private static int getReps(int index, LinkedList sched) 
    {
	//if we are at the last element return 1
	if (index >= sched.size() - 1)
	    return 1;
	//a repetition should not end at the end of the schedule
	Object current = sched.get(index);
	int reps = 1;
	for (int i = index + 1; i < sched.size() - 1; i++) {
	    //if they are both lists, check the list elements
	    if (current instanceof List &&
		sched.get(i) instanceof List &&
		listEqual((List)current, (List)sched.get(i))) {
		reps++;
	    }
	    else if (current instanceof Coordinate &&
		     sched.get(i) instanceof Coordinate &&
		     current == sched.get(i)) {
		reps++;
	    }
	    else {
		break;
	    }
	}
	if (reps == 0) 
	    Utils.fail("Reps must be greater than 1");
	return reps;
    }
    
    private static boolean listEqual(List one, List two) 
    {
	if (one.size() != two.size())
	    return false;
	for (int i = 0; i < one.size(); i++) {
	    if (one.get(i) != two.get(i))
		return false;
	}
	return true;
    }

    private static String toHex2(int i) 
    {
	if (i > 255)
	    Utils.fail("The magic network on supports tiles configurations up to 256.");
	String s = 
	    Integer.toHexString(i);
	if (s.length() < 2)
		s = "0" + s;
	return s;
    }

    private static void generateMagicRoute(Object routes,
					   FileWriter fw) throws Exception
    {
	//create the data structure
	fw.write("temp.magic = malloc(82);\n");
	
	if (routes instanceof Coordinate) {
	    //this is a receive instruction
	    fw.write("SetByte(temp.magic, 0x" + toHex2(Layout.getTileNumber((Coordinate)routes)) +
		     ");\n");
	    //add this FF because there is no source for csti2
	    fw.write("SetByte(temp.magic + 1, 0xFF);\n");
	    //there are no destinations, so set them all to FF
	    fw.write("for (i = 2; i < 82; i ++)\n");
	    fw.write("  SetByte(temp.magic + i, 0xFF);\n");
	    return;
	}
	else {
	    Iterator it = ((LinkedList)routes).listIterator(0);
	    int i = 2;

	    //there are no sources 
	    fw.write("SetByte(temp.magic, 0xFF);\n");
	    fw.write("SetByte(temp.magic + 1, 0xFF);\n");

	    //iterate thru the destinations 
	    while (it.hasNext()) {
		fw.write("SetByte(temp.magic + " + i + ", 0x" + 
			 toHex2(Layout.getTileNumber((Coordinate)it.next())) +
			 ");\n");
		i++;
	    }
	    //zero out the remaining destinations
	    fw.write("for (i = " + i + ";i < 82;i++)\n");
	    fw.write("  SetByte(temp.magic + i, 0xFF);\n");
	}	
    }

    private static void printSchedules() 
    {
	System.out.println("Init ============");
	System.out.println("Receive =====");
	printAll(initReceiveSchedules);
	System.out.println("Send =====");
	printAll(initSendSchedules);

	System.out.println("Steady ============");
	System.out.println("Receive =====");
	printAll(steadyReceiveSchedules);
	System.out.println("Send =====");
	printAll(steadySendSchedules);
    }

    private static void printAll(HashMap hm) 
    {
	Iterator it = hm.keySet().iterator();

	while (it.hasNext()) {
	    Coordinate current = (Coordinate)it.next();
	    System.out.println(current);
	    System.out.println("----------");
	    printSched((LinkedList)hm.get(current));
	    System.out.println();
	    
	}	
    }

    private static void printSched(LinkedList ll) 
    {
	Iterator it = ll.iterator();
	
	while(it.hasNext()){
	    printNode(it.next());
	    System.out.println();
	}
    }

    private static void printNode(Object obj) 
    {
	if (obj instanceof LinkedList) {
	    Iterator it = ((LinkedList)obj).iterator();
	    while(it.hasNext())
		printNode(it.next());
	}
	else
	    System.out.print(obj + " " );
    }
    
	    
}


