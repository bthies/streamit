package at.dms.kjc.spacetime;

import java.util.ListIterator;
import java.util.Iterator;
import at.dms.kjc.sir.*;
import at.dms.kjc.*;
import at.dms.kjc.spacetime.switchIR.*;
import at.dms.util.Utils;
import java.util.LinkedList;
import java.util.Vector;
import at.dms.kjc.flatgraph2.*;

/** This class will rawify the SIR code and it creates the 
 * switch code.  It does not rawify the compute code in place. 
 **/
public class Rawify
{
    public static void run(Iterator traces, RawChip rawChip,
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
		SpaceTimeBackend.println("Rawify: " + traceNode);
		//do the appropiate code generation
		if (traceNode.isFilterTrace()) {
		    FilterTraceNode filterNode = (FilterTraceNode)traceNode;
		    assert !filterNode.isPredefined() :
			"Predefined filters should not appear in the trace traversal";
		    RawTile tile = rawChip.getTile((filterNode).getX(), 
						   (filterNode).getY());
		    //create the filter info class
		    FilterInfo filterInfo = FilterInfo.getFilterInfo(filterNode);
			//add the dram command if this filter trace is an endpoint...
		    generateFilterDRAMCommand(filterNode, filterInfo, tile, init, false);
		    
		    if(filterInfo.isLinear()) {
			assert FilterInfo.getFilterInfo(filterNode).remaining == 0 :
			    "Items remaining on buffer for init for linear filter";
			createSwitchCodeLinear(filterNode,
					       trace,filterInfo,init,false,tile,rawChip);
		    }
		    
		    else 
			createSwitchCode(filterNode, 
					 trace, filterInfo, init, false, tile, rawChip);
		    //used for debugging, nothing more
		    tile.addFilterTrace(init, false, filterNode);
		    //do every again for the primepump if it is the init stage and add the
		    //compute code for the init stage
		    if (init) {
			//add the dram command if this filter trace is an endpoint...
			generateFilterDRAMCommand(filterNode, filterInfo, tile, false, true);
			//create the prime pump stage switch code 
			//after the initialization switch code
			createSwitchCode(filterNode, 
					 trace, filterInfo, false, true, tile, rawChip);
			//generate the compute code for the trace and place it in
			//the tile			
			tile.getComputeCode().addTraceInit(filterInfo);
			//used for debugging
			tile.addFilterTrace(false, true, filterNode);
		    }
		    else {
			//generate the compute code for the trace and place it in
			//the tile
			tile.getComputeCode().addTraceSteady(filterInfo);
		    }
		}
		else if (traceNode.isInputTrace() && !KjcOptions.magicdram) {
		    assert StreamingDram.differentDRAMs((InputTraceNode)traceNode) :
			"inputs for a single InputTraceNode coming from same DRAM";
		    handleFileInput((InputTraceNode)traceNode, init, false, 
					rawChip);
		    //create the switch code to perform the joining
		    joinInputTrace((InputTraceNode)traceNode, init, false);
		    //generate the dram command to execute the joining
		    generateInputDRAMCommands((InputTraceNode)traceNode, init, false);
		    //now create the primepump code
		    if (init) {
			handleFileInput((InputTraceNode)traceNode, false, true, 
					rawChip);
			joinInputTrace((InputTraceNode)traceNode, false, true);
			generateInputDRAMCommands((InputTraceNode)traceNode, false, true);
		    }
		}
		else if (traceNode.isOutputTrace() && !KjcOptions.magicdram) {
		    assert StreamingDram.differentDRAMs((OutputTraceNode)traceNode) :
			"outputs for a single OutputTraceNode going to same DRAM";
		    handleFileOutput((OutputTraceNode)traceNode, init, false,
				     rawChip);
		    //create the switch code to perform the splitting
		    splitOutputTrace((OutputTraceNode)traceNode, init, false);
		    //generate the DRAM command
		    outputDRAMCommands((OutputTraceNode)traceNode, init, false);
		    //now create the primepump code
		    if (init) {
			handleFileOutput((OutputTraceNode)traceNode, false, true,
					 rawChip);
			splitOutputTrace((OutputTraceNode)traceNode, false, true);
			outputDRAMCommands((OutputTraceNode)traceNode, false, true);
		    }
		}
		//get the next tracenode
		traceNode = traceNode.getNext();
	    }
	    
	}
	
    }

    private static void handleFileInput(InputTraceNode input, boolean init, boolean primepump, 
					RawChip chip)
    {
	//if there are no files, do nothing
	if (!input.hasFileInput())
	    return;
	for (int i = 0; i < input.getSources().length; i++) {
	    //do nothing for non-file readers
	    if (!input.getSources()[i].getSrc().isFileReader())
		continue;
	    
	    OutputTraceNode fileO = input.getSources()[i].getSrc();

	    IntraTraceBuffer buf = IntraTraceBuffer.getBuffer(fileO.getPrevFilter(),
							      fileO);
	    assert fileO.getPrevFilter().getFilter() instanceof FileInputContent :
		"FileReader should be a FileInputContent";
	    
	    FileInputContent fileIC = (FileInputContent)fileO.getPrevFilter().getFilter();
	    FilterInfo filterInfo = FilterInfo.getFilterInfo(fileO.getPrevFilter());

	    //do nothing if we have already generated the code for this file reader 
	    //for this stage
	    if (buf.getDRAM().isFileReader() &&
		buf.getDRAM().getFileReader().isVisited(init, primepump))
		continue;
	    
	    //now generate the code, both the dram commands and the switch code
	    //to perform the splitting, if there is only one output, do nothing
	    if (!OffChipBuffer.unnecessary(fileO)) {
		//generate dram command
		outputDRAMCommands(fileO, init, primepump);
		//perform the splitting
		splitOutputTrace(fileO, init, primepump);
	    }
	}
    }
    
    private static void handleFileOutput(OutputTraceNode output, boolean init, boolean primepump, 
					RawChip chip)
    {
	//if there are no files, do nothing
	if (!output.hasFileOutput())
	    return;
	
	Vector fileOutputs = new Vector();
	Iterator dests = output.getDestSet().iterator();
	while (dests.hasNext()) {
	    Edge edge = (Edge)dests.next();
	    if (!edge.getDest().isFileWriter()) 
		continue;
	    InputTraceNode fileI = edge.getDest();
	    
	    IntraTraceBuffer buf = IntraTraceBuffer.getBuffer(fileI, 
							      fileI.getNextFilter());
	    assert fileI.getNextFilter().getFilter() instanceof FileOutputContent :
		"File Writer shoudlbe a FileOutputContent";
	    
	    FileOutputContent fileOC = (FileOutputContent)fileI.getNextFilter().getFilter();
	    FilterInfo filterInfo = FilterInfo.getFilterInfo(fileI.getNextFilter());
	    //do nothing if we have already generated the code for this file writer
	    //for this stage
	    if (buf.getDRAM().isFileWriter() &&
		buf.getDRAM().getFileWriter().isVisited(init, primepump))
		continue;
	  
	    if (!OffChipBuffer.unnecessary(fileI)) {
		//generate the dram commands
		generateInputDRAMCommands(fileI, init, primepump);
		//generate the switch code
		joinInputTrace(fileI, init, primepump);
	    }
	}
    }
    

    private static void generateInputDRAMCommands(InputTraceNode input, boolean init, boolean primepump) 
    {
	FilterTraceNode filter = (FilterTraceNode)input.getNext();

	//don't do anything for redundant buffers
	if (IntraTraceBuffer.getBuffer(input, filter).redundant())
	    return;

	//number of total items that are being joined
	int items = FilterInfo.getFilterInfo(filter).totalItemsReceived(init, primepump);
	//do nothing if there is nothing to do
	if (items == 0)
	    return;
	
	//add to the init code with the init buffers except in steady
	int stage = 0;
	if (!init && !primepump)
	    stage = 3;

	assert items % input.totalWeights() == 0: 
	    "weights on input trace node does not divide evenly with items received";
	//iterations of "joiner"
	int iterations = items / input.totalWeights();
	int typeSize = Util.getTypeSize(filter.getFilter().getInputType());
	    
	//generate the commands to read from the o/i temp buffer
	//for each input to the input trace node
	for (int i = 0; i < input.getSources().length; i++) {
	    //get the first non-redundant buffer
	    OffChipBuffer srcBuffer = 
		InterTraceBuffer.getBuffer(input.getSources()[i]).getNonRedundant();
	    SpaceTimeBackend.println("Generate the DRAM read command for " + srcBuffer);
	    int readWords = iterations * typeSize * 
		input.getWeight(input.getSources()[i]);
	    if (srcBuffer.getDest() instanceof OutputTraceNode &&
		((OutputTraceNode)srcBuffer.getDest()).isFileReader())
		srcBuffer.getOwner().getComputeCode().addFileCommand(true, init || primepump,
								 readWords, srcBuffer);
	    else 
		srcBuffer.getOwner().getComputeCode().addDRAMCommand(true, stage,
								 Util.cacheLineDiv(readWords * 4), 
								 srcBuffer, true);
	}

	//generate the command to write to the dest of the input trace node
	OffChipBuffer destBuffer = IntraTraceBuffer.getBuffer(input, filter);
	int writeWords = items * typeSize;
	if (input.isFileWriter() && OffChipBuffer.unnecessary(input))
	    destBuffer.getOwner().getComputeCode().addFileCommand(false, init || primepump,
								  writeWords, destBuffer);
	else						      
	    destBuffer.getOwner().getComputeCode().addDRAMCommand(false, stage,
								  Util.cacheLineDiv(writeWords * 4), 
								  destBuffer, false);
    }

    private static void outputDRAMCommands(OutputTraceNode output, boolean init, boolean primepump)
    {
	FilterTraceNode filter = (FilterTraceNode)output.getPrevious();
	FilterInfo filterInfo = FilterInfo.getFilterInfo(filter);

	//don't do anything for a redundant buffer
	if (OffChipBuffer.unnecessary(output))
	    return;
	
	//if we are in the init set to zero, 1 to primepump
	//if steady set to 3, used for addDRAMCommand(...)
	int stage;
	if (init)
	    stage = 0;
	else if (primepump)
	    stage = 1;
	else //if (!init && !primepump)
	    stage = 3;
	
	OffChipBuffer srcBuffer = IntraTraceBuffer.getBuffer(filter, output);
	int readWords = FilterInfo.getFilterInfo(filter).totalItemsSent(init, primepump) *
	    Util.getTypeSize(filter.getFilter().getOutputType());
	if (readWords > 0) {
	    SpaceTimeBackend.println("Generating the read command for " + output + " on " +
				     srcBuffer.getOwner() + (primepump ? "(primepump)" : ""));
	    //in the primepump stage a real output trace always reads from the init buffers
	    //never use stage 2 for reads
	    if (output.isFileReader() && OffChipBuffer.unnecessary(output))
		srcBuffer.getOwner().getComputeCode().addFileCommand(true, init || primepump, 
								     readWords,
								     srcBuffer);
	    else
		srcBuffer.getOwner().getComputeCode().addDRAMCommand(true, (stage < 3 ? 0 : 3),
								     Util.cacheLineDiv(readWords * 4),
								     srcBuffer, true);
	}
	
	
	//now generate the store drm command
	Iterator dests = output.getDestSet().iterator();
	while (dests.hasNext()) {
	    Edge edge = (Edge)dests.next();
	    InterTraceBuffer destBuffer = InterTraceBuffer.getBuffer(edge);
	    int typeSize = Util.getTypeSize(edge.getType());
	    int writeWords = typeSize;
	    //do steady-state
	    if (stage == 3) 
		writeWords *= edge.steadyItems();
	    else if (stage == 0)
		writeWords *= edge.initItems();
	    else 
		writeWords *= edge.primePumpInitItems();
	    //make write bytes cache line div
	    if (writeWords > 0) {
		if (destBuffer.getEdge().getDest().isFileWriter() && 
		    OffChipBuffer.unnecessary(destBuffer.getEdge().getDest()))
		    destBuffer.getOwner().getComputeCode().addFileCommand(false, init || primepump,
									  writeWords,
									  destBuffer);
		else 
		    destBuffer.getOwner().getComputeCode().addDRAMCommand(false, stage,
									  Util.cacheLineDiv(writeWords * 4), 
									  destBuffer, false);
	    }
	    
	    
	    //generate the dram commands to write into the steady buffer in the primepump stage
	    if (primepump) {
		writeWords = typeSize * 
		    (edge.primePumpItems() - edge.primePumpInitItems());
		//generate the dram command in stage 2 (init schedule, with steady buffers...)
		if (writeWords > 0) {
		    if (destBuffer.getEdge().getDest().isFileWriter()  && 
			OffChipBuffer.unnecessary(destBuffer.getEdge().getDest()))
			destBuffer.getOwner().getComputeCode().addFileCommand(false, init || primepump,
									      writeWords,
									      destBuffer);
		    else
			destBuffer.getOwner().getComputeCode().addDRAMCommand(false, 2,
									      Util.cacheLineDiv(writeWords * 4), 
									      destBuffer, false);
		}
		
	    }
	    
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
	    OffChipBuffer buffer = IntraTraceBuffer.getBuffer((InputTraceNode)filterNode.getPrevious(),
							      filterNode).getNonRedundant();
	    
	    if (buffer == null)		
		return;
	    
	    //get the number of items received
	    int items = filterInfo.totalItemsReceived(init, primepump); 
	    
	    //return if there is nothing to receive
	    if (items == 0)
		return;
	    
	    int stage = 0;
	    if (!init && !primepump)
		stage = 3;

	    //the transfer size rounded up to by divisible by a cacheline
	    int words = 
		(items * Util.getTypeSize(filterNode.getFilter().getInputType()));
	 
	    if (buffer.getDest() instanceof OutputTraceNode &&
		((OutputTraceNode)buffer.getDest()).isFileReader())
		tile.getComputeCode().addFileCommand(true, init || primepump,
						     words, buffer);
	    else
		tile.getComputeCode().addDRAMCommand(true, stage, 
						     Util.cacheLineDiv(words * 4),
						     buffer, true);
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
	    OutputTraceNode output = (OutputTraceNode)filterNode.getNext();
	    OffChipBuffer buffer =
		IntraTraceBuffer.getBuffer(filterNode,
					   output).getNonRedundant();
	    if (buffer == null)
		return;

	    //set to true if the only destination is a file, and 
	    //everything in between is unnecessary
	    boolean fileDest = false;
	    if (output.oneOutput() && OffChipBuffer.unnecessary(output) &&
		output.getSingleEdge().getDest().isFileWriter() &&
		OffChipBuffer.unnecessary(output.getSingleEdge().getDest()))
		fileDest = true;
		
	    
	    int stage = 0;
	    if (!init && !primepump)
		stage = 3;

	    //get the number of items sent
	    int items = filterInfo.totalItemsSent(init, primepump);
	    //if this is the primepump, subtract the primepump items not comsumed
	    //in the primepump stage, they get transfered below
	    if (primepump) {
		SpaceTimeBackend.println("Filter Output DRAM command " + filterNode + " has " +
					 filterInfo.primePumpItemsNotConsumed() + "steady primp-pump items");
		
		items -= filterInfo.primePumpItemsNotConsumed();
	    }
	    
	    if (items > 0 ) {
		int words = 
		    (items * Util.getTypeSize(filterNode.getFilter().getOutputType()));
		if (fileDest) 
		    tile.getComputeCode().addFileCommand(false, init || primepump, 
							 words, buffer);
		else {
		    SpaceTimeBackend.println("Generating DRAM store command with " + items + " items, typesize " + 
					     Util.getTypeSize(filterNode.getFilter().getOutputType()) + 
					     " and " + words + " words");
		    tile.getComputeCode().addDRAMCommand(false, stage, 
							 Util.cacheLineDiv(words * 4),
							 buffer, false);
		}
	    }
	    if (primepump && filterInfo.primePumpItemsNotConsumed() > 0) {
		int words =
		    (filterInfo.primePumpItemsNotConsumed() * 
		     Util.getTypeSize(filterNode.getFilter().getOutputType()));
		if (fileDest) 
		    tile.getComputeCode().addFileCommand(false, init || primepump, 
							 words, buffer);
		else {
		    SpaceTimeBackend.println("Generating DRAM store command with " + filterInfo.primePumpItemsNotConsumed()
					     + " items, typesize " + 
					     Util.getTypeSize(filterNode.getFilter().getOutputType()) + 
					     " and " + words + " words at end of primepipe");
		    tile.getComputeCode().addDRAMCommand(false, 2, 
							 Util.cacheLineDiv(words * 4),
							 buffer, false);
		}
	    }
	}
    }
    
    //see if the switch for the filter needs disregard some of the input because
    //it is not a multiple of the cacheline
    private static void handleUnneededInput(FilterTraceNode traceNode, boolean init, boolean primepump, 
					    int items) 
    {
	InputTraceNode in = (InputTraceNode)traceNode.getPrevious();

	FilterInfo filterInfo = FilterInfo.getFilterInfo(traceNode);
	//int items = filterInfo.totalItemsReceived(init, primepump), typeSize;
	int typeSize;
	
	typeSize = Util.getTypeSize(traceNode.getFilter().getInputType());
	
	//see if it is a mulitple of the cache line
	if ((items * typeSize) % RawChip.cacheLineWords != 0) {
	    int dummyItems = RawChip.cacheLineWords - ((items * typeSize) % RawChip.cacheLineWords);
	    SpaceTimeBackend.println("Received items (" + (items * typeSize) + 
				     ") not divisible by cache line, disregard " + dummyItems);
	    SwitchCodeStore.disregardIncoming(IntraTraceBuffer.getBuffer(in, traceNode).getDRAM(),
					      dummyItems,
					      init || primepump);
	}
    }
    
    //see if the switch needs to generate dummy values to fill a cache line in the streaming
    //dram 
    private static void fillCacheLine(FilterTraceNode traceNode, boolean init, boolean primepump,
				      int items) 
    {

	OutputTraceNode out = (OutputTraceNode)traceNode.getNext();
	FilterInfo filterInfo = FilterInfo.getFilterInfo(traceNode);
	
	//get the number of items sent
	//	int items = filterInfo.totalItemsSent(init, primepump), typeSize;
	int typeSize;

	typeSize = Util.getTypeSize(traceNode.getFilter().getOutputType());
	//see if a multiple of cache line, if not generate dummy values...
	if ((items * typeSize) % RawChip.cacheLineWords != 0) {
	    int dummyItems = RawChip.cacheLineWords - ((items * typeSize) % RawChip.cacheLineWords);
	    SpaceTimeBackend.println("Sent items (" + (items * typeSize) + 
				     ") not divisible by cache line, add " + dummyItems);
	    
	    SwitchCodeStore.dummyOutgoing(IntraTraceBuffer.getBuffer(traceNode, out).getDRAM(),
					  dummyItems,
					  init || primepump);
	}
    }
    

    private static void joinInputTrace(InputTraceNode traceNode, boolean init, boolean primepump)
    {
	FilterTraceNode filter = (FilterTraceNode)traceNode.getNext();
	
	//do not generate the switch code if it is not necessary
	if (OffChipBuffer.unnecessary(traceNode))
	    return;
	    
	FilterInfo filterInfo = FilterInfo.getFilterInfo(filter);
	//calculate the number of items received
	int items = filterInfo.totalItemsReceived(init, primepump),
	    iterations, stage = 1, typeSize;

	//noting to do for this stage
	if (items == 0)
	    return;

	//the stage we are generating code for as used below for generateSwitchCode()
	if (!init && !primepump) 
	    stage = 2;
	
	typeSize = Util.getTypeSize(filter.getFilter().getInputType());
	//the numbers of times we should cycle thru this "joiner"
	assert items % traceNode.totalWeights() == 0: 
	    "weights on input trace node does not divide evenly with items received";
	iterations = items / traceNode.totalWeights();
	
	StreamingDram[] dest = {IntraTraceBuffer.getBuffer(traceNode, filter).getDRAM()};
	
	//generate comments to make the code easier to read when debugging
	dest[0].getNeighboringTile().getSwitchCode().
	    appendComment(init || primepump, 
			  "Start join: This is the dest (" + filter.toString() + ")");

	Iterator sources = traceNode.getSourceSet().iterator();
	while (sources.hasNext()) {
	    StreamingDram dram = 
		InterTraceBuffer.getBuffer((Edge)sources.next()).getNonRedundant().getDRAM();
	    dram.getNeighboringTile().getSwitchCode().
		appendComment(init || primepump, 
			      "Start join: This a source (" + dram.toString() + ")");
	}
	//end of comments 

	for (int i = 0; i < iterations; i++) {
	    for (int j = 0; j < traceNode.getWeights().length; j++) {
		//get the source buffer, pass thru redundant buffer(s)
		StreamingDram source = 
		    InterTraceBuffer.getBuffer(traceNode.getSources()[j]).getNonRedundant().getDRAM();
		for (int k = 0; k < traceNode.getWeights()[j]; k++) {
		    for (int q = 0; q < typeSize; q++)
			SwitchCodeStore.generateSwitchCode(source, dest, stage);
		}
	    }
	}
	//because transfers must be cache line size divisible...
	//generate dummy values to fill the cache line!
	if ((items * typeSize) % RawChip.cacheLineWords != 0 &&
	    !(traceNode.isFileWriter() && OffChipBuffer.unnecessary(traceNode))) {
	    int dummy = RawChip.cacheLineWords - ((items * typeSize) % RawChip.cacheLineWords);
	    SwitchCodeStore.dummyOutgoing(dest[0], dummy, init || primepump);
	}
	//disregard remainder of inputs coming from temp offchip buffers
	for (int i = 0; i < traceNode.getSources().length; i++) {
	    Edge edge = traceNode.getSources()[i];
	    int remainder =
		((iterations * typeSize * 
		 traceNode.getWeight(edge)) % RawChip.cacheLineWords);
	    if (remainder > 0 && !(edge.getSrc().isFileReader() && 
				   OffChipBuffer.unnecessary(edge.getSrc())))
		SwitchCodeStore.disregardIncoming(InterTraceBuffer.getBuffer(edge).getDRAM(),
						  RawChip.cacheLineWords - remainder, 
						  init || primepump);
	}

	//generate comments to make the code easier to read when debugging
	dest[0].getNeighboringTile().getSwitchCode().
	    appendComment(init || primepump, 
			  "End join: This is the dest (" + filter.toString() + ")");

	sources = traceNode.getSourceSet().iterator();
	while (sources.hasNext()) {
	    StreamingDram dram = 
		InterTraceBuffer.getBuffer((Edge)sources.next()).getNonRedundant().getDRAM();
	    dram.getNeighboringTile().getSwitchCode().
		appendComment(init || primepump, 
			  "End join: This a source (" + dram.toString() + ")");
	    
	}
	//end of comments 
    }
    
    //generate the switch code to split the output trace 
    //be careful about the primepump stage
    //set cacheAlign to true if we must transfers to cache line
    private static void splitOutputTrace(OutputTraceNode traceNode, boolean init, boolean primepump)
					
    {
	FilterTraceNode filter = (FilterTraceNode)traceNode.getPrevious();
	//check to see if the splitting is necessary
	if (OffChipBuffer.unnecessary(traceNode))
	    return;
	
	FilterInfo filterInfo = FilterInfo.getFilterInfo(filter);
	//calculate the number of items sent
	int items = filterInfo.totalItemsSent(init, primepump);
	StreamingDram sourcePort = IntraTraceBuffer.getBuffer(filter, traceNode).getDRAM();
	//the numbers of times we should cycle thru this "splitter"
	assert items % traceNode.totalWeights() == 0: 
	    "weights on output trace node does not divide evenly with items sent";
	int iterations = items / traceNode.totalWeights();
	//number of iterations that we store into the steady buffer during pp
	int ppSteadyIt = 0;
	
	//adjust iterations to first be only the iterations of the splitter that are
	//stored in the init buffer and set ppSteadyIt to be the remaining iterations
	if (primepump) {
	    Iterator dests = traceNode.getDestSet().iterator();
	    assert dests.hasNext() :
		"Output should have at least one dest";
	    Edge edge = (Edge)dests.next();
	    SpaceTimeBackend.println(edge.getDest().debugString(false));
	    FilterInfo downstream = FilterInfo.getFilterInfo(edge.getDest().getNextFilter());
	    //the number of times the source filter fires in the pp while feeding the steady buffer
	    int ppFilterIt = 
		(filterInfo.primePumpTrue - downstream.primePumpTrue) * filterInfo.steadyMult;
	    assert ((ppFilterIt * filterInfo.push) % traceNode.totalWeights() == 0) :
		"Error: Inconsistent primepump stats for output trace node";
	    ppSteadyIt = (ppFilterIt * filterInfo.push) / traceNode.totalWeights();
	    //subtract from the iterations
	    iterations -= ppSteadyIt;
	    //check the sanity of the primepump stage
	    while (dests.hasNext()){
		edge = (Edge)dests.next();
		SpaceTimeBackend.println(edge.getDest().debugString(false));
		assert ppFilterIt == filterInfo.steadyMult * (filterInfo.primePumpTrue - 
				 FilterInfo.getFilterInfo(edge.getDest().getNextFilter()).primePumpTrue) :
		    "Error: Inconsistent primepump stats for output trace node\n " + traceNode.debugString(false);
	    }
	}

	//add some comments to the switch code
	sourcePort.getNeighboringTile().getSwitchCode().
	    appendComment(init || primepump,
			  "Start split: This is the source (" + filter.toString() + ")");
	Iterator dests = traceNode.getDestSet().iterator();
	while (dests.hasNext()) {
	    StreamingDram dram =
		InterTraceBuffer.getBuffer((Edge)dests.next()).getDRAM();
	    dram.getNeighboringTile().getSwitchCode().
		appendComment(init || primepump, 
			      "Start split: This a dest (" + dram.toString() + ")");
	}
	
	//	SpaceTimeBackend.println("Split Output Trace: " + traceNode + "it: " + iterations + " ppSteadyIt: " + 
	//ppSteadyIt);
    //	System.out.println(traceNode.debugString());
	performSplitOutputTrace(traceNode, filter, filterInfo, init, primepump, iterations);
	
	if (primepump && ppSteadyIt > 0)
	    performSplitOutputTrace(traceNode, filter, filterInfo, init, primepump, ppSteadyIt);
	
	//because transfers must be cache line size divisible...
	//disregard the dummy values coming out of the dram
	//for the primepump we always read out of the init buffer for real output tracenodes
	int typeSize = Util.getTypeSize(filterInfo.filter.getOutputType());
	int mod = 
		(((iterations + ppSteadyIt) * traceNode.totalWeights() * typeSize) % RawChip.cacheLineWords);
	//don't cache align file readers
	if (mod > 0 && !(traceNode.isFileReader() && OffChipBuffer.unnecessary(traceNode))) {
	    int remainder = RawChip.cacheLineWords - mod;
	    //System.out.println("Remainder for disregarding input on split trace: " + remainder);
	    SwitchCodeStore.disregardIncoming(sourcePort, remainder, init || primepump);
	}
	
	//add some comments to the switch code
	sourcePort.getNeighboringTile().getSwitchCode().
	    appendComment(init || primepump,
			  "End split: This is the source (" + filter.toString() + ")");
	dests = traceNode.getDestSet().iterator();
	while (dests.hasNext()) {
	    StreamingDram dram =
		InterTraceBuffer.getBuffer((Edge)dests.next()).getDRAM();
	    dram.getNeighboringTile().getSwitchCode().
		appendComment(init || primepump, 
			      "End split: This a dest (" + dram.toString() + ")");
	}
	
								      
    }

    private static void performSplitOutputTrace(OutputTraceNode traceNode, FilterTraceNode filter,
						FilterInfo filterInfo, boolean init, boolean primepump,
						int iterations)
    {
	if (iterations > 0) {
	    int stage = 1, typeSize;
	    //the stage we are generating code for as used below for generateSwitchCode()
	    if (!init && !primepump) 
		stage = 2;
	    
	    typeSize = Util.getTypeSize(filter.getFilter().getOutputType());
	    
	    SpaceTimeBackend.println("Generating Switch Code for " + traceNode +
				     " iterations " + iterations);
	    
	    //is there a load immediate in the switch instruction set?!
	    //I guess not, if switch instruction memory is a problem
	    //this naive implementation will have to change
	    StreamingDram sourcePort = IntraTraceBuffer.getBuffer(filter, traceNode).getDRAM();
	    for (int i = 0; i < iterations; i++) {
		for (int j = 0; j < traceNode.getWeights().length; j++) {
		    for (int k = 0; k < traceNode.getWeights()[j]; k++) {
			//generate the array of compute node dests
			ComputeNode dests[] = new ComputeNode[traceNode.getDests()[j].length];
			for (int d = 0; d < dests.length; d++) 
			    dests[d] = InterTraceBuffer.getBuffer(traceNode.getDests()[j][d]).getDRAM();
			for (int q = 0; q < typeSize; q++)
			    SwitchCodeStore.generateSwitchCode(sourcePort, 
							       dests, stage);
		    }
		}
	    }
	    
	    //write dummy values into each temp buffer with a remainder
	    Iterator it = traceNode.getDestSet().iterator();
	    while (it.hasNext()) {
		Edge edge = (Edge)it.next();
		int remainder = ((typeSize * iterations * traceNode.getWeight(edge)) %
				 RawChip.cacheLineWords);
		//don't fill cache line for files
		if (remainder > 0 && !(edge.getDest().isFileWriter() && 
				       OffChipBuffer.unnecessary(edge.getDest())))
		    SwitchCodeStore.dummyOutgoing(InterTraceBuffer.getBuffer(edge).getDRAM(),
						  RawChip.cacheLineWords - remainder, 
						  init || primepump);
	    }   
	}
    }
    
    private static void createSwitchCodeLinear(FilterTraceNode node, Trace parent, 
					       FilterInfo filterInfo, boolean init, boolean primePump, 
					       RawTile tile, RawChip rawChip) {
	System.err.println("Creating switchcode linear: "+node);

	//if we are in the init or primepump call the regular create switch code
	if (init || primePump) {
	    createSwitchCode(node, parent, filterInfo, init, primePump, tile, rawChip);
	    return;
	}
	
	//START Copy Gordo
	int mult, sentItems = 0;
	
	//don't cache align if the only source is a file reader
	boolean cacheAlignSource = true;
	if (node.getPrevious() instanceof InputTraceNode) {
	    OffChipBuffer buf = IntraTraceBuffer.getBuffer((InputTraceNode)node.getPrevious(), 
							   node).getNonRedundant();
	    if (buf != null && buf.getDest() instanceof OutputTraceNode &&
		((OutputTraceNode)buf.getDest()).isFileReader())
		cacheAlignSource = false;
	}
	
	//don't cache align the dest if the true dest is a file writer
	boolean cacheAlignDest = true; 
	if (node.getNext() instanceof OutputTraceNode) {
	    OutputTraceNode output = (OutputTraceNode)node.getNext();
	    if (output.oneOutput() && OffChipBuffer.unnecessary(output) &&
		output.getSingleEdge().getDest().isFileWriter() &&
		OffChipBuffer.unnecessary(output.getSingleEdge().getDest()))
		cacheAlignDest = false;
	}
	

      
	if (primePump)
	    mult = filterInfo.primePump - (filterInfo.push == 0 ? 0 :
					   (filterInfo.primePumpItemsNotConsumed() / filterInfo.push));
	else if (init)
	    mult = filterInfo.initMult;
	else 
	    mult = filterInfo.steadyMult;
	//STOP Copy from Gordo


	ComputeNode sourceNode = null;

	//int mult;
	//if (init)
	//mult = filterInfo.initMult;
	//else 
	//mult = filterInfo.steadyMult;
	
	/* OLD
	  if (node.getPrevious().isFilterTrace())
	  sourceNode = rawChip.getTile(((FilterTraceNode)node.getPrevious()).getX(),  
	  ((FilterTraceNode)node.getPrevious()).getY());
	  else {
	  if (KjcOptions.magicdram && node.getPrevious() !=  null &&
	  node.getPrevious().isInputTrace() &&
	  tile.hasIODevice()) 
	  sourceNode = tile.getIODevice();
	  else { 
	  System.err.println("BLAH1");
	  return;
	  }
	  }*/
	
	if (node.getPrevious().isFilterTrace())
	    sourceNode = rawChip.getTile(((FilterTraceNode)node.getPrevious()).getX(), 
					 ((FilterTraceNode)node.getPrevious()).getY());
	else {
	    if (KjcOptions.magicdram && node.getPrevious() != null &&
		node.getPrevious().isInputTrace() &&
		tile.hasIODevice()) 
		sourceNode = tile.getIODevice();
	    else 
		sourceNode = 
		    IntraTraceBuffer.getBuffer((InputTraceNode)node.getPrevious(), 
					       node).getNonRedundant().getDRAM();
	}
	
	SwitchIPort src = rawChip.getIPort(sourceNode, tile);
	SwitchIPort src2 = rawChip.getIPort2(sourceNode, tile);
	sourceNode = null;
	ComputeNode destNode = null;

	/* OLD
	  if (node.getNext().isFilterTrace())
	  destNode = rawChip.getTile(((FilterTraceNode)node.getNext()).getX(),  
	  ((FilterTraceNode)node.getNext()).getY());
	  else {
	  if (KjcOptions.magicdram && node.getNext() !=  null &&
	  node.getNext().isOutputTrace() && tile.hasIODevice())
	  destNode = tile.getIODevice();
	  else {
	  System.err.println("BLAH2");
	  return;
	  }
	  }*/

	if (node.getNext().isFilterTrace())
	    destNode = rawChip.getTile(((FilterTraceNode)node.getNext()).getX(), 
				       ((FilterTraceNode)node.getNext()).getY());
	else {
	    if (KjcOptions.magicdram && node.getNext() != null &&
		node.getNext().isOutputTrace() && tile.hasIODevice())
		destNode = tile.getIODevice();
	    else {
		destNode = 
		    IntraTraceBuffer.getBuffer(node, (OutputTraceNode)node.getNext()).
		    getNonRedundant().getDRAM();
	    }
	    
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
	System.err.println("Getting HERE!");
	code.appendIns(new MoveIns(SwitchReg.R3,SwitchIPort.CSTO),init||primePump);
	code.appendIns(new Comment("HERE!"),init||primePump);
	boolean first=true;
	for(int i = 0; i<numPop-1; i++)
	    for(int j = 0; j<pop; j++) {
		FullIns ins = new FullIns(tile, new MoveIns(SwitchReg.R1, src));
		/*if (KjcOptions.magicdram && node.getPrevious() != null &&
		  node.getPrevious().isInputTrace())
		  createMagicDramLoad((InputTraceNode)node.getPrevious(), 
		  node, (init || primePump), rawChip);*/
		ins.addRoute(src, SwitchOPort.CSTI);
		//if(!first) {
		if(!end) {
		    /*if (KjcOptions.magicdram && node.getPrevious() != null &&
		      node.getPrevious().isInputTrace())
		      createMagicDramLoad((InputTraceNode)node.getPrevious(), 
		      node, (init || primePump), rawChip);
		      if (KjcOptions.magicdram && node.getNext() != null &&
		      node.getNext().isOutputTrace())
		      createMagicDramStore((OutputTraceNode)node.getNext(), 
		      node, (init || primePump), rawChip);*/
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
		    /*if (KjcOptions.magicdram && node.getPrevious() != null &&
		      node.getPrevious().isInputTrace())
		      createMagicDramLoad((InputTraceNode)node.getPrevious(), 
		      node, (init || primePump), rawChip);*/
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
		/*if (KjcOptions.magicdram && node.getPrevious() != null &&
		  node.getPrevious().isInputTrace())
		  createMagicDramLoad((InputTraceNode)node.getPrevious(), 
		  node, (init || primePump), rawChip);*/
		ins.addRoute(src, SwitchOPort.CSTI);
		if(!first) {
		    if(!end) {
			/*if (KjcOptions.magicdram && node.getPrevious() != null &&
			  node.getPrevious().isInputTrace())
			  createMagicDramLoad((InputTraceNode)node.getPrevious(), 
			  node, (init || primePump), rawChip);
			  if (KjcOptions.magicdram && node.getNext() != null &&
			  node.getNext().isOutputTrace())
			  createMagicDramStore((OutputTraceNode)node.getNext(), 
			  node, (init || primePump), rawChip);*/
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
		    /*if (KjcOptions.magicdram && node.getPrevious() != null &&
		      node.getPrevious().isInputTrace())
		      createMagicDramLoad((InputTraceNode)node.getPrevious(), 
		      node, (init || primePump), rawChip);*/
		    newIns.addRoute(src2, SwitchOPort.CSTI2);
		    code.appendIns(newIns, init||primePump);
		}
		if(!first) {
		    newIns = new FullIns(tile);
		    /*if (KjcOptions.magicdram && node.getNext() != null &&
		      node.getNext().isOutputTrace())
		      createMagicDramStore((OutputTraceNode)node.getNext(), 
		      node, (init || primePump), rawChip);*/
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
	for(int turn=0;turn<turns2+1;turn++) {
	    if(turn==turns2)
		code.appendIns(label, init||primePump);
	    for(int i = 0;i<numTimes;i++) {
		for(int j=0;j<pop;j++) {
		    if(numPop==1&&j==0)
			pendingSends++;
		    times++;
		    ins = new FullIns(tile, new MoveIns(SwitchReg.R1, src));
		    /*if (KjcOptions.magicdram && node.getPrevious() != null &&
		      node.getPrevious().isInputTrace())
		      createMagicDramLoad((InputTraceNode)node.getPrevious(), 
		      node, (init || primePump), rawChip);*/
		    ins.addRoute(src, SwitchOPort.CSTI);
		    if(!end) {
			/*if (KjcOptions.magicdram && node.getPrevious() != null &&
			  node.getPrevious().isInputTrace())
			  createMagicDramLoad((InputTraceNode)node.getPrevious(), 
			  node, (init || primePump), rawChip);
			  if (KjcOptions.magicdram && node.getNext() != null &&
			  node.getNext().isOutputTrace())
			  createMagicDramStore((OutputTraceNode)node.getNext(), 
			  node, (init || primePump), rawChip);*/
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
				/*if (KjcOptions.magicdram && node.getPrevious() != null &&
				  node.getPrevious().isInputTrace())
				  createMagicDramLoad((InputTraceNode)node.getPrevious(), 
				  node, (init || primePump), rawChip);*/
					ins.addRoute(src2,SwitchOPort.CSTI2);
				    }
				    /*if (KjcOptions.magicdram && node.getNext() != null &&
				      node.getNext().isOutputTrace())
				      createMagicDramStore((OutputTraceNode)node.getNext(), 
				      node, (init || primePump), rawChip);*/
			    
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
					    /*if (KjcOptions.magicdram && node.getPrevious() != null &&
					      node.getPrevious().isInputTrace())
					      createMagicDramLoad((InputTraceNode)node.getPrevious(), 
					      node, (init || primePump), rawChip);*/
					    ins.addRoute(src2,SwitchOPort.CSTI2);
					}
				/*if (KjcOptions.magicdram && node.getNext() != null &&
				  node.getNext().isOutputTrace())
				  createMagicDramStore((OutputTraceNode)node.getNext(), 
				  node, (init || primePump), rawChip);*/
				
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
	
	//ins.setProcessorIns(new JumpIns(label.getLabel()));
	ins.setProcessorIns(new BnezdIns(SwitchReg.R3,SwitchReg.R3,label.getLabel()));

	//START Copy from Gordo
	//now we must take care of the remaining items on the input tape 
	//after the initialization phase if the upstream filter produces more than
	//we consume in init
	if (init && filterInfo.remaining > 0) {
	    appendReceiveInstructions(node, 
				      filterInfo.remaining * Util.getTypeSize(node.getFilter().getInputType()),
				      filterInfo, init, false, tile, rawChip);
	}

	//we must add some switch instructions to account for the fact
	//that we must transfer cacheline sized chunks in the streaming dram
	//do it for the init and the steady state, primepump 

	//some sanity checks!

	/*if (primePump)
	  assert (sentItems == (filterInfo.totalItemsSent(init, primePump) - 
	  filterInfo.primePumpItemsNotConsumed())) :
	  "insane";
	  else
	  assert (sentItems == filterInfo.totalItemsSent(init, primePump)) :
	  "insane";*/

	//generate code to fill the remainder of the cache line
	if (!KjcOptions.magicdram && node.getNext().isOutputTrace() && 
	    cacheAlignDest)
	    fillCacheLine(node, init, primePump, sentItems);

	if (primePump && filterInfo.push > 0 &&
	    filterInfo.primePumpItemsNotConsumed() / filterInfo.push > 0) {
	    mult = (filterInfo.primePumpItemsNotConsumed() / filterInfo.push);
	    for (int i = 0; i < mult; i++) {
		//append the receive code
		createReceiveCode(i, node, parent, filterInfo, init, primePump, tile, rawChip);
		//append the send code 
		createSendCode(i, node, parent, filterInfo, init, primePump, tile, rawChip);
	    }
	    //handle filling the cache line for the steady buffer of the primepump 
	    //stage
	    if (!KjcOptions.magicdram && node.getNext().isOutputTrace() && cacheAlignDest)
		fillCacheLine(node, init, primePump, 
			      filterInfo.primePumpItemsNotConsumed());
	}
	//because all dram transfers must be multiples of cacheline
	//generate code to disregard the remainder of the transfer
	if (!KjcOptions.magicdram && node.getPrevious().isInputTrace() && cacheAlignSource)
	    handleUnneededInput(node, init, primePump, 
				filterInfo.totalItemsReceived(init, primePump));

	//STOP Copy From Gordo
    }


    //create the intra-trace filter switch code...
    private static void createSwitchCode(FilterTraceNode node, Trace parent, 
					 FilterInfo filterInfo,
					 boolean init, boolean primePump, RawTile tile,
					 RawChip rawChip) 
    {
	int mult, sentItems = 0;
	
	//don't cache align if the only source is a file reader
	boolean cacheAlignSource = true;
	if (node.getPrevious() instanceof InputTraceNode) {
	    OffChipBuffer buf = IntraTraceBuffer.getBuffer((InputTraceNode)node.getPrevious(), 
							   node).getNonRedundant();
	    if (buf != null && buf.getDest() instanceof OutputTraceNode &&
		((OutputTraceNode)buf.getDest()).isFileReader())
		cacheAlignSource = false;
	}
	
	//don't cache align the dest if the true dest is a file writer
	boolean cacheAlignDest = true; 
	if (node.getNext() instanceof OutputTraceNode) {
	    OutputTraceNode output = (OutputTraceNode)node.getNext();
	    if (output.oneOutput() && OffChipBuffer.unnecessary(output) &&
		output.getSingleEdge().getDest().isFileWriter() &&
		OffChipBuffer.unnecessary(output.getSingleEdge().getDest()))
		cacheAlignDest = false;
	}
	

      
	if (primePump)
	    mult = filterInfo.primePump - (filterInfo.push == 0 ? 0 :
					   (filterInfo.primePumpItemsNotConsumed() / filterInfo.push));
	else if (init)
	    mult = filterInfo.initMult;
	else 
	    mult = filterInfo.steadyMult;

	for (int i = 0; i < mult; i++) {
	    //append the receive code
	    createReceiveCode(i, node, parent, filterInfo, init, primePump, tile, rawChip);
	    //append the send code 
	    sentItems += createSendCode(i, node, parent, filterInfo, init, primePump, tile, rawChip);
	}
	
	//now we must take care of the remaining items on the input tape 
	//after the initialization phase if the upstream filter produces more than
	//we consume in init
	if (init && filterInfo.remaining > 0) {
	    appendReceiveInstructions(node, 
				      filterInfo.remaining * Util.getTypeSize(node.getFilter().getInputType()),
				      filterInfo, init, false, tile, rawChip);
	}

	//we must add some switch instructions to account for the fact
	//that we must transfer cacheline sized chunks in the streaming dram
	//do it for the init and the steady state, primepump 

	//some sanity checks!
	if (primePump)
	    assert (sentItems == (filterInfo.totalItemsSent(init, primePump) - 
				  filterInfo.primePumpItemsNotConsumed())) :
	    "insane";
	else
	    assert (sentItems == filterInfo.totalItemsSent(init, primePump)) :
	    "insane";

	//generate code to fill the remainder of the cache line
	if (!KjcOptions.magicdram && node.getNext().isOutputTrace() && 
	    cacheAlignDest)
	    fillCacheLine(node, init, primePump, sentItems);

	if (primePump && filterInfo.push > 0 &&
	    filterInfo.primePumpItemsNotConsumed() / filterInfo.push > 0) {
	    mult = (filterInfo.primePumpItemsNotConsumed() / filterInfo.push);
	    for (int i = 0; i < mult; i++) {
		//append the receive code
		createReceiveCode(i, node, parent, filterInfo, init, primePump, tile, rawChip);
		//append the send code 
		createSendCode(i, node, parent, filterInfo, init, primePump, tile, rawChip);
	    }
	    //handle filling the cache line for the steady buffer of the primepump 
	    //stage
	    if (!KjcOptions.magicdram && node.getNext().isOutputTrace() && cacheAlignDest)
		fillCacheLine(node, init, primePump, 
			      filterInfo.primePumpItemsNotConsumed());
	}
	//because all dram transfers must be multiples of cacheline
	//generate code to disregard the remainder of the transfer
	if (!KjcOptions.magicdram && node.getPrevious().isInputTrace() && cacheAlignSource)
	    handleUnneededInput(node, init, primePump, 
				filterInfo.totalItemsReceived(init, primePump));

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
	
	//do nothing if there is nothing to do
	if (itemsReceiving == 0)
	    return;

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
		sourceNode = 
		    IntraTraceBuffer.getBuffer((InputTraceNode)node.getPrevious(), 
					       node).getNonRedundant().getDRAM();
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

    private static int createSendCode(int iteration, FilterTraceNode node, Trace parent, 
				       FilterInfo filterInfo, boolean init, boolean primePump, 
				       RawTile tile, RawChip rawChip) 
    {
	//get the items needed to fire and multiply it by the type 
	//size
	int items = filterInfo.itemsFiring(iteration, init);
	
	int words = items * Util.getTypeSize(node.getFilter().getOutputType());
	
	if (words == 0)
	    return 0;

	ComputeNode destNode = null;
	
	if (node.getNext().isFilterTrace())
	    destNode = rawChip.getTile(((FilterTraceNode)node.getNext()).getX(), 
				       ((FilterTraceNode)node.getNext()).getY());
	else {
	    if (KjcOptions.magicdram && node.getNext() != null &&
		node.getNext().isOutputTrace() && tile.hasIODevice())
		destNode = tile.getIODevice();
	    else {
		destNode = 
		    IntraTraceBuffer.getBuffer(node, (OutputTraceNode)node.getNext()).
		    getNonRedundant().getDRAM();
	    }
	    
	}
	
	for (int j = 0; j < words; j++) {
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
	
	return items;
    }
    
    /* worry about magic stuff later
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
    */

   private static void createMagicDramLoad(InputTraceNode node, FilterTraceNode next,
					    boolean init, RawChip rawChip) 
    {
	/*
	if (!rawChip.getTile(next.getX(), next.getY()).hasIODevice()) 
	    Utils.fail("Tile not connected to io device");
	
	MagicDram dram = (MagicDram)rawChip.getTile(next.getX(), next.getY()).getIODevice();
	
	LinkedList insList = init ? dram.initInsList : dram.steadyInsList;
	OutputTraceNode output = TraceBufferSchedule.getOutputBuffer(node);
	insList.add(new MagicDramLoad(node, output));
	dram.addBuffer(output, node);
	*/
    }

    /**
     * Generate a single magic dram store instruction for this output trace node
     **/
    private static void createMagicDramStore(OutputTraceNode node, FilterTraceNode prev, 
					     boolean init, RawChip rawChip)
					      
    {
	/*
	if (!rawChip.getTile(prev.getX(), prev.getY()).hasIODevice()) 
	    Utils.fail("Tile not connected to io device");
	//get the dram
	MagicDram dram = (MagicDram)rawChip.getTile(prev.getX(), prev.getY()).getIODevice();
	//get the list we should add to
	LinkedList insList = init ? dram.initInsList : dram.steadyInsList;
	//add the instruction
	insList.add(new MagicDramStore(node, 
				       TraceBufferSchedule.getInputBuffers(node)));
	*/
    }

      /*
    private static void generateOutputDRAMCommands(OutputTraceNode output, boolean init, 
					      boolean primepump, FilterTraceNode filter,
					      int items, int stage)
    {
	if (items == 0)
	    return;
	int iterations, typeSize;

	typeSize = Util.getTypeSize(filter.getFilter().getOutputType());
	
	//the numbers of times we should cycle thru this "splitter"
	assert items % output.totalWeights() == 0: 
	    "weights on output trace node does not divide evenly with items sent";
	iterations = items / output.totalWeights();
	
	//generate the command to read from the src of the output trace node
	OffChipBuffer srcBuffer = IntraTraceBuffer.getBuffer(filter, output);
	int readBytes = FilterInfo.getFilterInfo(filter).totalItemsSent(init, primepump) *
	    Util.getTypeSize(filter.getFilter().getOutputType()) * 4;
	readBytes = Util.cacheLineDiv(readBytes);
	SpaceTimeBackend.println("Generating the read command for " + output + " on " +
				 srcBuffer.getOwner() + (primepump ? "(primepump)" : ""));
	//in the primepump stage a real output trace always reads from the init buffers
	//never use stage 2 for reads
	srcBuffer.getOwner().getComputeCode().addDRAMCommand(true, (stage < 3 ? 1 : 3),
							     readBytes, srcBuffer, true);
 
	//generate the commands to write the o/i temp buffer dest
	Iterator dests = output.getDestSet().iterator();
	while (dests.hasNext()){
	    Edge edge = (Edge)dests.next();
	    OffChipBuffer destBuffer = InterTraceBuffer.getBuffer(edge);
	    int writeBytes = iterations * typeSize *
		output.getWeight(edge) * 4;
	    writeBytes = Util.cacheLineDiv(writeBytes);
	    destBuffer.getOwner().getComputeCode().addDRAMCommand(false, stage,
								  writeBytes, destBuffer, false);
	}
    }
    */    
}

