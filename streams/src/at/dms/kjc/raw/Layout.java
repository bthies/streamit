package at.dms.kjc.raw;

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
 *The Layout class generates mapping of filters to raw tiles.  It assumes that the 
 * namer has been run and that the stream graph has been partitioned.
 */
public class Layout extends at.dms.util.Utils implements FlatVisitor {

    private static HashMap SIRassignment;
    /* coordinate -> flatnode */
    private static HashMap tileAssignment;
    private static HashSet assigned;
    private static Coordinate[][] coordinates;
    private static BufferedReader inputBuffer;
    private static Random random;
    private static FlatNode toplevel;
    /* hashset of Flatnodes representing all the joiners
       that are mapped to tiles */
    public static HashSet joiners;
    
    public static int MINTEMPITERATIONS = 50000;
    public static int MAXTEMPITERATIONS = 50000;
    public static int ANNEALITERATIONS = 100;
    public static double TFACTR = 0.9;
    
    public Layout() 
    {
	joiners = new HashSet();
    }
    
    private static void init(FlatNode top) 
    {
	toplevel = top;
	int rows = StreamItOptions.rawRows;
	int columns = StreamItOptions.rawColumns;
		
	//determine if there is a file reader in the graph
	//call init() to traverse the graph 
	// if there is create a new column to place the fileReader
	// it will become a bc file reading device
	FileReaderVisitor.init(top);
	if (FileReaderVisitor.foundReader){
	    rows++;
	}
	
	coordinates  =
	    new Coordinate[rows][columns];
	for (int row = 0; row < rows; row++)
	    for (int column = 0; column < columns; column++)
		coordinates[row][column] = new Coordinate(row, column);
	
	SIRassignment = new HashMap();
	tileAssignment = new HashMap();
	assigned = new HashSet();

	top.accept(new Layout(), new HashSet(), false);
	
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
	if (!(tileNumber / StreamItOptions.rawColumns < StreamItOptions.rawRows))
	    Utils.fail("tile number too high");
	
	return coordinates[tileNumber / StreamItOptions.rawColumns]
	    [tileNumber % StreamItOptions.rawColumns];
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
	if (StreamItOptions.rawColumns > 4)
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
	int nsucc, j;
	//number of paths tried at a temp
	int nover = 10 * StreamItOptions.rawRows * StreamItOptions.rawColumns;
	//max number of sucessful path lengths before continuing
	int nlimit = 10 * StreamItOptions.rawRows * StreamItOptions.rawColumns;
	int cost = 0;
	double t = 0.5;
	
	init(node);
	random = new Random(17);
	//random placement
	randomPlacement();
	
	System.out.println("Initial Cost: " + placementCost());
	for (j = 0; j < ANNEALITERATIONS; j++) {
	    nsucc = 0;
	    for (int k = 0; k < nover; k++) {
		if (perturbConfiguration(t)) {
		    nsucc++;
		}
		if (nsucc >= nlimit) break;
		if (placementCost() == 0)
		    break;
	    }
	    t *= TFACTR;
	    if (nsucc == 0) break;
	    if (placementCost() == 0)
		    break;
	}
	System.out.println("Final Cost: " + placementCost() + " in  " + j + " iterations.");
      
	dumpLayout();

    }
    
    private static void randomPlacement() 
    {
	//assign the fileReaders to the last row
	if (FileReaderVisitor.foundReader) {
	    Iterator frs = FileReaderVisitor.fileReaders.iterator();
	    int column = 0;
	    while (frs.hasNext()) {
		assign(coordinates[StreamItOptions.rawRows][column], 
		       (FlatNode)frs.next());
		column ++;
	    }
	}
	Iterator nodes = assigned.iterator();
	int tile = 0;
	while(nodes.hasNext()) {
	    FlatNode node = (FlatNode)nodes.next();
	    assign(getTile(tile), node);
	    tile++;
	}
    }
    
    private static int placementCost() 
    {
	HashSet nodes = (HashSet)assigned.clone();
	RawBackend.addAll(nodes, FileReaderVisitor.fileReaders);
	
	Iterator nodesIt = nodes.iterator();
	HashSet routers = new HashSet();
	int sum = 0;
	while(nodesIt.hasNext()) {
	    sum += placementCostHelper((FlatNode)nodesIt.next(), routers);
	}
	return sum;
    }
	
    private static int placementCostHelper(FlatNode node, HashSet routers) 
    {
	//get all placed downstream nodes
	Iterator downstream = getDownStream(node).iterator();
	int sum = 0;
	while (downstream.hasNext()) {
	    FlatNode dest = (FlatNode)downstream.next();
	    Coordinate[] route = 
		(Coordinate[])Router.getRoute(node, dest).toArray(new Coordinate[1]);
	    //find the number assigned on the path
	    double numAssigned = 0.0;
	    for (int i = 1; i < route.length - 1; i++) {
		if (getNode(route[i]) != null) 
		    numAssigned += 2.0;
		else {
		    //router tile
		    if (routers.contains(route[i]))
			numAssigned += 0.5;
		    routers.add(route[i]);
		}
	    }
	    //sqrt(calculate steadyStateExcutions * hops * pushed)
	    int hops = route.length -2;
	    int items;
	    //if joiner then number of executions of source * push of source
	    //if splitter then number of executions of dest * pop of dest
	    if (node.edges[0].contents instanceof SIRSplitter) {
		items = ((Integer)RawBackend.steadyExecutionCounts.get(dest)).intValue() *
		    ((SIRFilter)dest.contents).getPopInt();
	    }
	    else {
		//check if the source is a joiner if so push = 1
		int push;
		if (node.contents instanceof SIRJoiner)
		    push = 1;
		else 
		    push = ((SIRFilter)node.contents).getPushInt();
		items = ((Integer)RawBackend.steadyExecutionCounts.get(node)).intValue() *
		    push;
	    }
	    //  System.out.println("Items, hops  " + Namer.getName(node.contents) + ": " + items);
	    sum += /*((int)Math.sqrt*/(items * hops) + items * Math.pow(numAssigned * 2.0, 3.0);
	}
	//System.out.println("Cost for node " + Namer.getName(node.contents) + ": " + sum);
	
	return sum;
    }
    
    //return true if the perturbation is accepted
    private static boolean perturbConfiguration(double T) 
    {
	int first;
	int second;
	
	int e_old = placementCost();
	
	//find 2 suitable nodes to swap
	while (true) {
	    first = getRandom();
	    second = getRandom();
	
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
	int e_new = placementCost();
	double P = 1.0;
	double R = random.nextDouble();

	if (e_new >= e_old)
	    P = Math.exp((((double)e_old) - ((double)e_new)) / T);
	if (R < P)
	    return true;
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
	return random.nextInt(StreamItOptions.rawRows*
			      StreamItOptions.rawColumns);
    }
    
    public static void dumpLayout() {
	StringBuffer buf = new StringBuffer();
	
	buf.append("digraph Layout {\n");
	buf.append("size = \"8, 10.5\"");
	buf.append("node [shape=box];\nedge[arrowhead=dot, style=dotted]\n");
	for (int i = 0; i < StreamItOptions.rawRows; i++) {
	    buf.append("{rank = same;");
	    for (int j = 0; j < StreamItOptions.rawColumns; j++) {
		buf.append("tile" + getTileNumber(getTile(i, j)) + ";");
	    }
	    buf.append("}\n");
	}
	for (int i = 0; i < StreamItOptions.rawRows; i++) {
	    for (int j = 0; j < StreamItOptions.rawColumns; j++) {
		Iterator neighbors = getNeighbors(getTile(i, j)).iterator();
		while (neighbors.hasNext()) {
		    Coordinate n = (Coordinate)neighbors.next();
		    buf.append("tile" + getTileNumber(getTile(i,j)) + " -> tile" +
			       getTileNumber(n) + " [weight = 10000];\n");
		}
	    }
	}
	buf.append("edge[color = red,arrowhead = normal, style = bold];\n");
	Iterator it = tileAssignment.values().iterator();
	while(it.hasNext()) {
	     FlatNode node = (FlatNode) it.next();
	     buf.append("tile" +   getTileNumber(node) + "[label=\"" + 
			Namer.getName(node.contents) + "\"];\n");
	     
	     //we only map joiners and filters to tiles and they each have
	     //only one output
	     Iterator downstream = getDownStream(node).iterator();
	     while(downstream.hasNext()) {
		 FlatNode n = (FlatNode)downstream.next();
		 buf.append("tile" + getTileNumber(node) + 
			    " -> tile" +
			    getTileNumber(n) + " [weight = 1];\n");
	     }
	     
	}
	buf.append("}\n");
	
	try {
	    FileWriter fw = new FileWriter("layout.dot");
	    fw.write(buf.toString());
	    fw.close();
	}
	catch (Exception e) {
	    System.err.println("Could not print layout");
	}
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
	else {
	    //SIRSplitter
	    HashSet ret = new HashSet();
	    for (int i = 0; i < node.ways; i++) {
		RawBackend.addAll(ret, getDownStreamHelper(node.edges[i]));
	    }
	    return ret;
	}
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
	if (column + 1 < StreamItOptions.rawColumns) 
	    neighbors.add(getTile(row, column + 1));	
	
	//if (row - 1 >= 0)
	//    neighbors.add(getTile(row - 1, column));
	if (row + 1 < StreamItOptions.rawRows) 
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
	dumpLayout();
    }

    private static void handAssignNode(FlatNode node) 
    {
	//Assign a filter, joiner to a tile 
	//perform some error checking.
	while (true) {
	    try {
		Integer row, column;
		
		//get row
		System.out.print(Namer.getName(node.contents) + "\nRow: ");
		row = Integer.valueOf(inputBuffer.readLine());
		if (row.intValue() < 0) {
		    System.err.println("Negative Value: Try again.");
		    continue;
		}
		if (row.intValue() > (StreamItOptions.rawRows -1)) {
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
		if (column.intValue() > (StreamItOptions.rawColumns -1)) {
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
	    !(node.contents instanceof SIRFileReader)) {
	    
	    assigned.add(node);
	    return;
	}
	if (node.contents instanceof SIRJoiner) {
	    if (node.edges[0] == null || !(node.edges[0].contents instanceof SIRJoiner)) {
		joiners.add(node);
		assigned.add(node);
		return;
	    } 
	}
    }
   
}

	
//     public static void simAnnealAssignElliot(FlatNode node) 
//     {
// 	double T, Tf;
// 	HashMap sirBest, tileBest;
// 	int bestCost;

// 	init(node);
// 	random = new Random(17);
// 	randomPlacement();
// 	System.out.println("Initial placement cost: " + placementCost());
// 	//clone the random placement

// 	HashMap sir  = (HashMap)SIRassignment.clone();
// 	HashMap tile = (HashMap)tileAssignment.clone();
// 	T = annealMaxTemp();

// 	//reset the initial random placement
// 	SIRassignment  = sir;
// 	tileAssignment = tile;
// 	Tf = annealMinTemp();

// 	//reset the initial random placement
// 	SIRassignment  = sir;
// 	tileAssignment = tile;

// 	int i, bestIter = 0;
      
// 	bestCost = placementCost();
// 	int cost = bestCost;
// 	sirBest = (HashMap)SIRassignment.clone();
// 	tileBest = (HashMap)tileAssignment.clone();

// 	for (i = 0; i < ANNEALITERATIONS; i++) {
// 	    perturbConfiguration(T);
// 	    T = .999 * T;
// 	    if (T <= Tf) break;
// 	    cost = placementCost();
// 	    if (cost < bestCost) {
// 		bestIter = i;
// 		sirBest = (HashMap)SIRassignment.clone();
// 		tileBest = (HashMap)tileAssignment.clone();
// 		bestCost = cost;
// 	    }
// 	    if (cost == 0)
// 		break;
// 	}
// 	if (cost > bestCost) {
// 	    SIRassignment = sirBest;
// 	    tileAssignment = tileBest;
// 	}
// 	else
// 	    bestIter = i;
// 	System.out.println("Final placement cost: " + placementCost() +
// 			   " " + bestIter);
// 	dumpLayout();
//     }
   
//     private static double annealMaxTemp() 
//     {
// 	double T = 1.0;
// 	int total = 0, accepted = 0;
// 	HashMap sirInit  = (HashMap)SIRassignment.clone();
// 	HashMap tileInit = (HashMap)tileAssignment.clone();
	
// 	for (int i = 0; i < MAXTEMPITERATIONS; i++) {
// 	    T = 2.0 * T;
// 	    //c_old <- c_init
// 	    SIRassignment = sirInit;
// 	    tileAssignment = tileInit;
// 	    if (perturbConfiguration(T))
// 		accepted ++;
// 	    total++;
// 	    if (((double)accepted) / ((double)total) > .999)
// 		break;
// 	}
// 	//c_old <- c_init
// 	SIRassignment = sirInit;
// 	tileAssignment = tileInit;
// 	return T;
//     }
    
//     private static double annealMinTemp () 
//     {
// 	double T = 1.0;
// 	int total = 0, accepted = 0;
// 	HashMap sirInit  = (HashMap)SIRassignment.clone();
// 	HashMap tileInit = (HashMap)tileAssignment.clone();
	
// 	for (int i = 0; i < MINTEMPITERATIONS; i++) {
// 	    T = 0.5 * T;
// 	    //c_old <- c_init
// 	    SIRassignment = sirInit;
// 	    tileAssignment = tileInit;
// 	    if (perturbConfiguration(T))
// 		accepted ++;
// 	    total++;
// 	    if (((double)accepted) / ((double)total) < .001)
// 		break;
// 	}
// 	//c_old <- c_init
// 	SIRassignment = sirInit;
// 	tileAssignment = tileInit;
// 	return T;
//     }
    
