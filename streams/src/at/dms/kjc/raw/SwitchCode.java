package at.dms.kjc.raw;

import at.dms.kjc.*;
import at.dms.kjc.sir.*;
import at.dms.kjc.sir.lowering.*;
import at.dms.util.Utils;
import java.util.HashSet;
import java.util.HashMap;
import java.util.Iterator;
import java.io.*;

/**
 * This class generates the switch code for each tile and writes it to a file 
 */

public class SwitchCode extends at.dms.util.Utils implements FlatVisitor 
{
    private static HashMap switchSendCode;
    private static HashMap switchReceiveCode;
    private static HashSet filters;
    
    static {
	filters = new HashSet();
	switchSendCode = new HashMap();
	switchReceiveCode = new HashMap();
    }

    public SwitchCode() 
    {
    }
    
    public static void generate(FlatNode top) 
    {
	//generate the schedules
	top.accept(new SwitchCode(), new HashSet(), true);
	//generate the switch assembly code based on the schedules
	Iterator it = filters.iterator();
	while(it.hasNext()) {
	    generateSwitchCode((FlatNode)it.next());
	}
    }
    
    public void visitNode(FlatNode node) {
	if (node.contents instanceof SIRFilter) {
	    filters.add(node);
	    if (((SIRFilter)node.contents).getPushInt() > 0)
		switchSendCode.put(node, PushSimulator.simulate(node));
	    if (((SIRFilter)node.contents).getPeekInt() > 0)
		switchReceiveCode.put(node, PopSimulator.simulate(node));
	}
    }

   
    
    private static void generateSwitchCode(FlatNode node) 
    {
	try {
	    SIRFilter filter = (SIRFilter)node.contents;
	    if (filter.getPeekInt() == 0 && filter.getPushInt() == 0) {
		noSwitchSchedule(node);
		return;
	    }
	    if (filter.getPeekInt() == 0) {
		sendOnly(node);
		return;
	    }
	    if (filter.getPushInt() == 0) {
		receiveOnly(node);
		return;
	    }
	    generalSchedule(node);
	}
	catch (Exception e) {
	    e.printStackTrace();
	    
	    System.err.println("Error Generating Switch Assembly Code");
	}
    }
    
    private static void noSwitchSchedule(FlatNode node) throws Exception
    {
	FileWriter fw = new FileWriter("sw" + Layout.getTile(node.contents) + ".s");
	
	fw.write("#  Switch code for a filter with no pushes or pops\n");
	fw.write(getHeader());
	fw.write("\tnop\n");
	fw.write("\tj\tsw_begin\n\n");
	fw.write(getTrailer());
	fw.close();
    }
    
    //send only schedule
    private static void sendOnly(FlatNode node) throws Exception
    {
	FileWriter fw = new FileWriter("sw" + Layout.getTile(node.contents) + ".s");
	
	fw.write("#  Switch code for a filter with no peeks\n");
	fw.write(getHeader());
	
	SwitchScheduleNode current = (SwitchScheduleNode)switchSendCode.get(node);
	while (current != null) {
	    fw.write(current.toAssembly(node, true));
	    current = current.next;
	}
	fw.write("\tj\tsw_begin\n\n");
	fw.write(getTrailer());
	fw.close();	
    }
    

    private static void receiveOnly(FlatNode node) throws Exception
    {
	FileWriter fw = new FileWriter("sw" + Layout.getTile(node.contents) + ".s");
	
	fw.write("#  Switch code for a filter with no pushes\n");
	fw.write(getHeader());
	
	SwitchScheduleNode current = (SwitchScheduleNode)switchReceiveCode.get(node);
	while (current != null) {
	    fw.write(current.toAssembly(node, false));
	    current = current.next;
	}
	fw.write("\tj\tsw_begin\n\n");
	fw.write(getTrailer());
	fw.close();
    }
    

    private static void generalSchedule(FlatNode node) throws Exception
    {
	FileWriter fw = new FileWriter("sw" + Layout.getTile(node.contents) + 
				       ".s");
	System.out.println("Inside the general Schedule");
	
	SIRFilter filter = (SIRFilter)node.contents;
	int peek, pop, push, receive;
	//must get peek items from the upstream node on the first invocation
	boolean first = true;
	
	peek = filter.getPeekInt();
	pop = filter.getPopInt();
	push = filter.getPushInt();
	
	fw.write("#  Switch code for a filter with pushes and pops\n");
	fw.write(getHeader());
	
	SwitchScheduleNode currentReceive, firstReceive; 
	currentReceive = firstReceive = 
	    (SwitchScheduleNode)switchReceiveCode.get(node);
	SwitchScheduleNode currentSend, firstSend;
	currentSend = firstSend = 
	    (SwitchScheduleNode)switchSendCode.get(node);
	
	//The first schedule with peek receives as the 
	//first quantum for receives
	while (currentReceive != null && currentSend != null) {
	    //receives first, for the first receive burst
	    //the first must receive peek items
	    //all others are pop items
	    if (first) {
		first  = false;
		receive = peek;
	    }
	    else 
		receive = pop;
	    for (int i = 0; i < receive; i++) {
		if (currentReceive == null)
		    currentReceive = firstReceive;
		fw.write(currentReceive.toAssembly(node, false));
		currentReceive = currentReceive.next;
	    }
	    //sends, just send in push intervals
	    for (int i = 0; i < push; i++) {
		if (currentSend == null)
		    currentSend = firstSend;
		fw.write(currentSend.toAssembly(node, true));
		currentSend = currentSend.next;
	    }
	}
	
	fw.write("sw_loop:\n");
	//now the steady state schedule
	//pop receives followed by peek sends
	//finish when they both end at that same time...
	currentReceive = firstReceive;
	currentSend = firstSend;
	while (currentReceive != null && currentSend != null) {
	    for (int i = 0; i < pop; i++) {
		if (currentReceive == null)
		    currentReceive = firstReceive;
		fw.write(currentReceive.toAssembly(node, false));
		currentReceive = currentReceive.next;
	    }
	    //sends, just send in push intervals
	    for (int i = 0; i < push; i++) {
		if (currentSend == null)
		    currentSend = firstSend;
		fw.write(currentSend.toAssembly(node, true));
		currentSend = currentSend.next;
	    }
	}
	//loop to the steady state schedule
	fw.write("\tj\tsw_loop\n\n");
	fw.write(getTrailer());
	fw.close();
	
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
    


    public static void dumpCode() {
	System.out.println("============== Push Schedules ==============");
	{
	    Iterator it = switchSendCode.keySet().iterator();
	    
	    while (it.hasNext()) {
		FlatNode node = (FlatNode)it.next();
		System.out.println("Push Schedule for " + 
				   Namer.getName(node.contents) + " on tile " +
				   Layout.getTile(node.contents));
		
		SwitchScheduleNode current = (SwitchScheduleNode)switchSendCode.get(node);
		while (current.next != null) {
		    current.printMe();
		    current = current.next;
		}
	    }
	}
	System.out.println("============== Pop Schedules ==============");
	{
	    Iterator it = switchReceiveCode.keySet().iterator();
	    
	    while (it.hasNext()) {
		FlatNode node = (FlatNode)it.next();
		System.out.println("Pop Schedule for " + 
				   Namer.getName(node.contents) + " on tile " +
				   Layout.getTile(node.contents));
		
		SwitchScheduleNode current = (SwitchScheduleNode)switchReceiveCode.get(node);
		while (current.next != null) {
		    current.printMe();
		    current = current.next;
		}
	    }
	    
	}
    }  
}
