package at.dms.kjc.spacetime;

import java.util.ListIterator;
import java.util.Iterator;
import at.dms.kjc.sir.*;
import at.dms.kjc.*;
import at.dms.kjc.spacetime.switchIR.*;
import at.dms.util.Utils;
import java.util.LinkedList;
import at.dms.kjc.flatgraph2.*;

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
		System.out.println("Rawify: " + traceNode);
		//do the appropiate code generation
		if (traceNode.isFilterTrace()) {
		    FilterTraceNode filterNode = (FilterTraceNode)traceNode;
		    if (filterNode.isPredefined()) {
			//predefined node, may have to do something
			if (KjcOptions.magicdram) 
			    magicHandlePredefined(filterNode, rawChip, init);
			else 
			    Utils.fail("Predefined filters not supported");
		    }
		    else { //regular filter!
			RawTile tile = rawChip.getTile((filterNode).getX(), 
						       (filterNode).getY());
			//create the filter info class
			FilterInfo filterInfo = FilterInfo.getFilterInfo(filterNode);
			//switch code for the trace
			//generate switchcode based on the presence of buffering		    
			int mult = (init) ? filterInfo.initMult : filterInfo.steadyMult;
			//add the dram command if this filter trace is an endpoint...
			generateFilterDRAMCommand(filterNode, filterInfo, tile, init, false);
			
			if(filterInfo.isLinear())
			    createSwitchCodeLinear(filterNode,
						   trace,filterInfo,init,false,tile,rawChip,mult);
			else if (filterInfo.isDirect())
			    createSwitchCode(filterNode, 
					     trace, filterInfo, init, false, tile, rawChip, mult);
			else
			    createSwitchCodeBuffered(filterNode, 
						     trace, filterInfo, init, tile, rawChip, mult);
			//we must add some switch instructions to account for the fact
			//that we must transfer cacheline sized chunks in the streaming dram
			//do it for the init and the steady state, primepump is handled below
			handleCacheLine(filterNode, init, false);
			
			//do every again for the primepump if it is the init stage and add the
			//compute code for the init stage
			if (init) {
			    //add the dram command if this filter trace is an endpoint...
			    generateFilterDRAMCommand(filterNode, filterInfo, tile, false, true);
			    //create the prime pump stage switch code 
			    //after the initialization switch code
			    createPrimePumpSwitchCode(filterNode, 
						      trace, filterInfo, false, tile, rawChip);
			    handleCacheLine(filterNode, false, true);
			    //generate the compute code for the trace and place it in
			    //the tile			
			    tile.getComputeCode().addTraceInit(filterInfo);
			}
			else {
			    //generate the compute code for the trace and place it in
			    //the tile
			    tile.getComputeCode().addTraceSteady(filterInfo);
			}
		    }
		    
		}
		else if (traceNode.isInputTrace() && !KjcOptions.magicdram) {
		    assert StreamingDram.differentDRAMs((InputTraceNode)traceNode) :
			"inputs for a single InputTraceNode coming from same DRAM";
		    //create the switch code to perform the joining
		    joinInputTrace((InputTraceNode)traceNode, init, false);
		    //now create the primepump code
		    if (init) {
			joinInputTrace((InputTraceNode)traceNode, false, true);
		    }
		}
		else if (traceNode.isOutputTrace() && !KjcOptions.magicdram) {
		    assert StreamingDram.differentDRAMs((OutputTraceNode)traceNode) :
			"outputs for a single OutputTraceNode going to same DRAM";
		    //create the switch code to perform the splitting
		    splitOutputTrace((OutputTraceNode)traceNode, init, false);
		    //now create the primepump code
		    if (init) {
			splitOutputTrace((OutputTraceNode)traceNode, false, true);
		    }
		}
		//get the next tracenode
		traceNode = traceNode.getNext();
	    }
	    
	}
	
    }

    private static void generateInputDRAMCommands(InputTraceNode input, boolean init, boolean primepump) 
    {
	FilterTraceNode filter = (FilterTraceNode)input.getNext();

	//don't do anything for redundant buffers
	if (OffChipBuffer.getBuffer(input, filter).redundant())
	    return;

	//number of total items that are being joined
	int items = FilterInfo.getFilterInfo(filter).totalItemsReceived(init, primepump);
	assert items % input.totalWeights() == 0: 
	    "weights on input trace node does not divide evenly with items received";
	//iterations of "joiner"
	int iterations = items / input.totalWeights();
	int typeSize = Util.getTypeSize(filter.getFilter().getInputType());
	    
	//generate the commands to read from the o/i temp buffer
	//for each input to the input trace node
	for (int i = 0; i < input.getSources().length; i++) {
	    OffChipBuffer srcBuffer = OffChipBuffer.getBuffer(input.getSources()[i], 
							      input);
	    int readBytes = iterations * typeSize * 
		input.getWeight(input.getSources()[i]) * 4;
	    readBytes = Util.cacheLineDiv(readBytes);
	    srcBuffer.getOwner().getComputeCode().addDRAMCommand(true, init || primepump,
								 readBytes, srcBuffer);
	}

	//generate the command to write to the dest of the input trace node
	OffChipBuffer destBuffer = OffChipBuffer.getBuffer(input, filter);
	int writeBytes = items * typeSize * 4;
	writeBytes = Util.cacheLineDiv(writeBytes);
	destBuffer.getOwner().getComputeCode().addDRAMCommand(false, init || primepump, 
							      writeBytes, destBuffer);
    }

    private static void generateOutputDRAMCommands(OutputTraceNode output, boolean init, boolean primepump) 
    {
	FilterTraceNode filter = (FilterTraceNode)output.getPrevious();
	
	//don't do anything for a redundant buffer
	if (output.oneOutput() && 
	    OffChipBuffer.getBuffer(output, output.getDests()[0][0]).redundant())
	    return;
	
	FilterInfo filterInfo = FilterInfo.getFilterInfo(filter);
	//calculate the number of items sent
	int items = filterInfo.totalItemsSent(init, primepump), 
	    iterations, typeSize;
	
	typeSize = Util.getTypeSize(filter.getFilter().getOutputType());
	
	//the numbers of times we should cycle thru this "splitter"
	assert items % output.totalWeights() == 0: 
	    "weights on output trace node does not divide evenly with items sent";
	iterations = items / output.totalWeights();
	
	//generate the command to read from the src of the output trace node
	OffChipBuffer srcBuffer = OffChipBuffer.getBuffer(filter, output);
	int readBytes = FilterInfo.getFilterInfo(filter).totalItemsSent(init, primepump) *
	    Util.getTypeSize(filter.getFilter().getOutputType()) * 4;
	readBytes = Util.cacheLineDiv(readBytes);
	srcBuffer.getOwner().getComputeCode().addDRAMCommand(true, init || primepump,
							     readBytes, srcBuffer);

	//generate the commands to write the o/i temp buffer dest
	Iterator dests = output.getDestSet().iterator();
	while (dests.hasNext()){
	    InputTraceNode input = (InputTraceNode)dests.next();
	    OffChipBuffer destBuffer = OffChipBuffer.getBuffer(output, input);
	    int writeBytes = iterations * typeSize *
		output.getWeight(input) * 4;
	    writeBytes = Util.cacheLineDiv(writeBytes);
	    destBuffer.getOwner().getComputeCode().addDRAMCommand(false, init || primepump,
								  writeBytes, destBuffer);
	}
    }
    
    //generate the dram commands for the input for a filter and the output from a filter
    //after it is joined and before it is split, respectively
    private static void generateFilterDRAMCommand(FilterTraceNode filterNode, FilterInfo filterInfo,
					    RawTile tile, boolean init, boolean primepump) 
    {
	generateInputFilterDRAMCommand(filterNode, filterInfo, tile, init, primepump);
	generateFilterOutputDRAMCommand(filterNode, filterInfo, tile, init, primepump);
    }
    
    //generate the dram command for the input for a filter from the dram after it is joined 
    //into the proper dram
    private static void generateInputFilterDRAMCommand(FilterTraceNode filterNode, FilterInfo filterInfo,
					    RawTile tile, boolean init, boolean primepump) 
    {
	//only generate a DRAM command for filters connected to input or output trace nodes
	if (filterNode.getPrevious() != null &&
	    filterNode.getPrevious().isInputTrace()) {
	    //get this buffer or this first upstream non-redundant buffer
	    OffChipBuffer buffer = OffChipBuffer.getBuffer(filterNode.getPrevious(),
							   filterNode).getNonRedundant();
	    
	    if (buffer == null)		
		return;
	    
	    //get the number of items received
	    int items = filterInfo.totalItemsReceived(init, primepump); 
	    
	    //return if there is nothing to receive
	    if (items == 0)
		return;
	    
	    //the transfer size rounded up to by divisible by a cacheline
	    int bytes = 
		Util.cacheLineDiv((items * Util.getTypeSize(filterNode.getFilter().getInputType())) *
				  4);

	    tile.getComputeCode().addDRAMCommand(true, init || primepump, bytes, buffer);
	} 
    }

    //generate the streaming dram command to send the output from the filter tile to the
    //dram before it is split
    private static void generateFilterOutputDRAMCommand(FilterTraceNode filterNode, FilterInfo filterInfo,
					    RawTile tile, boolean init, boolean primepump) 
    {
	if (filterNode.getNext() != null &&
		 filterNode.getNext().isOutputTrace()) {
	    //get this buffer or null if there are no outputs
	    OffChipBuffer buffer = OffChipBuffer.getBuffer(filterNode,
							   filterNode.getNext()).getNonRedundant();
	    if (buffer == null)
		return;
	    
	    //get the number of items sent
	    int items = filterInfo.totalItemsSent(init, primepump);	    
	    //return if there is nothing to send
	    if (items == 0)
		return;

	    int bytes = 
		Util.cacheLineDiv((items * Util.getTypeSize(filterNode.getFilter().getOutputType())) *
				  4);
	    
	    tile.getComputeCode().addDRAMCommand(false, init || primepump, bytes, buffer);
	}
    }
    

    //we must add some switch instructions to account for the fact
    //that we must transfer cacheline sized chunks in the streaming dram
    private static void handleCacheLine(FilterTraceNode filterNode, boolean init, boolean primepump) 
    {
	//because all dram transfers must be multiples of cacheline
	//generate code to disregard the remainder of the transfer
	if (!KjcOptions.magicdram && filterNode.getPrevious().isInputTrace())
	    handleUnneededInput(filterNode, init, primepump);
	//generate code to fill the remainder of the cache line
	if (!KjcOptions.magicdram && filterNode.getNext().isOutputTrace())
	    fillCacheLine(filterNode, init, primepump);
    }
    

    //see if the switch for the filter needs disregard some of the input because
    //it is not a multiple of the cacheline
    private static void handleUnneededInput(FilterTraceNode traceNode, boolean init, boolean primepump) 
    {
	InputTraceNode in = (InputTraceNode)traceNode.getPrevious();

	FilterInfo filterInfo = FilterInfo.getFilterInfo(traceNode);
	int items = filterInfo.totalItemsReceived(init, primepump), typeSize;
	
	typeSize = Util.getTypeSize(traceNode.getFilter().getInputType());
	
	//see if it is a mulitple of the cache line
	if ((items * typeSize) % RawChip.cacheLineWords != 0) {
	    SwitchCodeStore.disregardIncoming(OffChipBuffer.getBuffer(in, traceNode).getDRAM(),
					      (items * typeSize) % RawChip.cacheLineWords,
					      init || primepump);
	}
    }
    
    //see if the switch needs to generate dummy values to fill a cache line in the streaming
    //dram 
    private static void fillCacheLine(FilterTraceNode traceNode, boolean init, boolean primepump) 
    {

	OutputTraceNode out = (OutputTraceNode)traceNode.getNext();
	FilterInfo filterInfo = FilterInfo.getFilterInfo(traceNode);
	
	//get the number of items sent
	int items = filterInfo.totalItemsSent(init, primepump), typeSize;

	typeSize = Util.getTypeSize(traceNode.getFilter().getOutputType());
	//see if a multiple of cache line, if not generate dummy values...
	if ((items * typeSize) % RawChip.cacheLineWords != 0) {
	    SwitchCodeStore.dummyOutgoing(OffChipBuffer.getBuffer(traceNode, out).getDRAM(),
					  (items * typeSize) % RawChip.cacheLineWords,
					  init || primepump);
	}
    }
    

    private static void joinInputTrace(InputTraceNode traceNode, boolean init, boolean primepump)
    {
	FilterTraceNode filter = (FilterTraceNode)traceNode.getNext();
	
	//do not generate the switch code if it is not necessary
	if (!OffChipBuffer.necessary(traceNode))
	    return;
	    
	FilterInfo filterInfo = FilterInfo.getFilterInfo(filter);
	//calculate the number of items received
	int items = filterInfo.totalItemsReceived(init, primepump),
	    iterations, stage = 1, typeSize;

	//the stage we are generating code for as used below for generateSwitchCode()
	if (!init) 
	    stage = 2;
	
	typeSize = Util.getTypeSize(filter.getFilter().getInputType());
	//the numbers of times we should cycle thru this "joiner"
	assert items % traceNode.totalWeights() == 0: 
	    "weights on input trace node does not divide evenly with items received";
	iterations = items / traceNode.totalWeights();
	
	StreamingDram[] dest = {OffChipBuffer.getBuffer(traceNode, filter).getDRAM()};
	
	for (int i = 0; i < iterations; i++) {
	    for (int j = 0; j < traceNode.getWeights().length; j++) {
		for (int k = 0; k < traceNode.getWeights()[j]; k++) {
		    //get the source buffer, pass thru redundant buffer(s)
		    StreamingDram source = OffChipBuffer.getBuffer(traceNode.getSources()[j],
								   traceNode).getNonRedundant().getDRAM();
		    for (int q = 0; q < typeSize; q++)
			SwitchCodeStore.generateSwitchCode(source, dest, stage);
		}
	    }
	}
	//because transfers must be cache line size divisible...
	//generate dummy values to fill the cache line!
	if ((items * typeSize) % RawChip.cacheLineWords != 0) {
	    int dummy = (items * typeSize) % RawChip.cacheLineWords;
	    SwitchCodeStore.dummyOutgoing(dest[0], dummy, init || primepump);
	}
	//disregard remainder of inputs coming from temp offchip buffers
	for (int i = 0; i < traceNode.getSources().length; i++) {
	    OutputTraceNode source = traceNode.getSources()[i];
	    int remainder = 
		(iterations * typeSize * 
		 traceNode.getWeight(source)) % RawChip.cacheLineWords;
	    SwitchCodeStore.disregardIncoming(OffChipBuffer.getBuffer(source, traceNode).getDRAM(),
					      remainder, init || primepump);
	}
    }
    
    private static void splitOutputTrace(OutputTraceNode traceNode, boolean init, boolean primepump)
    {
	FilterTraceNode filter = (FilterTraceNode)traceNode.getPrevious();
	
	//check to see if the splitting is necessary
	if (!OffChipBuffer.necessary(traceNode))
	    return;
				    
	FilterInfo filterInfo = FilterInfo.getFilterInfo(filter);
	//calculate the number of items sent
	int items = filterInfo.totalItemsSent(init, primepump), 
	    iterations, stage = 1, typeSize;
	
	//the stage we are generating code for as used below for generateSwitchCode()
	if (!init) 
	    stage = 2;

	typeSize = Util.getTypeSize(filter.getFilter().getOutputType());
	    
	//the numbers of times we should cycle thru this "splitter"
	assert items % traceNode.totalWeights() == 0: 
	    "weights on output trace node does not divide evenly with items sent";
	iterations = items / traceNode.totalWeights();
	
	//is there a load immediate in the switch instruction set?!
	//I guess not, if switch instruction memory is a problem
	//this naive implementation will have to change
	StreamingDram sourcePort = OffChipBuffer.getBuffer(filter, traceNode).getDRAM();
	for (int i = 0; i < iterations; i++) {
	    for (int j = 0; j < traceNode.getWeights().length; j++) {
		for (int k = 0; k < traceNode.getWeights()[j]; k++) {
		    //generate the array of compute node dests
		    ComputeNode dests[] = new ComputeNode[traceNode.getDests()[j].length];
		    for (int d = 0; d < dests.length; d++) 
			dests[d] = OffChipBuffer.getBuffer(traceNode, 
							   traceNode.getDests()[j][d]).getDRAM();
		    for (int q = 0; q < typeSize; q++)
			SwitchCodeStore.generateSwitchCode(sourcePort, 
							   dests, stage);
		}
	    }
	}
	//because transfers must be cache line size divisible...
	//disregard the dummy values coming out of the dram
	if ((items * typeSize) % RawChip.cacheLineWords != 0) {
	    int remainder = (items * typeSize) % RawChip.cacheLineWords;
	    SwitchCodeStore.disregardIncoming(sourcePort, remainder, init || primepump);
	}
	//write dummy values into each temp buffer with a remainder
	Iterator it = traceNode.getDestSet().iterator();
	while (it.hasNext()) {
	    InputTraceNode in = (InputTraceNode)it.next();
	    int remainder = (typeSize * iterations * traceNode.getWeight(in)) %
		RawChip.cacheLineWords;
	    SwitchCodeStore.dummyOutgoing(OffChipBuffer.getBuffer(traceNode, in).getDRAM(),
					  remainder, init || primepump);
	}   
    }
    

    private static void magicHandlePredefined(FilterTraceNode predefined, RawChip rawChip, boolean init) 
    {
	if (init) {
	    //tell the magic dram that it should open the file and create vars for this file
	    if (predefined.isFileInput()) {
		//get the filter connected to this file output, just take the first one
		//because they all should be mapped to the same tile
		FilterTraceNode next = FilterInfo.getFilterInfo(predefined).getNextFilters()[0];
		if (!rawChip.getTile(next.getX(), next.getY()).hasIODevice()) 
		    Utils.fail("Tile not connected to io device");
		MagicDram dram = (MagicDram)rawChip.getTile(next.getX(), next.getY()).getIODevice();
		dram.inputFiles.add((FileInputContent)predefined.getFilter());
	    }
	    else if (predefined.isFileOutput()) {
		//tell the magic dram that it should open the file and create vars for this file
		
		//get the filter connected to this file output, just take the first one
		//because they all should be mapped to the same tile
		FilterTraceNode prev = FilterInfo.getFilterInfo(predefined).getPreviousFilters()[0];
		//find the iodevice
		if (!rawChip.getTile(prev.getX(), prev.getY()).hasIODevice()) 
		    Utils.fail("Tile not connected to io device");
		//get the dram
		MagicDram dram = (MagicDram)rawChip.getTile(prev.getX(), prev.getY()).getIODevice();
		dram.outputFiles.add((FileOutputContent)predefined.getFilter());		
	    }
	}
    }
    
    private static void createPrimePumpSwitchCode(FilterTraceNode node, Trace parent,
						  FilterInfo filterInfo,
						  boolean init, RawTile tile, RawChip rawChip) 
    {
	//call create switch code with init false (not in init) and primepump true
	createSwitchCode(node, parent, filterInfo, false, true, tile, rawChip, filterInfo.primePump);
    }
    
    private static void createMagicDramLoad(InputTraceNode node, FilterTraceNode next,
					    boolean init, RawChip rawChip) 
    {
	if (!rawChip.getTile(next.getX(), next.getY()).hasIODevice()) 
	    Utils.fail("Tile not connected to io device");
	
	MagicDram dram = (MagicDram)rawChip.getTile(next.getX(), next.getY()).getIODevice();
	
	LinkedList insList = init ? dram.initInsList : dram.steadyInsList;
	OutputTraceNode output = TraceBufferSchedule.getOutputBuffer(node);
	insList.add(new MagicDramLoad(node, output));
	dram.addBuffer(output, node);
    }

    /**
     * Generate a single magic dram store instruction for this output trace node
     **/
    private static void createMagicDramStore(OutputTraceNode node, FilterTraceNode prev, 
					     boolean init, RawChip rawChip)
					      
    {
	if (!rawChip.getTile(prev.getX(), prev.getY()).hasIODevice()) 
	    Utils.fail("Tile not connected to io device");
	//get the dram
	MagicDram dram = (MagicDram)rawChip.getTile(prev.getX(), prev.getY()).getIODevice();
	//get the list we should add to
	LinkedList insList = init ? dram.initInsList : dram.steadyInsList;
	//add the instruction
	insList.add(new MagicDramStore(node, 
				       TraceBufferSchedule.getInputBuffers(node)));
    }
    
    private static void createSwitchCodeLinear(FilterTraceNode node, Trace parent, 
					       FilterInfo filterInfo, boolean init, boolean primePump, 
					       RawTile tile, RawChip rawChip, int mult) {
	//createReceiveCode(0,  node,  parent,  filterInfo,  init,  primePump,  tile,  rawChip);
	ComputeNode sourceNode = null;
	if (node.getPrevious().isFilterTrace())
	    sourceNode = rawChip.getTile(((FilterTraceNode)node.getPrevious()).getX(),  
					 ((FilterTraceNode)node.getPrevious()).getY());
	else {
	    if (KjcOptions.magicdram && node.getPrevious() !=  null &&
		node.getPrevious().isInputTrace() &&
		tile.hasIODevice()) 
		sourceNode = tile.getIODevice();
	    else 
		return;
	}
	SwitchIPort src = rawChip.getIPort(sourceNode, tile);
	SwitchIPort src2 = rawChip.getIPort2(sourceNode, tile);
	sourceNode = null;
	ComputeNode destNode = null;
	if (node.getNext().isFilterTrace())
	    destNode = rawChip.getTile(((FilterTraceNode)node.getNext()).getX(),  
				       ((FilterTraceNode)node.getNext()).getY());
	else {
	    if (KjcOptions.magicdram && node.getNext() !=  null &&
		node.getNext().isOutputTrace() && tile.hasIODevice())
		destNode = tile.getIODevice();
	    else
		return;
	}
	SwitchOPort dest = rawChip.getOPort(tile, destNode);
	SwitchOPort dest2 = rawChip.getOPort2(tile, destNode);
	destNode = null;
	FilterContent content = node.getFilter();
	final int peek = content.getArray().length;
	final int pop = content.getPopCount();
	final int numPop = peek/pop;
	final boolean begin=content.getBegin();
	final boolean end=content.getEnd();
	System.out.println("SRC: "+src);
	System.out.println("DEST: "+dest);
	SwitchCodeStore code = tile.getSwitchCode();
	boolean first=true;
	for(int i = 0; i<numPop-1; i++)
	    for(int j = 0; j<pop; j++) {
		FullIns ins = new FullIns(tile, new MoveIns(SwitchReg.R1, src));
		if (KjcOptions.magicdram && node.getPrevious() != null &&
		    node.getPrevious().isInputTrace())
		    createMagicDramLoad((InputTraceNode)node.getPrevious(), 
					node, (init || primePump), rawChip);
		ins.addRoute(src, SwitchOPort.CSTI);
		//if(!first) {
		if(!end) {
		    if (KjcOptions.magicdram && node.getPrevious() != null &&
			node.getPrevious().isInputTrace())
			createMagicDramLoad((InputTraceNode)node.getPrevious(), 
					    node, (init || primePump), rawChip);
		    if (KjcOptions.magicdram && node.getNext() != null &&
			node.getNext().isOutputTrace())
			createMagicDramStore((OutputTraceNode)node.getNext(), 
					     node, (init || primePump), rawChip);
		    ins.addRoute(src,dest);
		    //}
		}
		code.appendIns(ins, init||primePump);
		for(int k = i-1; k>= 0; k--) {
		    FullIns newIns = new FullIns(tile);
		    newIns.addRoute(SwitchReg.R1, SwitchOPort.CSTI);
		    code.appendIns(newIns, init||primePump);
		}
		FullIns newIns=null;
		if(!begin) {
		    newIns=new FullIns(tile);
		    if (KjcOptions.magicdram && node.getPrevious() != null &&
			node.getPrevious().isInputTrace())
			createMagicDramLoad((InputTraceNode)node.getPrevious(), 
					    node, (init || primePump), rawChip);
		    newIns.addRoute(src2, SwitchOPort.CSTI2);
		    code.appendIns(newIns, init||primePump);
		}
		/*if(!first) {
		    newIns = new FullIns(tile);
		    newIns.addRoute(SwitchIPort.CSTO,dest);
		    code.appendIns(newIns, init||primePump);
		}
		first=false;*/
	    }
	final int turns=content.getPos();
	for(int turn=0;turn<turns;turn++)
	    for(int j = 0; j<pop; j++) {
		FullIns ins = new FullIns(tile, new MoveIns(SwitchReg.R1, src));
		if (KjcOptions.magicdram && node.getPrevious() != null &&
		    node.getPrevious().isInputTrace())
		    createMagicDramLoad((InputTraceNode)node.getPrevious(), 
					node, (init || primePump), rawChip);
		ins.addRoute(src, SwitchOPort.CSTI);
		if(!first) {
		    if(!end) {
			if (KjcOptions.magicdram && node.getPrevious() != null &&
			    node.getPrevious().isInputTrace())
			    createMagicDramLoad((InputTraceNode)node.getPrevious(), 
						node, (init || primePump), rawChip);
			if (KjcOptions.magicdram && node.getNext() != null &&
			    node.getNext().isOutputTrace())
			    createMagicDramStore((OutputTraceNode)node.getNext(), 
						 node, (init || primePump), rawChip);
			ins.addRoute(src,dest);
		    }
		}
		code.appendIns(ins, init||primePump);
		for(int k = numPop-2; k>= 0; k--) {
		    FullIns newIns = new FullIns(tile);
		    newIns.addRoute(SwitchReg.R1, SwitchOPort.CSTI);
		    code.appendIns(newIns, init||primePump);
		}
		FullIns newIns=null;
		if(!begin) {
		    newIns=new FullIns(tile);
		    if (KjcOptions.magicdram && node.getPrevious() != null &&
			node.getPrevious().isInputTrace())
			createMagicDramLoad((InputTraceNode)node.getPrevious(), 
					    node, (init || primePump), rawChip);
		    newIns.addRoute(src2, SwitchOPort.CSTI2);
		    code.appendIns(newIns, init||primePump);
		}
		if(!first) {
		    newIns = new FullIns(tile);
		    if (KjcOptions.magicdram && node.getNext() != null &&
			node.getNext().isOutputTrace())
			createMagicDramStore((OutputTraceNode)node.getNext(), 
					     node, (init || primePump), rawChip);
		    newIns.addRoute(SwitchIPort.CSTO,dest2);
		    code.appendIns(newIns, init||primePump);
		}
		first=false;
	    }
	Label label = code.getFreshLabel();
	//code.appendIns(label, init||primePump);
	final int numTimes = Linear.getMult(peek);
	int pendingSends=0;
	int times=0;
	FullIns ins=null;
	//final int turns2=content.getTotal()-content.getPos()+1;
	final int turns2=content.getPos();
	//final int turns2=1;
	for(int turn=0;turn<turns2+4;turn++) {
	    if(turn==turns2)
		code.appendIns(label, init||primePump);
	for(int i = 0;i<numTimes;i++) {
	    for(int j=0;j<pop;j++) {
		if(numPop==1&&j==0)
		    pendingSends++;
		times++;
		ins = new FullIns(tile, new MoveIns(SwitchReg.R1, src));
		if (KjcOptions.magicdram && node.getPrevious() != null &&
		    node.getPrevious().isInputTrace())
		    createMagicDramLoad((InputTraceNode)node.getPrevious(), 
					node, (init || primePump), rawChip);
		ins.addRoute(src, SwitchOPort.CSTI);
		if(!end) {
		    if (KjcOptions.magicdram && node.getPrevious() != null &&
			node.getPrevious().isInputTrace())
			createMagicDramLoad((InputTraceNode)node.getPrevious(), 
					    node, (init || primePump), rawChip);
		    if (KjcOptions.magicdram && node.getNext() != null &&
			node.getNext().isOutputTrace())
			createMagicDramStore((OutputTraceNode)node.getNext(), 
					     node, (init || primePump), rawChip);
		    ins.addRoute(src,dest);
		}

		if(turn>0&&times>1) {
		    if(!end)
			ins.addRoute(SwitchIPort.CSTO,dest2);
		    else
			ins.addRoute(SwitchIPort.CSTO,dest);
		}
		
		/*if(times>2) {
		  if(!begin) {
		  if (KjcOptions.magicdram && node.getPrevious() != null &&
		  node.getPrevious().isInputTrace())
		  createMagicDramLoad((InputTraceNode)node.getPrevious(), 
		  node, (init || primePump), rawChip);
		  ins.addRoute(src2,SwitchOPort.CSTI2);
		  }
		  pendingSends--;
		  }*/
		/*if(turn>1) {
		  if(!begin) {
		  if (KjcOptions.magicdram && node.getPrevious() != null &&
		  node.getPrevious().isInputTrace())
		  createMagicDramLoad((InputTraceNode)node.getPrevious(), 
		  node, (init || primePump), rawChip);
		  ins.addRoute(src2,SwitchOPort.CSTI2);
		  }
		  }*/
		code.appendIns(ins, init||primePump);
		if(times==4) {
		    times=0;
		    if(true||!begin) {
		    //if(turn==0)
		    //pendingSends++;
		    if(pendingSends>0) {
			for(int l=0;l<pendingSends;l++) {
			    ins=new FullIns(tile);
			    if(/*turn>0&&*/!begin) {
				if (KjcOptions.magicdram && node.getPrevious() != null &&
				    node.getPrevious().isInputTrace())
				    createMagicDramLoad((InputTraceNode)node.getPrevious(), 
							node, (init || primePump), rawChip);
				ins.addRoute(src2,SwitchOPort.CSTI2);
			    }
			    if (KjcOptions.magicdram && node.getNext() != null &&
				node.getNext().isOutputTrace())
				createMagicDramStore((OutputTraceNode)node.getNext(), 
						     node, (init || primePump), rawChip);
			    
			    if(turn>0&&l<1) {
				if(!end)
				    ins.addRoute(SwitchIPort.CSTO,dest2);
				else
				    ins.addRoute(SwitchIPort.CSTO,dest);
			      }
			    
			    //if(!begin)
			    code.appendIns(ins,init||primePump);
			}
			pendingSends=0;
		    }
		    }
		}
		for(int k = 1;k<numPop;k++) {
		    if(j==0&&k==numPop-1)
			pendingSends++;
		    times++;
		    ins=new FullIns(tile);
		    ins.addRoute(SwitchReg.R1, SwitchOPort.CSTI);
		    code.appendIns(ins, init||primePump);
		    if(times==4) {
			times=0;
			if(true||!begin) {
			//if(turn==0)
			//pendingSends++;
			if(pendingSends>0) {
			    for(int l=0;l<pendingSends;l++) {
				ins=new FullIns(tile);
				if(/*turn>0&&*/!begin) {
				    if (KjcOptions.magicdram && node.getPrevious() != null &&
					node.getPrevious().isInputTrace())
					createMagicDramLoad((InputTraceNode)node.getPrevious(), 
							    node, (init || primePump), rawChip);
				    ins.addRoute(src2,SwitchOPort.CSTI2);
				}
				if (KjcOptions.magicdram && node.getNext() != null &&
				    node.getNext().isOutputTrace())
				    createMagicDramStore((OutputTraceNode)node.getNext(), 
							 node, (init || primePump), rawChip);
				
				if(turn>0&&l<1) {
				    if(!end)
					ins.addRoute(SwitchIPort.CSTO,dest2);
				    else
					ins.addRoute(SwitchIPort.CSTO,dest);
				}

				//if(!begin)
				code.appendIns(ins,init||primePump);
			    }
			    pendingSends=0;
			}
			}
		    }
		    }
	    }
	    
	}
	}
	/*for(int i=0;i<numTimes-1;i++) {
	  FullIns newIns=new FullIns(tile);
	  newIns.addRoute(SwitchIPort.CSTO,dest);
	  code.appendIns(newIns,init||primePump);
	  }
	  FullIns newIns=new FullIns(tile,new JumpIns(label.getLabel()));
	  newIns.addRoute(SwitchIPort.CSTO,dest);
	  code.appendIns(newIns,init||primePump);*/
	//code.appendIns(new JumpIns(label.getLabel()),init||primePump);
	ins.setProcessorIns(new JumpIns(label.getLabel()));
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
	if (init && filterInfo.remaining > 0) {
	    appendReceiveInstructions(node, 
				      filterInfo.remaining * Util.getTypeSize(node.getFilter().getInputType()),
				      filterInfo, init, false, tile, rawChip);
	}
    }
    
    
    
    private static void createReceiveCode(int iteration, FilterTraceNode node, Trace parent, 
				   FilterInfo filterInfo, boolean init, boolean primePump, RawTile tile,
				   RawChip rawChip) 
    {
	//if this is the init and it is the first time executing
	//and a twostage filter, use initpop and multiply this
	//by the size of the type it is receiving
	int itemsReceiving = filterInfo.itemsNeededToFire(iteration, init) *
	    Util.getTypeSize(node.getFilter().getInputType());

	appendReceiveInstructions(node, itemsReceiving, filterInfo, init, 
				  primePump, tile, rawChip);
    }
    
    private static void appendReceiveInstructions(FilterTraceNode node, int itemsReceiving,
						  FilterInfo filterInfo,
						  boolean init, boolean primePump, RawTile tile,
						  RawChip rawChip) 
    {
	//the source of the data, either a device or another raw tile
	ComputeNode sourceNode = null;
	
	if (node.getPrevious().isFilterTrace())
	    sourceNode = rawChip.getTile(((FilterTraceNode)node.getPrevious()).getX(), 
					 ((FilterTraceNode)node.getPrevious()).getY());
	else {
	    if (KjcOptions.magicdram && node.getPrevious() != null &&
		node.getPrevious().isInputTrace() &&
		tile.hasIODevice()) 
		sourceNode = tile.getIODevice();
	    else 
		sourceNode = OffChipBuffer.getBuffer(node.getPrevious(), node).getDRAM();
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
	    //if we are receiving from an inputtracenode and 
	    //magic dram is enabled, generate the magic dram load ins
	    if (KjcOptions.magicdram && node.getPrevious() != null &&
		node.getPrevious().isInputTrace())
		createMagicDramLoad((InputTraceNode)node.getPrevious(), 
				    node, (init || primePump), rawChip);
	    
	}
    }

    private static void createSendCode(int iteration, FilterTraceNode node, Trace parent, 
				       FilterInfo filterInfo, boolean init, boolean primePump, 
				       RawTile tile, RawChip rawChip) 
    {
	//get the items needed to fire and multiply it by the type 
	//size
	int items = filterInfo.itemsFiring(iteration, init) * 
	    Util.getTypeSize(node.getFilter().getOutputType());
	
	ComputeNode destNode = null;
	
	if (node.getNext().isFilterTrace())
	    destNode = rawChip.getTile(((FilterTraceNode)node.getNext()).getX(), 
				       ((FilterTraceNode)node.getNext()).getY());
	else {
	    if (KjcOptions.magicdram && node.getNext() != null &&
		node.getNext().isOutputTrace() && tile.hasIODevice())
		destNode = tile.getIODevice();
	    else {
		destNode = OffChipBuffer.getBuffer(node, node.getNext()).getDRAM();
	    }
	    
	}
	
	for (int j = 0; j < items; j++) {
	    RouteIns ins = new RouteIns(tile);
	    //add the route from this tile to the next trace node
	    ins.addRoute(tile, destNode);
	    //append the instruction
	    //for the primepump append to the end of the init stage
	    //so set final arg to true if init or primepump
	    tile.getSwitchCode().appendIns(ins, (init||primePump));
	    //if we are connected to an output trace node and 
	    //magicdram is enabled, create the magic dram store instuction
	    if (KjcOptions.magicdram && node.getNext() != null &&
		node.getNext().isOutputTrace())
		createMagicDramStore((OutputTraceNode)node.getNext(), 
				     node, (init || primePump), rawChip);
	}	
    }



}

