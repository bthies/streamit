package at.dms.kjc.spacedynamic;

import at.dms.kjc.flatgraph.FlatNode;
import at.dms.kjc.flatgraph.FlatVisitor;
import at.dms.kjc.*;
import at.dms.kjc.sir.*;
import at.dms.kjc.sir.lowering.*;
import at.dms.util.Utils;
import java.io.*;
import java.util.List;
import java.util.*;
import java.util.LinkedList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;


/**
 *The Layout class generates mapping of filters to raw tiles.  It assumes that the Sis
 * namer has been run and that the stream graph has been partitioned.
 */
public class Layout extends at.dms.util.Utils implements 
						  StreamGraphVisitor, FlatVisitor {
    
    public Router router;
    
    /** SIRStream -> RawTile **/
    private  HashMap SIRassignment;
    /* RawTile -> flatnode */
    private  HashMap tileAssignment;
    /** set of all flatnodes that are assigned to tiles **/
    private  HashSet assigned;
    //set of all the identity filters not mapped to tiles
    private   HashSet identities;
    private  BufferedReader inputBuffer;
    private  Random random;
    
    private StreamGraph streamGraph;

    /* hashset of Flatnodes representing all the joiners
       that are mapped to tiles */
    private  HashSet joiners;
    
    public static int MINTEMPITERATIONS = 200;
    public static int MAXTEMPITERATIONS = 200;
    public static int ANNEALITERATIONS = 10000;
    public static double TFACTR = 0.9;

    private FileWriter filew;
    
    private RawChip rawChip;

    public Layout(StreamGraph streamGraph) 
    {
	this.streamGraph = streamGraph;
	joiners = new HashSet();
	rawChip = streamGraph.getRawChip();	

	int rows = rawChip.getYSize();
	int columns = rawChip.getXSize();
	
	router = new YXRouter();

	if (streamGraph.getFileVisitor().foundReader || 
	    streamGraph.getFileVisitor().foundWriter){
	    assert false;
	    columns++;
	}

	SIRassignment = new HashMap();
	tileAssignment = new HashMap();
	assigned = new HashSet();
	identities = new HashSet();

	//find out exactly what we should layout !!!
	streamGraph.getTopLevel().accept(this, null, true);
	
	System.out.println("Tiles layout.assigned: " + assigned.size());

	if (assigned.size() > 
	    (rawChip.getXSize() * rawChip.getYSize())) {
	    System.err.println("\nLAYOUT ERROR: Need " + assigned.size() +
			       " tiles, have " + 
			       (rawChip.getYSize() * rawChip.getXSize()) +
			       " tiles.");
	    System.exit(-1);
	}
    }

    /** visit each node of the ssg and find out which flatnodes should be 
	assigned to tiles **/
    public void visitStaticStreamGraph(StaticStreamGraph ssg) 
    {
	ssg.getTopLevel().accept(this, new HashSet(), false);
    }

    public int getTilesAssigned() {
	return assigned.size();
    }

    public boolean isAssigned(FlatNode node) {
	return assigned.contains(node);
    }
    
    public Set getTiles() {
	return tileAssignment.keySet();
    }
    
    public HashSet getJoiners() 
    {
	return joiners;
    }
    
    public HashSet getIdentities() 
    {
	return identities;
    }
    

    /**
     * Returns the tile number assignment for <str>, or null if none has been layout.assigned.
     */
    public RawTile getTile(SIROperator str) 
    {
	if (SIRassignment == null) return null;
	return (RawTile)SIRassignment.get(str);
    }
    
    public RawTile getTile(FlatNode str) 
    {
	if (SIRassignment == null){
	    return null;
	}
	return (RawTile)SIRassignment.get(str.contents);
    }

    
    public int getTileNumber(FlatNode node)
    {
	return getTile(node).getTileNumber();
    }	
    
    public int getTileNumber(SIROperator str)
    {
	return getTile(str).getTileNumber();
    }	
    
    public FlatNode getNode(RawTile tile) 
    {
	return (FlatNode)tileAssignment.get(tile);
    }
    
    
    private void assign(RawTile tile, FlatNode node) 
    {
	//if node == null, remove assignment
	if (node == null) {
	    tileAssignment.remove(tile);
	    return;
	}
	tileAssignment.put(tile, node);
	SIRassignment.put(node.contents, tile);
    }

    private void assign(HashMap sir, HashMap tileAss, RawTile tile, FlatNode node) 
    {
	if (node == null) {
	    tileAss.remove(tile);
	    return;
	}
	tileAss.put(tile, node);
	sir.put(node.contents, tile);
    }    
    
    public void dumpLayout(String fileName) {
	StringBuffer buf = new StringBuffer();
	
	buf.append("digraph Layout {\n");
	buf.append("size = \"8, 10.5\"");
	buf.append("node [shape=box,fixedsize=true,width=2.5,height=1];\nnodesep=.5;\nranksep=\"2.0 equally\";\nedge[arrowhead=dot, style=dotted]\n");
	for (int i = 0; i < rawChip.getYSize(); i++) {
	    buf.append("{rank = same;");
	    for (int j = 0; j < rawChip.getXSize(); j++) {
		buf.append("tile" + rawChip.getTile(j, i).getTileNumber() + ";");
	    }
	    buf.append("}\n");
	}
	for (int i = 0; i < rawChip.getYSize(); i++) {
	    for (int j = 0; j < rawChip.getXSize(); j++) {
		Iterator neighbors = rawChip.getTile(j, i).getSouthAndEastNeighbors().iterator();
		while (neighbors.hasNext()) {
		    RawTile n = (RawTile)neighbors.next();
		    buf.append("tile" + rawChip.getTile(j, i).getTileNumber() + " -> tile" +
			       n.getTileNumber() + " [weight = 100000000];\n");
		}
	    }
	}
	buf.append("edge[color = red,arrowhead = normal, arrowsize = 2.0, style = bold];\n");
	Iterator it = tileAssignment.values().iterator();
	while(it.hasNext()) {
	    FlatNode node = (FlatNode) it.next();
	    if (streamGraph.getFileVisitor().fileNodes.contains(node))
		continue;
	    buf.append("tile" +   getTileNumber(node) + "[label=\"" + 
		       //getTileNumber(node) + "\"];\n");
		       shortName(node.getName()) + "\"];\n");

	     //we only map joiners and filters to tiles and they each have
	     //only one output
	     Iterator downstream = getDownStream(node).iterator();
	     //	     System.out.println("Getting downstream of " + node);
	     int y=getTile(node).getY();
	     while(downstream.hasNext()) {
		 FlatNode n = (FlatNode)downstream.next();
		 if (!tileAssignment.values().contains(n))
		     continue;
		 //		 System.out.println(" " + n);
		 buf.append("tile" + getTileNumber(node) + 
			    " -> tile" +
			    getTileNumber(n) + " [weight = 1]");
		 int thisRow=getTile(n).getY();
		 if(Math.abs(thisRow-y)<=1)
		     buf.append(";\n");
		 else
		     buf.append(" [constraint=false];\n");
	     }
	     
	}

	//put in the dynamic connections in blue
	buf.append("edge[color = blue,arrowhead = normal, arrowsize = 2.0, style = bold];\n");
	for (int i = 0; i < streamGraph.getStaticSubGraphs().length; i++) {
	    StaticStreamGraph ssg =  streamGraph.getStaticSubGraphs()[i];
	    for (int out = 0; out < ssg.getOutputs().length; out++) {
		assert getTile(ssg.getOutputs()[out]) != null;
		buf.append("tile" + getTileNumber(ssg.getOutputs()[out]) + " -> tile" +
			   getTileNumber(ssg.getNext(ssg.getOutputs()[out])) + "[weight = 1];");
	    }
	}
	
	
	buf.append("}\n");
	
	try {
	    FileWriter fw = new FileWriter(fileName);
	    fw.write(buf.toString());
	    fw.close();
	}
	catch (Exception e) {
	    System.err.println("Could not print layout");
	}
    }

    private String shortName(String str) {
	//	return str.substring(0, Math.min(str.length(), 15));
	return str;
    }


    /** get all the downstream *assigned* nodes of <node> **/
    private HashSet getDownStream(FlatNode node) 
    {
	if (node == null)
	    return new HashSet();
	HashSet ret = new HashSet();
	for (int i = 0; i < node.ways; i++) {
	    SpaceDynamicBackend.addAll(ret, getDownStreamHelper(node.edges[i]));
	}
	return ret;
    }
    

    /** called by getDownStream to recursive pass thru all the non-assigned 
	nodes and get the assiged destinations of a node **/
    private HashSet getDownStreamHelper(FlatNode node) 
    {
	if (node == null)
	    return new HashSet();
	//if this node must be assigned to a tile, return it
	HashSet ret = new HashSet();	
	if (assigned.contains(node)) {
	    ret.add(node);
	    return ret;
	}
	else { //otherwise find the downstream neighbors that are assigned
	    for (int i = 0; i < node.edges.length; i++) {
		if (node.weights[i] != 0)
		    SpaceDynamicBackend.addAll(ret, 
					       getDownStreamHelper(node.edges[i]));
	    }
	    return ret;
	}
    }
        
    /** Assign a StreamGraph that is composed of one filter 
	in one SSG on a RawChip with one tile **/
    public void singleFilterAssignment() 
    {
	assert assigned.size() == 1;
	assert rawChip.getTotalTiles() == 1;
	assert (FlatNode)(assigned.toArray()[0]) == 
	    streamGraph.getStaticSubGraphs()[0].getTopLevel();

	assign(rawChip.getTile(0), 
	       (FlatNode)(assigned.toArray()[0]));
    }
    

    public void handAssign() 
    {
	System.out.println("Enter desired tile for each filter of " + 
			   streamGraph.getStaticSubGraphs().length + " subgraphs: ");
	inputBuffer = new BufferedReader(new InputStreamReader(System.in));

	for (int i = 0; i < streamGraph.getStaticSubGraphs().length; i++) {
	    StaticStreamGraph ssg = streamGraph.getStaticSubGraphs()[i];
	    Iterator flatNodes = ssg.getFlatNodes().iterator();
	    while (flatNodes.hasNext()) {
		FlatNode node = (FlatNode)flatNodes.next();
	    
		//do not try to assign a node that should not be assigned
		if (!assigned.contains(node))
		    continue;
		//Assign a filter, joiner to a tile 
		//perform some error checking.
		while (true) {
		    int tileNumber;
		    String str = null;
		    
		    System.out.print(node.getName() + " of " + ssg + ": ");
		    try {
			str = inputBuffer.readLine();			
			tileNumber = Integer.valueOf(str).intValue();
		    }
		    catch (Exception e) {
			System.out.println("Bad number " + str);
			continue;
		    }
		    if (tileNumber < 0 || tileNumber >= rawChip.getTotalTiles()) {
			System.out.println("Bad tile number!");
			continue;
		    }
		    RawTile tile = rawChip.getTile(tileNumber);
		    if (SIRassignment.values().contains(tile)) {
			System.out.println("Tile Already Assigned!");
			continue;
		    }
		    //other wise the assignment is valid, assign and break!!
		    assign(tile, node);
		    break;
		}
	    }    
	}
	double cost = placementCost(true);
	dumpLayout("layout.dot");
	System.out.println("Layout cost: " + cost);
	//assert cost >= 0.0 : "Illegal Layout";
    }
    
    /** return the cost of this layout calculated by the cost function, 
	if the cost is negative, this layout is illegal.
	if <debug> then print out why this layout was illegal
    **/
    public double placementCost(boolean debug) 
    {
	/** tiles used already to route dynamic data between 
	    SSGs, a tile can only be used once **/
	HashSet dynTilesUsed = new HashSet();
	/** Tiles already used by previous SSGs to route data
	    over the static network, a switch can only route 
	    data from one SSG **/
	HashSet staticTilesUsed = new HashSet();
	double cost = 0.0;

	for (int i = 0; i < streamGraph.getStaticSubGraphs().length; i++) {
	    StaticStreamGraph ssg = streamGraph.getStaticSubGraphs()[i];
	    double dynamicCost = 0.0;
	    double staticCost = 0.0;
	    
	    dynamicCost = getDynamicCost(ssg, dynTilesUsed);
	    if (dynamicCost < 0.0) {
		if (debug)
		    System.out.println("Dynamic routes cross!");
		return -1.0;
	    }
	    staticCost = getStaticCost(ssg, staticTilesUsed);
	    if (staticCost < 0.0) {
		if (debug)
		    System.out.println("Static route of SSG crosses another SSG!");
		return -1.0;
	    }
	    cost += dynamicCost + staticCost;
	}
	
	return cost;
    }
    
    
    /** get the cost of the static communication of this ssg and also
	determine if the communication is legal, it does not cross paths with 
	other SSGs, usedTiles holds tiles that have been used by previous SSGs **/
    private double getStaticCost(StaticStreamGraph ssg, HashSet usedTiles) 
    {
	//the tiles used by THIS SSG for routing
	//this set is filled with tiles that are not assigned but 
	//have been used to route items previously by this SSG
	HashSet routers = new HashSet();
	//allt tiles used for this SSG, add it to used tiles at the end, if legal
	HashSet tiles = new HashSet();
	
	Iterator nodes = ssg.getFlatNodes().iterator();
	double cost = 0.0;

	//calculate the communication cost for each node that is assign a tile in
	//the ssg
	while (nodes.hasNext()) {
	    FlatNode src = (FlatNode)nodes.next();
	    if (!(assigned.contains(src)))
		continue;
	    
	    assert getTile(src) != null;

	    //add the src tile to the list of tiles used by this SSG
	    tiles.add(getTile(src));
	    
	    //make sure we have not previously tried to route through this tile
	    //in a previous SSG
	    if (usedTiles.contains(getTile(src))) {
		System.out.println(getTile(src));
		return -1.0;
	    }
	    
	    //don't worry about nodes that aren't assigned tiles
	    if (!assigned.contains(src))
		continue;
	    //get all the dests for this node that are assigned tiles
	    Iterator dsts = getDownStream(src).iterator();


	    while (dsts.hasNext()) {
		FlatNode dst = (FlatNode)dsts.next();
		assert assigned.contains(dst);
		assert getTile(dst) != null;
		//add the dst tile to the list of tiles used by this SSG
		tiles.add(getTile(dst));
		
		//make sure we have not previously (in another SSG) tried to route 
		//thru the tile assigned to the dst
		if (usedTiles.contains(getTile(dst))) {
		    //System.out.println(getTile(dst));
		    return -1.0;
		}
		
		RawTile[] route = 
		    (RawTile[])router.getRoute(ssg, getTile(src), getTile(dst)).toArray(new RawTile[0]);
		
		//find the cost of the route, penalize routes that go thru 
		//tiles assigned to filters or joiners, reward routes that go thru 
		//non-assigned tiles
		double numAssigned = 0.0;
		
		for (int i = 1; i < route.length - 1; i++) {
		    //make sure that this route does not pass thru any tiles assigned to other SSGs
		    //otherwise we have a illegal layout!!!!
		    if (usedTiles.contains(route[i]))
			return -1.0;
		    //add this tile to the set of tiles used by this SSG
		    tiles.add(route[i]);
		    
		    if (getNode(route[i]) != null) //assigned tile
			numAssigned += 100.0;
		    else {
			//router tile, only penalize it if we have routed through it before
			if (routers.contains(route[i]))
			    numAssigned += 0.5;
			else //now it is a router tile
			    routers.add(route[i]);
		    }
		}
		
		int hops = route.length - 2;
		//the number of items sent over this channel for one execution of entire
		//SSG, from src to dest
		int items = 0;
		
		//now calculate the number of items sent per firing of SSG
		
		//if we are sending thru a splitter we have to be careful because not
		//all the data that the src produces goes to the dest
		if (src.edges[0].isSplitter()) {
		    //if the dest is a filter, then just calculate the number of items
		    //the dest filter receives
		    if (dst.isFilter())
			items = ssg.getMult(dst, false) * dst.getFilter().getPopInt();
		    else {
			//this is a joiner
			assert dst.isJoiner();
			//the percentage of items that go to this dest
			double rate = 1.0;
			
			//we are sending to a joiner thru a splitter, this will only happen
			//for a feedback loop, the feedback path is always way 0 thru the joiner
			if (dst.inputs > 1) 
			    rate = ((double)dst.incomingWeights[0]) / 
				((double) dst.getTotalIncomingWeights());
			//now calculate the rate at which the splitter sends to the joiner
			rate = rate * 
			    (((double)src.edges[0].weights[0]) / ((double)src.edges[0].getTotalOutgoingWeights()));
			//now calculate the number of items sent to this dest by this filter
			items = (int) rate * ssg.getMult(dst, false) * src.getFilter().getPopInt();
		    }
		}
		else {
		    //sending without intermediate splitter
		    //get the number of items sent
		    int push = 0;
		    if (src.isFilter())
			push = src.getFilter().getPushInt();
		    else //joiner
			push = 1;
		    items = ssg.getMult(src, false) * push;
		}

		//calculate communication cost of this node and add it to the cost sum
		cost += ((items * hops) + (items * Util.getTypeSize(Util.getOutputType(src)) *
					   numAssigned * 10));
	    }   
	}
	SpaceDynamicBackend.addAll(usedTiles, tiles);
	return cost;	
    }


    

    
    
    /** Get the cost of sending output of this SSG over the dynamic network 
	for right now disregard the min, max, and average rate declarations **/ 
    private double getDynamicCost(StaticStreamGraph ssg, HashSet usedTiles) 
    {
	double cost = 0.0;
    
	 //check if the dynamic communication is legal
	for (int j = 0; j < ssg.getOutputs().length; j++) {
	    FlatNode src = ssg.getOutputs()[j];
	    FlatNode dst = ssg.getNext(src);
	    
	    Iterator route = XYRouter.getRoute(ssg, getTile(src), getTile(dst)).iterator();
	    //System.out.print("Dynamic Route: ");

	    //********* TURNS IN DYNAMIC NETWORK COST IN TERMS OF LATENCY ****//
	    //*** ADD THIS COMPONENT ***//

	    //** Don't share links, could lead to starvation?? ***///

	    // ** but below is too restrictive so relax it **//

	    while (route.hasNext()) {
		ComputeNode tile = (ComputeNode)route.next();
		assert tile != null;
		//System.out.print(tile);
		//add to cost only if these an no endpoints of the route
		if (tile != getTile(src) && tile != getTile(dst))
		    cost += 1.0;
		if (usedTiles.contains(tile)) {
		    System.out.println("tile uesd twice for dynamic route: " + tile);
		    return -1.0;
		}
		
		usedTiles.add(tile);
	    }
	    //System.out.println();
	}
	return cost;
    }
    
    
    //return true if the node should be assigned to a tile
    public static boolean assignNode(FlatNode node) 
    {
	//if this is a filter and not an file reader/write/identity/predefined
	if (node.contents instanceof SIRFilter && 
	    !(node.contents instanceof SIRIdentity  ||
	      node.contents instanceof SIRPredefinedFilter))
	    return true;
	
	if (node.contents instanceof SIRJoiner) {
	    if (node.edges[0] == null || !(node.edges[0].contents instanceof SIRJoiner)) {
		//do not assign the joiner if JoinerRemoval wants is removed...
		//if (JoinerRemoval.unnecessary != null && 
		//   JoinerRemoval.unnecessary.contains(node))
		//   return false;
		//do not assign the joiner if it is a null joiner, so if we find a 
		//no null weight return true..
		for (int i = 0; i < node.inputs;i++) {
		    if (node.incomingWeights[i] != 0) {
			return true;
		    }
		} 
	    }
	}
	return false;
    }


    public void simAnnealAssign(FlatNode node) 
    {
	System.out.println("Simulated Annealing Assignment");
	int nsucc =0, j = 0;
	double currentCost = 0.0, minCost = 0.0;
	//number of paths tried at an iteration
	int nover = 100; //* RawBackend.rawRows * RawBackend.rawColumns;

	try {
	    random = new Random(17);
	    //random placement
	    //randomPlacement();
	    assert false;
	    
	    filew = new FileWriter("simanneal.out");
	    int configuration = 0;

	    currentCost = placementCost(false);
	    assert currentCost >= 0.0;
	    System.out.println("Initial Cost: " + currentCost);
	    
	    if (KjcOptions.noanneal || KjcOptions.decoupled) {
		dumpLayout("noanneal.dot");
		return;
	    }
	    //as a little hack, we will cache the layout with the minimum cost
	    //these two hashmaps store this layout
	    HashMap sirMin = (HashMap)SIRassignment.clone();
	    HashMap tileMin = (HashMap)tileAssignment.clone();
	    minCost = currentCost;
	    
	    if (currentCost == 0.0) {
		dumpLayout("layout.dot");
		return;
	    }

	    //The first iteration is really just to get a 
	    //good initial layout.  Some random layouts really kill the algorithm
	    for (int two = 0; two < rawChip.getYSize() ; two++) {
		double t = annealMaxTemp(); 
		double tFinal = annealMinTemp();
		while (true) {
		    int k = 0;
		    nsucc = 0;
		    for (k = 0; k < nover; k++) {
			//true if config change was accepted
			boolean accepted = perturbConfiguration(t);
			currentCost = placementCost(false);
			//the layout should always be legal
			assert currentCost >= 0.0;
			
			if (accepted) {
			    filew.write(configuration++ + " " + currentCost + "\n");
			    nsucc++;
			}

			//this will be the final layout
			if (currentCost == 0.0)
			    break;
			//keep the layout with the minimum cost
			if (currentCost < minCost) {
			    minCost = currentCost;
			    //save the layout with the minimum cost
			    sirMin = (HashMap)SIRassignment.clone();
			    tileMin = (HashMap)tileAssignment.clone();
			}
		    }
		    
		    t *= TFACTR;
    
		    if (nsucc == 0) break;
		    if (currentCost == 0)
			break;
		    if (t <= tFinal)
			break;
		    j++;
		}
		if (currentCost == 0)
		    break;
	    }
	   
	    currentCost = placementCost(false);
	    System.out.println("Final Cost: " + currentCost + 
			       " Min Cost : " + minCost + 
			       " in  " + j + " iterations.");
	    if (minCost < currentCost) {
		SIRassignment = sirMin;
		tileAssignment = tileMin;
	    }
	    
	    filew.close();
	}
	catch (Exception e) {
	    e.printStackTrace();
	}
	dumpLayout("layout.dot");
    }
    
    
     private double annealMaxTemp() throws Exception
     {
 	double T = 1.0;
 	int total = 0, accepted = 0;
 	HashMap sirInit  = (HashMap)SIRassignment.clone();
 	HashMap tileInit = (HashMap)tileAssignment.clone();
	
 	for (int i = 0; i < MAXTEMPITERATIONS; i++) {
 	    T = 2.0 * T;
	    total = 0;
	    accepted = 0;
	    for (int j = 0; j < 100; j++) {
		//c_old <- c_init
		SIRassignment = sirInit;
		tileAssignment = tileInit;
		if (perturbConfiguration(T))
		    accepted ++;
		total++;
	    }
 	    if (((double)accepted) / ((double)total) > .9)
 		break;
 	}
 	//c_old <- c_init
 	SIRassignment = sirInit;
 	tileAssignment = tileInit;
 	return T;
     }

  private double annealMinTemp() throws Exception
     {
 	double T = 1.0;
 	int total = 0, accepted = 0;
 	HashMap sirInit  = (HashMap)SIRassignment.clone();
 	HashMap tileInit = (HashMap)tileAssignment.clone();
	
 	for (int i = 0; i < MINTEMPITERATIONS; i++) {
 	    T = 0.5 * T;
	    total = 0;
	    accepted = 0;
	    for (int j = 0; j < 100; j++) {
		//c_old <- c_init
		SIRassignment = sirInit;
		tileAssignment = tileInit;
		if (perturbConfiguration(T))
		    accepted ++;
		total++;
	    }
 	    if (((double)accepted) / ((double)total) > .1)
 		break;
 	}
 	//c_old <- c_init
 	SIRassignment = sirInit;
 	tileAssignment = tileInit;
 	return T;
     }

     //return true if the perturbation is accepted
    private boolean perturbConfiguration(double T) throws Exception
    {
	int first, second;
	//the cost of the new layout and the old layout
	double e_new, e_old = placementCost(false);
	//the nodes to swap
	FlatNode firstNode, secondNode;
	
	//find 2 suitable nodes to swap
	while (true) {
	    first = getRandom();
	    second = getRandom();
	    //do not swap same tile or two null tiles
	    if (first == second)
		continue;
	    if ((getNode(rawChip.getTile(first)) == null) &&
		(getNode(rawChip.getTile(second)) == null))
		continue;
	    	    
	    firstNode = getNode(rawChip.getTile(first));
	    secondNode = getNode(rawChip.getTile(second));
	    //perform swap
	    assign(rawChip.getTile(first), secondNode);
	    assign(rawChip.getTile(second), firstNode);
	    
	    //the new placement cost
	    e_new = placementCost(false);
	    
	    if (e_new < 0.0) {
		//illegal tile assignment so revert the assignment
		assign(rawChip.getTile(second), secondNode);
		assign(rawChip.getTile(first), firstNode);
		continue;
	    }
	    else  //found a successful new layout
		break;
	}
	
	
	double P = 1.0;
	double R = random.nextDouble();
	
	if (e_new >= e_old)
	    P = Math.exp((((double)e_old) - ((double)e_new)) / T);
	
	if (R < P) {
	    return true;
	}
	else {
	    //reject configuration
	    assign(rawChip.getTile(second), secondNode);
	    assign(rawChip.getTile(first), firstNode);
	    return false;
	}
    }
    
    private int getRandom() 
    {
	return random.nextInt(rawChip.getTotalTiles());
    }
    
    /** END simulated annealing methods **/

    /** visit each flatnode and decide whether is should be assigned **/
    public void visitNode(FlatNode node) 
    {
	if (node.contents instanceof SIRFilter &&
	    ! (streamGraph.getFileVisitor().fileNodes.contains(node))) {
	    //do not map layout.identities, but add them to the layout.identities set
	    if (node.contents instanceof SIRIdentity) {
		identities.add(node);
		return;
	    }
	    //now check for predefined filters because we want to skip them...
	    //but not identities, see above...
	    if (node.contents instanceof SIRPredefinedFilter) 
		return;
	    
	    assigned.add(node);
	    return;
	}
	if (node.contents instanceof SIRJoiner) {
	    //don't assign a null joiner that does not have an output
	    if (node.edges.length == 0) {
		assert node.getTotalIncomingWeights() == 0;
		return;
	    }
	    //do not assign joiners directly connected to other joiners or null joiners
	    if (node.edges[0] == null || !(node.edges[0].contents instanceof SIRJoiner)) {
		//do not assign the joiner if JoinerRemoval wants is removed...
		//if (JoinerRemoval.unnecessary != null && 
		//    JoinerRemoval.unnecessary.contains(node))
		//    return;
		//do not assign the joiner if it is a null joiner
		for (int i = 0; i < node.inputs;i++) {
		    if (node.incomingWeights[i] != 0) {
			joiners.add(node);
			assigned.add(node);
			return;
		    }
		} 
	    }
	}
    }
   
}

