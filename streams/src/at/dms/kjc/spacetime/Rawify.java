package at.dms.kjc.spacetime;

import java.util.ListIterator;
import at.dms.kjc.sir.*;
import at.dms.kjc.*;
import at.dms.kjc.spacetime.switchIR.*;
import at.dms.util.Utils;
import java.util.LinkedList;

/** This class will rawify the SIR code and it creates the 
 * switch code.  It does not rawify the compute code in place. 
**/
public class Rawify
{
    public static void run(ListIterator traces, RawChip rawChip,
			   boolean init) 
    {
	//iterate over the traces in the given order and generate the 
	//switch code, the tile code, and the off chip stuff for 
	//each TraceNode
	Trace trace;
	
	while (traces.hasNext()) {
	    trace = (Trace)traces.next();
	    //iterate over the TraceNodes
	    TraceNode traceNode = trace.getHead();
	    while (traceNode != null) {
		//do the appropiate code generation
		if (traceNode.isFilterTrace()) {
		    RawTile tile = rawChip.getTile(((FilterTraceNode)traceNode).getX(), 
						   ((FilterTraceNode)traceNode).getY());
		    //create the filter info class
		    FilterInfo filterInfo = FilterInfo.getFilterInfo((FilterTraceNode)traceNode);
		    //switch code for the trace
		    //generate switchcode based on the presence of buffering		    
		    if (filterInfo.isDirect()) 
			createSwitchCodeDirect((FilterTraceNode)traceNode, 
				     trace, filterInfo, init, tile, rawChip);
		    else
			createSwitchCodeBuffered((FilterTraceNode)traceNode, 
				     trace, filterInfo, init, tile, rawChip);
		    //generate the compute code for the trace and place it in
		    //the tile
		    if (init)
			tile.getComputeCode().addTraceInit(filterInfo);
		    else
			tile.getComputeCode().addTraceSteady(filterInfo);
		}
		else if (traceNode.isOutputTrace()) {
		    if (KjcOptions.magicdram) 
			createMagicDramOutput((OutputTraceNode)traceNode,
					      trace, init, rawChip);
		}
		else {
		    //input trace    
		    if (KjcOptions.magicdram)
			createMagicDramInput((InputTraceNode)traceNode, 
					     trace, init, rawChip);
		}
		
		//get the next tracenode
		traceNode = traceNode.getNext();
	    }
	}
	
	//generate code need in between init and steady
	if (init) 
	    EndInitialization(rawChip);
	else 
	    EndSteadyState(rawChip);
    }

    private static void createMagicDramInput(InputTraceNode node, Trace parent,
					     boolean init, RawChip rawChip) 
    {
	FilterTraceNode next = (FilterTraceNode)node.getNext();
	FilterInfo filterInfo = FilterInfo.getFilterInfo(next);

	if (!rawChip.getTile(next.getX(), next.getY()).hasIODevice()) 
	    Utils.fail("Tile not connected to io device");
	
	MagicDram dram = (MagicDram)rawChip.getTile(next.getX(), next.getY()).getIODevice();
	
	LinkedList insList = init ? dram.initInsList : dram.steadyInsList;
	//get the multiplicity based on the init variable
	int mult = (init) ? next.getInitMult() : next.getSteadyMult();
	
	//the current number of items that remain to get from the current output buffer
	int currentWeight = node.getWeights()[0];
	//the current buffer, index into the sources array
	int currentBuffer = 0;
	//the total number of items the upstream filter is receiving for this stage
	int totalItemsRec = 0;

	//generate the magic dram statements
	//iterate over the number of firings 
	for (int i = 0; i < mult; i++) {
	    int itemsReceiving = itemsNeededToFire(filterInfo, i, init) *
		Util.getTypeSize(next.getFilter().getInputType());
	    totalItemsRec += itemsReceiving;
	    //for each item generate an instruction
	    for (int j = 0; j < itemsReceiving; j++) {
		//keep track of the output buffer we are loading from
		if (currentWeight <= 0) {
		    currentBuffer = (currentBuffer + 1) % (node.getSources().length);
		    //reset the round-robin weight
		    currentWeight = node.getWeights()[currentBuffer];
		}
		
		insList.add(new MagicDramLoad(node, node.getSources()[currentBuffer]));
		currentWeight--;
	    }
	}

	//take care of the remaining items, same as above
	if (init &&
	    filterInfo.remaining > 0) {
	    for (int i = 0; 
		 i < filterInfo.remaining * Util.getTypeSize(next.getFilter().getInputType()); 
		 i++) {
		totalItemsRec++;
		if (currentWeight <= 0) {
		    currentBuffer = (currentBuffer + 1) % (node.getSources().length);
		    currentWeight = node.getWeights()[currentBuffer];
		}
		insList.add(new MagicDramLoad(node, node.getSources()[currentBuffer]));
		currentWeight--;
	    }
	}
	
	//remember the buffers that we have seen and the sizes...
	for (int i = 0; i < node.getSources().length; i++) {
	    int size = (int)((double)(node.getWeights()[i] / node.totalWeights()) * totalItemsRec);
	    if (init) size += filterInfo.remaining;
	    dram.addBuffer(node.getSources()[i], node, size);
	}
    }
    
    private static void createMagicDramOutput(OutputTraceNode node, Trace parent,
					     boolean init, RawChip rawChip) 
    {
	FilterTraceNode prev = (FilterTraceNode)node.getPrevious();
	FilterInfo filterInfo = FilterInfo.getFilterInfo(prev);
	
	if (!rawChip.getTile(prev.getX(), prev.getY()).hasIODevice()) 
	    Utils.fail("Tile not connected to io device");
	
	MagicDram dram = (MagicDram)rawChip.getTile(prev.getX(), prev.getY()).getIODevice();	
	
	LinkedList insList = init ? dram.initInsList : dram.steadyInsList;
	//get the multiplicity based on the init variable
	int mult = (init) ? prev.getInitMult() : prev.getSteadyMult();
	
	//the current number of items that remain to get from the current output buffer
	int currentWeight = node.getWeights()[0];
	//the current buffer, index  into the sources array
	int currentBuffer = 0;

	//generate the magic dram statements
	//iterate over the number of firings 
	for (int i = 0; i < mult; i++) {
	    //for each item generate an instruction
	    int items = itemsFiring(filterInfo, i, init) * 
		Util.getTypeSize(prev.getFilter().getOutputType());
	    for (int j = 0; j < items; j++) {
		//keep track of the output buffer we are loading from
		if (currentWeight <= 0) {
		    currentBuffer = (currentBuffer + 1) % (node.getDests().length);
		    //reset the round-robin weight
		    currentWeight = node.getWeights()[currentBuffer];
		}
		
		insList.add(new MagicDramStore(node, node.getDests()[currentBuffer]));
		currentWeight--;
	    }
	}
    }
    
    private static void createSwitchCodeDirect(FilterTraceNode node, Trace parent, 
						 FilterInfo filterInfo,
						 boolean init, RawTile tile,
						 RawChip rawChip) 
    {
	//get the multiplicity based on the init variable
	int mult = (init) ? node.getInitMult() : node.getSteadyMult();
		
	for (int i = 0; i < mult; i++) {
	    //append the receive code
	    if (generateSwitchCodeReceive(node) && node.getPrevious() != null)
		createReceiveCode(i, node, parent, filterInfo, init, tile, rawChip);
	    //append the send code
	    if (generateSwitchCodeSend(node) && node.getNext() != null)
		createSendCode(i, node, parent, filterInfo, init, tile, rawChip);
	}
	//don't have to worry about remaining
    }
    
    //determine whether we would generate switch code for this node, 
    //it may be doing internal inter-trace communication
    private static boolean generateSwitchCodeReceive(FilterTraceNode node) 
    {
	//always generate switch code for magic drams
	if (KjcOptions.magicdram)
	    return true;
	
	//otherwise only generate intra-trace switch code
	if (node.getPrevious() != null && node.getPrevious().isFilterTrace()) 
	    return true;
	
	return false;
    }
    
    //determine whether we would generate switch code for this node, 
    //it may be doing internal inter-trace communication
    private static boolean generateSwitchCodeSend(FilterTraceNode node) 
    {
	//always generate switch code for magic drams
	if (KjcOptions.magicdram)
	    return true;
	
	//otherwise only generate intra-trace switch code
	if (node.getNext() != null && node.getNext().isFilterTrace()) 
	    return true;
	
	return false;
    }
    
    private static void createSwitchCodeBuffered(FilterTraceNode node, Trace parent, 
						 FilterInfo filterInfo,
						 boolean init, RawTile tile,
						 RawChip rawChip) 
    {
	//get the multiplicity based on the init variable
	int mult = (init) ? node.getInitMult() : node.getSteadyMult();
	
	for (int i = 0; i < mult; i++) {
	    //append the receive code
	    if (generateSwitchCodeReceive(node) && node.getPrevious() != null)
		createReceiveCode(i, node, parent, filterInfo, init, tile, rawChip);
	    //append the send code
	    if (generateSwitchCodeSend(node) && node.getNext() != null)
		createSendCode(i, node, parent, filterInfo, init, tile, rawChip);
	}
	
	//now we must take care of the remaining items on the input tape 
	//after the initialization phase if the upstream filter produces more than
	//we consume in init
	if (init) {
	    if (node.getPrevious() != null &&
		node.getPrevious().isFilterTrace()) {		
		if (filterInfo.remaining > 0) {
		    for (int i = 0; 
			 i < filterInfo.remaining * Util.getTypeSize(node.getFilter().getInputType()); 
			 i++) {
			RouteIns ins = new RouteIns(tile);
			//add the route from the source tile to this
			//tile's compute processor
			ins.addRoute(rawChip.getTile(((FilterTraceNode)node.getPrevious()).getX(), 
						     ((FilterTraceNode)node.getPrevious()).getY()),
				     tile);
			tile.getSwitchCode().appendIns(ins, init);
		    }   
		}
	    }
	}
    }
    
    private static void createReceiveCode(int iteration, FilterTraceNode node, Trace parent, 
				   FilterInfo filterInfo, boolean init, RawTile tile,
				   RawChip rawChip) 
    {
	//if this is the init and it is the first time executing
	//and a twostage filter, use initpop and multiply this
	//by the size of the type it is receiving
	int itemsReceiving = itemsNeededToFire(filterInfo, iteration, init) *
	    Util.getTypeSize(node.getFilter().getInputType());

	//the source of the data, either a device or another raw tile
	ComputeNode sourceNode = null;
	
	if (node.getPrevious().isFilterTrace())
	    sourceNode = rawChip.getTile(((FilterTraceNode)node.getPrevious()).getX(), 
					 ((FilterTraceNode)node.getPrevious()).getY());
	else {
	    if (KjcOptions.magicdram && node.getPrevious().isInputTrace() &&
		tile.hasIODevice()) 
		sourceNode = tile.getIODevice();
	    else 
		return;
	}
	
	for (int j = 0; j < itemsReceiving; j++) {
	    RouteIns ins = new RouteIns(tile);
	    //add the route from the source tile to this
	    //tile's compute processor
	    ins.addRoute(sourceNode,
			 tile);
	    tile.getSwitchCode().appendIns(ins, init);
	}
    }

    private static void createSendCode(int iteration, FilterTraceNode node, Trace parent, 
				   FilterInfo filterInfo, boolean init, RawTile tile,
				   RawChip rawChip) 
    {
	//get the items needed to fire and multiply it by the type 
	//size
	int items = itemsFiring(filterInfo, iteration, init) * 
	    Util.getTypeSize(node.getFilter().getOutputType());
	
	ComputeNode destNode = null;
	
	if (node.getNext().isFilterTrace())
	    destNode = rawChip.getTile(((FilterTraceNode)node.getNext()).getX(), 
				       ((FilterTraceNode)node.getNext()).getY());
	else {
	    if (KjcOptions.magicdram && node.getNext().isOutputTrace() &&
		tile.hasIODevice())
		destNode = tile.getIODevice();
	    else
		return;
	}
	
	for (int j = 0; j < items; j++) {
	    RouteIns ins = new RouteIns(tile);
	    //add the route from this tile to the next trace node
	    ins.addRoute(tile, destNode);
	    //append the instruction
	    tile.getSwitchCode().appendIns(ins, init);
	}	
    }


    private static int itemsFiring(FilterInfo filterInfo, int exeCount, boolean init) 
    {
	int items = filterInfo.push;
	
	if (init && exeCount == 0 && (filterInfo.isTwoStage()))
	    items = filterInfo.prePush;
	
	return items;
    }
    

    private static int itemsNeededToFire(FilterInfo filterInfo, int exeCount, boolean init) 
    {
	int items = filterInfo.pop;
	
	//if we and this is the first execution we need either peek or initPeek
	if (init && exeCount == 0) {
	    if (filterInfo.isTwoStage())
		items = filterInfo.prePeek;
	    else
		items = filterInfo.peek;
	}
	
	return items;
    }

    private static void EndInitialization(RawChip rawChip) 
    {
    }
    
    private static void EndSteadyState(RawChip rawChip) 
    {
	
    }
}

