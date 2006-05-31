/**
 * 
 */
package at.dms.kjc.spacedynamic;

import at.dms.kjc.flatgraph.FlatNode;
import java.util.*;

/**
 * @author mgordon
 *
 */
public class Compression {
    /** when we are generating c code for the compressed sequence, generate for loops
     * only for sequences that are repeated more than  generate_for_loop times.
     */
    private static final int GENERATE_FOR_LOOP = 2;
    
    // the max-ahead is the maximum number of objects that this will
    // recognize as a pattern for folding into a loop
    private static final int MAX_LOOKAHEAD = 1000;
    
    private boolean didCompress;
    /** the compression level (number of nested compressions) that for the last
     * compressed sequence.
     */
    public int compressionLevel;
    
    /** the sequence to compress */
    private Object[] nodes;
    
    /** the compressed sequence, after compression is performed */
    public CompressedSequence compressed;
    
    /**
     * Create a new compression object that will compress the 
     * sequence nodes.
     * 
     * @param nodes The sequence to compress.
     */
    public Compression(Object[] nodes) {
        this.nodes = nodes;
    }
    
    /** 
     * Fully compress a sequcence. 
     * 
     * @param nodes The sequence to compress.
     */
    public void fullyCompress() {
        System.out.println("Joiner code length: " + nodes.length);
        CompressedSequence comp = compress(nodes); 
        compressionLevel = 0;
       
        //keep compressing until we cannot compress anymore!!!
        do {      
            didCompress = false;
            compressionLevel++;
            comp = compress(comp.getSequence());
            
        } while (didCompress);
        
        this.compressed = comp;
    }
    
    /**
     * Compressed the sequence of nodes. 
     * 
     * @param nodes The node schedule to compress
     * @return An array of CompressedSequence's that represent <nodes> compressed.
     */
    private CompressedSequence compress(Object[] seq) {
        //This is the compressed list that we will return compossed of CompressedSequences 
        CompressedSequence compressed = new CompressedSequence(1); 
        // System.out.println("Joiner sched size " + seq.length);
        // pos is our location in <seq>
        int pos = 0;
        // keep going 'til we've printed all the seq
        while (pos < seq.length) {
            // ahead is our repetition-looking device
            int ahead = 1;
            do {
                while (ahead <= MAX_LOOKAHEAD && pos + ahead < seq.length
                       && !seq[pos].equals(seq[pos + ahead])) {
                    //System.out.println(seq[pos] + " != " + seq[pos + ahead]);
                    ahead++;
                }
                // if we found a match, try to build on it. <reps> denotes
                // how many iterations of a loop we have.
                int reps = 0;
                if (ahead <= MAX_LOOKAHEAD && pos + ahead < seq.length
                    && seq[pos].equals(seq[pos + ahead])) {
                    // see how many repetitions of the loop we can make...
                    do {
                        int i;
                        for (i = pos + reps * ahead; i < pos + (reps + 1)
                                 * ahead; i++) {
                            // quit if we reach the end of the array
                            if (i + ahead >= seq.length) {
                                break;
                            }
                            // quit if there's something non-matching
                            if (!seq[i].equals(seq[i + ahead])) {
                                break;
                            }
                        }
                        // if we finished loop, increment <reps>; otherwise
                        // break
                        if (i == pos + (reps + 1) * ahead) {
                            reps++;
                        } else {
                            break;
                        }
                    } while (true);
                }
                //add one to reps because we now want reps to count how many repetitions of the 
                //ahead sequence we have and we at least have one...
                reps++;
                // if reps is <= 1, no loop was found                
                if (reps <= 1) {
                    // if we've't exhausted the possibility of finding
                    // loops, then make a single statement and we will pop out of the loop
                    //and try the next position with a new ahead value
                    if (ahead >= MAX_LOOKAHEAD) {
                        //create a compressed sequence to add that is only looped once,
                        //with one element in the sequence
                        CompressedSequence seq1 = new CompressedSequence(1);
                        seq1.addToSeq(seq[pos]);
                        compressed.addToSeq(seq1);
                        pos++;
                    }
                } else {
                    CompressedSequence comp = new CompressedSequence(reps);                   
                    // add the component code
                    for (int i = 0; i < ahead; i++) {
                        comp.addToSeq(seq[pos + i]);
                    }
                    compressed.addToSeq(comp);
                    //remember that some compression was performed
                    didCompress = true;
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
        //all done, return the compressed sequence array
        return compressed;
    }
    
    /** 
     * Given a compressed sequence that is composed of objects with an appropriate
     * toString() method defined, generate code (nested C for loops in an outer block) to execute the 
     * compressed schedule.  At first, generate C code for the variable declarations.  Make sure that you
     * do not compress another sequence using this Compression object, because we use the value of 
     * compressionlevel for the var declarations.
     * 
     * @param seq
     * @param varPrefix The prefix to use for loop indices (these have been declared previously).
     * @return
     */
    public String compressedCode(String varPrefix) {
        StringBuffer buf = new StringBuffer();
        
        buf.append("{\n");
        //create the var defs
        for (int i = 0; i < compressionLevel; i++) 
            buf.append("  int " + varPrefix + i + ";\n");
        //create the nested loops and the code for the schedule
        buf.append(compressedSequence(compressed, 0, varPrefix));
        buf.append("}\n");
        return buf.toString();
    }
    
    private String indent(int level) {
        StringBuffer buf = new StringBuffer();
        //print some indentation
        for (int t = 0; t <= level; t++)
            buf.append("  "); 
        return buf.toString();
    }
    
    private String elementString(Object obj, int level, String varPrefix) {
        if (obj instanceof CompressedSequence) {
            return compressedSequence((CompressedSequence)obj, level, varPrefix);
        }
        else {
            return indent(level) + obj.toString();
        }
    }
    
    /**
     * 
     * @param seq
     * @param level
     * @param varPrefix
     * @return
     */
    public String compressedSequence(CompressedSequence seq, int level, String varPrefix) {
        StringBuffer buf = new StringBuffer();
        //generate a for loop if the current repetition count is high enough
        if (seq.getReps() > GENERATE_FOR_LOOP) {
            String var = varPrefix + level;
            buf.append(indent(level));
            buf.append("for (" + var + " = 0; " + var + " < " + seq.getReps() + 
                       "; " + var + "++) {\n");
            for (int i = 0; i < seq.getSequence().length; i++) {
                buf.append(elementString(seq.getSequence()[i], level + 1, varPrefix));
            }
            buf.append("}\n");
        }
        else {
            //the rep count is low, so just unroll the loop
            for (int reps = 0; reps < seq.getReps(); reps++) {
                for (int ele = 0; ele < seq.getSequence().length; ele++) {
                    buf.append(elementString(seq.getSequence()[ele], level, varPrefix));
                }
            }
        }
        return buf.toString();
    }
    
    public String toString() {
        return compressed.toString();
    }
}
