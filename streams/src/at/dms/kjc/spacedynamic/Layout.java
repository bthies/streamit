package at.dms.kjc.spacedynamic;

import at.dms.kjc.flatgraph.FlatNode;
import at.dms.kjc.flatgraph.FlatVisitor;
import at.dms.kjc.*;
import at.dms.kjc.sir.*;
//import at.dms.kjc.sir.lowering.*;
//import at.dms.util.Utils;
import at.dms.kjc.common.CommonUtils;
import java.io.*;
//import java.util.List;
import java.util.*;
import java.util.LinkedList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import at.dms.kjc.cluster.DataEstimate;

/**
 * The Layout class generates mapping of filters to raw tiles. It operates on
 * the entire StreamGraph.
 */
public class Layout extends at.dms.util.Utils implements StreamGraphVisitor,
                                                         FlatVisitor {
    public Router router;

    /** SIRStream -> RawTile * */
    private HashMap SIRassignment;

    /* RawTile -> flatnode */
    private HashMap tileAssignment;

    /** set of all flatnodes that are assigned to tiles * */
    private HashSet assigned;

    // set of all the identity filters not mapped to tiles
    private HashSet identities;

    /***************************************************************************
     * map of flatNode -> Integer that stores the memory footprint of fields (in
     * bytes) of the filter, used for memory cost analysis, it is populated in
     * visitNode
     **************************************************************************/
    private HashMap memoryFP;

    /** work estimates for the filters and joiner of the graph * */
    private WorkEstimatesMap workEstimates;

    private BufferedReader inputBuffer;

    private Random random;

    private SpdStreamGraph streamGraph;

    /** this set contains ComputeNodes that appear as an hop in a route (not an endpoint) */
    private HashSet intermediateTiles;
    
    /*
     * hashset of Flatnodes representing all the joiners that are mapped to
     * tiles
     */
    private HashSet joiners;

    /** cost function constants * */

    // the multipler for the dynamic network cost
    public static double DYN_MULT = 1;

    // the weight of an ilegale static layout
    public static double ILLEGAL_WEIGHT = 1E6;

    // the weights assigned to a static hop that goes thru an assigned tile
    // public static double ASSIGNED_WEIGHT = 1000;

    // the weight assigned to routing an item through a router tile
    public static double ROUTER_WEIGHT = 1000.0;// 0.5;

    // scaling factor for the memory cost of the layout (for tiles that allocate
    // more
    // than the dcache size
    public static double MEMORY_SCALE = 1.0;

    // the weight assigned to routing an item through a tile that already
    // has a dynamic route going through it
    public static double DYN_CROSSED_ROUTE = 1000.0;

    // simualted annealing constants
    public static int MINTEMPITERATIONS = 200;

    public static int MAXTEMPITERATIONS = 200;

    public static int ANNEALITERATIONS = 10000;

    public static double TFACTR = 0.9;

    private FileWriter filew;

    private RawChip rawChip;

    private FileState fileState;

    private double oldCost = 0.0;

    public Layout(SpdStreamGraph streamGraph) {
        this.streamGraph = streamGraph;
        fileState = streamGraph.getFileState();
        joiners = new HashSet();
        rawChip = streamGraph.getRawChip();

        // router = new YXRouter();
        router = new FreeTileRouter();

        SIRassignment = new HashMap();
        tileAssignment = new HashMap();
        assigned = new HashSet();
        identities = new HashSet();
        memoryFP = new HashMap();
        workEstimates = new WorkEstimatesMap(streamGraph);

        // find out exactly what we should layout !!!
        ((SpdStaticStreamGraph)streamGraph.getTopLevel()).accept(this, null, true);

        System.out.println("Tiles assigned in layout: " + assigned.size());

        if (assigned.size() > (rawChip.getXSize() * rawChip.getYSize())) {
            System.err.println("\nLAYOUT ERROR: Need " + assigned.size()
                               + " tiles, have "
                               + (rawChip.getYSize() * rawChip.getXSize()) + " tiles.");
            System.exit(-1);
        }
    }

    /***************************************************************************
     * visit each node of the ssg and find out which flatnodes should be
     * assigned to tiles
     **************************************************************************/
    public void visitStaticStreamGraph(SpdStaticStreamGraph ssg) {
        ssg.getTopLevel().accept(this, new HashSet(), false);
    }

    public int getTilesAssigned() {
        int totalAssToTile = 0;

        Iterator assignedNodes = tileAssignment.values().iterator();
        while (assignedNodes.hasNext()) {
            if (assignToATile((FlatNode) assignedNodes.next()))
                totalAssToTile++;
        }
        return totalAssToTile;
    }

    public boolean isAssigned(RawTile tile) {
        assert SIRassignment.values().contains(tile) == tileAssignment.keySet()
            .contains(tile);

        return tileAssignment.keySet().contains(tile);
    }

    public boolean isAssigned(FlatNode node) {
        return assigned.contains(node);
    }

    public Set getTiles() {
        return tileAssignment.keySet();
    }

    public HashSet getJoiners() {
        return joiners;
    }

    public HashSet getIdentities() {
        return identities;
    }

    /**
     * Returns the tile number assignment for <pre>str</pre>, or null if none has been
     * layout It will die if this str should not be assigned to RawTile
     */
    public RawTile getTile(SIROperator str) {
        if (SIRassignment == null)
            return null;

        assert SIRassignment.get(str) != null
            || !(SIRassignment.get(str) instanceof RawTile) : "Calling getTile() on a stream that is not mapped to tile";
        return (RawTile) SIRassignment.get(str);
    }

    /**
     * Returns the tile number assignment for <pre>str</pre>, or null if none has been
     * layout It will die if <pre>str</pre> should not be assigned to RawTile
     */
    public RawTile getTile(FlatNode str) {
        if (SIRassignment == null) {
            return null;
        }

        return (RawTile) SIRassignment.get(str.contents);
    }

    /***************************************************************************
     * Return the tile number for the <pre>node</pre> it will die if the node has not
     * been assigned to a tile
     **************************************************************************/
    public int getTileNumber(FlatNode node) {
        return getTile(node).getTileNumber();
    }

    /***************************************************************************
     * Return the tile number for the <pre>str</pre> it will die if the node has not been
     * assigned to a tile
     **************************************************************************/
    public int getTileNumber(SIROperator str) {
        return getTile(str).getTileNumber();
    }

    /** Return the flatNode assigned to this <pre>tile</pre> * */
    public FlatNode getNode(RawTile tile) {
        return (FlatNode) tileAssignment.get(tile);
    }

    /** return the compute node (tile or ioport) assigned to <pre>node</pre> * */
    public ComputeNode getComputeNode(FlatNode node) {
        return getComputeNode(node.contents);
    }

    /** return the compute node (tile or ioport) assigned to <pre>str</pre> * */
    public ComputeNode getComputeNode(SIROperator str) {
        if (SIRassignment == null)
            return null;

        assert SIRassignment.get(str) != null : "calling getComputeNode() on str that is not assigned to a compute node: "
            + str;
        return (ComputeNode) SIRassignment.get(str);
    }

    private void assign(ComputeNode cn, FlatNode node) {
        // if node == null, remove assignment
        if (node == null) {
            tileAssignment.remove(cn);
            return;
        }
        tileAssignment.put(cn, node);
        SIRassignment.put(node.contents, cn);
    }

    // place the file filter assignment in the hashmaps, assigning them to ports
    private void assignFileFilters() {
        Iterator fileReaders = fileState.getFileReaderDevs().iterator();
        while (fileReaders.hasNext()) {
            FileReaderDevice dev = (FileReaderDevice) fileReaders.next();
            assign(dev.getPort(), dev.getFlatNode());
        }
        Iterator fileWriters = fileState.getFileWriterDevs().iterator();
        while (fileWriters.hasNext()) {
            FileWriterDevice dev = (FileWriterDevice) fileWriters.next();
            assign(dev.getPort(), dev.getFlatNode());
        }
    }

    public RawChip getRawChip() {
        return rawChip;
    }

    public void dumpLayout(String fileName) {
        StringBuffer buf = new StringBuffer();

        buf.append("digraph Layout {\n");
        buf.append("size = \"8, 10.5\"");
        buf
            .append("node [shape=box,fixedsize=true,width=2.5,height=1];\nnodesep=.5;\nranksep=\"2.0 equally\";\nedge[arrowhead=dot, style=dotted]\n");
        for (int i = 0; i < rawChip.getYSize(); i++) {
            buf.append("{rank = same;");
            for (int j = 0; j < rawChip.getXSize(); j++) {
                buf
                    .append("tile" + rawChip.getTile(j, i).getTileNumber()
                            + ";");
            }
            buf.append("}\n");
        }
        for (int i = 0; i < rawChip.getYSize(); i++) {
            for (int j = 0; j < rawChip.getXSize(); j++) {
                Iterator neighbors = rawChip.getTile(j, i)
                    .getSouthAndEastNeighbors().iterator();
                while (neighbors.hasNext()) {
                    RawTile n = (RawTile) neighbors.next();
                    buf.append("tile" + rawChip.getTile(j, i).getTileNumber()
                               + " -> tile" + n.getTileNumber()
                               + " [weight = 100000000];\n");
                }
            }
        }
        buf
            .append("edge[color = red,arrowhead = normal, arrowsize = 2.0, style = bold];\n");
        Iterator it = tileAssignment.values().iterator();
        while (it.hasNext()) {
            FlatNode node = (FlatNode) it.next();
            if (streamGraph.getFileState().fileNodes.contains(node))
                continue;
            buf.append("tile" + getTileNumber(node) + "[label=\"" +
                       // getTileNumber(node) + "\"];\n");
                       shortName(node.getName()) + "\"];\n");

            // we only map joiners and filters to tiles and they each have
            // only one output
            Iterator downstream = getDownStream(node).iterator();
            // System.out.println("Getting downstream of " + node);
            while (downstream.hasNext()) {
                int y = getTile(node).getY();
                FlatNode n = (FlatNode) downstream.next();
                if (!assignToATile(n))
                    continue;
                // System.out.println(" " + n);
                buf.append("tile" + getTileNumber(node) + " -> tile"
                           + getTileNumber(n) + " [weight = 1]");
                int thisRow = getTile(n).getY();
                if (Math.abs(thisRow - y) <= 1)
                    buf.append(";\n");
                else
                    buf.append(" [constraint=false];\n");
            }

        }

        // put in the dynamic connections in blue
        buf
            .append("edge[color = blue,arrowhead = normal, arrowsize = 2.0, style = bold];\n");
        for (int i = 0; i < streamGraph.getStaticSubGraphs().length; i++) {
            SpdStaticStreamGraph ssg = (SpdStaticStreamGraph)streamGraph.getStaticSubGraphs()[i];
            for (int out = 0; out < ssg.getOutputs().length; out++) {
                if (!assignToATile(ssg.getOutputs()[out]) || 
                    !assignToATile(ssg.getNext(ssg.getOutputs()[out])))
                    continue;
                assert getTile(ssg.getOutputs()[out]) != null : ssg.toString()
                    + " has an output not assigned to a tile " + out;
                buf.append("tile" + getTileNumber(ssg.getOutputs()[out])
                           + " -> tile"
                           + getTileNumber(ssg.getNext(ssg.getOutputs()[out]))
                           + "[weight = 1];");
            }
        }

        buf.append("}\n");

        try {
            FileWriter fw = new FileWriter(fileName);
            fw.write(buf.toString());
            fw.close();
        } catch (Exception e) {
            System.err.println("Could not print layout");
        }
    }

    private String shortName(String str) {
        // return str.substring(0, Math.min(str.length(), 15));
        return str;
    }

    /** get all the downstream *assigned* nodes and file writers (!!) of <pre>node</pre> * */
    private HashSet getDownStream(FlatNode node) {
        if (node == null)
            return new HashSet();
        HashSet ret = new HashSet();
        for (int i = 0; i < node.ways; i++) {
            SpaceDynamicBackend.addAll(ret, getDownStreamHelper(node.edges[i]));
        }
        return ret;
    }

    /***************************************************************************
     * called by getDownStream to recursive pass thru all the non-assigned nodes
     * and get the assiged destinations of a node
     **************************************************************************/
    private HashSet getDownStreamHelper(FlatNode node) {
        if (node == null)
            return new HashSet();
        // if this node must be assigned to a tile
        // or is a file writer..., return it
        HashSet ret = new HashSet();
        if (assigned.contains(node) || node.contents instanceof SIRFileWriter) {
            ret.add(node);
            return ret;
        } else { // otherwise find the downstream neighbors that are assigned
            for (int i = 0; i < node.edges.length; i++) {
                if (node.weights[i] != 0)
                    SpaceDynamicBackend.addAll(ret,
                                               getDownStreamHelper(node.edges[i]));
            }
            return ret;
        }
    }

    /***************************************************************************
     * Assign a StreamGraph that is composed of one filter in one SSG on a
     * RawChip with one tile
     **************************************************************************/
    public void singleFilterAssignment() {
        assert assigned.size() == 1;
        assert rawChip.getTotalTiles() == 1;
        assert (FlatNode) (assigned.toArray()[0]) == streamGraph
            .getStaticSubGraphs()[0].getTopLevel();
        //assign the one filter
        assign(rawChip.getTile(0), (FlatNode) (assigned.toArray()[0]));
        
        //set up the hash sets for future passes!
        getStaticCost((SpdStaticStreamGraph)streamGraph.getStaticSubGraphs()[0], new HashSet());
    }

    /** read the layout from a new-line separated file * */
    public void fileAssign() {
        assert KjcOptions.layoutfile != null : "Error: Null file in layout file assign";
        BufferedReader in = null;
        FileReader fr = null;
        try {
            fr = new FileReader(KjcOptions.layoutfile);
            in = new BufferedReader(fr);

            assignFileFilters();
            System.out.println("Tile Assignment from file "
                               + KjcOptions.layoutfile);
        } catch (Exception e) {
            System.err.println("Error while reading layout from file "
                               + KjcOptions.layoutfile);
            System.exit(1);
        }

        for (int i = 0; i < streamGraph.getStaticSubGraphs().length; i++) {
            SpdStaticStreamGraph ssg = (SpdStaticStreamGraph)streamGraph.getStaticSubGraphs()[i];
            Iterator flatNodes = ssg.getFlatNodes().iterator();
            while (flatNodes.hasNext()) {
                FlatNode node = (FlatNode) flatNodes.next();

                // do not try to assign a node that should not be assigned
                if (!assigned.contains(node))
                    continue;
                assignFromReader(in, ssg, node);
            }
        }
        try {
            in.close();
            fr.close();
        } catch (Exception e) {
            System.err.println("Error closing layout file");
            System.exit(1);
        }

        double cost = placementCost(true);
        dumpLayout("layout-from-file.dot");
        System.out.println("Layout cost: " + cost);
        assert cost >= 0.0 : "Illegal Layout";

    }

    /**
     * Given a Buffered reader, get the tile number assignment from the reader
     * for <pre>node</pre>
     */
    private void assignFromReader(BufferedReader inputBuffer,
                                  SpdStaticStreamGraph ssg, FlatNode node) {
        // Assign a filter, joiner to a tile
        // perform some error checking.
        while (true) {
            int tileNumber;
            String str = null;

            System.out.print(node.getName() + " of " + ssg + ": ");
            try {
                str = inputBuffer.readLine();
                tileNumber = Integer.valueOf(str).intValue();
            } catch (Exception e) {
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
            // other wise the assignment is valid, assign and break!!
            System.out.println("Assigning " + node.getName() + " to tile "
                               + tileNumber);
            assign(tile, node);
            break;
        }
    }

    public void handAssign() {
        assignFileFilters();
        System.out.println("Enter desired tile for each filter of "
                           + streamGraph.getStaticSubGraphs().length + " subgraphs: ");
        BufferedReader inputBuffer = new BufferedReader(new InputStreamReader(
                                                                              System.in));

        for (int i = 0; i < streamGraph.getStaticSubGraphs().length; i++) {
            SpdStaticStreamGraph ssg = (SpdStaticStreamGraph)streamGraph.getStaticSubGraphs()[i];
            Iterator flatNodes = ssg.getFlatNodes().iterator();
            while (flatNodes.hasNext()) {
                FlatNode node = (FlatNode) flatNodes.next();

                // do not try to assign a node that should not be assigned
                if (!assigned.contains(node))
                    continue;
                assignFromReader(inputBuffer, ssg, node);
            }
        }

        double cost = placementCost(true);
        dumpLayout("hand-layout.dot");
        System.out.println("Layout cost: " + cost);
        assert cost >= 0.0 : "Illegal Layout";
    }

    /**
     * return the cost of this layout calculated by the cost function, if the
     * cost is negative, this layout is illegal. if <pre>debug</pre> then print out why
     * this layout was illegal
     */
    public double placementCost(boolean debug) {

        /***********************************************************************
         * tiles used already to route dynamic data between SSGs, a tile can
         * only be used once
         **********************************************************************/
        HashSet dynTilesUsed = new HashSet();
        /***********************************************************************
         * Tiles already used by previous SSGs to route data over the static
         * network, a switch can only route data from one SSG
         **********************************************************************/
        HashSet staticTilesUsed = new HashSet();
        double cost = 0.0;

        for (int i = 0; i < streamGraph.getStaticSubGraphs().length; i++) {
            SpdStaticStreamGraph ssg = (SpdStaticStreamGraph)streamGraph.getStaticSubGraphs()[i];
            double dynamicCost = 0.0;
            double staticCost = 0.0;
            double memoryCost = 0.0;

            // right now we assume all layouts are legal from dynamic
            // perspective
            dynamicCost = getDynamicCost(ssg, dynTilesUsed);

            // get the static cost
            staticCost = getStaticCost(ssg, staticTilesUsed);
            if (staticCost < 0.0) {
                if (debug)
                    System.out
                        .println("Static route of SSG crosses another SSG!");
                return -1.0;
            }

            memoryCost = getMemoryCost(ssg);

            cost += (dynamicCost * DYN_MULT) + staticCost
                + (memoryCost * MEMORY_SCALE);
        }

        return cost;
    }

    private double getMemoryCost(SpdStaticStreamGraph ssg) {
        int memReq = 0;
        double memCost = 0.0;

        Iterator nodes = ssg.getFlatNodes().iterator();
        while (nodes.hasNext()) {
            FlatNode node = (FlatNode) nodes.next();
            // we only care about assigned filters
            if (!assigned.contains(node) || !node.isFilter())
                continue;

            assert memoryFP.keySet().contains(node);
            memReq = ((Integer) memoryFP.get(node)).intValue();

            // if we cannot fit in the cache then, calculate
            // how long it takes to get to the nearest side of the chip
            // and add this for each word that does not fit in the cache...
            if (memReq > RawChip.dCacheSizeBytes) {
                RawTile tile = getTile(node);
                // System.out.println(node + " has memory fp of " + memReq + " >
                // " + RawChip.dCacheSizeBytes);

                // divide by 4 because we send 4 bytes over the network at a
                // time
                memCost += ((memReq - RawChip.dCacheSizeBytes) / 4)
                    * tile.hopsToEdge();
            }

            // System.out.println(" *** Memory req of " + node + ": " +
            // memCost);
        }

        return memCost;
    }

    /***************************************************************************
     * get the cost of the static communication of this ssg and also determine
     * if the communication is legal, it does not cross paths with other SSGs,
     * usedTiles holds tiles that have been used by previous SSGs
     **************************************************************************/
    private double getStaticCost(SpdStaticStreamGraph ssg, HashSet usedTiles) {
        // the tiles used by THIS SSG for routing
        // this set is filled with tiles that are not assigned but
        // have been used to route items previously by this SSG
        HashSet routers = new HashSet();
        // allt tiles used for this SSG, add it to used tiles at the end, if
        // legal
        HashSet tiles = new HashSet();
        //reset the intermediate tiles list
        intermediateTiles = new HashSet();
        
        Iterator nodes = ssg.getFlatNodes().iterator();
        double cost = 0.0;

        // calculate the communication cost for each node that is assign a tile
        // in
        // the ssg
        while (nodes.hasNext()) {
            FlatNode src = (FlatNode) nodes.next();
            if (!assignToAComputeNode(src))
                continue;

            ComputeNode srcNode = getComputeNode(src);

            assert srcNode != null;

            // add the src tile to the list of tiles used by this SSG
            if (srcNode.isTile())
                tiles.add(srcNode);

            // make sure we have not previously tried to route through this tile
            // in a previous SSG
            if (srcNode.isTile() && usedTiles.contains(srcNode)) {
                // System.out.println(srcNode);
                // return -1.0;
                cost += ILLEGAL_WEIGHT;
            }

            // get all the dests for this node that are assigned tiles
            Iterator dsts = getDownStream(src).iterator();

            while (dsts.hasNext()) {
                FlatNode dst = (FlatNode) dsts.next();
                if (!assignToAComputeNode(dst))
                    continue;
                ComputeNode dstNode = getComputeNode(dst);

                assert dstNode != null;

                // add the dst tile to the list of tiles used by this SSG
                if (dstNode.isTile())
                    tiles.add(dstNode);

                // make sure we have not previously (in another SSG) tried to
                // route
                // thru the tile assigned to the dst
                if (dstNode.isTile() && usedTiles.contains(dstNode)) {
                    // System.out.println(dstNode);
                    cost += ILLEGAL_WEIGHT;
                    // return -1.0;
                }

                ComputeNode[] route = (ComputeNode[]) router.getRoute(ssg,
                                                                      srcNode, dstNode).toArray(new ComputeNode[0]);

                // check if we cannot find a route from src to dst that does not
                // go
                // thru another ssg
                if (route.length == 0) {
                    // System.out.println("Cannot find route from src to dst
                    // within SSG " +
                    // src + "(" + srcNode + ") -> " + dst + "(" + dstNode +
                    // ")");
                    cost += ILLEGAL_WEIGHT;
                    // return -1.0;
                }

                // find the cost of the route, penalize routes that go thru
                // tiles assigned to filters or joiners, reward routes that go
                // thru
                // non-assigned tiles
                double numAssigned = 0.0;

                for (int i = 1; i < route.length - 1; i++) {
                    assert route[i].isTile();
                    //add this intermediate hop tile to the list of intermediate tiles
                    intermediateTiles.add(route[i]);
                    // make sure that this route does not pass thru any tiles
                    // assigned to other SSGs
                    // otherwise we have a illegal layout!!!!
                    if (usedTiles.contains(route[i])) {
                        cost += ILLEGAL_WEIGHT;
                        // return -1.0;
                    }

                    // add this tile to the set of tiles used by this SSG
                    tiles.add(route[i]);

                    /*
                     * if (getNode((RawTile)route[i]) != null) //assigned tile
                     * numAssigned += ASSIGNED_WEIGHT * (1.0 /
                     * workEstimates.getEstimate(getNode((RawTile)route[i])));
                     * else {
                     */

                    // router tile, only penalize it if we have routed through
                    // it before
                    if (routers.contains(route[i])
                        || (getNode((RawTile) route[i]) != null))
                        numAssigned += ROUTER_WEIGHT;
                    else
                        // now it is a router tile
                        routers.add(route[i]);
                    /* } */
                }

                int hops = route.length - 2;
                // the number of items sent over this channel for one execution
                // of entire
                // SSG, from src to dest
                int items = 0;

                // now calculate the number of items sent per firing of SSG

                // if we are sending thru a splitter we have to be careful
                // because not
                // all the data that the src produces goes to the dest
                if (src.edges[0].isSplitter()) {
                    // if the dest is a filter, then just calculate the number
                    // of items
                    // the dest filter receives
                    if (dst.isFilter())
                        items = ssg.getMult(dst, false)
                            * dst.getFilter().getPopInt();
                    else {
                        // this is a joiner
                        assert dst.isJoiner();
                        // the percentage of items that go to this dest
                        double rate = 1.0;

                        // we are sending to a joiner thru a splitter, this will
                        // only happen
                        // for a feedback loop, the feedback path is always way
                        // 0 thru the joiner
                        if (dst.inputs > 1)
                            rate = ((double) dst.incomingWeights[0])
                                / ((double) dst.getTotalIncomingWeights());
                        // now calculate the rate at which the splitter sends to
                        // the joiner
                        rate = rate
                            * (((double) src.edges[0].weights[0]) / ((double) src.edges[0]
                                                                     .getTotalOutgoingWeights()));
                        // now calculate the number of items sent to this dest
                        // by this filter
                        items = (int) rate * ssg.getMult(dst, false)
                            * Util.getItemsProduced(src);
                    }
                } else {
                    // sending without intermediate splitter
                    // get the number of items sent
                    int push = 0;
                    if (src.isFilter())
                        push = src.getFilter().getPushInt();
                    else
                        // joiner
                        push = 1;
                    items = ssg.getMult(src, false) * push;
                }

                items *= Util.getTypeSize(CommonUtils.getOutputType(src));
                // calculate communication cost of this node and add it to the
                // cost sum

                // what we really want to do here is add to the latency the work
                // estimation sum of all the routes that this route crosses...
                // um, yeah
                cost += ((items * hops) + (/* items */numAssigned));
            }
        }
        SpaceDynamicBackend.addAll(usedTiles, tiles);
        return cost;
    }

    /**
     * Get the cost of sending output of this SSG over the dynamic network for
     * right now disregard the min, max, and average rate declarations
     * 
     * NOTE: THIS FUNCTION DOES NOT CHECK FOR STARVATION OR DEADLOCK FROM
     * SHARING DYNAMIC LINKS. WE SHOULD CHECK THAT TWO PARALLEL SECTIONs OF A
     * STREAM GRAPH DO NOT SHARE A LINK.
     */
    private double getDynamicCost(SpdStaticStreamGraph ssg, HashSet usedTiles) {
        double cost = 0.0;

        // check if the dynamic communication is legal
        for (int j = 0; j < ssg.getOutputs().length; j++) {
            FlatNode src = ssg.getOutputs()[j];
            FlatNode dst = ssg.getNext(src);
            int typeSize = Util.getTypeSize(ssg.getOutputType(src));

            ComputeNode srcTile;
            try {
                srcTile = getComputeNode(src);
            } catch (AssertionError e) {
                srcTile = getComputeNode(Util.getFilterUpstreamAssigned(this,src));
            }
            ComputeNode dstTile;
            try {
                dstTile = getComputeNode(dst);
            } catch (AssertionError e) {
                dstTile = getComputeNode(Util.getFilterDownstreamAssigned(this,dst));
            }

            Iterator route = XYRouter.getRoute(ssg, srcTile, dstTile)
                .iterator();
            // System.out.print("Dynamic Route: ");

            // ********* TURNS IN DYNAMIC NETWORK COST IN TERMS OF LATENCY
            // ****//
            if (srcTile.getX() != dstTile.getX()
                && srcTile.getY() != dstTile.getY())
                cost += (1.0 * typeSize);

            // ** Don't share links, could lead to starvation?? ***///

            while (route.hasNext()) {
                ComputeNode tile = (ComputeNode) route.next();
                assert tile != null;

                if (usedTiles.contains(tile)) // this tile already used to
                    // dynamic route
                    cost += DYN_CROSSED_ROUTE; // ILLEGAL_WEIGHT/(DYN_MULT *
                // 100);
                else
                    usedTiles.add(tile);

                // System.out.print(tile);
                // add to cost only if these are not endpoints of the route
                if (tile != srcTile && tile != dstTile) {
                    cost += (1.0 * typeSize);
                }

            }
        }
        return cost;
    }

    public void simAnnealAssign() {
        System.out.println("Simulated Annealing Assignment");
        int nsucc = 0, j = 0;
        double currentCost = 0.0, minCost = 0.0;
        // number of paths tried at an iteration
        int nover = 100; // * RawBackend.rawRows * RawBackend.rawColumns;

        try {
            random = new Random(17);
            assignFileFilters();
            // create an initial placement
            initialPlacement();

            filew = new FileWriter("simanneal.out");
            int configuration = 0;

            currentCost = placementCost(false);
            assert currentCost >= 0.0;
            System.out.print("Initial Cost: " + currentCost);

            if (KjcOptions.manuallayout || KjcOptions.decoupled) {
                dumpLayout("layout-without-annealing.dot");
                return;
            }
            // as a little hack, we will cache the layout with the minimum cost
            // these two hashmaps store this layout
            HashMap sirMin = (HashMap) SIRassignment.clone();
            HashMap tileMin = (HashMap) tileAssignment.clone();
            minCost = currentCost;

            if (currentCost == 0.0) {
                dumpLayout("layout.dot");
                return;
            }

            // The first iteration is really just to get a
            // good initial layout. Some random layouts really kill the
            // algorithm
            for (int two = 0; two < rawChip.getYSize(); two++) {
                System.out.print("\nRunning Annealing Step (" + currentCost
                                 + ", " + minCost + ")");
                double t = annealMaxTemp();
                double tFinal = annealMinTemp();
                while (true) {
                    int k = 0;
                    nsucc = 0;
                    for (k = 0; k < nover; k++) {
                        // true if config change was accepted
                        boolean accepted = perturbConfiguration(t);
                        currentCost = placementCost(false);
                        // the layout should always be legal
                        assert currentCost >= 0.0;

                        if (accepted) {
                            filew.write(configuration++ + " " + currentCost
                                        + "\n");
                            nsucc++;
                        }

                        if (configuration % 500 == 0)
                            System.out.print(".");

                        // keep the layout with the minimum cost
                        if (currentCost < minCost) {
                            minCost = currentCost;
                            // save the layout with the minimum cost
                            sirMin = (HashMap) SIRassignment.clone();
                            tileMin = (HashMap) tileAssignment.clone();
                        }

                        // this will be the final layout
                        if (currentCost == 0.0)
                            break;
                    }

                    t *= TFACTR;

                    if (nsucc == 0)
                        break;
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
            System.out.println("\nFinal Cost = " + currentCost + "; Min Cost = "
                               + minCost + " in " + j + " iterations.");
            if (minCost < currentCost) {
                SIRassignment = sirMin;
                tileAssignment = tileMin;
                currentCost = minCost;
            }

            filew.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        dumpLayout("layout.dot");
    }

    private void initialPlacement() {
        assert router instanceof FreeTileRouter : "Using non-supported router";

        // build the list of nodes that we have to assign to tiles
        // and build it in data-flow order for each SSG
        LinkedList assignMeToATile = new LinkedList();
        for (int i = 0; i < streamGraph.getStaticSubGraphs().length; i++) {
            SpdStaticStreamGraph ssg = (SpdStaticStreamGraph)streamGraph.getStaticSubGraphs()[i];
            Iterator flatNodes = ssg.getFlatNodes().iterator();
            while (flatNodes.hasNext()) {
                FlatNode node = (FlatNode) flatNodes.next();
                // if we should assign this node to a tile
                // then add it to the traversal
                if (assignToATile(node))
                    assignMeToATile.add(node);
            }

        }

        Iterator traversal = assignMeToATile.iterator();

        int row = 0;
        int column = 0;

        // start laying out at top right corner
        for (row = 0; row < rawChip.getYSize(); row++) {
            if (row % 2 == 0) {
                for (column = rawChip.getXSize() - 1; column >= 0;) {
                    if (!traversal.hasNext())
                        break;
                    FlatNode node = (FlatNode) traversal.next();
                    assign(rawChip.getTile(column, row), node);
                    column--;
                }
            } else {
                for (column = 0; column < rawChip.getXSize();) {
                    if (!traversal.hasNext())
                        break;
                    FlatNode node = (FlatNode) traversal.next();
                    assign(rawChip.getTile(column, row), node);
                    column++;
                }
            }
            // if nothing more to do then break
            if (!traversal.hasNext())
                break;
        }

        dumpLayout("initial-layout.dot");
    }

    private double annealMaxTemp() throws Exception {
        double T = 1.0;
        int total = 0, accepted = 0;
        HashMap sirInit = (HashMap) SIRassignment.clone();
        HashMap tileInit = (HashMap) tileAssignment.clone();

        for (int i = 0; i < MAXTEMPITERATIONS; i++) {
            T = 2.0 * T;
            total = 0;
            accepted = 0;
            for (int j = 0; j < 100; j++) {
                // c_old <- c_init
                SIRassignment = sirInit;
                tileAssignment = tileInit;
                if (perturbConfiguration(T))
                    accepted++;
                total++;
            }
            if (((double) accepted) / ((double) total) > .9)
                break;
        }
        // c_old <- c_init
        SIRassignment = sirInit;
        tileAssignment = tileInit;
        return T;
    }

    private double annealMinTemp() throws Exception {
        double T = 1.0;
        int total = 0, accepted = 0;
        HashMap sirInit = (HashMap) SIRassignment.clone();
        HashMap tileInit = (HashMap) tileAssignment.clone();

        for (int i = 0; i < MINTEMPITERATIONS; i++) {
            T = 0.5 * T;
            total = 0;
            accepted = 0;
            for (int j = 0; j < 100; j++) {
                // c_old <- c_init
                SIRassignment = sirInit;
                tileAssignment = tileInit;
                if (perturbConfiguration(T))
                    accepted++;
                total++;
            }
            if (((double) accepted) / ((double) total) > .1)
                break;
        }
        // c_old <- c_init
        SIRassignment = sirInit;
        tileAssignment = tileInit;
        return T;
    }

    // return true if the perturbation is accepted
    private boolean perturbConfiguration(double T) throws Exception {
        int first, second;
        // the cost of the new layout and the old layout
        double e_new, e_old = placementCost(false);
        // the nodes to swap
        FlatNode firstNode, secondNode;

        // find 2 suitable nodes to swap
        while (true) {
            first = getRandom();
            second = getRandom();
            // do not swap same tile or two null tiles
            if (first == second)
                continue;
            if ((getNode(rawChip.getTile(first)) == null)
                && (getNode(rawChip.getTile(second)) == null))
                continue;

            firstNode = getNode(rawChip.getTile(first));
            secondNode = getNode(rawChip.getTile(second));
            // perform swap
            assign(rawChip.getTile(first), secondNode);
            assign(rawChip.getTile(second), firstNode);

            // the new placement cost
            e_new = placementCost(false);

            if (e_new < 0.0) {
                // illegal tile assignment so revert the assignment
                assign(rawChip.getTile(second), secondNode);
                assign(rawChip.getTile(first), firstNode);
                continue;
            } else
                // found a successful new layout
                break;
        }

        double P = 1.0;
        double R = random.nextDouble();

        if (e_new >= e_old)
            P = Math.exp((((double) e_old) - ((double) e_new)) / T);

        if (R < P) {
            return true;
        } else {
            // reject configuration
            assign(rawChip.getTile(second), secondNode);
            assign(rawChip.getTile(first), firstNode);
            return false;
        }
    }

    private int getRandom() {
        return random.nextInt(rawChip.getTotalTiles());
    }

    /***************************************************************************
     * Return true if this flatnode is or should be assigned to a compute node
     * (a tile or a port)
     **************************************************************************/
    public static boolean assignToAComputeNode(FlatNode node) {
        return assignToATile(node) || node.contents instanceof SIRFileReader
            || node.contents instanceof SIRFileWriter;
    }

    /** return true if this flatnode is or should be assigned to tile * */
    public static boolean assignToATile(FlatNode node) {
        if (node.isFilter()
            && !(node.contents instanceof SIRIdentity
                 || node.contents instanceof SIRFileReader || node.contents instanceof SIRFileWriter))
            return true;
        else if (node.isJoiner())
            return assignedJoiner(node);

        return false;
    }

    /**
     * Decide whether this joiner should be assigned to a tile.  Don't assign
     * null joiners or joiners that output to a joiner.
     * 
     * @param node The flatnode to check.
     * @return True if we should assign this joiner to a tile.
     */
    public static boolean assignedJoiner(FlatNode node) {
        assert node.isJoiner();
        //don't assign null joiners with zero weights
        if (node.edges.length == 0) {
            assert node.getTotalIncomingWeights() == 0;
            return false;
        }
        
        //don't need this joiner if it is connected to a joiner downstream
        if (node.edges[0] != null && node.edges[0].isJoiner())
            return false;
        
        return true;
    }

    /** END simulated annealing methods * */

    
    /**
     * @return Return the set of tiles that are intermediate hops for routes. 
     */
    public HashSet getIntermediateTiles() {
        assert intermediateTiles != null : "Did not setup intermediate tiles in layout.";
        return intermediateTiles;
    }
   
    /**
     * @param tile  The tile to test.
     * @return True if this tile is involved in an intermediate hop of a route (not an
     * endpoint).
     */
    public boolean isIntermediateTile(RawTile tile) {
        assert intermediateTiles != null : "Did not setup intermediate tiles in layout.";
        return intermediateTiles.contains(tile);
    }
    
    /** visit each flatnode and decide whether is should be assigned * */
    public void visitNode(FlatNode node) {
        if (node.isFilter()) {
            // create an entry in the memory foot print map
            memoryFP.put(node, new Integer(DataEstimate
                                           .computeFilterGlobalsSize(node.getFilter())));
            // create an entry in the work estimation map
            workEstimates.addEstimate(node);
            // do not map layout.identities, but add them to the
            // layout.identities set
            if (node.contents instanceof SIRIdentity) {
                identities.add(node);
                return;
            }
            /*
             * // this if statement possibly breaks automatic layout (?) // so
             * only perform test for manual if (!KjcOptions.noanneal ||
             * assignToATile(node)) { assigned.add(node); }
             */
            if (assignToATile(node))
                assigned.add(node);
            return;
        }
        if (node.contents instanceof SIRJoiner && assignedJoiner(node)) {
            // create an entry in the work estimation map
            workEstimates.addEstimate(node);
            joiners.add(node);
            assigned.add(node);
        }
    }
}
