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
		    int mult = (init) ? filterInfo.initMult : filterInfo.steadyMult;
		    
		    if (filterInfo.isDirect())
			createSwitchCode((FilterTraceNode)traceNode, 
					 trace, filterInfo, init, false, tile, rawChip, mult);
		    else
			createSwitchCodeBuffered((FilterTraceNode)traceNode, 
				     trace, filterInfo, init, tile, rawChip, mult);
		    //generate the compute code for the trace and place it in
		    //the tile
		    if (init) {
			//create the prime pump stage switch code 
			//after the initialization switch code
			createPrimePumpSwitchCode((FilterTraceNode)traceNode, 
				     trace, filterInfo, init, tile, rawChip);
			tile.getComputeCode().addTraceInit(filterInfo);
		    }
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

    private static void createPrimePumpSwitchCode(FilterTraceNode node, Trace parent,
						  FilterInfo filterInfo,
						  boolean init, RawTile tile, RawChip rawChip) 
    {
	//call create switch code with init false (not in init) and primepump true
	createSwitchCode(node, parent, filterInfo, false, true, tile, rawChip, filterInfo.primePump);
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
	
	//generate the magic dram statements
	//iterate over the number of firings 
	for (int i = 0; i < mult; i++) {
	    int itemsReceiving = itemsNeededToFire(filterInfo, i, init) *
		Util.getTypeSize(next.getFilter().getInputType());
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

	//take care of the remaining items and the items generated in the
	//primepump stage, same as above
	if (init) {
	    int items = filterInfo.remaining + 
		(itemsNeededToFire(filterInfo, 1, !init) * filterInfo.primePump);
	    
	    for (int i = 0; 
		 i < items * Util.getTypeSize(next.getFilter().getInputType()); 
		 i++) {
		if (currentWeight <= 0) {
		    currentBuffer = (currentBuffer + 1) % (node.getSources().length);
		    currentWeight = node.getWeights()[currentBuffer];
		}
		insList.add(new MagicDramLoad(node, node.getSources()[currentBuffer]));
		currentWeight--;
	    }
	}
	
	//add the buffers to this drams list of buffer so that it knows
	//to generate the malloc of the buffer and the associated indices
	for (int i = 0; i < node.getSources().length; i++) {
            dram.addBuffer(node.getSources()[i], node);
        }

    }

    private static void createMagicDramOutput(OutputTraceNode node, Trace parent,
					     boolean init, RawChip rawChip) 
    {
	FilterTraceNode prev = (FilterTraceNode)node.getPrevious();

	
	if (!rawChip.getTile(prev.getX(), prev.getY()).hasIODevice()) 
	    Utils.fail("Tile not connected to io device");
	
	MagicDram dram = (MagicDram)rawChip.getTile(prev.getX(), prev.getY()).getIODevice();	
	
	LinkedList insList = init ? dram.initInsList : dram.steadyInsList;
	//get the multiplicity based on the init variable
	int mult = (init) ? prev.getInitMult() : prev.getSteadyMult();

	//generate the individual store commands, store the total items
	createMagicDramStores(node, prev, init, mult, insList);
	
	//generate the code for the primepump stage
	if (init) 
	    createMagicDramStores(node, prev, !init, prev.getPrimePumpMult(),
				  dram.initInsList);
	
    }
    
    /**
     * Give the multiplicity and some other stuff, generate the store instructions for
     * the dram.
     **/
    private static void createMagicDramStores(OutputTraceNode node, FilterTraceNode prev, 
					      boolean init, int mult, LinkedList insList)
					      
    {
	FilterInfo filterInfo = FilterInfo.getFilterInfo(prev);
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
    

    private static void createSwitchCode(FilterTraceNode node, Trace parent, 
					 FilterInfo filterInfo,
					 boolean init, boolean primePump, RawTile tile,
					 RawChip rawChip, int mult) 
    {
	for (int i = 0; i < mult; i++) {
	    //append the receive code
	    if (generateSwitchCodeReceive(node) && node.getPrevious() != null)
		createReceiveCode(i, node, parent, filterInfo, init, primePump, tile, rawChip);
	    //append the send code 
	    if (generateSwitchCodeSend(node) && node.getNext() != null)
		createSendCode(i, node, parent, filterInfo, init, primePump, tile, rawChip);
	}
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
						 RawChip rawChip, int mult) 
    {
	//create the switch code for each firing 
	createSwitchCode(node, parent, filterInfo, init, false, tile, rawChip, mult);
	
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
				   FilterInfo filterInfo, boolean init, boolean primePump, RawTile tile,
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
	    //append the instruction to the appropriate schedule
	    //for the primepump append to the end of the init stage
	    //so set final arg to true if init or primepump
	    tile.getSwitchCode().appendIns(ins, (init || primePump));
	}
    }

    private static void createSendCode(int iteration, FilterTraceNode node, Trace parent, 
				       FilterInfo filterInfo, boolean init, boolean primePump, 
				       RawTile tile, RawChip rawChip) 
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
	    //for the primepump append to the end of the init stage
	    //so set final arg to true if init or primepump
	    tile.getSwitchCode().appendIns(ins, (init||primePump));
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

