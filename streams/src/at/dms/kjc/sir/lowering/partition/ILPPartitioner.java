package at.dms.kjc.sir.lowering;

import java.util.*;
import lpsolve.*;

import at.dms.kjc.*;
import at.dms.kjc.linprog.*;
import at.dms.kjc.iterator.*;
import at.dms.kjc.raw.*;
import at.dms.kjc.sir.*;
import at.dms.kjc.sir.lowering.fusion.*;
import at.dms.kjc.sir.lowering.fission.*;

public class ILPPartitioner {

    /**
     * The toplevel stream we're operating on.
     */
    private SIRStream str;
    /**
     * The target number of tiles this partitioner is going for.
     */
    private int numTiles;

    private ILPPartitioner(SIRStream str, int numTiles) {
	this.str = str;
	this.numTiles = numTiles;
    }
    
    /**
     * Tries to adjust <str> into <num> pieces of equal work.
     */
    public static void doit(SIRStream str, int numTiles) {
	new ILPPartitioner(str, numTiles).doit();
    }

    private void doit() {
	System.err.println("Starting ILP Partitioner . . . ");
	// dump the orig graph
	StreamItDot.printGraph(str, "before.dot");

	// Lift filters out of pipelines if they're the only thing in
	// the pipe
	System.err.print("Lifting filters... ");
	Lifter.lift(str);
	System.err.println("done.");

	// count tiles 
	System.err.print("count tiles... ");
	int count = new RawFlattener(str).getNumTiles();
	System.err.println(count + " tiles.");

	// if we have too few tiles, then use the other partitioner to
	// fiss the big ones
	if (count < numTiles) {
	    System.err.println("More tiles than nodes; fissing with greedy partitioner...");
	    Partitioner.doit(str, numTiles);
	    return;
	} else {
	    fuseAll();
	}

	// dump the final graph
	StreamItDot.printGraph(str, "after.dot");
	System.err.println("Done with ILP Partitioner.");
    }
    
    private void fuseAll() {
 
    }

}

class CalcPartitions {
    
    /**
     * The toplevel stream we're operating on.
     */
    private SIRStream str;
    /**
     * The target number of tiles this partitioner is going for.
     */
    private int numTiles;
    /**
     * List of NODES (i.e., filters and joiners) in the stream graph.
     * This list is in the "canonicalized order" (see lp-partition
     * document.)
     */
    private LinkedList nodes;
    /**
     * Maps a stream container (pipeline, splitjoin, feedbackloop) to
     * an Integer denoting the first index in <nodes> that belongs to
     * the structure.
     */
    private HashMap first;
    /**
     * Maps a stream container (pipeline, splitjoin, feedbackloop) to
     * an Integer denoting the first index in <nodes> that belongs to
     * the structure.
     */
    private HashMap last;
    /**
     * The work estimate of the stream.
     */
    private WorkEstimate work;

    private CalcPartitions(SIRStream str, int numTiles) {
	this.str = str;
	this.numTiles = numTiles;
	this.nodes = new LinkedList();
	this.first = new HashMap();
	this.last = new HashMap();
    }
    
    /**
     * Returns a mapping from every stream structure in <str> to an
     * integer partition number, from -1...(numTiles-1).  If a stream
     * structure is entirely contained on a given tile, then it has
     * that tile number.  If it is split across multiple tiles, then
     * it has a target of -1.
     */
    public static HashMap doit(SIRStream str, int numTiles) {
	return new CalcPartitions(str, numTiles).doit();
    }

    private HashMap doit() {
	this.work = WorkEstimate.getWorkEstimate(str);
	buildNodesList();
	double[] sol = calcSolution();
	return null;
    }

    /**
     * Constructs <nodes>, <first> and <last> out of <str>.
     */
    private void buildNodesList() {
	// add dummy start node
	nodes.add(new DummyNode());

	// add nodes in stream
	SIRIterator it = IterFactory.createIter(str);
	it.accept(new EmptyStreamVisitor() {

		public void preVisitStream(SIRStream self,
					   SIRIterator iter) {
		    first.put(self, new Integer(nodes.size()));
		}

		public void postVisitStream(SIRStream self,
					    SIRIterator iter) {
		    last.put(self, new Integer(nodes.size()));
		}

		public void visitFilter(SIRFilter self,
					SIRFilterIter iter) {
		    nodes.add(self);
		}

		public void preVisitFeedbackLoop(SIRFeedbackLoop self,
						 SIRFeedbackLoopIter iter) {
		    super.preVisitFeedbackLoop(self, iter);
		    nodes.add(self.getJoiner());
		}
		
		public void postVisitSplitJoin(SIRSplitJoin self,
					       SIRSplitJoinIter iter) {
		    nodes.add(self.getJoiner());
		    super.postVisitSplitJoin(self, iter);
		}
	    });

	// add dummy end node
	nodes.add(new DummyNode());
    }

    class DummyNode extends Object {}

    /**
     * Returns the variable number of w_max, the variable that
     * represents the maximum amount of work across all tiles.
     */
    private int wmaxNum() {
	return 0;
    }

    /**
     * Returns the variable number of the indicator variable, P, for
     * whether or not node <n> is present on tile <t>, where <n> and
     * <t> are zero-indexed.
     */
    private int pNum(int n, int t) {
	return wmaxNum() + 1 + numTiles * n + t;
    }

    /**
     * Returns the variable number of the distance variable, d, for
     * tile <t> and index <n> in <nodes>.  <n> should range from 0 to
     * nodes.size()-2.
     */
    private int dNum(int n, int t) {
	return pNum(nodes.size(), numTiles) + numTiles * n + t;
    }

    /**
     * Returns an array holding the values of the variables in the
     * optimum of the partitioning problem.
     */
    private double[] calcSolution() {
	// make the linear program...
	int numVars = 1 + numTiles * (2 * nodes.size() - 1);
	// this is a *conservative* estimate of the number of
	// constraints, derived from the paper
	int numConstraints = 1 + (int)(6.25 * numTiles * nodes.size()) + 2*numTiles + nodes.size();
	LinearProgram lp = new LPSolve(numConstraints, numVars);
	
	setupObjective(lp);
	setupVariables(lp);
	setupConstraints(lp);

	// get solution, including objective function
	double[] sol = lp.solve();
	// eliminate the objective function from the solution
	double[] result = new double[sol.length-1];
	System.arraycopy(sol, 1, result, 0, sol.length-1);
	return result;
    }

    /**
     * Sets up objective function for <lp>
     */
    private void setupObjective(LinearProgram lp) {
	double[] obj = lp.getEmptyConstraint();
	obj[wmaxNum()] = 1;
	lp.setObjective(obj);
    }

    /**
     * Sets up variables in <lp>.
     */
    private void setupVariables(LinearProgram lp) {
	// set the indicator variables to be integral
	for (int i=0; i<numTiles; i++) {
	    for (int j=0; j<nodes.size(); j++) {
		lp.setIntVar(pNum(i, j));
	    }
	}
    }

    /**
     * Sets up constraints in <lp>.
     */
    private void setupConstraints(LinearProgram lp) {
	constrainOneTilePerNode(lp);
	constrainWMax(lp);
	constrainConnectedPartitions(lp);
	constrainHierarchicalPartitions(lp);
	constrainSeparateJoiners(lp);
    }

    private void constrainOneTilePerNode(LinearProgram lp) {
	for (int i=0; i<nodes.size(); i++) {
	     double[] con = lp.getEmptyConstraint();
	     for (int j=0; j<numTiles; j++) {
		 con[pNum(i,j)] = 1;
	     }
	     lp.addConstraintEQ(con, 1);
	}
    }

    private void constrainWMax(LinearProgram lp) {
	for (int i=0; i<numTiles; i++) {
	    //!!!
	    double[] con = lp.getEmptyConstraint();
	    lp.addConstraintGE(con, 0);
	}
    }

    private void constrainConnectedPartitions(LinearProgram lp) {
	double[] con = lp.getEmptyConstraint();
	lp.addConstraintGE(con, 0);
    }

    private void constrainHierarchicalPartitions(LinearProgram lp) {
	double[] con = lp.getEmptyConstraint();
	lp.addConstraintGE(con, 0);
    }

    private void constrainSeparateJoiners(LinearProgram lp) {
	double[] con = lp.getEmptyConstraint();
	lp.addConstraintGE(con, 0);
    }

}

