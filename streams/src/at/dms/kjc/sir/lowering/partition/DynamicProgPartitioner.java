package at.dms.kjc.sir.lowering.partition;

import java.util.*;
import java.io.*;
import lpsolve.*;

import at.dms.kjc.*;
import at.dms.util.*;
import at.dms.kjc.linprog.*;
import at.dms.kjc.iterator.*;
import at.dms.kjc.raw.*;
import at.dms.kjc.sir.*;
import at.dms.kjc.sir.lowering.*;
import at.dms.kjc.sir.lowering.fusion.*;
import at.dms.kjc.sir.lowering.fission.*;

public class DynamicProgPartitioner extends ListPartitioner {
    /**
     * The overhead of work estimated for each fissed node.
     */
    private static final int FISSION_OVERHEAD = 1;

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
    
    public void toplevelFusion() {
	long start = System.currentTimeMillis();
	StreamTransform st = calcPartitions();
	System.err.println("Dynamic programming partitioner took " + 
			   (System.currentTimeMillis()-start)/1000 + " secs to calculate partitions.");
	st.doTransform(str);
    }

    /**
     * Returns a stream transform that will perform the partitioning
     * for <str>.
     */
    private StreamTransform calcPartitions() {
	this.work = WorkEstimate.getWorkEstimate(str);

	// build stream config
	DPConfig topConfig = buildStreamConfig();
	// build up tables
	int bottleneck = topConfig.get(numTiles);
	System.err.println("Found bottleneck work is " + bottleneck + ".  Tracing back...");
	// expand config stubs that were shared for symmetry optimizations
	expandSharedConfigs();
	
	// build up assignment (map and tilecounter are for historical reasons)
	HashMap map = new HashMap();
	int[] tileCounter = { 0 };
	StreamTransform result = topConfig.traceback(map, tileCounter, numTiles);
	Utils.assert(tileCounter[0]<numTiles, "Assigned " + tileCounter[0] + " tiles, but we only have " + numTiles);

	PartitionUtil.printTileWork(map, work, numTiles);
	return result;
    }

    /**
     * Builds up mapping from stream to array in this, also
     * identifying the uniform splitjoins.  Returns a config for the
     * toplevel stream.
     */
    private DPConfig buildStreamConfig() {
	return (DPConfig)str.accept(new EmptyAttributeStreamVisitor() {
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
			    System.err.println("Detected symmetry between " + 
					       firstChild.getName() + " and " + child.getName());
			    configMap.put(child, lastConfig);
			} else {
			    lastEquiv = child;
			    lastConfig = (DPConfig)child.accept(this);
			}
		    }
		    // if all were equivalent, then add them to uniform list
		    if (lastEquiv== self.get(0)) {
			System.err.println("Detected uniform splitjoin: " + self.getName());
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
	    });
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

    /**
     * Returns a DPConfig for <str>
     */
    private DPConfig createConfig(SIRStream str) {
	if (str instanceof SIRFilter) {
	    return new DPConfigFilter((SIRFilter)str);
	} else if (str instanceof SIRSplitJoin) {
	    return new DPConfigSplitJoin((SIRSplitJoin)str);
	} else {
	    Utils.assert(str instanceof SIRContainer, "Unexpected stream type: " + str);
	    return new DPConfigContainer((SIRContainer)str);
	}
    }

    abstract class DPConfig implements Cloneable {
	/**  
	 * A[i,j,k] that gives the bottleneck work for segment i-j of
	 * the structure if children i through j are assigned to k
	 * tiles.  If this corresponds to a filter's config, then A is
	 * null.
	 */
	protected int[][][] A;

	/**
	 * Return the bottleneck work if this config is fit on
	 * <tileLimit> tiles.
	 */
	abstract protected int get(int tileLimit);

	/**
	 * Traceback through a pre-computed optimal solution, storing
	 * the optimal tile assignment to <map> and returning stream
	 * transform to perform best partitioning.  <tileCounter> is a
	 * one-element array holding the value of the current tile we
	 * are assigning to filters (in a depth-first way).
	 */
	abstract public StreamTransform traceback(HashMap map, int[] tileCounter, int tileLimit);

	/**
	 * Returns the stream this config is wrapping.
	 */
	abstract public SIRStream getStream();

	/**
	 * Returns a copy of this with the same A matrix as this
	 * (object identity is the same), but with <str> as the
	 * stream.
	 */
	public DPConfig copyWithStream(SIRStream str) {
	    // use cloning instead of a new constructor so that we
	    // don't reconstruct a fresh A array.
	    DPConfig result = null;
	    try {
		result = (DPConfig)this.clone();
	    } catch (CloneNotSupportedException e) {
		e.printStackTrace();
	    }
	    result.setStream(str);
	    return result;
	}

	/**
	 * Sets this to wrap <str>.
	 */
	protected abstract void setStream(SIRStream str);
	
    }

    class DPConfigContainer extends DPConfig {
	/**
	 * The stream for this container.
	 */
	protected SIRContainer cont;

	public DPConfigContainer(SIRContainer cont) {
	    this.cont = cont;
	    this.A = new int[cont.size()][cont.size()][numTiles+1];
	}

	public SIRStream getStream() {
	    return cont;
	}

	/**
	 * Requires <str> is a container.
	 */
	protected void setStream(SIRStream str) {
	    Utils.assert(str instanceof SIRContainer);
	    this.cont = (SIRContainer)str;
	}

	protected int get(int tileLimit) {
	    // otherwise, compute it
	    return get(0, cont.size()-1, tileLimit);
	}

	protected int get(int child1, int child2, int tileLimit) {
	    // if we've memoized the value before, return it
	    if (A[child1][child2][tileLimit]>0) {
		/*
		System.err.println("Found memoized A[" + child1 + "][" + child2 + "][" + tileLimit + "] = " + 
				   A[child1][child2][tileLimit] + " for " + cont.getName());
		*/
		return A[child1][child2][tileLimit];
	    }

	    // if we are down to one child, then descend into child
	    if (child1==child2) {
		int childCost = childConfig(child1).get(tileLimit);
		A[child1][child2][tileLimit] = childCost;
		//System.err.println("Returning " + childCost + " from descent into child.");
		return childCost;
	    }	    

	    // otherwise, if <tileLimit> is 1, then just sum the work
	    // of our components
	    if (tileLimit==1) {
		int sum = get(child1, child1, tileLimit) + get(child1+1, child2, tileLimit);
		A[child1][child2][tileLimit] = sum;
		//System.err.println("Returning sum " + sum + " from fusion.");
		return sum;
	    }

	    // otherwise, find the lowest-cost child AFTER WHICH to
	    // make a partition at this level
	    /*
	    System.err.println("Allocating " + tileLimit + " between children " + child1 + " and " + child2 + 
			       " of " + cont.getClass() + " " + cont.getName());
	    */
	    int min = Integer.MAX_VALUE;
	    for (int i=child1; i<child2; i++) {
		for (int j=1; j<tileLimit; j++) {
		    int cost = Math.max(get(child1, i, j), get(i+1, child2, tileLimit-j));
		    if (cost < min) {
			/*
			System.err.println(" found new min of " + cost + " for " + cont.getName() + " [" 
					   + child1 + "," + child2 + "] with " + tileLimit + " tiles:\n" +
					   "\t" + j + " tiles to children " + child1 + "-" + i + "\n" + 
					   "\t" + (tileLimit-j) + " tiles to children " + (i+1) + "-" + 
					   child2);
			*/
			min = cost;
		    }
		}
	    }
	    /*
	    System.err.println("Assigning MIN A[" + child1 + "][" + child2 + "][" + tileLimit + "]=" + min + 
			       " for " + cont.getName());
	    */
	    A[child1][child2][tileLimit] = min;
	    return min;
	}

	/**
	 * Traceback function.
	 */
	public StreamTransform traceback(HashMap map, int[] tileCounter, int tileLimit) {
	    // only support fusion for now.
	    FusionTransform st = new FusionTransform();
	    // add partitions at beginning and end
	    st.addPartition(0);
	    st.addPartition(cont.size());

	    traceback(st, map, tileCounter, 0, cont.size()-1, tileLimit);
	    // if the whole container is assigned to one tile, record
	    // it as such.  otherwise record as -1
	    if (tileLimit==1) {
		map.put(cont, new Integer(tileCounter[0]));
	    } else {
		map.put(cont, new Integer(-1));
	    }
	    return st;
	}
	
	/**
	 * Traceback helper function. The child1, child2, and
	 * tileLimit are as above.  The stream transform <st> is the
	 * one we're building up to carry out the final partitioning.
	 */
	protected void traceback(FusionTransform st, HashMap map, int[] tileCounter,
				 int child1, int child2, int tileLimit) {
	    // if we only have one tile left, or if we are only
	    // looking at one child, then just recurse into children
	    if (child1==child2 || tileLimit==1) {
		for (int i=child1; i<=child2; i++) {
		    st.add(childConfig(i).traceback(map, tileCounter, tileLimit));
		}
		return;
	    }

	    // otherwise, find the best partitioning of this into
	    // <tileLimit> sizes.  See where the first break was,
	    // breaking ties by fewest number of tiles required.
	    int min = Integer.MAX_VALUE;
	    for (int i=child1; i<child2; i++) {
		for (int j=1; j<tileLimit; j++) {
		    int cost = Math.max(get(child1, i, j), get(i+1, child2, tileLimit-j));
		    if (cost==A[child1][child2][tileLimit]) {
			/*
			System.err.println("Found best split of " + cont.getName() + " [" + child1 + "," + child2 + 
					   "]: " + j + " to [" + child1 + "," + i + "],  " + (tileLimit-j) + " to [" +
					   (i+1) + "," + child2 + "]");
			*/
			// if we found our best cost, then the
			// division is at this <i> with <j> partitions
			// on the left.  First recurse left, then
			// increment tile counter, then recurse right.
			traceback(st, map, tileCounter, child1, i, j);
			tileCounter[0]++;
			traceback(st, map, tileCounter, i+1, child2, tileLimit-j);
			// remember that we had a partition here
			st.addPartition(i+1);
			return;
		    }
		}
	    }	    
	}
	
	/**
	 * Returns config for child at index <childIndex>
	 */
	protected DPConfig childConfig(int childIndex) {
	    SIRStream child = cont.get(childIndex);
	    return (DPConfig) configMap.get(child);
	}
    }

    class DPConfigSplitJoin extends DPConfigContainer {
	public DPConfigSplitJoin(SIRSplitJoin split) {
	    super(split);
	}

	protected int get(int tileLimit) {
	    /*
	    if (uniformSJ.contains(cont)) {
		// optimize uniform splitjoins
		return getUniform(tileLimit);
	    } else {
	    */
	    // otherwise, use normal procedure
	    if (tileLimit==1 || 
		(cont.getParent()!=null && 
		 cont.getParent().getSuccessor(cont) instanceof SIRJoiner)) {
		// if the tileLimit is 1 or joiner will be collapsed,
		// then all tiles are available for the splitjoin
		return super.get(tileLimit);
	    } else {
		// however, if we want to break the splitjoin up, then
		// we have to subtract one from the tileLimit to
		// account for the joiner tiles
		return super.get(tileLimit-1);
	    }
	}

	/**
	 * Return bottleneck for uniform splitjoins.
	 */
	private int getUniform(int tileLimit) {
	    // get cost of child
	    int childCost = ((DPConfig)configMap.get(cont.get(0))).get(tileLimit);
	    if (tileLimit<=2) {
		// if one tile or two, have to fuse the whole thing
		return childCost * cont.size();
	    } else {
		// otherwise, return the cost of a group.  subtract one
		// tile to account for the joiner.
		int groupSize = (int)Math.ceil(((double)cont.size()) / ((double)(tileLimit-1)));
		return childCost * groupSize;
	    }
	}

	/**
	 * Do traceback for uniform splitjoins.
	 */
	/*
	private int tracebackUniform(HashMap map, int[] tileCounter, int tileLimit) {
	}
	*/

	public StreamTransform traceback(HashMap map, int[] tileCounter, int tileLimit) {
	    /*
	    if (uniformSJ.contains(cont)) {
		// optimize uniform splitjoins
		tracebackUniform(map, tileCounter, tileLimit);
	    } else {
	    */
	    StreamTransform result = null;
	    if (tileLimit==1 || (cont.getParent() instanceof SIRSplitJoin)) {
		result = super.traceback(map, tileCounter, tileLimit);
	    } else {
		// if we're not fusing into a single tile, need to:
		// 1) decrease the tileLimit since one will be reserved for the joiner
		result = super.traceback(map, tileCounter, tileLimit-1);
		// 2) unless we have a tileLimit of 2 (in which case
		//    the joiner should just as well be fused in with
		//    the parallel streams) increment the tile count
		//    before adding the joiner
		if (tileLimit!=2) {
		    tileCounter[0]++;
		}
	    }
	    // add the joiner
	    map.put(((SIRSplitJoin)cont).getJoiner(), new Integer(tileCounter[0]));
	    return result;
	}
    }

    class DPConfigFilter extends DPConfig {
	/**
	 * The filter corresponding to this.
	 */
	private SIRFilter filter;
	/**
	 * Whether or not <filter> is stateless.
	 */
	private boolean isFissable;
	
	public DPConfigFilter(SIRFilter filter) {
	    this.filter = filter;
	    this.isFissable = StatelessDuplicate.isFissable(filter);
	    this.A = null;
	}

	public int get(int tileLimit) {
	    int workCount = work.getWork(filter);
	    // return decreased work if we're fissable
	    if (tilesForFission(tileLimit)>1 && isFissable) {
		/*
		System.err.println("Trying " + filter + " on " + tileLimit + " tiles and splitting " + 
				   tilesForFission(tileLimit) + " ways with bottlneck of " + 
				   workCount / tilesForFission(tileLimit) + FISSION_OVERHEAD);
		*/
		return workCount / tilesForFission(tileLimit) + FISSION_OVERHEAD;
	    } else {
		return workCount;
	    }
	}

	// see how many tiles we can devote to fissed filters;
	// depends on if we need a separate tile for a joiner.
	// This is a conservative approximation (joiner disappears
	// if next downstream filter is a joiner, even if parent
	// is not sj)
	private int tilesForFission(int tileLimit) {
	    return (filter.getParent()!=null && 
		    filter.getParent().getSuccessor(filter) instanceof SIRJoiner ?
		    tileLimit : 
		    tileLimit - 1);
	}

	public SIRStream getStream() {
	    return filter;
	}

	/**
	 * Requires <str> is a filter.
	 */
	protected void setStream(SIRStream str) {
	    Utils.assert(str instanceof SIRFilter);
	    this.filter = (SIRFilter)str;
	}

	/**
	 * Add this to the map and return.
	 */
	public StreamTransform traceback(HashMap map, int[] tileCounter, int tileLimit) {
	    // increment the tile counter by the number of extra
	    // filters assigned to this
	    tileCounter[0] += (tileLimit - 1);
	    // remember the last of these in the tile map
	    map.put(filter, new Integer(tileCounter[0]));

	    // do fission if we can
	    /*
	    System.err.println("For " + filter.getName() + 
			       ", traceback allocated " + tileLimit + " tiles; allocated " + tilesForFission(tileLimit)
			       + " for parallel streams.  isFissable=" + isFissable);
	    */
	    if (tilesForFission(tileLimit)>1 && isFissable) {
		return new FissionTransform(tilesForFission(tileLimit));
	    } else {
		return new IdentityTransform();
	    }
	}

    }
}
