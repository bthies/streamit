package at.dms.kjc.spacedynamic;

import at.dms.kjc.flatgraph.FlatNode;
import at.dms.kjc.*;
import at.dms.kjc.sir.*;
import at.dms.kjc.sir.lowering.*;
import at.dms.util.Utils;
import java.util.HashSet;
import java.util.Set;
import java.util.LinkedList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Arrays;
import java.util.StringTokenizer;
import java.io.*;

//import sun.java2d.pipe.LoopPipe;

/**
 * This class generates the switch code for each tile and writes it to a file
 */

public class SwitchCode extends at.dms.util.Utils {
    /**
     * if there are few overlapping routes, we can try to generate more compact
     * switch code, if this var is true, then try to produce compact switch
     * code, ignoring the output of the communication scheduler.
     */
    private static final boolean OPT_FOR_SIMPLE_LAYOUT = false;
    
    /** HashSet to hold RawTile for which we generated simple switch code 
        (disregarding output of communication simulator */ 
    public static HashSet switchCodeSimple;
    
    /** if we find a sequence of anything greater we will loop it if possible **/
    public static final int LOOP_IF_BIGGER = 5; 
    
    /** use heavy-weight compression algorithm */
    private static final boolean USE_BETTER_COMP = true;

    /**
     * the max-ahead is the maximum number of lines that this will recognize as
     * a pattern for folding into a loop
     */
    private static final int MAX_LOOKAHEAD = 10000;

    /**
     * the maximum number of repetitions allowed for a switch sequence this is
     * 2^16 because that is the largest immediate allowed
     */
    private static final int MAX_IMM = 65535;

    /**
     * the name of the function that sends the loop trip counts to the switch
     * for switch code compression in the steady state
     */
    public static final String SW_SS_TRIPS = "raw_init2";

    private static SpdStreamGraph streamGraph;

    private static RawChip rawChip;

    private static Layout layout;

    public static void generate(final SpdStreamGraph sg) {
        streamGraph = sg;
        rawChip = sg.getRawChip();
        layout = streamGraph.getLayout();

        switchCodeSimple = new HashSet();
        
        
        // create the joiner schedules before simulation
        sg.joinerSimulator = new JoinerSimulator(streamGraph);
        sg.joinerSimulator.createJoinerSchedules();

        
        /*
         * StaticStreamGraph current = streamGraph.getTopLevel();
         * 
         * while (current != null) {
         * current.scheduleCommunication(sg.joinerSimulator); current =
         * current.getNext(); }
         */

        ((SpdStaticStreamGraph)streamGraph.getTopLevel()).accept(new StreamGraphVisitor() {
                public void visitStaticStreamGraph(SpdStaticStreamGraph ssg) {
                    ssg.scheduleCommunication(sg.joinerSimulator);
                }

            }, null, true);
        
        // try to generate compact switch code if possible for this node
        // the simple simulator does not generate switch code for filters?
        /*if (streamGraph.isSimple()) {
            simpleSchedules();
        } else {
            //not a simple graph with a simple layout, so generate the
            //switch code from the simulator
            dumpSchedules();
        }
        */
        dumpSchedules();
    }

    /**
     * Generate the switch code for a graph that has a simple (non-overlapping)
     * layout.  We generate the switch code for filters here, SimpleScheduler
     * does nothing for filter, only for joiners. 
     *
     */
    private static void simpleSchedules() {
        for (int i = 0; i < streamGraph.getStaticSubGraphs().length; i++) {
            SpdStaticStreamGraph ssg = (SpdStaticStreamGraph)streamGraph.getStaticSubGraphs()[i];
            
            Iterator nodes = ssg.getFlatNodes().iterator();
            while (nodes.hasNext()) {
                FlatNode node = (FlatNode)nodes.next();
                if (layout.isAssigned(node))    
                    simpleSwitchCode(ssg, layout.getTile(node));
            }
        }
    }
    
    public static void dumpSchedules() {
        // this is a hash set that keeps tiles that we have already generated
        // code for
        HashSet<RawTile> tilesGenerated = new HashSet<RawTile>();

        for (int i = 0; i < (streamGraph.getStaticSubGraphs()).length; i++) {
            // get all the nodes that have either init switch code
            // or steady state switch code
            HashSet<Object> computeNodes = new HashSet<Object>();

            // SpaceDynamicBackend.addAll(computeNodes, layout.getTiles());

            SpdStaticStreamGraph ssg = (SpdStaticStreamGraph)streamGraph.getStaticSubGraphs()[i];
            SpaceDynamicBackend.addAll(computeNodes,
                                       ssg.simulator.initSchedules.keySet());
            SpaceDynamicBackend.addAll(computeNodes,
                                       ssg.simulator.steadySchedules.keySet());

            // now add any tiles that don't perform any switching and were
            // assigned filters
        
            Iterator flatNodes = ssg.getFlatNodes().iterator();
            while (flatNodes.hasNext()) {
                FlatNode node = (FlatNode) flatNodes.next();
                if (node.isFilter() && Layout.assignToATile(node))
                    computeNodes.add(layout.getTile(node));
            }

            Iterator<Object> tileIterator = computeNodes.iterator();

            // for each tiles dump the code
            while (tileIterator.hasNext()) {
                System.gc();
                ComputeNode cn = (ComputeNode) tileIterator.next();
                //System.out.println(cn);
                assert cn != null;
                // ignore ioports because we do not generate switch code for
                // them
                if (!cn.isTile())
                    continue;
                RawTile tile = (RawTile) cn;

                //get the node that is assigned to this tile, it might be null...
                FlatNode node = layout.getNode(tile);
                                
                assert !tilesGenerated.contains(tile) : "Trying to generated switch code for a tile that has already been seen";

                tilesGenerated.add(tile);

                //System.out.println("Generating Switch for tile "
                //                   + tile.getTileNumber() + "...");

                try {
                    // true if we are compressing the switch code
                    boolean compression = false;

                    // get the code
                    String steadyCode = "";
                    String initCode = "";
                    //System.out.println("Converting switch code to string ...");
                    if (ssg.simulator.initSchedules.get(tile) != null)
                        initCode = ssg.simulator.initSchedules
                                    .get(tile).toString();
                    System.gc();
                    if (ssg.simulator.steadySchedules.get(tile) != null)
                        steadyCode = ssg.simulator.steadySchedules
                                      .get(tile).toString();

                    // the sequences we are going to compress if compression is
                    // needed
                    Repetition[] big3init = null;
                    Repetition[] big3work = null;

                    //System.out.println("Compressing? ...");
                    int codeSize = getCodeLength(steadyCode);
                    codeSize += getCodeLength(initCode);
                    System.gc();
                    if (codeSize > 5000) {
                        //System.out.println("Compression needed.  Code size = "
                        //                   + codeSize);
                        compression = true;
                        if (USE_BETTER_COMP) {
                            // use the more heavyweight compression algorithm...
                            big3init = threeBiggestRepetitions(initCode);
                            big3work = threeBiggestRepetitions(steadyCode);
                        } else {
                            big3init = threeBiggestOneReps(initCode);
                            big3work = threeBiggestOneReps(steadyCode);
                        }
                    }

                    FileWriter fw = new FileWriter("sw" + tile.getTileNumber()
                                                   + ".s");
                    fw.write("#  Switch code\n");
                    fw.write(getHeader());
                    // if this tile is the north neighbor of a bc file i/o
                    // device
                    // we need to send a data word to it
                    printIOStartUp(tile, fw);
                    // print the code to get the repetition counts from the
                    // processor
                    // print the init switch code
                    if (big3init != null)
                        getRepetitionCounts(big3init, fw);
                    toASM(initCode, "i", big3init, fw);
                    // loop label
                    if (big3work != null)
                        getRepetitionCounts(big3work, fw);
                    fw.write("sw_loop:\n");
                    // print the steady state switch code
                    if (ssg.simulator.steadySchedules.get(tile) != null)
                        toASM(steadyCode, "w", big3work, fw);
                    // print the jump ins
                    fw.write("\tj\tsw_loop\n\n");
                    fw.write(getTrailer(tile, big3init, big3work));
                    fw.close();
                    /*
                     * if (threeBiggest != null) { System.out.print("Found
                     * Seqeunces of: " + threeBiggest[0].repetitions + " " + t" " +
                     * threeBiggest[1].repetitions + " " +
                     * threeBiggest[1].length + " " +
                     * threeBiggest[2].repetitions + " " +
                     * threeBiggest[2].length + "\n"); }
                     */

                    //System.out.println("sw" + tile.getTileNumber()
                    //                   + ".s written");
                } catch (Exception e) {
                    e.printStackTrace();

                    Utils.fail("Error creating switch code file for tile "
                               + tile.getTileNumber());
                }
            }
        }
    }

    // this tile is the neightbor of a bc file reader device, we must send a
    // dummy value to start the streaming of the file
    private static void printIOStartUp(RawTile tile, FileWriter fw)
        throws Exception {
        if (streamGraph.getFileState().isConnectedToFileReader(tile)) {
            LinkedList<FileReaderDevice> frs = tile.getAttachedFileReaders();
            for (int i = 0; i < frs.size(); i++) {
                fw.write("\tnop\troute $csto->$c"
                        + rawChip.getDirection(tile, frs.get(i).getPort()) + "o\n");
            }
        }
    }

    // receives the constants from the tile processor
    private static void getRepetitionCounts(Repetition[] compressMe,
                                            FileWriter fw) throws Exception {
        if (compressMe != null) {
            // print the code to get the immediates from the
            for (int i = 0; i < compressMe.length; i++) {
                fw.write("\tmove $" + i + ", $csto\n");
            }
        }
    }

    // print the assemble for the tile routing instructions. if init is true
    // then we are printing the init schedule and if we have repetitions
    // we must get the constants from the processor
    private static void toASM(String ins, String side, Repetition[] compressMe,
                              FileWriter fw) throws Exception {
        int seq = 0;
        StringTokenizer t = new StringTokenizer(ins, "\n");
        int instrCount = t.countTokens();
        // a count of the instructions we have produced (including loop trip
        // counts)
        int instrDumped = 0;

        if (compressMe == null) {
            // no compression --- just dump the routes with nops as the
            // instructions
            while (t.hasMoreTokens()) {
                instrDumped++;
                fw.write("\tnop\t" + t.nextToken() + "\n");
            }
        } else {
            // print the switch instructions compressing according to compressMe
            int counter = 0;
            String current;
            while (t.hasMoreTokens()) {
                // get the next route instruction
                current = t.nextToken();
                counter++;
                int repetitions = 1;
                // the size of the repetition, if one starts at this counter
                // (line num)
                int repSize = 1;
                for (int i = 0; i < compressMe.length; i++) {
                    if (compressMe[i].hasLine(counter)) {
                        repetitions = compressMe[i].repetitions;
                        assert repetitions > 1 : "Invalid repetition size for switch code compression.";
                        repSize = compressMe[i].getSize(counter);
                        fw.write("\tmove $3, $" + i + "\n");
                        fw.write("seq_start" + side + seq + ":\n");
                        // fw.write("\tnop\t" + current + "\n");
                        // fw.write("\tbnezd $3, $3, seq_start" + i + "\n");
                        if (repSize == 0) {
                            fw.write("\tbnezd $3, $3, seq_start" + side + seq
                                     + "\t" + current + "\n");
                        } else {
                            for (int j = 0; j < repSize - 1; j++) {
                                fw.write("\tnop\t" + current + "\n");
                                current = t.nextToken();
                                counter++;
                            }
                            fw.write("\tbnezd $3, $3, seq_start" + side + seq
                                     + "\t" + current + "\n");
                        }

                        seq++;
                        break;
                    }
                }
                if (repetitions == 1) {
                    instrDumped++;
                    fw.write("\tnop\t" + current + "\n");
                } else { // there was a loop found starting a this line...
                    for (int i = 0; i < (repetitions - 1) * repSize; i++) {
                        // skip over remainders
                        t.nextToken();
                        counter++;
                    }
                    instrDumped += (repetitions * repSize);
                }
            }
        }
        assert instrCount == instrDumped : "Error in Switch Compression! ("
            + instrCount + " != " + instrDumped + ")";
    }

    private static int getCodeLength(String str) {
        StringTokenizer t = new StringTokenizer(str, "\n");
        return t.countTokens();
    }

    private static Repetition[] threeBiggestOneReps(String str) {
        StringTokenizer t = new StringTokenizer(str, "\n");
        Repetition[] threeBiggest = new Repetition[3];

        // force the repetition count to be > 4 because there needs to be 3
        // instructions for a compressed loop...
        for (int i = 0; i < 3; i++)
            threeBiggest[i] = new Repetition(4);

        String last = "";
        // the current number of repetitions for the sequence
        int repetitions = 1;
        // the total lines seen so far
        int counter = 0;
        // the starting line of the sequence we are currently at
        int line = 1;
        String current;

        while (t.hasMoreTokens()) {
            // get the next route instruction
            current = t.nextToken();
            counter++;
            if (last.equals(current) && repetitions < MAX_IMM) {
                repetitions++;
            } else {
                // see if the repetition count is larger than any of the
                // previous
                addToThreeBiggest(threeBiggest, line, repetitions, 1);
                repetitions = 1;
                last = current;
                line = counter;
            }
        }
        // see if the repetition count is larger for the last sequence
        addToThreeBiggest(threeBiggest, line, repetitions, 1);
        return threeBiggest;
    }

    private static void addToThreeBiggest(Repetition[] threeBiggest, int line,
                                          int repetitions, int size) {
        for (int i = 0; i < 3; i++) {
            if (repetitions == threeBiggest[i].repetitions) {
                threeBiggest[i].addLoop(line, size);
                break;
            }
            if (repetitions > threeBiggest[i].repetitions) {
                // shuffle the remainder down:
                for (int j = threeBiggest.length - 1; j > i; j--)
                    threeBiggest[j] = threeBiggest[j - 1];
                // add the new one:
                threeBiggest[i] = new Repetition(repetitions);
                threeBiggest[i].addLoop(line, size);
                break;
            }
        }
    }

    private static String[] getStringArray(StringTokenizer st) {
        String[] ret = new String[st.countTokens()];
        for (int i = 0; i < ret.length; i++)
            ret[i] = st.nextToken();
        return ret;
    }

    /**
     * finds the loops in the switch code and records them in the Repetition
     * array. it finds the 3 biggest trip counts it can generate, and records
     * all loops of that trip count in the source. Only 3 because there are only
     * 3 switch regs (well, there are 4 but we use one for scratch).
     */
    private static Repetition[] threeBiggestRepetitions(String str) {
        String[] nodes = getStringArray(new StringTokenizer(str, "\n"));
        System.out.println("Size of switch instruction array: " + nodes.length);

        Repetition[] threeBiggest = new Repetition[3]; // force the repetition
        // count to be > 1
        for (int i = 0; i < 3; i++)
            threeBiggest[i] = new Repetition(4);
        // pos is our location in <pre>nodes</pre>
        int pos = 0;
        // keep going 'til we've printed all the nodes
        while (pos < nodes.length) {
            // ahead is our repetition-looking device
            int ahead = 1;
            do {
                while (ahead <= MAX_LOOKAHEAD && pos + ahead < nodes.length
                       && !nodes[pos].equals(nodes[pos + ahead])) {
                    ahead++;
                }
                // if we found a match, try to build on it. <pre>reps</pre> denotes
                // how many iterations of a loop we have.
                int reps = 0;
                if (ahead <= MAX_LOOKAHEAD && pos + ahead < nodes.length
                    && nodes[pos].equals(nodes[pos + ahead])) {
                    // see how many repetitions of the loop we can make...
                    do {
                        int i;
                        for (i = pos + reps * ahead; i < pos + (reps + 1)
                                 * ahead; i++) {
                            // quit if we reach the end of the array
                            if (i + ahead >= nodes.length) {
                                break;
                            } // quit if there's something non-matching
                            if (!nodes[i].equals(nodes[i + ahead])) {
                                break;
                            }
                        }
                        // if we finished loop, increment <pre>reps</pre>; otherwise
                        // break
                        if (i == pos + (reps + 1) * ahead) {
                            reps++;
                        } else {
                            break;
                        }
                    } while (true);
                }
                // if reps is <= 1, it's not worth the loop, so just
                // add the statement (or keep looking for loops) and
                // continue
                if (reps <= 1) {
                    // if we've't exhausted the possibility of finding
                    // loops, then make a single statement
                    if (ahead >= MAX_LOOKAHEAD) {
                        pos++;
                    }
                } else {
                    // we need to add one to reps because it counts the number
                    // of repetitions
                    // of the sequence, for the subsequent calculation we need
                    // it to count the first
                    // occurance also
                    reps++;

                    // see if the repetition count is larger for the last
                    // sequence
                    // we need to add the 1 to the position because we use
                    // tokens everywhere else
                    // and the first token is at position one...
                    addToThreeBiggest(threeBiggest, pos + 1, reps, ahead);
                    // increment the position
                    pos += reps * ahead;
                    // quit looking for loops
                    break;
                }
                // increment ahead so that we have a chance the next time
                // through
                ahead++;
            } while (ahead <= MAX_LOOKAHEAD);
        }

        return threeBiggest;
    }
    
    private static String getHeader() {
        StringBuffer buf = new StringBuffer();

        buf.append("#include \"module_test.h\"\n\n");
        buf.append(".swtext\n");
        buf.append(".global sw_begin\n");
        buf.append(".global raw_init\n");
        buf.append(".global raw_init2\n\n");
        buf.append("sw_begin:\n");

        return buf.toString();
    }

    private static String sendTripCountToSwitch(int tc) {
        if (tc <= MAX_IMM) {
            return ("\tori! $0, $0, " + (tc - 1) + "\n");
        } else {
            // if the trip count is greater than 16 bits, we must use
            // li to load it, then send it over to the switch
            String ret = "\tli $8, " + (tc - 1) + "\n";
            ret = ret + "\tor! $0, $0, $8\n";
            return ret;
        }
    }

    private static String getTrailer(RawTile tile, Repetition[] compressInit,
                                     Repetition[] compressWork) {
        StringBuffer buf = new StringBuffer();

        buf.append(".text\n\n");
        buf.append("raw_init:\n");
        // buf.append("\tmtsri SW_PC, %lo(sw_begin)\n");
        buf.append("\tla $3, sw_begin\n");
        buf.append("\tmtsr SW_PC, $3\n");
        buf.append("\tmtsri SW_FREEZE, 0\n");
        if (streamGraph.getFileState().isConnectedToFileReader(tile))
            buf.append("\tori! $0, $0, 1\n");

        if (compressInit != null) {
            for (int i = 0; i < compressInit.length; i++) {
                // System.out.println("line: " + compressMe[i].line + " reps: "
                // + compressMe[i].repetitions);

                // need to subtract 1 because we are adding the route
                // instruction to the
                // branch and it will execute regardless of whether we branch
                // buf.append("\tori! $0, $0, " + (compressInit[i].repetitions -
                // 1) + "\n");
                buf.append(sendTripCountToSwitch(compressInit[i].repetitions));
            }
        }
        buf.append("\tjr $31\n");

        buf.append(SW_SS_TRIPS + ":\n");
        if (compressWork != null) {
            for (int i = 0; i < compressWork.length; i++) {

                // need to subtract 1 because we are adding the route
                // instruction to the
                // branch and it will execute regardless of whether we branch
                // buf.append("\tori! $0, $0, " + (compressWork[i].repetitions -
                // 1) + "\n");
                buf.append(sendTripCountToSwitch(compressWork[i].repetitions));
            }
        }
        buf.append("\tjr $31\n");

        return buf.toString();
    }

    // class used to encapsulate a sequence: the starting line, the
    // repetition count, and the size of the repetition
    static class Repetition {
        //
        public HashMap<Integer, Integer> lineToSize;

        public int repetitions;

        public Repetition(int r) {
            lineToSize = new HashMap<Integer, Integer>();
            // assert r < SwitchCode.MAX_IMM : "Trying to create a switch loop
            // larger than immediate size";
            repetitions = r;
        }

        public void addLoop(int line, int size) {
            lineToSize.put(new Integer(line), new Integer(size));
        }

        public int getSize(int l) {
            return lineToSize.get(new Integer(l)).intValue();
        }

        public boolean hasLine(int l) {
            return lineToSize.containsKey(new Integer(l));
        }

        public String toString() {
            String ret = "reps: " + repetitions;
            Iterator<Integer> it = lineToSize.keySet().iterator();
            while (it.hasNext()) {
                Integer line = it.next();
                ret = ret + "(" + line.toString() + ", " + lineToSize.get(line)
                    + ")";

            }

            return ret;
        }

    }
    
    /**
     * Try to generate compact switch code for a tile if it is connected to 
     * its inputs and outputs with no overlapping routes.
     *   
     * @param tile The tile to generate code for.
     * @return true if we generated code for this tile.  If false, we have 
     * to generate code the standard way.
     */
    private static boolean simpleSwitchCode(SpdStaticStreamGraph ssg, RawTile tile) {
        FlatNode node = layout.getNode(tile);
        if (node == null)
            return false;
        
        //joiner's will not work except in for a very restricted split join, so just 
        //forget about it
        if (node.isJoiner())
            return false;
        
        //make sure this node does not have any other static routes running through it
        assert !layout.isIntermediateTile(tile);
                    
        //only handle joiners that are neighbors of all their inputs
        if (node.isJoiner()) {
            if (node.inputs > 3)
                return false;
            for (int i = 0; i < node.inputs; i++ ) {
                if (!rawChip.areNeighbors(layout.getTile(node), layout.getTile(node.incoming[i])))
                    return false;
            }
        }
        
        //for now only handle joiners whose output is not split
        if (node.isJoiner() && node.ways > 0 && node.getEdges()[0].isSplitter()) 
            return false;
        
        //only handle filters whose output is split once
        if (node.isFilter() && node.ways > 0 && node.getEdges()[0].isSplitter()) {
            for (int j = 0; j < node.getEdges()[0].ways; j++) {
                if (node.getEdges()[0].getEdges()[j].isSplitter())
                    assert false : 
                        "Case of filter split more than once not support by Simple Switch Code.";
            }
        }
        
        try {
            FileWriter fw = new FileWriter("sw" + tile.getTileNumber()
                                           + ".s");
            fw.write("#  Switch code\n");
            fw.write(getHeader());
            // if this tile is the north neighbor of a bc file i/o
            // device
            // we need to send a data word to it
            printIOStartUp(tile, fw);
        
            //now generate the compact switch code for the node
            if (node.isJoiner()) {
                createSimpleJoinerCode(ssg, fw, tile, node);
            }
            else {
                //now generate code for a filter
                createSimpleFilterCode(ssg, fw, tile, node);
            }
            fw.close();

        }
        catch (Exception e) {
            System.out.println("Error creating Simple Switch Code!");
            e.printStackTrace();
            System.exit(1);
        }
        
        return true;
    }

    /**
     * Create the trailer for the switch code that includes sending the loop trip counts
     * to the switch from the processor.  (So we are defining function that are called on the
     * compute processor but we are placing them in the "switch code").
     *  
     * @param fw
     * @param node
     * @param rec
     * @throws Exception
     */
    private static void simpleTrailer(SpdStaticStreamGraph ssg, FileWriter fw, FlatNode node, RawExecutionCode rec) 
        throws Exception {
        fw.write(".text\n\n");
        fw.write("raw_init:\n");
        // buf.append("\tmtsri SW_PC, %lo(sw_begin)\n");
        fw.write("\tla $3, sw_begin\n");
        fw.write("\tmtsr SW_PC, $3\n");
        fw.write("\tmtsri SW_FREEZE, 0\n");
        if (streamGraph.getFileState().isConnectedToFileReader(layout.getTile(node)))
            fw.write("\tori! $0, $0, 1\t# connected to file reader\n");

        //now send the init exe count and remaining count for the init stage
        if (node.isFilter()) {
            if (rec.getInitFire() - 2 > 0) 
                fw.write(sendTripCountToSwitch(rec.getInitFire() - 2));
            if (rec.getRemaining() > 0)
                fw.write(sendTripCountToSwitch(rec.getRemaining()));
        }

        fw.write("\tjr $31\n");

        fw.write(SW_SS_TRIPS + ":\n");
        if (node.isFilter()) {
            //for the steady state, just send the ss multiplicity
            if (node.getFilter().getPopInt() > LOOP_IF_BIGGER) 
                fw.write(sendTripCountToSwitch(node.getFilter().getPopInt()));
            if (node.getFilter().getPushInt() > LOOP_IF_BIGGER)
                fw.write(sendTripCountToSwitch(node.getFilter().getPushInt()));
        }
        
        fw.write("\tjr $31\n");
    }
    
    /**
     * Create compact switch code for a filter with no routes crossing through
     * it. Create the init switch code, then loop one execution of the filter.
     * 
     * @param ssg
     * @param fw
     * @param tile
     * @param node
     * @throws Exception
     */
    private static void createSimpleFilterCode(SpdStaticStreamGraph ssg,
                                               FileWriter fw, RawTile tile, FlatNode node) throws Exception {
        assert node.isFilter();
        int pop = node.getFilter().getPopInt();
        int push = node.getFilter().getPushInt();

        // use the raw execution code class to calucate stats of the filter
        RawExecutionCode rec = new RawExecutionCode(ssg);
        rec.calculateItems(node.getFilter());

        
        // get the source direction...
        FlatNode source = null;
        LinkedList<ComputeNode> route = null;
        String dir = null;
        //if the actually pop's then set the direction             
        if (pop > 0) {
            source = Util.getFilterUpstreamAssigned(layout, node);
            route = layout.router.getRoute(ssg, layout
                              .getComputeNode(source), tile);
            dir = rawChip.getDirection(tile, route.get(route.size() - 2));
            
        }
        // get the destination schedule
        CircularSchedule destSched = new CircularSchedule(Util
                                                          .getSendingSchedule(layout, node));

        // receive the the loop trip counts into the switch regs
        // $0 = initfire - 2 (one execution if for initWork, the other is taken
        // care of by bottom peek
        // $1 = remaining
        if (rec.getInitFire() - 2  > 0) 
            fw.write("\tmove $0, $csto\n");
        if (rec.getRemaining() > 0)    
            fw.write("\tmove $1, $csto\n");

        // init
        // if this is a two stage filter, receive init peek items
        if (node.getFilter() instanceof SIRTwoStageFilter) {
            for (int i = 0; i < ((SIRTwoStageFilter) node.getFilter())
                     .getInitPeekInt(); i++) {
                fw.write("\tnop\troute $c" + dir + "i->$csti\n");
            }
            // now the push rate of the pre work function
            for (int i = 0; i < ((SIRTwoStageFilter) node.getFilter())
                     .getInitPushInt(); i++) {
                String destDir = getCurrentDestDir(ssg, destSched, tile);
                fw.write("\tnop\troute $csto->$c" + destDir + "o\n");
            }
        }

        if (rec.getInitFire() - 1 > 0) {
            // now receive the bottom peek elements
            for (int i = 0; i < rec.getBottomPeek(); i++) {
                fw.write("\tnop\troute $c" + dir + "i->$csti\n");
            }
            
            // write the items from the first execution of work in the init!
            for (int i = 0; i < node.getFilter().getPushInt(); i++) {
                String destDir = getCurrentDestDir(ssg, destSched, tile);
                fw.write("\tnop\troute $csto->$c" + destDir + "o\n");
            }
            
            //now generate the sends/receives for the rest of work() in init
            if (rec.getInitFire() - 2 > 0) {
                fw.write("\tmove $3, $0\n");
                fw.write("seq_init:\n");
                // reasd pop items for firing
                for (int j = 0; j < node.getFilter().getPopInt(); j++) {
                    fw.write("\tnop\troute $c" + dir + "i->$csti\n");
                }
                // write push items from work function firing
                for (int x = 0; x < node.getFilter().getPushInt(); x++) {
                    String destDir = getCurrentDestDir(ssg, destSched, tile);
                    fw.write("\tnop\troute $csto->$c" + destDir + "o\n");
                }
                fw.write("\tbnezd $3, $3, seq_init\n");
            }
        }

        // receive remaining items the are produced upstream but not consumed by
        // me!
        if (rec.getRemaining() > 0) {
            fw.write("\tmove $3, $1\n");
            fw.write("seq_remaining:\n");
            fw.write("\tbnezd $3, $3, seq_remaining" + "\troute $c" + dir
                     + "i->$csti\n");
        }

        // receive the the loop trip counts into the switch regs
        // $1 = pop rate
        // $2 = push rate
        if (pop > LOOP_IF_BIGGER)
            fw.write("\tmove $0, $csto\n");
        if (push > LOOP_IF_BIGGER)
            fw.write("\tmove $1, $csto\n");

        // steady state
        fw.write("sw_steady:\n");
        
        // create the receives for (pop count), filter has only one input
        if (pop > LOOP_IF_BIGGER) {
            // generate the receive instructions
            fw.write("\tmove $3, $0\n");
            fw.write("seq_pop_steady:\n");
            fw.write("\tbnezd $3, $3, seq_pop_steady" + "\troute $c" + dir
                     + "i->$csti\n");
        }
        else { //don't loop, not worth it!
            for (int i = 0; i < pop; i++)
                fw.write("\tnop\troute $c" + dir + "i->$csti\n");
        }

        //generate pushes (sends)
        if (push > 0) {
            System.out.println("DestSched Size = " + destSched.size());
            System.out.println(destSched.toString());
            if (destSched.size() == 1 && push > LOOP_IF_BIGGER) {
                // there is only one destination for the push of this filter,
                // so we can loop the push statements because they all go to the
                // same dest...
                String destDir = getCurrentDestDir(ssg, destSched, tile);
                fw.write("\tmove $3, $1\n");
                fw.write("seq_push_steady:\n");
                fw.write("\tbnezd $3, $3, seq_push_steady\troute $csto->$c"
                         + destDir + "o\n");
            } else {
                // create the sends for push count, we cannot loop the sends
                // because they might
                // not all go in the same direction!
                for (int x = 0; x < node.getFilter().getPushInt(); x++) {
                    String destDir = getCurrentDestDir(ssg, destSched, tile);
                    fw.write("\tnop\troute $csto->$c" + destDir + "o\n");
                }
            }
        }
        assert destSched.isHead();

        fw.write("\tj\tsw_steady\n\n");

        simpleTrailer(ssg, fw, node, rec);
    }
    
    /**
     * Given the destination schedule of a filter whose output that is might be split, 
     * get the direction of the current destination for an item and return the direction
     * that the item should be routed from the tile assigned to the filter.
     * 
     * @param destSched
     * @param tile
     * @return the direction to route.
     */
    private static String getCurrentDestDir(SpdStaticStreamGraph ssg, CircularSchedule destSched, RawTile tile) {
        FlatNode curDest = destSched.next();
        
        LinkedList<ComputeNode> route = layout.router.getRoute(ssg, tile, layout.getComputeNode(curDest));
        
        return rawChip.getDirection(tile, route.get(1));
    }
    
    /**
     * Create compact switch code for a joiner with no routes crossing through it.
     * Just loop through on cycle of the joiner.
     * @param ssg 
     * @param fw
     * @param tile
     * @param node
     * @throws Exception
     */
    private static void createSimpleJoinerCode(SpdStaticStreamGraph ssg, FileWriter fw, RawTile tile, FlatNode node) 
        throws Exception {
        assert false : "Simple switch code for joiners is broken!";
        assert node.isJoiner();
        //get the destination direction for this joiner
        LinkedList<ComputeNode> destRoute = layout.router.getRoute(ssg, tile, layout.getComputeNode(node.getEdges()[0]));
        String destDir = rawChip.getDirection(tile, destRoute.get(1));
        
        //if the node is a joiner, generate code that will
        //pass on items from each input according to the 
        //weights    
        for (int i = 0; i < node.inputs; i++) {
            //get the source tiles last intermediate route
            LinkedList<ComputeNode> sourceRoute = layout.router.getRoute(ssg, layout.getComputeNode(node.incoming[i]), tile);
            ComputeNode sourceTile = sourceRoute.get(sourceRoute.size() - 2);  
            //now write the instruction to receive from the current source and send to the dest                    
            for (int j = 0; j < node.incomingWeights[i]; j++) {
                fw.write("\tnop\troute ");
                fw.write("$c" + rawChip.getDirection(tile, sourceTile) + "i->" + 
                         "$c" + destDir + "o\n");
            }
        }
        fw.write("\tj\tsw_begin\n\n");
        
        simpleTrailer(ssg, fw, node, null);
    }
    
}
