package at.dms.kjc.raw;

import java.util.HashMap;
import java.util.*;

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
    

    public static void generateSchedules(FlatNode top) 
    {
	steadyReceiveSchedules = new HashMap();
	initReceiveSchedules = new HashMap();
	steadySendSchedules = new HashMap();
	initSendSchedules = new HashMap();

	RawBackend.simulator.simulate(top);
	//now the above hashmaps should be filled
	dumpSchedules();
    }
    private static void dumpSchedules() 
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


