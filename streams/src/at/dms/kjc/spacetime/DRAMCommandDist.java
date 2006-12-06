/**
 * 
 */
package at.dms.kjc.spacetime;

import java.util.Iterator;

import at.dms.kjc.common.CommonUtils;
import at.dms.kjc.slicegraph.Edge;

/**
 * Determine the number of read and write commands that each 
 * dram has issued to it for a given schedule of slices.
 * 
 * @author mgordon
 *
 */
public class DRAMCommandDist {
    /** The schedule we are executing */
    private Trace[] schedule;
    /** The raw chip we are executing on */
    private RawChip rawChip;
    /** the number of reads for <pre>schedule</pre> to each dram while
     * executing within a trace
     */
    private int[] intraReads;
    /** the number of writes for <pre>schedule</pre> to each dram while
     * executing within a trace
     */
    private int[] intraWrites;
    /** the number of reads for <pre>schedule</pre> to each dram while
     * executing splitting and joining
     */
    private int[] interReads;
    /** the number of writes for <pre>schedule</pre> to each dram while
     * executing splitting and joining
     */
    private int[] interWrites;
    
    /**
     * Create a new object that will calculate how many DRAM commands 
     * each DRAM has issued to it for schedule on rawChip.
     * 
     * @param schedule The schedule. 
     * @param rawChip The raw chip.
     */
    public DRAMCommandDist(Trace[] schedule, RawChip rawChip) {
        this.schedule = schedule;
        this.rawChip = rawChip;
        intraReads = new int[rawChip.getNumDev()];
        intraWrites = new int[rawChip.getNumDev()];
        interReads = new int[rawChip.getNumDev()];
        interWrites = new int[rawChip.getNumDev()];
    }
    
    /**
     * Return the number of writes from <pre>dram</pre> during schedule for 
     * splitting and joining.
     * 
     * @param dram
     * @return The number of writes from <pre>dram</pre> during schedule for 
     * splitting and joining
     */
    public int getInterWrites(StreamingDram dram) {
        return interWrites[dram.port];
    }
    
    /**
     * Return the number of reads from <pre>dram</pre> during schedule for
     * splitting and joining.
     * 
     * @param dram
     * 
     * @return The number of reads from <pre>dram</pre> during schedule for
     * splitting and joining
     */
    public int getInterReads(StreamingDram dram) {
        return interReads[dram.port];
    }

    /**
     * Return the number of writes from <pre>dram</pre> during schedule within
     * a trace (intra trace buffers).
     * 
     * @param dram
     * 
     * @return The number of writes from <pre>dram</pre> during schedule within
     * a trace (intra trace buffers).
     */
    public int getIntraWrites(StreamingDram dram) {
        return intraWrites[dram.port];
    }
    
    /**
     * Return the number of reads from <pre>dram</pre> during schedule within
     * a trace (intra trace buffer).
     * 
     * @param dram
     * @return The number of reads from <pre>dram</pre> during schedule within
     * a trace (intra trace buffer).
     */
    public int getIntraReads(StreamingDram dram) {
        return intraReads[dram.port];
    }
    
    public void calcDRAMDist() {
        for (int i = 0; i < schedule.length; i++) {
            Trace trace = schedule[i];
            if (!OffChipBuffer.unnecessary(trace.getHead())) {
                //if we have a inputtracebuffer that does something, count its 
                //reads and writers
                //the reads of the incoming arcs of the joiner
                for (int s = 0; s < trace.getHead().getSources().length; s++) {
                    Edge edge = trace.getHead().getSources()[s];
                    OffChipBuffer buf = InterTraceBuffer.getBuffer(edge);
                    interReads[buf.getDRAM().port]++;
                }
                //the write for the intra-trace-node
                interWrites[IntraTraceBuffer.getSrcIntraBuf(trace).getDRAM().port]++;
            }
            if (!OffChipBuffer.unnecessary(trace.getTail())) {
                //if we have an outputtracenode that splits, count its read
                //and all of its writes
                interReads[IntraTraceBuffer.getDstIntraBuf(trace).getDRAM().port]++;
                Iterator dsts = trace.getTail().getDestSet().iterator();
                while (dsts.hasNext()) {
                    Edge edge = (Edge)dsts.next();
                    OffChipBuffer buf = InterTraceBuffer.getBuffer(edge);
                    interWrites[buf.getDRAM().port]++;
                }
            }
            //now account for the read of the intra-trace-buffer 
            intraReads[IntraTraceBuffer.getSrcIntraBuf(trace).getDRAM().port]++;
            //now account for the write of the intra-trace-buffer
            intraWrites[IntraTraceBuffer.getDstIntraBuf(trace).getDRAM().port]++;
        }
    }
    
    public void printDramCommands() {
        for (int i = 0; i < intraReads.length; i++) {
            CommonUtils.println_debugging("intra DRAM " + i + ": " + 
                    intraReads[i] + " reads " + intraWrites[i] + " writes.");
            CommonUtils.println_debugging("inter DRAM " + i + ": " + 
                    interReads[i] + " reads " + interWrites[i] + " writes.");
        }
    }
}