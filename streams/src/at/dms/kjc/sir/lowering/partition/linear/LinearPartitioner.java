package at.dms.kjc.sir.lowering.partition.linear;

import java.util.*;
import java.io.*;

import at.dms.kjc.*;
import at.dms.util.*;
import at.dms.kjc.iterator.*;
import at.dms.kjc.sir.*;
import at.dms.kjc.sir.linear.*;
import at.dms.kjc.sir.lowering.*;
import at.dms.kjc.sir.lowering.fusion.*;
import at.dms.kjc.sir.lowering.fission.*;
import at.dms.kjc.sir.lowering.partition.*;

public class LinearPartitioner {
    /**
     * Debugging.
     */
    public static final boolean DEBUG = false;

    /**
     * The threshold for the number of multiplies in a node before we
     * stop unrolling and generate a matrix multiply with loops.
     */
    public static final int MAX_MULT_TO_UNROLL = 256;

    /**
     * Whether or not we're trying to cut splitjoins horizontally and
     * vertically (otherwise we just consider each child pipeline).
     */
    static final boolean ENABLE_TWO_DIMENSIONAL_CUTS = true;
    /**
     * Whether or not we're currently tracing back.
     */
    static boolean tracingBack;

    /**
     * Different configurations to look for.
     */
    public static final int COLLAPSE_NONE = 0;
    public static final int COLLAPSE_ANY = 1;
    public static final int COLLAPSE_LINEAR = 2;
    public static final int COLLAPSE_FREQ = 3;

    /**
     * String names for collapse values
     */
    public static final String COLLAPSE_STRING(int collapse) {
	switch(collapse) {
	case COLLAPSE_NONE: return   "NONE  ";
	case COLLAPSE_ANY: return    "ANY   ";
	case COLLAPSE_LINEAR: return "LINEAR";
	case COLLAPSE_FREQ: return   "FREQ  ";
	default: return "UNKNOWN_COLLAPSE_TYPE: " + collapse;
	}
    }

    /**
     * Map from stream structures to LDPConfig's.
     */
    private final HashMap configMap;

    /**
     * The linear analyzer for this.
     */
    private final LinearAnalyzer lfa;
    /**
     * Stream we're partitioning.
     */
    private final SIRStream str;
    /**
     * Execution counts for <str> (given original factoring of containers).
     */
    private HashMap[] counts;
    
    public LinearPartitioner(SIRStream str, LinearAnalyzer lfa) {
	this.str = str;
	this.lfa = lfa;
	this.configMap = new HashMap();
    }

    /**
     * This is the toplevel call for doing partitioning.
     */
    public SIRStream toplevel() {
	// lift before and after
	Lifter.lift(str);

	// debug setup
	long start = System.currentTimeMillis();

	// calculate partitions
	StreamTransform st = calcPartitions();
	if (DEBUG) { st.printHierarchy(); }

	// debug output
	System.err.println("Linear partitioner took " + 
			   (System.currentTimeMillis()-start)/1000 + " secs to calculate partitions.");

	// perform partitioning transformations
	SIRStream result = st.doTransform(str);

	// remove unnecessary identities
	Lifter.eliminateIdentities(result);
	// lift before and after
	Lifter.lift(result);

	// reclaim children here, since they might've been shuffled
	// around in the config process
	if (result instanceof SIRContainer) {
	    ((SIRContainer)result).reclaimChildren();
	}

	return result;
    }

    /**
     * Returns a stream transform that will perform the partitioning
     * for <str>.
     */
    private StreamTransform calcPartitions() {
	// build stream config
	LDPConfig topConfig = buildStreamConfig();
	// set execution counts here, because we want to account for
	// identities that we added to the stream
	this.counts = SIRScheduler.getExecutionCounts(str);
	// build up tables.
	long cost = topConfig.get(COLLAPSE_ANY);
	// clear dot traces
	LDPConfig.numAssigned = 0;
	LDPConfig.partitions = new HashMap();
	tracingBack = true;
	StreamTransform result = topConfig.traceback(COLLAPSE_ANY);
	tracingBack = false;
	// make dot graph of partitions
	PartitionDot.printPartitionGraph(str, "linear-partitions.dot", LDPConfig.partitions);
	return result;
    }

    /**
     * Builds up mapping from stream to array in this. Returns a
     * config for the toplevel stream.
     */
    private LDPConfig buildStreamConfig() {
	RefactorSplitJoin.addDeepRectangularSyncPoints(str);
	StreamItDot.printGraph(str, "ldp-partition-input.dot");
	return (LDPConfig)str.accept(new ConfigBuilder());
    }

    public LDPConfig getConfig(SIRStream str) {
	return (LDPConfig) configMap.get(str);
    }

    public LinearAnalyzer getLinearAnalyzer() {
	return this.lfa;
    }

    /**
     * Returns the pre-computed execution counts for the stream that
     * we're partitioning.
     */
    public HashMap[] getExecutionCounts() {
	return counts;
    }

    /**
     * Returns a LDPConfig for <str>
     */
    private LDPConfig createConfig(SIRStream str) {
	if (str instanceof SIRFilter) {
	    return new LDPConfigFilter((SIRFilter)str, this);
	} else if (str instanceof SIRPipeline) {
	    return new LDPConfigPipeline((SIRPipeline)str, this);
	} else if (str instanceof SIRSplitJoin) {
	    return new LDPConfigSplitJoin((SIRSplitJoin)str, this);
	} else {
	    Utils.assert(str instanceof SIRFeedbackLoop, "Unexpected stream type: " + str);
	    return new LDPConfigFeedbackLoop((SIRFeedbackLoop)str, this);
	}
    }

    class ConfigBuilder extends EmptyAttributeStreamVisitor {

	public Object visitSplitJoin(SIRSplitJoin self,
				     JFieldDeclaration[] fields,
				     JMethodDeclaration[] methods,
				     JMethodDeclaration init,
				     SIRSplitter splitter,
				     SIRJoiner joiner) {
	    // shouldn't have 0-sized SJ's
	    Utils.assert(self.size()!=0, "Didn't expect SJ with no children.");
	    super.visitSplitJoin(self, fields, methods, init, splitter, joiner);
	    // if parent is a pipeline, don't need a config for this splitjoin
	    if (self.getParent() instanceof SIRPipeline) {
		return self;
	    } else {
		return makeConfig(self);
	    }
	}

	public Object visitPipeline(SIRPipeline self,
				    JFieldDeclaration[] fields,
				    JMethodDeclaration[] methods,
				    JMethodDeclaration init) {
	    super.visitPipeline(self, fields, methods, init);
	    return makeConfig(self);
	}

	/* pre-visit a feedbackloop */
	public Object visitFeedbackLoop(SIRFeedbackLoop self,
					JFieldDeclaration[] fields,
					JMethodDeclaration[] methods,
					JMethodDeclaration init,
					JMethodDeclaration initPath) {
	    super.visitFeedbackLoop(self, fields, methods, init, initPath);
	    return makeConfig(self);
	}

	public Object visitFilter(SIRFilter self,
				  JFieldDeclaration[] fields,
				  JMethodDeclaration[] methods,
				  JMethodDeclaration init,
				  JMethodDeclaration work,
				  CType inputType, CType outputType) {
	    super.visitFilter(self, fields, methods, init, work, inputType, outputType);
	    return makeConfig(self);
	}

	private LDPConfig makeConfig(SIRStream self) {
	    LDPConfig config = createConfig(self);
	    configMap.put(self, config);
	    return config;
	}
    }

}

