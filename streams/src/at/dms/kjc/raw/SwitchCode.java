package at.dms.kjc.raw;

import at.dms.kjc.flatgraph.FlatNode;
import at.dms.kjc.*;
import at.dms.kjc.sir.*;
import at.dms.kjc.sir.lowering.*;
import at.dms.util.Utils;
import java.util.HashSet;
import java.util.Set;
import java.util.HashMap;
import java.util.Iterator;
import java.util.StringTokenizer;
import java.io.*;

/**
 * This class generates the switch code for each tile and writes it to a file
 */

public class SwitchCode extends at.dms.util.Utils {
    //use heavy-weight compression algorithm
    private static final boolean USE_BETTER_COMP = true;
    // the max-ahead is the maximum number of lines that this will
    // recognize as a pattern for folding into a loop
    private static final int MAX_LOOKAHEAD = 100;

    // the maximum number of repetitions allowed for a switch sequence
    // this is 2^16 because that is the largest immediate allowed
    private static final int MAX_REP = 65535;
    //the name of the function that sends the loop trip counts to the 
    //switch for switch code compression in the steady state
    public static final String  SW_SS_TRIPS = "raw_init2";

    public static void generate(FlatNode top) {
        // Use the simulator to create the switch schedules

        RawBackend.simulator.simulate(top);
        // now print the schedules
        dumpSchedules();
    }

    public static void dumpSchedules() {
        // get all the nodes that have either init switch code
        // or steady state switch code
        HashSet<Object> tiles = new HashSet<Object>(RawBackend.simulator.initSchedules.keySet());

        RawBackend.addAll(tiles, RawBackend.simulator.steadySchedules.keySet());
        RawBackend.addAll(tiles, Layout.getTiles());

        // do not generate switchcode for Tiles assigned to file readers/writers
        // they are just dummy tiles
        Iterator<Object> fs = FileVisitor.fileNodes.iterator();
        while (fs.hasNext()) {
            tiles.remove(Layout.getTile((FlatNode) fs.next()));
        }

        Iterator<Object> tileIterator = tiles.iterator();

        // for each tiles dump the code
        while (tileIterator.hasNext()) {
            Coordinate tile = (Coordinate) tileIterator.next();
            try {
                // true if we are compressing the switch code
                boolean compression = false;

                // get the code
                String steadyCode = "";
                String initCode = "";
                if (RawBackend.simulator.initSchedules.get(tile) != null)
                    initCode = RawBackend.simulator.initSchedules
                                .get(tile).toString();
                if (RawBackend.simulator.steadySchedules.get(tile) != null)
                    steadyCode = RawBackend.simulator.steadySchedules
                                  .get(tile).toString();

                // the sequences we are going to compress if compression is
                // needed
                Repetition[] big3init = null;
                Repetition[] big3work = null;

                int codeSize = getCodeLength(steadyCode + initCode);
                if (codeSize > 5000) {
                    System.out.println("Compression needed.  Code size = "
                                       + codeSize);
                    compression = true;
                    if (USE_BETTER_COMP) {
                        //use the more heavyweight compression algorithm...
                        big3init = threeBiggestRepetitions(initCode);
                        big3work = threeBiggestRepetitions(steadyCode);
                    }
                    else {
                        big3init = threeBiggestOneReps(initCode);
                        big3work = threeBiggestOneReps(steadyCode);
                    }
                }

                FileWriter fw = new FileWriter("sw"
                                               + Layout.getTileNumber(tile) + ".s");
                fw.write("#  Switch code\n");
                fw.write(getHeader());
                // if this tile is the north neighbor of a bc file i/o device
                // we need to send a data word to it
                printIOStartUp(tile, fw);
                // print the code to get the repetition counts from the
                // processor
                // print the init switch code
                if (big3init != null)
                    getRepetitionCounts(big3init, fw);
                toASM(initCode, "i", big3init, fw);

                // loop labels
                if (big3work != null) {
                    System.out.println("Compression for : " + tile);
                    for (int i = 0; i < big3work.length; i++) {
                        System.out.println(big3work[i].toString());
                    }
                    getRepetitionCounts(big3work, fw);
                }

                fw.write("sw_loop:\n");
                // print the steady state switch code
                if (RawBackend.simulator.steadySchedules.get(tile) != null)
                    toASM(steadyCode, "w", big3work, fw);
                // print the jump ins
                fw.write("\tj\tsw_loop\n\n");
                fw.write(getTrailer(tile, big3init, big3work));
                fw.close();
                /*
                 * if (threeBiggest != null) { System.out.print("Found Seqeunces
                 * of: " + threeBiggest[0].repetitions + " " + t" " +
                 * threeBiggest[1].repetitions + " " + threeBiggest[1].length + " " +
                 * threeBiggest[2].repetitions + " " + threeBiggest[2].length +
                 * "\n"); }
                 */

                System.out.println("sw" + Layout.getTileNumber(tile)
                                   + ".s written");
            } catch (Exception e) {
                e.printStackTrace();

                Utils.fail("Error creating switch code file for tile "
                           + Layout.getTileNumber(tile));
            }
        }
    }

    // this this tile is the north neightbor of a bc file i/o device, we must
    // send a
    // dummy
    private static void printIOStartUp(Coordinate tile, FileWriter fw)
        throws Exception {
        if (FileVisitor.connectedToFR(tile))
            fw.write("\tnop\troute $csto->$cEo\n");
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
        //a count of the instructions we have produced (including loop trip counts)
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
                //the size of the repetition, if one starts at this counter (line num)
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
                        }
                        else {
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
                }
                else {  //there was a loop found starting a this line...
                    for (int i = 0; i < (repetitions - 1) * repSize; i++) {
                        // skip over remainders
                        t.nextToken();
                        counter++;
                    }
                    instrDumped += (repetitions * repSize);
                }
            }
        }
        assert instrCount == instrDumped : "Error in Switch Compression! (" + instrCount + " != " + 
            instrDumped + ")";
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
            if (last.equals(current) && repetitions < MAX_REP) {
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
     * finds the loops in the switch code and records them in the 
     * Repetition array.  it finds the 3 biggest trip counts it can generate,
     * and records all loops of that trip count in the source.  Only 3 because there
     * are only 3 switch regs (well, there are 4 but we use one for scratch).
     */    
    private static Repetition[] threeBiggestRepetitions(String str) {
        String[] nodes = getStringArray(new StringTokenizer(str, "\n")); 
        System.out.println("Size of switch instruction array: " + nodes.length);
    
        Repetition[] threeBiggest = new Repetition[3]; //force the repetition count to be > 1
        for (int i = 0; i < 3; i++) 
            threeBiggest[i] = new Repetition(4);
        // pos is our location in <nodes> 
        int pos = 0; 
        // keep going 'til we've printed all the nodes 
        while (pos<nodes.length) { 
            // ahead is our repetition-looking device 
            int ahead=1; 
            do { 
                while (ahead <= MAX_LOOKAHEAD && 
                       pos+ahead < nodes.length && 
                       !nodes[pos].equals(nodes[pos+ahead])) {
                    ahead++; 
                } 
                // if we found a match, try to build on it. <reps> denotes
                //how many iterations of a loop we have. 
                int reps = 0; 
                if (ahead <= MAX_LOOKAHEAD && pos+ahead < nodes.length &&
                    nodes[pos].equals(nodes[pos+ahead])) { 
                    // see how many repetitions of the loop we can make... 
                    do { 
                        int i; 
                        for (i=pos+reps*ahead; i<pos+(reps+1)*ahead; i++) { 
                            // quit if we reach the end of the array 
                            if (i+ahead >= nodes.length) { 
                                break; 
                            } // quit if there's something non-matching 
                            if (!nodes[i].equals(nodes[i+ahead])) { 
                                break; 
                            } 
                        } 
                        // if we finished loop, increment <reps>; otherwise break 
                        if (i==pos+(reps+1)*ahead) { 
                            reps++; 
                        }
                        else { 
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
                    //we need to add one to reps because it counts the number of repetitions
                    //of the sequence, for the subsequent calculation we need it to count the first 
                    //occurance also
                    reps++;
            
                    //see if the repetition count is larger for the last sequence 
                    //we need to add the 1 to the position because we use tokens everywhere else
                    //and the first token is at position one...
                    addToThreeBiggest(threeBiggest, pos + 1, reps, ahead);
                    // increment the position 
                    pos += reps*ahead; 
                    // quit looking for loops 
                    break; 
                } 
                // increment ahead so that we have a chance the next time through 
                ahead++; 
            } while (ahead<=MAX_LOOKAHEAD); 
        }
    
        return threeBiggest;
    }
    

    private static String getHeader() {
        StringBuffer buf = new StringBuffer();

        buf.append("#include \"module_test.h\"\n\n");
        buf.append(".swtext\n");
        buf.append(".global sw_begin\n");
        buf.append(".global raw_init\n");
        buf.append(".global " + SW_SS_TRIPS + "\n\n");
        buf.append("sw_begin:\n");

        return buf.toString();
    }

    private static String getTrailer(Coordinate tile,
                                     Repetition[] compressInit, Repetition[] compressWork) {
        StringBuffer buf = new StringBuffer();

        buf.append(".text\n\n");
        buf.append("raw_init:\n");
        // buf.append("\tmtsri SW_PC, %lo(sw_begin)\n");
        buf.append("\tla $3, sw_begin\n");
        buf.append("\tmtsr SW_PC, $3\n");
        buf.append("\tmtsri SW_FREEZE, 0\n");
        if (FileVisitor.connectedToFR(tile))
            buf.append("\tori! $0, $0, 1\n");

        if (compressInit != null) {
            for (int i = 0; i < compressInit.length; i++) {
                // System.out.println("line: " + compressMe[i].line + " reps: "
                // + compressMe[i].repetitions);

                // need to subtract 1 because we are adding the route
                // instruction to the
                // branch and it will execute regardless of whether we branch
                buf.append("\tori! $0, $0, "
                           + (compressInit[i].repetitions - 1) + "\n");
            }
        }
        buf.append("\tjr $31\n");

        buf.append(SW_SS_TRIPS + ":\n");
        if (compressWork != null) {
            for (int i = 0; i < compressWork.length; i++) {

                // need to subtract 1 because we are adding the route
                // instruction to the
                // branch and it will execute regardless of whether we branch
                buf.append("\tori! $0, $0, "
                           + (compressWork[i].repetitions - 1) + "\n");
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
            repetitions = r;
        }

        public void addLoop(int line, int size) 
        {
            assert size < MAX_REP : "Trying to create a switch loop larger than immediate size";
            lineToSize.put(new Integer(line), new Integer(size));
        }
    
        public int getSize(int l) 
        {
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
                ret = ret + "(" + line.toString() + ", " +
                    lineToSize.get(line) + ")";
        
            }
        
            return ret;
        }

    }

}
