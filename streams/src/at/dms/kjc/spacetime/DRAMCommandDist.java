/**
 * 
 */
package at.dms.kjc.spacetime;

import java.util.Iterator;

/**
 * Determine the number of read and write commands that each 
 * dram has issued to it for a given schedule of slices.
 * 
 * @author mgordon
 *
 */
public class DRAMCommandDist {
    private Trace[] schedule;
    private RawChip rawChip;
    
    /** the number of reads for <schedule> to each dram while
     * executing within a trace
     */
    private int[] intraReads;
    /** the number of writes for <schedule> to each dram while
     * executing within a trace
     */
    private int[] intraWrites;
    /** the number of reads for <schedule> to each dram while
     * executing splitting and joining
     */
    private int[] interReads;
    /** the number of writes for <schedule> to each dram while
     * executing splitting and joining
     */
    private int[] interWrites;
    public DRAMCommandDist(Trace[] schedule, RawChip rawChip) {
        this.schedule = schedule;
        this.rawChip = rawChip;
        intraReads = new int[rawChip.getNumDev()];
        intraWrites = new int[rawChip.getNumDev()];
        interReads = new int[rawChip.getNumDev()];
        interWrites = new int[rawChip.getNumDev()];
    }
    
    /**
     * @param dram
     * @return The number of writes from <dram> during schedule for 
     * splitting and joining
     */
    public int getInterWrites(StreamingDram dram) {
        return interWrites[dram.port];
    }
    
    /**
     * @param dram
     * @return The number of reads from <dram> during schedule for
     * splitting and joining
     */
    public int getInterReads(StreamingDram dram) {
        return interReads[dram.port];
    }
    /**
     * @param dram
     * @return The number of writes from <dram> during schedule within
     * a trace (intra trace buffers).
     */
    
    public int getIntraWrites(StreamingDram dram) {
        return intraWrites[dram.port];
    }
    
    /**
     * @param dram
     * @return The number of reads from <dram> during schedule within
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
                interWrites[trace.getSrcIntraBuf().getDRAM().port]++;
            }
            if (!OffChipBuffer.unnecessary(trace.getTail())) {
                //if we have an outputtracenode that splits, count its read
                //and all of its writes
                interReads[trace.getDstIntraBuf().getDRAM().port]++;
                Iterator dsts = trace.getTail().getDestSet().iterator();
                while (dsts.hasNext()) {
                    Edge edge = (Edge)dsts.next();
                    OffChipBuffer buf = InterTraceBuffer.getBuffer(edge);
                    interWrites[buf.getDRAM().port]++;
                }
            }
            //now account for the read of the intra-trace-buffer 
            intraReads[trace.getSrcIntraBuf().getDRAM().port]++;
            //now account for the write of the intra-trace-buffer
            intraWrites[trace.getDstIntraBuf().getDRAM().port]++;
        }
    }
    
    public void printDramCommands() {
        for (int i = 0; i < intraReads.length; i++) {
            System.out.println("intra DRAM " + i + ": " + 
                    intraReads[i] + " reads " + intraWrites[i] + " writes.");
            System.out.println("inter DRAM " + i + ": " + 
                    interReads[i] + " reads " + interWrites[i] + " writes.");
        }
    }
}
