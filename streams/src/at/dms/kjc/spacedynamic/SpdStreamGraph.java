package at.dms.kjc.spacedynamic;

import at.dms.kjc.flatgraph.FlatNode;
import at.dms.kjc.spacedynamic.SpdStaticStreamGraph;
//import at.dms.util.IRPrinter;
//import at.dms.util.SIRPrinter;
import at.dms.kjc.*;
import at.dms.kjc.sir.*;
import java.util.*;
import java.io.*;

/**
 * This class represents the entire stream graph of the application we are
 * compiling. It is composed of StaticStreamGraphs in which all filters
 * communicate over static rate channels. The connections between
 * StaticStreamGraphs (SSGs) are dynamic rate.
 * 
 * The StreamGraph includes objects (passes) that operate on it and store
 * information for compilation, such as layout, FileState, etc.
 * 
 * This extension to at.dms.kjc.flatgraph.StreamGraph is to allow layout
 * and scheduling information to be associated with a StreamGraph and its
 * associated StatisStreamGraph's
 * 
 * It is a bit confusing casting something which already appears to be
 * of type StreamGraph to StreamGraph, wh should have had different names.
 */

public class SpdStreamGraph extends at.dms.kjc.flatgraph.StreamGraph {

    /** the tile assignment for the stream graph * */
    private Layout layout;

    /** Information on all the file readers and writer of the graph * */
    private FileState fileState;

//    /** map of flat nodes to parent ssg * */
//    public HashMap<FlatNode,StaticStreamGraph> parentMap;

    /** schedules of sending for all the joiners of the stream graph * */
    public JoinerSimulator joinerSimulator;

    /** Maps RawTile -> switch code schedule * */
    public HashMap initSwitchSchedules;

    /** Maps RawTile -> switch code schedule * */
    public HashMap steadySwitchSchedules;

    private RawChip rawChip;

    /**
     * Create the static stream graph for the application that has <pre>top</pre> as the
     * top level FlatNode, and compile it to <pre>rawChip</pre>
     */
    public SpdStreamGraph(FlatNode top, RawChip rawChip) {
        super(top);
        this.rawChip = rawChip;
//        this.topLevelFlatNode = top;
//        parentMap = new HashMap();
        initSwitchSchedules = new HashMap();
        steadySwitchSchedules = new HashMap();
    }

    
    /**
     * Use in place of "new StaticStreamGraph" for subclassing.
     * 
     * A subclass of StreamGraph may refer to a subclass of StaticStreamGraph.
     * If we just used "new StaticStreamGraph" in this class we would
     * only be making StaticStreamGraph's of the type in this package.
     * Note that we are only able to overridet a method if the inputs
     * match the type of the overridden method, so we expect a StreamGraph
     * but have to claim that the input is a  at.dms.kjc.flatgraph.StreamGraph
     * 
     * @param sg       a SpdStreamGraph
     * @param realTop  the top node
     * @return         a new SpdStaticStreamGraph
     */
    @Override protected SpdStaticStreamGraph new_StaticStreamGraph(at.dms.kjc.flatgraph.StreamGraph sg, FlatNode realTop) {
        assert sg instanceof SpdStreamGraph;
        return new SpdStaticStreamGraph((SpdStreamGraph)sg,realTop);
    }

    
   /** 
     * @return True if the graph is laid out with no overlapping routes, no
     * routing tiles, and each joiner has at most 3 incoming nodes with no
     * chained joiners, and each splitter has as most 3 outgoing nodes with
     * no chained splitters.  
     * 
     */
    public boolean isSimple() {
        //check there are no overlapping routes and not router tiles, 
        //this check should be sufficient on a 2d raw configuration
        if (!(layout.getIntermediateTiles().size() == 0))
            return false;
        
        return true;
    }
    
    /**
     * This will automatically assign the single tile of the raw chip to the one
     * filter of the one SSG, all these conditions must be true
     */
    public void tileAssignmentOneFilter() {
        assert rawChip.getTotalTiles() == 1;
        assert staticSubGraphs.length == 1;

        SpdStaticStreamGraph ssg = (SpdStaticStreamGraph)staticSubGraphs[0];
        assert ssg.filterCount() == 1;
        // set the number of tiles of the static sub graph
        ssg.setNumTiles(1);
    }

    /** Ask the user for the number of tiles to assign to each SSG * */
    public void handTileAssignment() {
        // if there is just one ssg, give all the tiles to it...
        /*
         * if (staticSubGraphs.length == 1) {
         * staticSubGraphs[0].setNumTiles(rawChip.getTotalTiles()); return; }
         */

        BufferedReader inputBuffer = new BufferedReader(new InputStreamReader(
                                                                              System.in));
        int numTilesToAssign = rawChip.getTotalTiles(), num;
        SpdStaticStreamGraph current;

        for (int i = 0; i < staticSubGraphs.length; i++) {
            current = (SpdStaticStreamGraph)staticSubGraphs[i];
            int assignedNodes = current.countAssignedNodes();
            
            //don't do anything for file readers and writers SSGs that do not need a tile
            if (assignedNodes < 1)
                continue;
            
            int allowedErrors = 5;  // used in batch mode to keep from infinite loop
                                    // if stdin = /dev/null, or if predefined inputs incorrect
            
            while (allowedErrors > 0) {
                System.out.print("Number of tiles for " + current + " ("
                                 + numTilesToAssign + " tiles left, " + assignedNodes
                                 + " nodes in subgraph): ");
                try {
                    num = Integer.valueOf(inputBuffer.readLine()).intValue();
                } catch (Exception e) {
                    System.out.println("Bad number!");
                    allowedErrors--;
                    continue;
                }

                if (num <= 0) {
                    allowedErrors--;
                    System.out.println("Enter number > 0!");
                    continue;
                }

                if ((numTilesToAssign - num) < 0) {
                    allowedErrors--;
                    System.out.println("Too many tiles desired!");
                    continue;
                }

                current.setNumTiles(num);
                numTilesToAssign -= num;
                break;
            }
            if (allowedErrors == 0) {
                System.err.println("Too many errors reading number of tiles");
                System.exit(1);
            }
        }
    }

    /** for each sub-graph, assign a certain number of tiles to it * */
    public void tileAssignment() {
        // if there is just one ssg, give all the tiles to it...
        if (staticSubGraphs.length == 1) {
            ((SpdStaticStreamGraph)staticSubGraphs[0]).setNumTiles(rawChip.getTotalTiles());
            return;
        }

        int numTilesToAssign = rawChip.getTotalTiles();
        SpdStaticStreamGraph current;

        // for right now just assign exactly the number of tiles as needed
        for (int i = 0; i < staticSubGraphs.length; i++) {
            current = (SpdStaticStreamGraph)staticSubGraphs[i];
            int assignedNodes = current.countAssignedNodes();
            current.setNumTiles(assignedNodes);
            numTilesToAssign -= assignedNodes;
            assert numTilesToAssign >= 0 : "Error: Not enough tiles for unpartitioned graph, can't use --nopartition";
        }
    }

    /** return the FileState object for this stream graph * */
    public FileState getFileState() {
        return fileState;
    }

    /** return the layout object for this stream graph * */
    public Layout getLayout() {
        return layout;
    }

    /** layout the entire Stream Graph on the RawChip * */
    public void layoutGraph() {
        // set up the parent map for other passes
        ((SpdStaticStreamGraph)getTopLevel()).accept(new StreamGraphVisitor() {
                public void visitStaticStreamGraph(SpdStaticStreamGraph ssg) {
                    parentMap.putAll(ssg.getParentMap());
                }

            }, null, true);

        // gather the information on file readers and writers in the graph
        fileState = new FileState(this);

        // now ready to layout
        layout = new Layout(this);
        // call the appropriate layout function
        if (KjcOptions.layoutfile != null)
            layout.fileAssign();
        else if (KjcOptions.manuallayout)
            layout.handAssign();
        else
            layout.simAnnealAssign();
    }

    /** return the Raw Chip we are compiling to * */
    public RawChip getRawChip() {
        return rawChip;
    }

    /** print out some stats on each SSG to STDOUT * */
    public void dumpStreamGraph() {
        SpdStaticStreamGraph current = (SpdStaticStreamGraph)getTopLevel();
        for (int i = 0; i < staticSubGraphs.length; i++) {
            current = (SpdStaticStreamGraph)staticSubGraphs[i];
            System.out.println("******* StaticStreamGraph ********");
            System.out.println("Dynamic rate input = "
                               + dynamicEntry(current.getTopLevelSIR()));
            System.out.println("InputType = "
                               + current.getTopLevelSIR().getInputType());
            System.out.println(current.toString());
            System.out.println("Tiles Assigned = " + current.getNumTiles());
            System.out.println("OutputType = "
                               + current.getTopLevelSIR().getOutputType());
            System.out.println("Dynamic rate output = "
                               + dynamicExit(current.getTopLevelSIR()));
            StreamItDot.printGraph(current.getTopLevelSIR(), current
                                   .getTopLevelSIR().getIdent()
                                   + ".dot");
            System.out.println("**********************************");
        }
    }

    /** create a stream graph with only one filter (thus one SSG). */
    public static SpdStreamGraph constructStreamGraph(SIRFilter filter) {
        return constructStreamGraph(new FlatNode(filter));
    }

    /**
     * create a stream graph with only one filter (thus one SSG), it's not laid
     * out yet.
     */
    public static SpdStreamGraph constructStreamGraph(FlatNode node) {
        assert node.isFilter();

        SpdStreamGraph streamGraph = new SpdStreamGraph(node, new RawChip(1, 1));
        streamGraph.createStaticStreamGraphs();
        streamGraph.tileAssignmentOneFilter();

        // gather the information on file readers and writers in the graph
        streamGraph.fileState = new FileState(streamGraph);
        // now ready to layout
        streamGraph.layout = new Layout(streamGraph);
        return streamGraph;
    }

}
