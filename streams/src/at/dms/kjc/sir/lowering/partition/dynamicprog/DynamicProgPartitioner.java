package at.dms.kjc.sir.lowering.partition.dynamicprog;

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

public class DynamicProgPartitioner extends ListPartitioner {
    /**
     * The overhead of work estimated for each fissed node.
     */
    static final int FISSION_OVERHEAD = 1;

    /**
     * Map from stream structures to DPConfig's.
     */
    private HashMap configMap;
    /**
     * The set of splitjoins whose children are structurally
     * equivalent (and have equal amounts of work.)
     */
    private HashSet uniformSJ;
    
    public DynamicProgPartitioner(SIRStream str, int numTiles) {
	super(str, numTiles);
	this.configMap = new HashMap();
	this.uniformSJ = new HashSet();
    }

    /**
     * Collect scaling statistics for all partitions 1...<maxTiles>.
     */
    public static void saveScalingStatistics(SIRStream str, int maxTiles) {
	PartitionUtil.setupScalingStatistics();
	for (int i=1; i<maxTiles; i++) {
	    LinkedList partitions = new LinkedList();
	    new DynamicProgPartitioner(str, i).calcPartitions(partitions);
	    PartitionUtil.doScalingStatistics(partitions, i);
	}
	PartitionUtil.stopScalingStatistics();
    }
    
    public void toplevelFusion() {
	// debug setup
	long start = System.currentTimeMillis();
	LinkedList partitions = new LinkedList();

	// calculate partitions
	StreamTransform st = calcPartitions(partitions);

	// debug output
	PartitionUtil.printTileWork(partitions, numTiles);
	PartitionDot.printGraph(str, "partitions.dot", PartitionRecord.asMap(partitions));
	Utils.assert(partitions.size()<=numTiles, "Assigned " + partitions.size() + " tiles, but we only have " + numTiles);
	System.out.println("Dynamic programming partitioner took " + 
			   (System.currentTimeMillis()-start)/1000 + " secs to calculate partitions.");

	// perform partitioning transformations
	st.doTransform(str);
    }

    /**
     * Returns a stream transform that will perform the partitioning
     * for <str>.  <partitions> must be a non-null linked list; it is
     * cleared and then filled with PartitionRecords representing the
     * partitions.
     */
    private StreamTransform calcPartitions(LinkedList partitions) {
	// build stream config
	DPConfig topConfig = buildStreamConfig();
	// build up tables
	int bottleneck = topConfig.get(numTiles);
	//System.err.println("Found bottleneck work is " + bottleneck + ".  Tracing back...");
	// expand config stubs that were shared for symmetry optimizations
	expandSharedConfigs();
	
	// build up list of partitions 
	partitions.clear();
	PartitionRecord curPartition = new PartitionRecord();
	partitions.add(curPartition);

	StreamTransform result = topConfig.traceback(partitions, curPartition, numTiles);

	Utils.assert(bottleneck==PartitionUtil.getMaxWork(partitions));
	return result;
    }

    /**
     * Builds up mapping from stream to array in this, also
     * identifying the uniform splitjoins.  Returns a config for the
     * toplevel stream.
     */
    private DPConfig buildStreamConfig() {
	return (DPConfig)str.accept(new ConfigBuilder());
    }

    /**
     * Expands shared config records into separate records so that the
     * traceback can give a full schedule.
     */
    private void expandSharedConfigs() {
	// these are config mappings that were once shared, but we
	// have expanded to be unshared
	HashMap unshared = new HashMap();
	// this is the working set under consideration -- contains
	// some shared and some non-shared items
	HashMap potentialShares = configMap;
	do {
	    // less shared is our first-level fix of shares we find in
	    // potential shares.  They might still have some sharing.
	    HashMap lessShared = new HashMap();
	    for (Iterator it = potentialShares.keySet().iterator(); it.hasNext(); ) {
		SIRStream str = (SIRStream)it.next();
		DPConfig config = (DPConfig)potentialShares.get(str);
		SIRStream configStr = config.getStream();
		// if <config> represents something other than <str>, then
		// replace it with an identical config that wraps <str>
		if (str!=configStr) {
		    unshared.put(str, config.copyWithStream(str));
		    // also need to take care of children of <str>.  Do
		    // this by associating them with the children of
		    // <configStr> and putting them back in the mix; will
		    // iterate 'til nothing is left.
		    if (str instanceof SIRContainer) {
			SIRContainer cont = (SIRContainer)str;
			for (int i=0; i<cont.size(); i++) {
			    lessShared.put(cont.get(i), configMap.get(((SIRContainer)configStr).get(i)));
			}
		    }
		}
	    }
	    potentialShares = lessShared;
	} while (!(potentialShares.isEmpty()));
	// add all from <unshared> to <configMap> (don't do above to
	// avoid modifying what we're iterating over)
	configMap.putAll(unshared);
    }

    public DPConfig getConfig(SIRStream str) {
	return (DPConfig) configMap.get(str);
    }

    /**
     * Returns a DPConfig for <str>
     */
    private DPConfig createConfig(SIRStream str) {
	if (str instanceof SIRFilter) {
	    return new DPConfigFilter((SIRFilter)str, this);
	} else if (str instanceof SIRPipeline) {
	    return new DPConfigPipeline((SIRPipeline)str, this);
	} else if (str instanceof SIRSplitJoin) {
	    return new DPConfigSplitJoin((SIRSplitJoin)str, this);
	} else {
	    Utils.assert(str instanceof SIRFeedbackLoop, "Unexpected stream type: " + str);
	    return new DPConfigFeedbackLoop((SIRFeedbackLoop)str, this);
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
	    // keep track of last one which a child was equivalent to
	    SIRStream firstChild = self.get(0);
	    SIRStream lastEquiv = firstChild;
	    DPConfig lastConfig = (DPConfig)firstChild.accept(this);
	    // look for equivalent children
	    for (int i=1; i<self.size(); i++) {
		SIRStream child = self.get(i);
		if (equivStructure(lastEquiv, child)) {
		    /*
		      System.err.println("Detected symmetry between " + 
		      firstChild.getName() + " and " + child.getName());
		    */
		    configMap.put(child, lastConfig);
		} else {
		    lastEquiv = child;
		    lastConfig = (DPConfig)child.accept(this);
		}
	    }
	    // if all were equivalent, then add them to uniform list
	    if (lastEquiv== self.get(0)) {
		System.out.println("Detected uniform splitjoin: " + self.getName());
		uniformSJ.add(self);
	    }
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

	private DPConfig makeConfig(SIRStream self) {
	    DPConfig config = createConfig(self);
	    configMap.put(self, config);
	    return config;
	}
    }

}

