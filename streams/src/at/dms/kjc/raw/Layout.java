package at.dms.kjc.raw;

import at.dms.kjc.common.CommonUtils;
import at.dms.kjc.flatgraph.FlatNode;
import at.dms.kjc.flatgraph.FlatVisitor;
import at.dms.kjc.*;
import at.dms.kjc.sir.*;
import at.dms.kjc.sir.lowering.*;
import at.dms.util.Utils;
import java.io.*;
import java.util.*;


/**
 *The Layout class generates mapping of filters to raw tiles.  It assumes that the Sis
 * namer has been run and that the stream graph has been partitioned.
 */
public class Layout extends at.dms.util.Utils implements FlatVisitor {

    private  HashMap<SIROperator, Coordinate> SIRassignment;
    /* coordinate -> flatnode */
    private  HashMap<Object, FlatNode> tileAssignment;
    private  HashSet<Object> assigned;
    //set of all the identity filters not mapped to tiles
    private   HashSet<FlatNode> identities;
    private  Coordinate[][] coordinates;
    private  BufferedReader inputBuffer;
    private  Random random;
    private  FlatNode toplevel;
    /* hashset of Flatnodes representing all the joiners
       that are mapped to tiles */
    private  HashSet<FlatNode> joiners;
    
    public  static int MINTEMPITERATIONS = 200;
    public static int MAXTEMPITERATIONS = 200;
    public static int ANNEALITERATIONS = 10000;
    public static double TFACTR = 0.9;

    private FileWriter filew;
    
    private static Layout layout;

    public Layout() 
    {
        joiners = new HashSet<FlatNode>();
    }
    

    //returns the state of the layout
    public static Layout getLayout() 
    {
        return layout;
    }
    
    //sets the state of the layout
    public static void setLayout(Layout lo) 
    {
        Layout.layout = lo;
    }
    

    private static void init(FlatNode top) 
    {
        layout.toplevel = top;
        int rows = RawBackend.rawRows;
        int columns = RawBackend.rawColumns;
        
        //determine if there is a file reader/writer in the graph
        //call init() to traverse the graph 
        // if there is create a new column to place the fileReader/writer
        // it will become a bc file reading device
        FileVisitor.init(top);
        if (FileVisitor.foundReader || FileVisitor.foundWriter) {
            columns++;
        }
    
        layout.coordinates  =
            new Coordinate[rows][columns];
        for (int row = 0; row < rows; row++)
            for (int column = 0; column < columns; column++)
                layout.coordinates[row][column] = new Coordinate(row, column);
    
        layout.SIRassignment = new HashMap<SIROperator, Coordinate>();
        layout.tileAssignment = new HashMap<Object, FlatNode>();
        layout.assigned = new HashSet<Object>();
        layout.identities = new HashSet<FlatNode>();

        top.accept(new Layout(), new HashSet<FlatNode>(), false);
    
        System.out.println("Tiles layout.assigned: " + layout.assigned.size());

        if (layout.assigned.size() > 
            (RawBackend.rawRows * RawBackend.rawColumns)) {
            System.err.println("\nLAYOUT ERROR: Need " + layout.assigned.size() +
                               " tiles, have " + 
                               (RawBackend.rawRows * RawBackend.rawColumns) +
                               " tiles.");
            System.exit(-1);
        }
    }

    public static  int getTilesAssigned() {
        return layout.assigned.size();
    }

    public static  boolean isAssigned(FlatNode node) {
        return layout.assigned.contains(node);
    }
    
    public static boolean isAssigned(Coordinate tile) 
    {
        assert layout.SIRassignment.values().contains(tile) == 
            layout.tileAssignment.keySet().contains(tile);
    
        return layout.tileAssignment.keySet().contains(tile);
    }
    

    public static Set<Object> getTiles() {
        return layout.tileAssignment.keySet();
    }
    
    public static HashSet<FlatNode> getJoiners() 
    {
        return layout.joiners;
    }
    
    public static HashSet<FlatNode> getIdentities() 
    {
        return layout.identities;
    }
    

    /**
     * Returns the tile number assignment for <str>, or null if none has been layout.assigned.
     */
    public static Coordinate getTile(SIROperator str) 
    {
        if (layout.SIRassignment == null) return null;
        return layout.SIRassignment.get(str);
    }
    
    public static Coordinate getTile(FlatNode str) 
    {
        if (layout.SIRassignment == null){
            return null;
        }
        Coordinate ret = layout.SIRassignment.get(str.contents);
        return ret;
    }

    public static Coordinate getTile(int row, int column) 
    {
        return layout.coordinates[row][column];
    }

    public static Coordinate getTile(int tileNumber) 
    {
        if (!(tileNumber / RawBackend.rawColumns < RawBackend.rawRows))
            Utils.fail("tile number too high");
    
        return layout.coordinates[tileNumber / RawBackend.rawColumns]
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
        return layout.tileAssignment.get(coord);
    }
    
    
    private static void assign(Coordinate coord, FlatNode node) 
    {
        if (node == null) {
            layout.tileAssignment.remove(coord);
            return;
        }
        layout.tileAssignment.put(coord, node);
        layout.SIRassignment.put(node.contents, coord);
    }

    private static void assign(HashMap<SIROperator, Coordinate> sir, HashMap<Coordinate, FlatNode> tile, Coordinate coord, FlatNode node) 
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
        layout = new Layout();
        System.out.println("Simulated Annealing Assignment");
        int nsucc =0, j = 0;
        double currentCost = 0.0, minCost = 0.0;
        //number of paths tried at an iteration
        int nover = 100; //* RawBackend.rawRows * RawBackend.rawColumns;

        try {
            init(node);
            layout.random = new Random(17);
            //random placement
            randomPlacement();
            layout.filew = new FileWriter("simanneal.out");
            int configuration = 0;

            currentCost = placementCost();
            System.out.println("Initial Cost: " + currentCost);
        
            if (KjcOptions.manuallayout || KjcOptions.decoupled) {
                dumpLayout("noanneal.dot");
                return;
            }
            //as a little hack, we will cache the layout with the minimum cost
            //these two hashmaps store this layout
            HashMap<SIROperator, Coordinate> sirMin = (HashMap<SIROperator, Coordinate>)layout.SIRassignment.clone();
            HashMap<Object, FlatNode> tileMin = (HashMap<Object, FlatNode>)layout.tileAssignment.clone();
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
                        boolean accepted = perturbConfiguration(t);
                        currentCost = placementCost();
            
                        if (accepted) {
                            layout.filew.write(configuration++ + " " + currentCost + "\n");
                            nsucc++;
                        }
            
            
                        //keep the layout with the minimum cost
                        //this will be the final layout
                        if (currentCost < minCost) {
                            minCost = currentCost;
                            //save the layout with the minimum cost
                            sirMin = (HashMap<SIROperator, Coordinate>)layout.SIRassignment.clone();
                            tileMin = (HashMap<Object, FlatNode>)layout.tileAssignment.clone();
                        }
            
                        if (currentCost == 0.0)
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
                layout.SIRassignment = sirMin;
                layout.tileAssignment = tileMin;
            }
        
            layout.filew.close();
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
        HashMap<SIROperator, Coordinate> sirInit  = (HashMap<SIROperator, Coordinate>)layout.SIRassignment.clone();
        HashMap<Object, FlatNode> tileInit = (HashMap<Object, FlatNode>)layout.tileAssignment.clone();
    
        for (int i = 0; i < MAXTEMPITERATIONS; i++) {
            T = 2.0 * T;
            total = 0;
            accepted = 0;
            for (int j = 0; j < 100; j++) {
                //c_old <- c_init
                layout.SIRassignment = sirInit;
                layout.tileAssignment = tileInit;
                if (perturbConfiguration(T))
                    accepted ++;
                total++;
            }
            if (((double)accepted) / ((double)total) > .9)
                break;
        }
        //c_old <- c_init
        layout.SIRassignment = sirInit;
        layout.tileAssignment = tileInit;
        return T;
    }

    private static double annealMinTemp() throws Exception
    {
        double T = 1.0;
        int total = 0, accepted = 0;
        HashMap<SIROperator, Coordinate> sirInit  = (HashMap<SIROperator, Coordinate>)layout.SIRassignment.clone();
        HashMap<Object, FlatNode> tileInit = (HashMap<Object, FlatNode>)layout.tileAssignment.clone();
    
        for (int i = 0; i < MINTEMPITERATIONS; i++) {
            T = 0.5 * T;
            total = 0;
            accepted = 0;
            for (int j = 0; j < 100; j++) {
                //c_old <- c_init
                layout.SIRassignment = sirInit;
                layout.tileAssignment = tileInit;
                if (perturbConfiguration(T))
                    accepted ++;
                total++;
            }
            if (((double)accepted) / ((double)total) > .1)
                break;
        }
        //c_old <- c_init
        layout.SIRassignment = sirInit;
        layout.tileAssignment = tileInit;
        return T;
    }

    private static void randomPlacement() 
    {
        //assign the fileReaders/writers to the last row
        if (FileVisitor.foundReader || FileVisitor.foundWriter) {
            Iterator<Object> frs = FileVisitor.fileReaders.iterator();
            Iterator<Object> fws = FileVisitor.fileWriters.iterator();
            int row = 0;
            //check to see if there are less then
            //file readers/writers the number of columns
            if (FileVisitor.fileReaders.size() + 
                FileVisitor.fileWriters.size() > RawBackend.rawRows)
                Utils.fail("Too many file readers/writers (must be less than rows).");
            //assign the file streams to the added column starting at the top
            //row
            while (frs.hasNext()) {
                assign(layout.coordinates[row][RawBackend.rawColumns], 
                       (FlatNode)frs.next());
                row++;
            }
            while (fws.hasNext()) {
                assign(layout.coordinates[row][RawBackend.rawColumns],
                       (FlatNode)fws.next());
                row++;
            }
        }
        //Iterator nodes = layout.assigned.iterator();
        ListIterator<FlatNode> dfTraversal = DFTraversal.getDFTraversal(layout.toplevel).listIterator(0);
        int row = 0;
        int column = 0;

        //start laying out at top right corner because of file reader
        for (row = 0; row < RawBackend.rawRows; row ++) {
            if (row % 2 == 0) {
                for (column = RawBackend.rawColumns -1; column >= 0;) {
                    if (!dfTraversal.hasNext())
                        break;
                    FlatNode node = dfTraversal.next();
                    if (!layout.assigned.contains(node))
                        continue;
                    assign(getTile(row, column), node);
                    column--;
                }
            }
            else {
                for (column = 0; column < RawBackend.rawColumns;) {
                    if (!dfTraversal.hasNext())
                        break; 
                    FlatNode node = dfTraversal.next();
                    if (!layout.assigned.contains(node))
                        continue;
                    assign(getTile(row, column), node); 
                    column++;
                }
            }
            if (!dfTraversal.hasNext())
                break;
        }


        dumpLayout("initial-layout.dot");
    }
    
    private static double placementCost() 
    {
        HashSet<Object> nodes = (HashSet<Object>)layout.assigned.clone();
        RawBackend.addAll(nodes, FileVisitor.fileReaders);
    
        Iterator<Object> nodesIt = nodes.iterator();
        HashSet<Coordinate> routers = new HashSet<Coordinate>();
        double sum = 0.0;
        while(nodesIt.hasNext()) {
            FlatNode node = (FlatNode)nodesIt.next();
            sum += placementCostHelper(node, routers);
        }
        return sum;
    }
    
    private static double placementCostHelper(FlatNode node, HashSet<Coordinate> routers) 
    {
        //get all placed downstream nodes
        Iterator<Object> downstream = getDownStream(node).iterator();
        double sum = 0.0;
        while (downstream.hasNext()) {
            FlatNode dest = (FlatNode)downstream.next();
            Coordinate[] route = 
                Router.getRoute(node, dest).toArray(new Coordinate[1]);
            //find the number layout.assigned on the path
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
            if (node.getEdges()[0].contents instanceof SIRSplitter) {
                //if the final dest is a filter then just get the execution count of the 
                //dest filter * its pop rate
                if (dest.contents instanceof SIRFilter) {
                    items = RawBackend.steadyExecutionCounts.get(dest).intValue() *
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
                    items = (int)(RawBackend.steadyExecutionCounts.get(dest).intValue() / rate);
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
                    items = RawBackend.steadyExecutionCounts.get(node).intValue() *
                        push;
            }
            //if (hops > 0 && numAssigned > 0.0)
            //      System.out.println(node + ": " + hops + " " + numAssigned);
        
            sum += ((items * hops) + (items * Util.getTypeSize(CommonUtils.getOutputType(node)) * 
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
        double R = layout.random.nextDouble();

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
        return layout.random.nextInt(RawBackend.rawRows*
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
                Iterator<Coordinate> neighbors = getNeighbors(getTile(i, j)).iterator();
                while (neighbors.hasNext()) {
                    Coordinate n = neighbors.next();
                    buf.append("tile" + getTileNumber(getTile(i,j)) + " -> tile" +
                               getTileNumber(n) + " [weight = 100000000];\n");
                }
            }
        }
        buf.append("edge[color = red,arrowhead = normal, arrowsize = 2.0, style = bold];\n");
        Iterator<FlatNode> it = layout.tileAssignment.values().iterator();
        while(it.hasNext()) {
            FlatNode node = it.next();
            if (FileVisitor.fileNodes.contains(node))
                continue;
            buf.append("tile" +   getTileNumber(node) + "[label=\"" + 
                       //getTileNumber(node) + "\"];\n");
                       shortName(node.getName()) + "\"];\n");
         
            //we only map joiners and filters to tiles and they each have
            //only one output
            Iterator<Object> downstream = getDownStream(node).iterator();
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
        //  return str.substring(0, Math.min(str.length(), 15));
        return str;
    }

    private static HashSet<Object> getDownStream(FlatNode node) 
    {
        if (node == null)
            return new HashSet<Object>();
        HashSet<Object> ret = new HashSet<Object>();
        for (int i = 0; i < node.ways; i++) {
            RawBackend.addAll(ret, getDownStreamHelper(node.getEdges()[i]));
        }
        return ret;
    }

    private static HashSet<Object> getDownStreamHelper(FlatNode node) {
        if (node == null)
            return new HashSet<Object>();
    
        if (node.contents instanceof SIRFilter) {
            if (layout.identities.contains(node))
                return getDownStreamHelper(node.getEdges()[0]);
            HashSet<Object> ret = new HashSet<Object>();
            ret.add(node);
            return ret;
        }
        else if (node.contents instanceof SIRJoiner) {
            if (layout.joiners.contains(node)) {
                HashSet<Object> ret = new HashSet<Object>();
                ret.add(node);
                return ret;
            }   //if this joiner was not mapped then visit its down stream
            else return (node.getEdges().length > 0 ? getDownStreamHelper(node.getEdges()[0]) :
                         getDownStreamHelper(null));  //be careful about null joiners
        }
        else if (node.contents instanceof SIRSplitter) {
            HashSet<Object> ret = new HashSet<Object>();
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
                    RawBackend.addAll(ret, getDownStreamHelper(node.getEdges()[i]));
            }
            return ret;
        }
        Utils.fail("Serious Error in Simulated Annealing");
    
        return null;
    }
        
    //but not north neighbor or west
    private static List<Coordinate> getNeighbors(Coordinate coord) 
    {
        int row = coord.getRow();
        int column = coord.getColumn();
        LinkedList<Coordinate> neighbors = new LinkedList<Coordinate>();
    
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
        layout = new Layout();
        init(node);
        System.out.println("Enter desired tile for each filter...");
        layout.inputBuffer = new BufferedReader(new InputStreamReader(System.in));
        Iterator<Object> keys = layout.assigned.iterator();
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
                row = Integer.valueOf(layout.inputBuffer.readLine());
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
                column = Integer.valueOf(layout.inputBuffer.readLine());
                if (column.intValue() < 0) {
                    System.err.println("Negative Value: Try again.");
                    continue;
                }
                if (column.intValue() > (RawBackend.rawColumns -1)) {
                    System.err.println("Value Too Large: Try again.");
                    continue;
                }
                //check if this tile has been already layout.assigned
                Iterator<Coordinate> it = layout.SIRassignment.values().iterator();
                boolean alreadyAssigned = false;
                while(it.hasNext()) {
                    Coordinate current = it.next();
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
            //do not map layout.identities, but add them to the layout.identities set
            if (node.contents instanceof SIRIdentity) {
                layout.identities.add(node);
                return;
            }
            layout.assigned.add(node);
            return;
        }
        if (node.contents instanceof SIRJoiner) {
            //we have a null joiner so just return
            if (node.getEdges().length == 0)
                return;
            //now see if we need this joiner
            if (node.getEdges()[0] == null || !(node.getEdges()[0].contents instanceof SIRJoiner)) {
                //do not assign the joiner if JoinerRemoval wants it removed...
                if (JoinerRemoval.unnecessary != null && 
                    JoinerRemoval.unnecessary.contains(node))
                    return;
                //do not assign the joiner if it is a null joiner
                for (int i = 0; i < node.inputs;i++) {
                    if (node.incomingWeights[i] != 0) {
                        layout.joiners.add(node);
                        layout.assigned.add(node);
                        return;
                    }
                } 
            }
        }
    }


   
}

