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
	    System.err.println("Error Generating Switch Assembly Code");
	}
    }
    
    private static void noSwitchSchedule(FlatNode node) throws Exception
    {
	FileWriter fw = new FileWriter("sw" + Layout.getTile(node.contents) + ".s");
	
	fw.write("#  Switch code for a filter with no pushes or pops\n");
	fw.write(".swtext\n");
	fw.write(".global sw_begin\n\n");
	fw.write("sw_begin:\n");
	fw.write("\tnop\n");
	fw.close();
    }
    
    //send only schedule
    private static void sendOnly(FlatNode node) throws Exception
    {
	FileWriter fw = new FileWriter("sw" + Layout.getTile(node.contents) + ".s");
	
	fw.write("#  Switch code for a filter with no peeks\n");
	fw.write(".swtext\n");
	fw.write(".global sw_begin\n\n");
	fw.write("sw_begin:\n");
	
	SwitchScheduleNode current = (SwitchScheduleNode)switchSendCode.get(node);
	while (current.next != null) {
	    fw.write(current.toAssembly(node, true));
	    current = current.next;
	}
	fw.write("\tj\tsw_begin\n\n");
	fw.close();	
    }
    

    private static void receiveOnly(FlatNode node) throws Exception
    {
	FileWriter fw = new FileWriter("sw" + Layout.getTile(node.contents) + ".s");
	
	fw.write("#  Switch code for a filter with no pushes\n");
	fw.write(".swtext\n");
	fw.write(".global sw_begin\n\n");
	fw.write("sw_begin:\n");
	
	SwitchScheduleNode current = (SwitchScheduleNode)switchReceiveCode.get(node);
	while (current.next != null) {
	    fw.write(current.toAssembly(node, false));
	    current = current.next;
	}
	fw.write("\tj\tsw_begin\n\n");
	fw.close();
    }
    

    private static void generalSchedule(FlatNode node) throws Exception
    {
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
