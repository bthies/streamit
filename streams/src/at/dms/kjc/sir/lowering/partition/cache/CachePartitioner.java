package at.dms.kjc.sir.lowering.partition.cache;

import java.util.*;
import java.io.*;

import at.dms.kjc.*;
import at.dms.util.*;
import at.dms.kjc.iterator.*;
import at.dms.kjc.sir.*;
import at.dms.kjc.sir.lowering.*;
import at.dms.kjc.sir.lowering.fusion.*;
import at.dms.kjc.sir.lowering.fission.*;
import at.dms.kjc.sir.lowering.partition.*;

public class CachePartitioner extends ListPartitioner {
    /**
     * Threshold value of instruction code size before cost goes up
     * dramatically.
     */
    static final int ICODE_THRESHOLD = 16000;
    
    /**
     * Whether or not we're transforming the stream on traceback.  If
     * not, then we're just gathering the partition info for dot
     * output.
     */
    static boolean transformOnTraceback;
    /**
     * Bottleneck in current run.
     */
    private int bottleneck;
    
    /**
     * Map from stream structures to CConfig's.
     */
    private HashMap configMap;

    public CachePartitioner(SIRStream str, WorkEstimate work, int numTiles) {
	super(str, work, numTiles);
	this.configMap = new HashMap();
    }

    /**
     * Toplevel call.
     */
    public static SIRStream doit(SIRStream str) {
	// Lift filters out of pipelines if they're the only thing in
	// the pipe
	Lifter.lift(str);

	// make work estimate
	WorkEstimate work = WorkEstimate.getWorkEstimate(str);
	work.printGraph(str, "work-before-partition.dot");
	work.getSortedFilterWork().writeToFile("work-before-partition.txt");

	str = new CachePartitioner(str, work, 0).toplevel();

	// lift the result
	Lifter.lift(str);

	// get the final work estimate
	work = WorkEstimate.getWorkEstimate(str);
	work.printGraph(str, "work-after-partition.dot");
	work.getSortedFilterWork().writeToFile("work-after-partition.txt");
	work.printWork();

	return str;
    }
    
    /**
     * This is the toplevel call for doing partitioning.  Returns the
     * partitioned stream.
     */
    public SIRStream toplevel() {
	LinkedList partitions = new LinkedList();
	return calcPartitions(partitions, true);
    }

    /**
     * The toplevel call for calculating partitions without fusing
     * anything in the stream.  Note that the stream might be
     * re-arranged if the partitioner couldn't undo the
     * canonicalization that it used; that's why a new stream is
     * returned.  The hashmap that is passed in is cleared and filled
     * with a mapping from SIROperator to String denoting list of
     * partition numbers that a given SIROperator is assigned to.
     */
    public SIRStream calcPartitions(HashMap partitionMap) {
	LinkedList partitions = new LinkedList();
	SIRStream result = calcPartitions(partitions, false);

	partitionMap.clear();
	partitionMap.putAll(PartitionRecord.asIntegerMap(partitions));

	return result;
    }

    /**
     * Returns a stream transform that will perform the partitioning
     * for <str>.  <partitions> must be a non-null linked list; it is
     * cleared and then filled with PartitionRecords representing the
     * partitions.  If <doTransform> is true, then the result of
     * partitioning the stream is returned; otherwise the stream is
     * left alone and only <partitions> are filled up.
     */
    private SIRStream calcPartitions(LinkedList partitions, boolean doTransform) {
	// build stream config
	System.out.println("  Building stream config... ");
	CConfig topConfig = buildStreamConfig();

	System.out.println("  Calculating a greedy partitioning... ");

	int gtiles = topConfig.numberOfTiles();
	System.out.println("  Greedy partitioning requires "+gtiles+" tiles.");

	/*

	// get lower bound on cost: one tile per filter (or limit
	// specified in command line)
	int count = (int)Math.min(KjcOptions.cluster, countFilters(str));
	System.err.println("Trying lower bound of " + count + " tiles.");
	CCost noFusion = topConfig.get(count);
	System.err.println("  Partitioner thinks bottleneck is " + noFusion.getCost());
	//System.err.println("  Max iCode size: " + noFusion.getICodeSize());

	// start with 1 filter and work our way up to as many filters
	// are needed.
	numTiles = 0;
	CCost cost;
	do {
	    numTiles++;
	    System.err.println("Trying " + numTiles + " tiles.");

	    // build up tables.
	    System.out.println("  Calculating partition info...");
	    cost = topConfig.get(numTiles);

	    if (KjcOptions.debug) { topConfig.printArray(numTiles); }

	    System.err.println("  Partitioner thinks bottleneck is " + cost.getCost());
	    //System.err.println("  Max iCode size: " + cost.getICodeSize());

 	} while (cost.greaterThan(noFusion));
	
	*/

	int tilesUsed = numTiles;
	
	System.out.println("  Tracing back...");

	// build up list of partitions 
	partitions.clear();
	PartitionRecord curPartition = new PartitionRecord();
	partitions.add(curPartition);

	transformOnTraceback = doTransform;
	SIRStream result = topConfig.traceback(partitions, curPartition, tilesUsed, str);

	// remove unnecessary identities
	Lifter.eliminateIdentities(result);

	// reclaim children here, since they might've been shuffled
	// around in the config process
	if (result instanceof SIRContainer) {
	    ((SIRContainer)result).reclaimChildren();
	}

	// can only print if we didn't transform
	if (!doTransform) {
	    Lifter.lift(result);
	    PartitionDot.printPartitionGraph(result, "partitions.dot", PartitionRecord.asStringMap(partitions));
	}
	
    	return result;
    }

    private int countFilters(SIRStream str) {
	final int[] count = new int[1];
	IterFactory.createFactory().createIter(str).accept(new EmptyStreamVisitor() {
		public void visitFilter(SIRFilter self,
					SIRFilterIter iter) {
		    count[0]++;
		}
	    });
	return count[0];
    }

    /**
     * Builds up mapping from stream to array in this. Returns a
     * config for the toplevel stream.
     */
    private CConfig buildStreamConfig() {
	return (CConfig)str.accept(new ConfigBuilder());
    }

    int getBottleneck() {
	return this.bottleneck;
    }

    public CConfig getConfig(SIRStream str) {
	return (CConfig) configMap.get(str);
    }

    /**
     * Returns a CConfig for <str>
     */
    private CConfig createConfig(SIRStream str) {
	if (str instanceof SIRFilter) {
	    return new CConfigFilter((SIRFilter)str, this);
	} else if (str instanceof SIRPipeline) {
	    return new CConfigPipeline((SIRPipeline)str, this);
	} else if (str instanceof SIRSplitJoin) {
	    return new CConfigSplitJoin((SIRSplitJoin)str, this);
	} else {
	    assert str instanceof SIRFeedbackLoop:
                "Unexpected stream type: " + str;
	    return new CConfigFeedbackLoop((SIRFeedbackLoop)str, this);
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
	    assert self.size()!=0: "Didn't expect SJ with no children.";
	    super.visitSplitJoin(self, fields, methods, init, splitter, joiner);
	    // if parent is a pipeline, don't need a config for this splitjoin
	    return makeConfig(self);
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

	private CConfig makeConfig(SIRStream self) {
	    CConfig config = createConfig(self);
	    configMap.put(self, config);
	    return config;
	}
    }
}
