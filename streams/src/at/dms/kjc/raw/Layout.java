package at.dms.kjc.raw;

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
public class Layout extends at.dms.util.Utils implements FlatVisitor {

    private static HashMap SIRassignment;
    /* coordinate -> flatnode */
    private static HashMap tileAssignment;
    private static HashSet assigned;
    //set of all the identity filters not mapped to tiles
    public static HashSet identities;
    private static Coordinate[][] coordinates;
    private static BufferedReader inputBuffer;
    private static Random random;
    private static FlatNode toplevel;
    /* hashset of Flatnodes representing all the joiners
       that are mapped to tiles */
    public static HashSet joiners;
    
    public static int MINTEMPITERATIONS = 200;
    public static int MAXTEMPITERATIONS = 200;
    public static int ANNEALITERATIONS = 10000;
    public static double TFACTR = 0.9;

    private static FileWriter filew;
        
    public Layout() 
    {
	joiners = new HashSet();
    }
    
    private static void init(FlatNode top) 
    {
	toplevel = top;
	int rows = RawBackend.rawRows;
	int columns = RawBackend.rawColumns;
		
	//determine if there is a file reader/writer in the graph
	//call init() to traverse the graph 
	// if there is create a new column to place the fileReader/writer
	// it will become a bc file reading device
	FileVisitor.init(top);
	if (FileVisitor.foundReader || FileVisitor.foundWriter){
	    columns++;
	}
	
	coordinates  =
	    new Coordinate[rows][columns];
	for (int row = 0; row < rows; row++)
	    for (int column = 0; column < columns; column++)
		coordinates[row][column] = new Coordinate(row, column);
	
	SIRassignment = new HashMap();
	tileAssignment = new HashMap();
	assigned = new HashSet();
	identities = new HashSet();

	top.accept(new Layout(), new HashSet(), false);
	
	System.out.println("Tiles assigned: " + assigned.size());

	if (assigned.size() > 
	    (RawBackend.rawRows * RawBackend.rawColumns)) {
	    System.err.println("\nLAYOUT ERROR: Need " + assigned.size() +
			       " tiles, have " + 
			       (RawBackend.rawRows * RawBackend.rawColumns) +
			       " tiles.");
	    System.exit(-1);
	}
    }

    public static int getTilesAssigned() {
	return assigned.size();
    }

    public static boolean isAssigned(FlatNode node) {
	return assigned.contains(node);
    }
    
    public static Set getTiles() {
	return tileAssignment.keySet();
    }

    /**
     * Returns the tile number assignment for <str>, or null if none has been assigned.
     */
    public static Coordinate getTile(SIROperator str) 
    {
	if (SIRassignment == null) return null;
	return (Coordinate)SIRassignment.get(str);
    }
    
    public static Coordinate getTile(FlatNode str) 
    {
	if (SIRassignment == null){
	    return null;
	}
	Coordinate ret = (Coordinate)SIRassignment.get(str.contents);
	return ret;
    }

    public static Coordinate getTile(int row, int column) 
    {
	return coordinates[row][column];
    }

    public static Coordinate getTile(int tileNumber) 
    {
	if (!(tileNumber / RawBackend.rawColumns < RawBackend.rawRows))
	    Utils.fail("tile number too high");
	
	return coordinates[tileNumber / RawBackend.rawColumns]
	    [tileNumber % RawBackend.rawColumns];
    }
	    
    public static String getDirection(Coordinate from,
				      Coordinate to) {
	if (from == to)
	    return "st";

	
	if (from.getRow() == to.getRow()) {
	    int dir = from.getColumn() - to.getColumn();
	    if (dir == -1)
		return "E";
	    else if (dir == 1)
		return "W";
	    else
		Utils.fail("calling getDirection on non-neighbors");
	}
	if (from.getColumn() == to.getColumn()) {
	    int dir = from.getRow() - to.getRow();
	    if (dir == -1) 
		return "S";
	    else if (dir == 1)
		return "N";
	    else
		Utils.fail("calling getDirection on non-neighbors");
	}
	Utils.fail("calling getDirection on non-neighbors");
	return "";
    }

    public static boolean areNeighbors(Coordinate tile1, Coordinate tile2) 
    {
	if (tile1 == tile2) 
	    return false;
	if (tile1.getRow() == tile2.getRow())
	    if (Math.abs(tile1.getColumn() - tile2.getColumn()) == 1)
		return true;
	if (tile1.getColumn() == tile2.getColumn())
	    if (Math.abs(tile1.getRow() - tile2.getRow()) == 1)
		return true;
	//not conntected
	return false;
    }
    
    public static int getTileNumber(FlatNode node)
    {
	return getTileNumber(getTile(node));
    }	
	
    public static int getTileNumber(Coordinate tile)
    {
	//because the simulator only simulates 4x4 or 8x8 we
	//have to translate the tile number according to these layouts
	int columns = 4;
	if (RawBackend.rawColumns > 4)
	    columns = 8;
	int row = tile.getRow();
	int column = tile.getColumn();
	return (row * columns) + column;
    }
    
    public static int getTileNumber(SIROperator str)
    {
	return getTileNumber(getTile(str));
    }	
    
    public static FlatNode getNode(Coordinate coord) 
    {
	return (FlatNode)tileAssignment.get(coord);
    }
    
    
    private static void assign(Coordinate coord, FlatNode node) 
    {
	if (node == null) {
	    tileAssignment.remove(coord);
	    return;
	}
	tileAssignment.put(coord, node);
	SIRassignment.put(node.contents, coord);
    }

    private static void assign(HashMap sir, HashMap tile, Coordinate coord, FlatNode node) 
    {
	if (node == null) {
	    tile.remove(coord);
	    return;
	}
	tile.put(coord, node);
	sir.put(node.contents, coord);
    }
    

    public static void simAnnealAssign(FlatNode node) 
    {
	System.out.println("Simulated Annealing Assignment");
	int nsucc =0, j = 0;
	double currentCost = 0.0, minCost = 0.0;
	//number of paths tried at an iteration
	int nover = 100; //* RawBackend.rawRows * RawBackend.rawColumns;

	try {
	    init(node);
	    random = new Random(17);
	    //random placement
	    randomPlacement();
	    filew = new FileWriter("simanneal.out");
	    int configuration = 0;

	    currentCost = placementCost();
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
	    //run the annealing twice.  The first iteration is really just to get a 
	    //good initial layout.  Some random layouts really kill the algorithm
	    for (int two = 0; two < RawBackend.rawRows ; two++) {
		double t = annealMaxTemp(); 
		double tFinal = annealMinTemp();
		while (true) {
		    int k = 0;
		    nsucc = 0;
		    for (k = 0; k < nover; k++) {
			if (perturbConfiguration(t)) {
			    filew.write(configuration++ + " " + placementCost() + "\n");
			    nsucc++;
			}
		
			currentCost = placementCost();
			//keep the layout with the minimum cost
			//this will be the final layout
			if (currentCost < minCost) {
			    minCost = currentCost;
			    //save the layout with the minimum cost
		        sirMin = (HashMap)SIRassignment.clone();
			tileMin = (HashMap)tileAssignment.clone();
			}
			if (placementCost() == 0.0)
			    break;
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
	   
	    currentCost = placementCost();
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
    
    
     private static double annealMaxTemp() throws Exception
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

  private static double annealMinTemp() throws Exception
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

    private static void randomPlacement() 
    {
	//assign the fileReaders/writers to the last row
	if (FileVisitor.foundReader || FileVisitor.foundWriter) {
	    Iterator frs = FileVisitor.fileReaders.iterator();
	    Iterator fws = FileVisitor.fileWriters.iterator();
	    int row = 0;
	    //check to see if there are less then
	    //file readers/writers the number of columns
	    if (FileVisitor.fileReaders.size() + 
		FileVisitor.fileWriters.size() > RawBackend.rawRows)
		Utils.fail("Too many file readers/writers (must be less than rows).");
	    //assign the file streams to the added column starting at the top
	    //row
	    while (frs.hasNext()) {
		assign(coordinates[row][RawBackend.rawColumns], 
 		       (FlatNode)frs.next());
		row++;
	    }
	    while (fws.hasNext()) {
		assign(coordinates[row][RawBackend.rawColumns],
		       (FlatNode)fws.next());
		row++;
	    }
	}
	//Iterator nodes = assigned.iterator();
	ListIterator dfTraversal = DFTraversal.getDFTraversal(toplevel).listIterator(0);
	int row = 0;
	int column = 0;

	for (row = 0; row < RawBackend.rawRows; row ++) {
	    if (row % 2 == 0) {
		for (column = 0; column < RawBackend.rawColumns;) {
		    if (!dfTraversal.hasNext())
			break;
		    FlatNode node = (FlatNode)dfTraversal.next();
		    if (!assigned.contains(node))
			continue;
		    assign(getTile(row, column), node);
		    column++;
		}
	    }
	    else {
		for (column = RawBackend.rawColumns -1; column >= 0;) {
		    if (!dfTraversal.hasNext())
			break; 
		    FlatNode node = (FlatNode)dfTraversal.next();
		    if (!assigned.contains(node))
			continue;
		    assign(getTile(row, column), node); 
		    column--;
		}
	    }
	    if (!dfTraversal.hasNext())
		break;
	}


	dumpLayout("initial-layout.dot");
    }
    
    private static double placementCost() 
    {
	HashSet nodes = (HashSet)assigned.clone();
	RawBackend.addAll(nodes, FileVisitor.fileReaders);
	
	Iterator nodesIt = nodes.iterator();
	HashSet routers = new HashSet();
	double sum = 0.0;
	while(nodesIt.hasNext()) {
	    FlatNode node = (FlatNode)nodesIt.next();
	    sum += placementCostHelper(node, routers);
	}
	return sum;
    }
	
    private static double placementCostHelper(FlatNode node, HashSet routers) 
    {
	//get all placed downstream nodes
	Iterator downstream = getDownStream(node).iterator();
	double sum = 0.0;
	while (downstream.hasNext()) {
	    FlatNode dest = (FlatNode)downstream.next();
	    Coordinate[] route = 
		(Coordinate[])Router.getRoute(node, dest).toArray(new Coordinate[1]);
	    //find the number assigned on the path
	    double numAssigned = 0.0;
	    for (int i = 1; i < route.length - 1; i++) {
		if (getNode(route[i]) != null) 
		    numAssigned += 100.0;
		else {
		    //router tile
		    if (routers.contains(route[i]))
			numAssigned += 0.5;
		    routers.add(route[i]);
		}
	    }
	    //sqrt(calculate steadyStateExcutions * hops * pushed)
	    int hops = route.length -2;
	    int items = 0;
	    	    
	    //case 1:
	    //if sending thru a splitter 
	    if (node.edges[0].contents instanceof SIRSplitter) {
		//if the final dest is a filter then just get the execution count of the 
		//dest filter * its pop rate
		if (dest.contents instanceof SIRFilter) {
		    items = ((Integer)RawBackend.steadyExecutionCounts.get(dest)).intValue() *
			((SIRFilter)dest.contents).getPopInt();
		}
		//we are sending to a joiner thru a splitter (should only happen when 
		//a splitter is connected to a feedback loop).
		else {
		    double rate = 1.0;
		    //calculate the percentage of time this path is taken for the
		    //feedback loop
		    if (dest.incomingWeights.length >= 2)
			rate = ((double)dest.incomingWeights[0])/(double)(dest.incomingWeights[0] +
							dest.incomingWeights[1]);
		    items = (int)(((Integer)RawBackend.steadyExecutionCounts.get(dest)).intValue() / rate);
		}
	    }
	    else {  //sending without an intermediate splitter
		//if sending from a joiner, items = execution count
		//if sending from a filter, items = exe count * items pushed
		int push;
		if (node.contents instanceof SIRJoiner)
		    push = 1;
		else 
		    push = ((SIRFilter)node.contents).getPushInt();
		if(RawBackend.steadyExecutionCounts.get(node)==null)
		    items=1;
		else
		    items = ((Integer)RawBackend.steadyExecutionCounts.get(node)).intValue() *
			push;
	    }
	    sum += ((items * hops) + (items * Util.getTypeSize(Util.getOutputType(node)) * 
		    /*Math.pow(*/numAssigned /** 2.0, 3.0)*/ * 10.0));
	}
	return sum;
    }
    
    
    
    //return true if the perturbation is accepted
    private static boolean perturbConfiguration(double T) throws Exception
    {
	int first;
	int second;
	
	double e_old = placementCost();
	
	//find 2 suitable nodes to swap
	while (true) {
	    first = getRandom();
	    second = getRandom();
	    //do not swap same tile or two null tiles
	    if (first == second)
		continue;
	    if ((getNode(getTile(first)) == null) &&
		(getNode(getTile(second)) == null))
		continue;
	    break;
	}
	
	FlatNode firstNode = getNode(getTile(first));
	FlatNode secondNode = getNode(getTile(second));
	//perform swap
	assign(getTile(first), secondNode);
	assign(getTile(second), firstNode);
	
	//DECIDE if we should keep the change
	double e_new = placementCost();
	double P = 1.0;
	double R = random.nextDouble();

	if (e_new >= e_old)
	    P = Math.exp((((double)e_old) - ((double)e_new)) / T);
	
	if (R < P) {
	    return true;
	}
	else {
	    //reject configuration

	    assign(getTile(second), secondNode);
	    assign(getTile(first), firstNode);
	    return false;
	}
    }
    
    //    private static double maxTemp(

    
    private static int getRandom() 
    {
	return random.nextInt(RawBackend.rawRows*
			      RawBackend.rawColumns);
    }
    
    public static void dumpLayout(String fileName) {
	StringBuffer buf = new StringBuffer();
	
	buf.append("digraph Layout {\n");
	buf.append("size = \"8, 10.5\"");
	buf.append("node [shape=box,fixedsize=true,width=2.5,height=1];\nnodesep=.5;\nranksep=\"2.0 equally\";\nedge[arrowhead=dot, style=dotted]\n");
	for (int i = 0; i < RawBackend.rawRows; i++) {
	    buf.append("{rank = same;");
	    for (int j = 0; j < RawBackend.rawColumns; j++) {
		buf.append("tile" + getTileNumber(getTile(i, j)) + ";");
	    }
	    buf.append("}\n");
	}
	for (int i = 0; i < RawBackend.rawRows; i++) {
	    for (int j = 0; j < RawBackend.rawColumns; j++) {
		Iterator neighbors = getNeighbors(getTile(i, j)).iterator();
		while (neighbors.hasNext()) {
		    Coordinate n = (Coordinate)neighbors.next();
		    buf.append("tile" + getTileNumber(getTile(i,j)) + " -> tile" +
			       getTileNumber(n) + " [weight = 100000000];\n");
		}
	    }
	}
	buf.append("edge[color = red,arrowhead = normal, arrowsize = 2.0, style = bold];\n");
	Iterator it = tileAssignment.values().iterator();
	while(it.hasNext()) {
	    FlatNode node = (FlatNode) it.next();
	    if (FileVisitor.fileNodes.contains(node))
		continue;
	    buf.append("tile" +   getTileNumber(node) + "[label=\"" + 
		       //getTileNumber(node) + "\"];\n");
		       shortName(node.getName()) + "\"];\n");
	     
	     //we only map joiners and filters to tiles and they each have
	     //only one output
	     Iterator downstream = getDownStream(node).iterator();
	     int y=getTile(node).getRow();
	     while(downstream.hasNext()) {
		 FlatNode n = (FlatNode)downstream.next();
		 if (FileVisitor.fileNodes.contains(n))
		     continue;
		 buf.append("tile" + getTileNumber(node) + 
			    " -> tile" +
			    getTileNumber(n) + " [weight = 1]");
		 int thisRow=getTile(n).getRow();
		 if(Math.abs(thisRow-y)<=1)
		     buf.append(";\n");
		 else
		     buf.append(" [constraint=false];\n");
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

    private static String shortName(String str) {
	//	return str.substring(0, Math.min(str.length(), 15));
	return str;
    }

    private static HashSet getDownStream(FlatNode node) 
    {
	if (node == null)
	    return new HashSet();
	HashSet ret = new HashSet();
	for (int i = 0; i < node.ways; i++) {
	    RawBackend.addAll(ret, getDownStreamHelper(node.edges[i]));
	}
	return ret;
    }

    private static HashSet getDownStreamHelper(FlatNode node) {
	if (node == null)
	    return new HashSet();
	
	if (node.contents instanceof SIRFilter) {
	    if (identities.contains(node))
		return getDownStreamHelper(node.edges[0]);
	    HashSet ret = new HashSet();
	    ret.add(node);
	    return ret;
	}
	else if (node.contents instanceof SIRJoiner) {
	    if (joiners.contains(node)) {
		HashSet ret = new HashSet();
		ret.add(node);
		return ret;
	    }	
	    else return getDownStreamHelper(node.edges[0]);
	}
	else if (node.contents instanceof SIRSplitter) {
	    HashSet ret = new HashSet();
	    /*
	      if (node.contents.getParent() instanceof SIRFeedbackLoop) {
	      //add the connection to all the nodes outside of the feedback
	      if (node.ways > 1) {
	      RawBackend.addAll(ret, getDownStreamHelper(node.edges[0]));
	      ret.add(node.edges[1]);
	      }
	      else 
	      ret.add(node.edges[0]);
	      }
			  */
	    for (int i = 0; i < node.ways; i++) {
		if(node.weights[i]!=0)
		    RawBackend.addAll(ret, getDownStreamHelper(node.edges[i]));
	    }
	    return ret;
	}
	Utils.fail("Serious Error in Simulated Annealing");
	
	return null;
    }
        
    //but not north neighbor or west
    private static List getNeighbors(Coordinate coord) 
    {
	int row = coord.getRow();
	int column = coord.getColumn();
	LinkedList neighbors = new LinkedList();
	
	//get east west neighbor
	//if (column - 1 >= 0)
	//    neighbors.add(getTile(row, column - 1));
	if (column + 1 < RawBackend.rawColumns) 
	    neighbors.add(getTile(row, column + 1));	
	
	//if (row - 1 >= 0)
	//    neighbors.add(getTile(row - 1, column));
	if (row + 1 < RawBackend.rawRows) 
	    neighbors.add(getTile(row + 1, column));

	return neighbors;
    }
    
    public static void handAssign(FlatNode node) 
    {
	init(node);
	System.out.println("Enter desired tile for each filter...");
	inputBuffer = new BufferedReader(new InputStreamReader(System.in));
	Iterator keys = assigned.iterator();
	while (keys.hasNext()) {
	    handAssignNode((FlatNode)keys.next());
	}
	System.out.println("Final Cost: " + placementCost());
	dumpLayout("layout.dot");
    }

    private static void handAssignNode(FlatNode node) 
    {
	//Assign a filter, joiner to a tile 
	//perform some error checking.
	while (true) {
	    try {
		Integer row, column;
		
		//get row
		System.out.print(node.getName() + "\nRow: ");
		row = Integer.valueOf(inputBuffer.readLine());
		if (row.intValue() < 0) {
		    System.err.println("Negative Value: Try again.");
		    continue;
		}
		if (row.intValue() > (RawBackend.rawRows -1)) {
		    System.err.println("Value Too Large: Try again.");
		    continue;
		}
		//get column
		System.out.print("Column: ");
		column = Integer.valueOf(inputBuffer.readLine());
		if (column.intValue() < 0) {
		    System.err.println("Negative Value: Try again.");
		    continue;
		}
		if (column.intValue() > (RawBackend.rawColumns -1)) {
		    System.err.println("Value Too Large: Try again.");
		    continue;
		}
		//check if this tile has been already assigned
		Iterator it = SIRassignment.values().iterator();
		boolean alreadyAssigned = false;
		while(it.hasNext()) {
		    Coordinate current = (Coordinate)it.next();
		    if (current.getRow() == row.intValue() &&
			current.getColumn() == column.intValue()){
			alreadyAssigned = true;
			System.err.println("Tile Already Assigned: Try Again.");
			break;
		    }
		}
		if (alreadyAssigned)
		    continue;
		assign(getTile(row.intValue(), column.intValue()), node);
		return;
	    }
	    catch (Exception e) {
		System.err.println("Error:  Try again.");
	    }
	}
    }
    

    public void visitNode(FlatNode node) 
    {
	if (node.contents instanceof SIRFilter &&
	    ! (FileVisitor.fileNodes.contains(node))) {
	    //do not map identities, but add them to the identities set
	    if (node.contents instanceof SIRIdentity) {
		identities.add(node);
		return;
	    }
	    assigned.add(node);
	    return;
	}
	if (node.contents instanceof SIRJoiner) {
	    if (node.edges[0] == null || !(node.edges[0].contents instanceof SIRJoiner)) {
		//do not assign the joiner if JoinerRemoval wants is removed...
		if (JoinerRemoval.unnecessary != null && 
		    JoinerRemoval.unnecessary.contains(node))
		    return;
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

