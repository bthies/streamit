package at.dms.kjc.spacedynamic;

import at.dms.kjc.common.*;
import at.dms.kjc.flatgraph.FlatNode;
import at.dms.kjc.flatgraph.GraphFlattener;
import at.dms.util.IRPrinter;
import at.dms.util.SIRPrinter;
import at.dms.kjc.*;
import at.dms.kjc.iterator.*;
import at.dms.kjc.sir.*;
import at.dms.kjc.sir.stats.StatisticsGathering;
import at.dms.kjc.sir.lowering.*;
import at.dms.kjc.sir.lowering.partition.*;
import at.dms.kjc.sir.lowering.fusion.*;
import at.dms.kjc.sir.lowering.fission.*;
import at.dms.kjc.lir.*;
import java.util.*;
import java.io.*;
import at.dms.util.Utils;
import java.io.*;

/**

*/

public class StreamGraph
{
    /** The toplevel stream container **/
    private FlatNode topLevelFlatNode;
    /** A list of all the static sub graphs **/
    private StaticStreamGraph[] staticSubGraphs;
    /** the entry point static subgraph **/
    private StaticStreamGraph topLevel;
    /** the tile assignment for the stream graph **/
    private Layout layout;
    /** Information on all the file readers and writer of the graph **/
    private FileVisitor fileVisitor;
    /** map of flat nodes to parent ssg **/
    public HashMap parentMap;
    /** schedules of sending for all the joiners of the stream graph **/
    public JoinerSimulator joinerSimulator;
    /** Maps RawTile -> switch code schedule **/
    public HashMap initSwitchSchedules;
    /** Maps RawTile -> switch code schedule **/
    public HashMap steadySwitchSchedules;
    private RawChip rawChip;

    public StreamGraph(FlatNode top, RawChip rawChip) 
    {
	this.rawChip = rawChip;
	this.topLevelFlatNode = top;
	parentMap = new HashMap();
	initSwitchSchedules = new HashMap();
	steadySwitchSchedules = new HashMap();
    }
    
    public void createStaticStreamGraphs() 
    {
	//flat nodes at a dynamic rate boundary that may constitute the
	//beginning of a new ssg
	LinkedList dynamicBoundary = new LinkedList();
	HashSet visited = new HashSet();
	//add the toplevel to the dynamicboundary list
	dynamicBoundary.add(topLevelFlatNode);
	
	//create the ssgs list
	LinkedList ssgs = new LinkedList();
	
	while (!dynamicBoundary.isEmpty()) {
	    FlatNode top = (FlatNode)dynamicBoundary.remove(0);
	    //we don't want to create a new SSG for something we have already added
	    assert !visited.contains(top);
	    //dynamic boundaries can only be filters!
	    assert top.isFilter();
	    StaticStreamGraph ssg = new StaticStreamGraph(this, top);
	    //set topleve 
	    if (topLevel == null)
		topLevel = ssg;
	    ssgs.add(ssg);
	    searchDownstream(top, ssg, visited, dynamicBoundary);
	}

	//set up the array of sub graphs
	staticSubGraphs = 
	    (StaticStreamGraph[])ssgs.toArray(new StaticStreamGraph[0]);

	
	
	//clean up the flat graph so that is can be converted to an SIR
	for (int i = 0; i < staticSubGraphs.length; i++)
	    staticSubGraphs[i].cleanUp();
	
	
	//create the connections of the graph!!!
	for (int i = 0; i < staticSubGraphs.length; i++)
	    staticSubGraphs[i].connect();

	//after we have created the ssg, convert their
	//flatgraphs to SIR graphs
	for (int i = 0; i < staticSubGraphs.length; i++) {
	    staticSubGraphs[i].createSIRGraph();
	    staticSubGraphs[i].dumpFlatGraph();
	}
    }

    /** recursive function to cut the graph into ssgs */
    private void searchDownstream(FlatNode current, StaticStreamGraph ssg, HashSet visited, 
				  List dynamicBoundary) 
    {
	assert current.ways == current.edges.length;

	//we may have already seen this node before, if so just exit
	if (visited.contains(current)) {
	    return;
	}


	//record that it has been visited...
	visited.add(current);
	//current has not been added yet
	ssg.addFlatNode(current);
	
	//if this flatnode is a filter, check if it has dynamic input
	if (current.isFilter()) {
	    
	    SIRFilter filter = (SIRFilter)current.contents;
	    //if this filter has dynamic output and their is a downstream neighbor, it will begin
	    //a new SSG, so add it to dynamicBoundary, but only if we have not seen the node yet
	    //we way have visited it in an upstream walk
	    if (filter.getPush().isDynamic()) {
		assert current.ways == 1 && current.edges.length == 1 && current.edges[0] != null;
		if (!visited.contains(current.edges[0])) {
		    dynamicBoundary.add(current.edges[0]);
		    //set the next field of the ssg for the current flatnode
		    ssg.addNext(current, current.edges[0]);
		    cutGraph(current, current.edges[0]);
		}
		return;
	    }
	    //if the downstream filter of this filter has dynamic input, then this is a
	    //dynamic boundary, so add the downstream to dynamicBoundary to start a new SSG
	    //but only if we haven't visited the node yet, we may have visited it in
	    //upstream walk...
	    if (current.ways > 0 && current.edges[0].isFilter()) {
		assert current.ways == 1 && current.edges.length == 1;
		SIRFilter downstream = (SIRFilter)current.edges[0].contents;
		if ((downstream.getPeek().isDynamic() || downstream.getPop().isDynamic())) {
		    if (!visited.contains(current.edges[0])) {
			dynamicBoundary.add(current.edges[0]);
			//set the next field of the ssg for the current flatnode
			ssg.addNext(current, current.edges[0]);
			cutGraph(current, current.edges[0]);
		    }
		    return;
		}
	    }
	    //otherwise continue the ssg if connected to more
	    if (current.edges.length > 0) {
		assert current.edges[0] != null;
		searchDownstream(current.edges[0], ssg, visited, dynamicBoundary);
	    }
	}	
	else if (current.isSplitter()) {
	    for (int i = 0; i < current.ways; i++) {
		assert current.edges[i] != null;
		searchDownstream(current.edges[i], ssg, visited, dynamicBoundary);
	    }
	}
	else {
	    assert current.isJoiner();

	    assert current.incoming.length == current.inputs;
	    //search backwards if we haven't seen this joiner and it is not a null joiner
	    if (!(((SIRJoiner)current.contents).getType().isNull() || 
		((SIRJoiner)current.contents).getSumOfWeights() == 0)) {
		for (int i = 0; i < current.inputs; i++) {
		    assert current.incoming[i] != null;
		    searchUpstream(current.incoming[i], ssg, visited, dynamicBoundary);
		}
	    }
	    
	    if (current.edges.length > 0) {
		assert current.edges[0] != null;
		searchDownstream(current.edges[0], ssg, visited, dynamicBoundary);
	    }
	}
    }
    
    private void cutGraph(FlatNode upstream, FlatNode downstream) 
    {
	System.out.println("*** Cut Graph ***");
	assert upstream.isFilter() && downstream.isFilter();
	SIRFilter upFilter = (SIRFilter)upstream.contents;
	SIRFilter downFilter = (SIRFilter)downstream.contents;
	
	assert upFilter.getPush().isDynamic() || downFilter.getPeek().isDynamic() ||
	    downFilter.getPop().isDynamic();
	
	//reset upstream	
	upFilter.setPush(new JIntLiteral(0));
	upFilter.setOutputType(CStdType.Void);
	upstream.removeEdges();
	//reset downstream
	downFilter.setPop(new JIntLiteral(0));
	downFilter.setPeek(new JIntLiteral(0));
	downFilter.setInputType(CStdType.Void);
	downstream.removeIncoming();
    }
    
    private void searchUpstream(FlatNode current, StaticStreamGraph ssg, HashSet visited, 
				List dynamicBoundary) 
    {
	assert current.incoming.length == current.inputs;

	//we have already added this flatnode to an ssg so just make sure and exit
	if (visited.contains(current)) {
	    assert !dynamicBoundary.contains(current);
	    return;
	}
	
	//stop at unprocessed dynamic boundaries, but add the boundary node and remove it
	//from the unprocess boundary list
	if (dynamicBoundary.contains(current)) {
	    ssg.addTopLevelFlatNode(current);
	    visited.add(current);
	    dynamicBoundary.remove(current);
	    return;
	}
	
	//we have not seen this flatnode before
	ssg.addFlatNode(current);
	visited.add(current);
	
	if (current.isFilter()) {
	    SIRFilter filter = (SIRFilter)current.contents;
	    //if this filter has dynamic input, and we hav
	    if (filter.getPop().isDynamic() || filter.getPeek().isDynamic()) {
		assert current.inputs == 1;
		cutGraph(current.incoming[0], current);
		ssg.addTopLevelFlatNode(current);
		return;
	    }	    
	    if (current.inputs > 0) {
		assert current.inputs == 1 && current.incoming[0] != null;
		//check if the upstream filter has dynamic output
		//if so return
		if (current.incoming[0].isFilter()) {
		    SIRFilter upstream = (SIRFilter)current.incoming[0].contents;
		    if (upstream.getPush().isDynamic()) { 
			cutGraph(current.incoming[0], current);
			ssg.addTopLevelFlatNode(current);
			return;
		    }
		}
		//if not, continue upstream walk
		searchUpstream(current.incoming[0], ssg, visited, dynamicBoundary);
	    }
	} else if (current.isSplitter()) {
	    if (current.inputs > 0) {
		assert current.incoming[0] != null && current.inputs == 1;
		searchUpstream(current.incoming[0], ssg, visited, dynamicBoundary);
	    }
	} else {
	    assert current.isJoiner();
	    
	    for (int i = 0; i < current.inputs; i++) {
		assert current.incoming[i] != null;
		searchUpstream(current.incoming[i], ssg, visited, dynamicBoundary);
	    }
	}
    }

    public boolean dynamicEntry(SIRStream stream) 
    {
	if (stream instanceof SIRFilter) {
	    return ((SIRFilter)stream).getPop().isDynamic() ||
		((SIRFilter)stream).getPeek().isDynamic();
	}
	else if (stream instanceof SIRPipeline) {  
	    //call dynamicExit on the last element of the pipeline
	    return dynamicEntry(((SIRPipeline)stream).get(0));
	}
	else //right now we can never have a dynamic entryunless in the above cases
	    return false;
    }
    
    public boolean dynamicExit(SIRStream stream) 
    {
	if (stream instanceof SIRFilter) {
	    return ((SIRFilter)stream).getPush().isDynamic();
	}
	else if (stream instanceof SIRPipeline) {  
	    //call dynamicExit on the last element of the pipeline
	    return dynamicExit(((SIRPipeline)stream).get(((SIRPipeline)stream).size() - 1));
	}
	else //right now we can never have a dynamic exit unless in the above cases
	    return false;
    }
    
    /** This will automatically assign the single tile of the raw chip
	to the one filter of the one SSG, all these conditions must be true **/
    public void tileAssignmentOneFilter() 
    {
	assert rawChip.getTotalTiles() == 1;
	assert staticSubGraphs.length == 1;
	
	StaticStreamGraph ssg = staticSubGraphs[0];
	assert ssg.filterCount() == 1;
	
	ssg.setNumTiles(1);
    }
    

    public void handTileAssignment() 
    {
	BufferedReader inputBuffer = new BufferedReader(new InputStreamReader(System.in));;
	int numTilesToAssign = rawChip.getTotalTiles(), num;
	StaticStreamGraph current = topLevel;
	
	for (int i = 0; i < staticSubGraphs.length; i++) {
	    current = staticSubGraphs[i];
	    int assignedNodes = current.countAssignedNodes();
	    
	    while (true) {
		System.out.print("Number of tiles for " + current + " (" + numTilesToAssign +
				   " tiles left, " + assignedNodes + " nodes in subgraph): ");
		try {
		    num = Integer.valueOf(inputBuffer.readLine()).intValue();
		}
		catch (Exception e) {
		    System.out.println("Bad number!");
		    continue;
		}
		
		if (num <= 0) {
		    System.out.println("Enter number > 0!");
		    continue;
		}
		
		if ((numTilesToAssign - num) < 0) {
		    System.out.println("Too many tiles desired!");
		    continue;
		}
		
		current.setNumTiles(num);
		numTilesToAssign -= num;
		break;
	    }
	}
	//	checkAssignment();
    }
    

    /** for each sub-graph, assign a certain number of tiles to it **/
    public void tileAssignment() 
    {
	StaticStreamGraph current = topLevel;
	//for right now just assign exactly the number of tiles as filters!
	for (int i = 0; i < staticSubGraphs.length; i++) {
	    current = staticSubGraphs[i];
	    final int[] filters = {0};
	    
	    IterFactory.createFactory().createIter(current.getTopLevelSIR()).accept(new EmptyStreamVisitor() {
		    public void visitFilter(SIRFilter self,
					    SIRFilterIter iter) {
			if (!(self instanceof SIRDummySource || self instanceof SIRDummySink)) {
			    filters[0]++;
			}
			
		    }
		}); 
	    current.setNumTiles(filters[0]);
	}
	//	checkAssignment();
    }

    public void putParentMap(FlatNode node, StaticStreamGraph ssg) 
    {
	parentMap.put(node, ssg);
    }
    

    public StaticStreamGraph getParentSSG(FlatNode node) 
    {
	assert parentMap.containsKey(node) : node;
	return (StaticStreamGraph)parentMap.get(node);
    }
    

    public FileVisitor getFileVisitor() 
    {
	return fileVisitor;
    }
    
    public Layout getLayout() 
    {
	return layout;
    }
    
    
    public void layoutGraph() 
    {
	//set up the parent map for other passes
	topLevel.accept(new StreamGraphVisitor() {
		public void visitStaticStreamGraph(StaticStreamGraph ssg) 
		{
		    parentMap.putAll(ssg.getParentMap());
		}
		
	    }, null, true);

	
	//gather the information on file readers and writers in the graph
	fileVisitor = new FileVisitor(this);

	//now ready to layout	
	layout = new Layout(this);
	//for right now just handAssign!!
	if (KjcOptions.noanneal)
	    layout.handAssign();
	else 
	    layout.simAnnealAssign();
    }


    /** make sure that we have every tile of the raw chip assigned
	the sum of tiles assigned to ssg's equals the number of raw tiles 
    **/
    private void checkAssignment() 
    {
	StaticStreamGraph current = topLevel;
	int totalTiles = 0;
	//for right now just assign exactly the number of tiles as filters!	
	for (int i = 0; i < staticSubGraphs.length; i++) {
	    current = staticSubGraphs[i];
	    totalTiles += current.getNumTiles();
	}
	assert totalTiles == rawChip.getTotalTiles() :
	    "Error: some tiles not assigned";
    }
    
    
    public StaticStreamGraph[] getStaticSubGraphs() 
    {
	return staticSubGraphs;
    }
    
    public StaticStreamGraph getTopLevel() 
    {
	return topLevel;
    }

    public RawChip getRawChip() 
    {
	return rawChip;
    }



    public void dumpStaticStreamGraph() 
    {
	StaticStreamGraph current = topLevel;
	for (int i = 0; i < staticSubGraphs.length; i++) {
	    current = staticSubGraphs[i];
	    System.out.println("******* StaticStreamGraph ********");
	    System.out.println("Dynamic rate input = " + dynamicEntry(current.getTopLevelSIR()));
	    System.out.println("InputType = " + current.getTopLevelSIR().getInputType());
	    System.out.println(current.toString());
	    System.out.println("Tiles Assigned = " + current.getNumTiles());
	    System.out.println("OutputType = " + current.getTopLevelSIR().getOutputType());
	    System.out.println("Dynamic rate output = " + dynamicExit(current.getTopLevelSIR()));
	    StreamItDot.printGraph(current.getTopLevelSIR(), current.getTopLevelSIR().getIdent() + ".dot");
	    System.out.println("**********************************");
	}
    }

    /** create a stream graph with only one filter (thus one SSG) **/
    public static StreamGraph constructStreamGraph(SIRFilter filter) 
    {
	return constructStreamGraph(new FlatNode(filter));
    }
    
    /** create a stream graph with only one filter (thus one SSG), it's not laid out yet**/
    public static StreamGraph constructStreamGraph(FlatNode node)  
    {
	assert node.isFilter();

	StreamGraph streamGraph = new StreamGraph(node, new RawChip(1, 1));
	streamGraph.createStaticStreamGraphs();
	streamGraph.tileAssignmentOneFilter();

	return streamGraph;
    }
    
}



//     /**
//      * not called anymore!!
//      * Convert all dynamic rate filter declarations to static approximations
//      **/
//     private void convertDynamicToStatic() 
//     {
// 	class ConvertDynamicToStatic extends EmptyStreamVisitor
// 	{
// 	    //set this to try as soon as we visit one filter!, this is used
// 	    //to make sure that dynamic rates only occur at the endpoints of static sub graphs
// 	    private boolean visitedAFilter = false;
// 	    //set this after we have seen a dynamic push to true, we should see no more
// 	    //filters after that!!
// 	    private boolean shouldSeeNoMoreFilters = false;

// 	    /** This is not used anymore!!!, part of an old implementation !!**/
// 	    private JIntLiteral convertRangeToInt(SIRRangeExpression exp) 
// 	    {
		
		
// 		if (exp.getAve() instanceof JIntLiteral) {
// 		    //first see if there is an average hint that is a integer
// 		    return (JIntLiteral)exp.getAve();
// 		} else if (exp.getMin() instanceof JIntLiteral &&
// 			   exp.getMax() instanceof JIntLiteral) {
// 		    //if we have both a max and min bound, then just take the 2 
// 		    //return the average..
// 		    return new JIntLiteral((((JIntLiteral)exp.getMin()).intValue() +
// 					    ((JIntLiteral)exp.getMax()).intValue()) / 2);
// 		} else if (exp.getMin() instanceof JIntLiteral) {
// 		    //if we have a min, just return that...
// 		    return (JIntLiteral)exp.getMin();
// 		} else {
// 		    //otherwise just return 1 
// 		    return new JIntLiteral(1);
// 		}
// 	    }
// 	    /** convert any dynamic rate declarations to static **/
// 	    public void visitFilter(SIRFilter self,
// 				    SIRFilterIter iter) {
// 		assert !shouldSeeNoMoreFilters : "Dynamic Rates in middle of static sub graph " + self;

		
// 		if (self.getPop().isDynamic()) {
// 		    //convert any dynamic pop rate to zero!!
// 		    self.setPop(new JIntLiteral(0));
// 		    assert !visitedAFilter : "Dynamic Rates in the middle of static sub graph " + self;
// 		}
// 		if (self.getPeek().isDynamic()) {
// 		    //convert any dynamic peek rate to zero!!
// 		    self.setPeek(new JIntLiteral(0));
// 		    assert !visitedAFilter : "Dynamic Rates in the middle of static sub graph " + self;
// 		}
// 		if (self.getPush().isDynamic()) {
// 		    //convert any dynamic push rate to zero
// 		    self.setPush(new JIntLiteral(0));
// 		    //now that we have seen a dynamic push, this should be the last filter!!
// 		    shouldSeeNoMoreFilters = true;
// 		}
// 		visitedAFilter = true;
// 	    }

// 	    public void visitPhasedFilter(SIRPhasedFilter self,
// 					  SIRPhasedFilterIter iter) {
// 		assert false : "Phased filters not supported!";
// 	    }
// 	}

// 	StaticStreamGraph current = topLevel;
// 	//visit all the subgraphs converting dynamic rates to static...
// 	while (current != null) {	 
// 	    IterFactory.createFactory().createIter(current.getTopLevelSIR()).accept(new ConvertDynamicToStatic());
// 	    current = current.getNext();
// 	}	
//     }
