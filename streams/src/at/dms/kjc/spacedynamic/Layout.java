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
	     int y=getTile(node).getY();
	     while(downstream.hasNext()) {
		 FlatNode n = (FlatNode)downstream.next();
		 if (!tileAssignment.values().contains(n))
		     continue;
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

    private HashSet getDownStreamHelper(FlatNode node) {
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
	    else {
		return getDownStreamHelper((node.edges.length > 0 ? node.edges[0] : null));
	    }
	}
	else if (node.contents instanceof SIRSplitter) {
	    HashSet ret = new HashSet();
	    /*
	      if (node.contents.getParent() instanceof SIRFeedbackLoop) {
	      //add the connection to all the nodes outside of the feedback
	      if (node.ways > 1) {
	      SpaceDynamicBackend.addAll(ret, getDownStreamHelper(node.edges[0]));
	      ret.add(node.edges[1]);
	      }
	      else 
	      ret.add(node.edges[0]);
	      }
			  */
	    for (int i = 0; i < node.ways; i++) {
		if(node.weights[i]!=0)
		    SpaceDynamicBackend.addAll(ret, getDownStreamHelper(node.edges[i]));
	    }
	    return ret;
	}
	Utils.fail("Serious Error in Simulated Annealing");
	
	return null;
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
	System.out.println("Enter desired tile for each filter...");
	inputBuffer = new BufferedReader(new InputStreamReader(System.in));
	Iterator keys = assigned.iterator();
	while (keys.hasNext()) {
	    handAssignNode((FlatNode)keys.next());
	}
	//System.out.println("Final Cost: " + placementCost());
	dumpLayout("layout.dot");
    }

    private void handAssignNode(FlatNode node) 
    {
	//Assign a filter, joiner to a tile 
	//perform some error checking.
	while (true) {
	    int tileNumber;
	    System.out.print(node.getName() + " of " + streamGraph.getParentSSG(node) + ": ");
	    try {
		tileNumber = Integer.valueOf(inputBuffer.readLine()).intValue();
	    }
	    catch (Exception e) {
		System.out.println("Bad number!");
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
	    //other wise the assignment is valid, assign and return!!
	    assign(tile, node);
	    return;
	}
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
	    if (node.edges.length == 0) {
		assert node.getTotalIncomingWeights() == 0;
		return;
	    }
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

