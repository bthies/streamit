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
	HashMap partitions = calcPartitions();
	ApplyPartitions.doit(str, partitions);
    }

    /**
     * Returns a mapping from every stream structure in <str> to an
     * integer partition number, from -1...(numTiles-1).  If a stream
     * structure is entirely contained on a given tile, then it has
     * that tile number.  If it is split across multiple tiles, then
     * it has a target of -1.
     */
    private HashMap calcPartitions() {
	this.work = WorkEstimate.getWorkEstimate(str);

	buildStreamConfig();
	HashMap result = null; //!!!

	PartitionUtil.printTileWork(result, work, numTiles);
	return result;
    }

    /**
     * Builds up mapping from stream to array in this, also
     * identifying the uniform splitjoins.
     */
    private void buildStreamConfig() {
	str.accept(new EmptyAttributeStreamVisitor() {
		public Object visitSplitJoin(SIRSplitJoin self,
					     JFieldDeclaration[] fields,
					     JMethodDeclaration[] methods,
					     JMethodDeclaration init,
					     SIRSplitter splitter,
					     SIRJoiner joiner) {
		    // shouldn't have 0-sized SJ's
		    Utils.assert(self.size()!=0, "Didn't SJ with no children.");
		    // keep track of last one which a child was equivalent to
		    SIRStream firstChild = self.get(0);
		    SIRStream lastEquiv = firstChild;
		    DPConfig lastConfig = (DPConfig)firstChild.accept(this);
		    // look for equivalent children
		    for (int i=1; i<self.size(); i++) {
			SIRStream child = self.get(i);
			if (equivStructure(lastEquiv, child)) {
			    configMap.put(child, lastConfig);
			} else {
			    lastEquiv = child;
			    lastConfig = (DPConfig)child.accept(this);
			}
		    }
		    // if all were equivalent, then add them to uniform list
		    if (lastEquiv== self.get(0)) {
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
		    DPConfig config = new DPConfig(self);
		    configMap.put(self, config);
		    return config;
		}
	    });
    }

    class DPConfig {
	/**
	 * A stream that can be used to access the children of this
	 * config.
	 */
	public final SIRStream str;
	/**  
	 * A[i,j,k] that gives the bottleneck work for segment i-j of the
	 * structure if children i through j are assigned to k tiles.
	 */
	public int[][][] A;
	
	public DPConfig(SIRStream str) {
	    this.str = str;
	    if (str instanceof SIRContainer) {
		SIRContainer cont = (SIRContainer)str;
		this.A = new int[cont.size()][cont.size()][numTiles];
	    } else {
		this.A = null;
	    }
	}

	public int get(int child1, int child2, int numTiles) {
	    return 0; //!!!
	}
    }
}
