package at.dms.kjc.spacetime;

import java.util.*;
import java.io.FileWriter;
import at.dms.kjc.sir.*;
import at.dms.util.Utils;
import at.dms.kjc.flatgraph2.*;
import at.dms.kjc.sir.linear.LinearAnalyzer;
import at.dms.kjc.sir.lowering.partition.*;

/**
 * An abstract class that a slice parititoner will subclass from. It holds the
 * partitioned stream graph.
 * 
 * @author mgordon
 * 
 */
public abstract class Partitioner {
    // trace->bottleNeck work estimation
    protected HashMap traceBNWork;

    // the completed trace graph
    protected Trace[] traceGraph;

    protected RawChip rawChip;

    protected UnflatFilter[] topFilters;

    protected HashMap[] exeCounts;

    protected LinearAnalyzer lfa;

    // sirfilter -> work estimation
    protected WorkEstimate work;

    protected Trace[] topTraces;

    public Trace[] io;

    // filtercontent -> work estimation
    protected HashMap workEstimation;

    public Partitioner(UnflatFilter[] topFilters, HashMap[] exeCounts,
                       LinearAnalyzer lfa, WorkEstimate work, RawChip rawChip) {
        this.rawChip = rawChip;
        this.topFilters = topFilters;
        this.exeCounts = exeCounts;
        this.lfa = lfa;
        this.work = work;
        topTraces = new Trace[topFilters.length];
        traceBNWork = new HashMap();
    }

    /**
     * Partition the stream graph into slices (traces) and return the traces.
     * @return The slices (traces) of the partitioned graph. 
     */
    public abstract Trace[] partition();

    /**
     * @param trace
     * @return Return true if this trace is an IO trace (file reader/writer).
     */
    public boolean isIO(Trace trace) {
        for (int i = 0; i < io.length; i++) {
            if (trace == io[i])
                return true;
        }
        return false;
    }

    /**
     * @return All the traces of the trace graph. 
     */
    public Trace[] getTraceGraph() {
        assert traceGraph != null;
        return traceGraph;
    }

    /**
     * @param node The Filter 
     * @return The work estimation for the filter trace node.
     */
    public int getFilterWork(FilterTraceNode node) {
        return ((Integer) workEstimation.get(node.getFilter())).intValue();
    }

    /**
     * @param trace
     * @return The work estimation for the trace (the estimation for the filter that does the
     * most work.
     */
    public int getTraceBNWork(Trace trace) {
        assert traceBNWork.containsKey(trace);
        return ((Integer) traceBNWork.get(trace)).intValue();
    }
}
