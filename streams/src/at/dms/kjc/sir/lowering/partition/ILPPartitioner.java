package at.dms.kjc.sir.lowering.partition;

import java.rmi.RemoteException;

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

public class ILPPartitioner extends ListPartitioner {
    /**
     * The following two parameters control when the ILPPartitioner
     * will terminate.  It terminates as soon as one of the following
     * three conditions are met:
     *
     * 1) An optimal solution is found.
     *
     * 2) The elapsed time (in secs) exceeds OPTIMAL_TIMEOUT and a gap
     * value of GAP_TOLERANCE is found.  The gap is the percent by
     * which the current best integer solution exceeds the lower bound
     * that the solver has established on the optimal solution.
     *
     * 3) The elapsed time (in secs) exceeds GAP_TIMEOUT.
     */
    protected static final long OPTIMAL_TIMEOUT = 30;    // stop looking for opt. solution
    protected static final double GAP_TOLERANCE = 0.20;  // fractional gap to be satisfied with
    protected static final long GAP_TIMEOUT = 30*60;     // stop looking for solution within gap_tolerance

    /**
     * The work estimate that is given to joiner nodes.
     */
    protected static final int JOINER_WORK_ESTIMATE = 1;
    
    public ILPPartitioner(SIRStream str, WorkEstimate work, int numTiles) {
	super(str, work, numTiles);
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
	double[] sol = calcSolution();
	HashMap result = buildPartitionMap(sol);
	return result;
    }

    /**
     * Returns whether or not <val1> and <val2> are separated by less
     * than TOLERANCE.
     */
    private static final double TOLERANCE = 0.001;
    private boolean almostEqual(double val1, double val2) {
	return Math.abs(val1 - val2) < TOLERANCE;
    }

    /**
     * Given a solution to the LP, build a hashmap from stream
     * structure to partition number meeting specification of <doit>.
     */
    private HashMap buildPartitionMap(double[] sol) {
	HashMap result = new HashMap();
	for (int i=1; i<nodes.size()-1; i++) {
	    // find tile that node <i> is assigned to
	    int tile = -1;
	    //System.err.println("Looking for tile for node #" + i + ": " + nodes.get(i));
	    for (int j=0; j<numTiles; j++) {
		if (almostEqual(sol[pNum(i,j)],1)) {
		    assert (tile==-1) :
				 "This node is assigned to both tile #" + tile + " and tile #" + j + 
				 " (and possibly others): " + nodes.get(i);
		    //System.err.println("  assigned to tile " + j);
		    tile = j;
		}
	    }
	    assert tile!=-1 : "This node is without a tile assigment: " + nodes.get(i);
	    // register node->tile map in result
	    result.put(nodes.get(i), new Integer(tile));

	    // adjust registration of all parents if necessary
	    SIRContainer par = ((SIROperator)nodes.get(i)).getParent();
	    boolean done = false;
	    while (par!=null && !done) {
		if (result.containsKey(par)) {
		    // if parent already given tile, see if it's same
		    // as this one or already -1; if so, quit.  If
		    // not, it must be assigned a different tile, so
		    // mark it as -1.
		    int parTile = ((Integer)result.get(par)).intValue();
		    if (parTile==tile || parTile==-1) {
			done = true;
		    } else {
			result.put(par, new Integer(-1));
		    }
		} else {
		    // if parent not given tile, then give it this tile
		    result.put(par, new Integer(tile));
		}
		par = par.getParent();
	    }
	}
	return result;
    }

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
	return pNum(nodes.size()-1, numTiles) + numTiles * n + t;
    }

    /**
     * Index of z_min for node n.
     */
    private int zMin(int n) {
	return dNum(nodes.size()-1, numTiles) + n;
    }

    /**
     * Index of z_max for node n.
     */
    private int zMax(int n) { 
	return zMin(nodes.size()) + n;
    }

    /**
     * Returns an array holding the values of the variables in the
     * optimum of the partitioning problem.
     */
    private double[] calcSolution() {
	// make the linear program...
	int numVars = zMax(nodes.size());
	// this is a *conservative* estimate of the number of
	// constraints, derived from the paper
	int numConstraints = 1 + (int)(8.25 * numTiles * nodes.size()) + 2*numTiles + nodes.size();
	System.err.println("nodes.size()=" + nodes.size());
	//System.err.println("numVars = " + numVars);
	//System.err.println("numConstraints <= " + numConstraints);
	LinearProgramSolver lp = new CPLEXSolve(numVars, 
						ILPPartitioner.OPTIMAL_TIMEOUT,
						ILPPartitioner.GAP_TOLERANCE,
						ILPPartitioner.GAP_TIMEOUT);
	
	setupObjective(lp);
	setupConstraints(lp);

	// get solution, including objective function
	System.err.println("Solving integer linear program...");
	double[] sol = null;
	sol = CPLEXClient.solveOverRMI((CPLEXSolve)lp);
	assert sol!=null : "Got a null value back from solver.";
	return sol;
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
     * Sets up constraints in <lp>.
     */
    private void setupConstraints(LinearProgram lp) {
	constrainZeroOneVars(lp);
	constrainOneTilePerNode(lp);
	constrainWMax(lp);
	constrainConnectedPartitions(lp);
	constrainHierarchicalPartitions(lp);
	constrainSeparateJoiners(lp);

	// constriants for the sake of speeding up the solution
	// process
	//constrainLinearOrder(lp);
	//constrainSymmetry(lp);
    }

    private void constrainZeroOneVars(LinearProgram lp) {
	// set the indicator variables to be integral and to be
	// between zero and one
	for (int i=0; i<nodes.size(); i++) {
	    for (int j=0; j<numTiles; j++) {
		lp.setBoolVar(pNum(i, j));
		/* now that "bool var" works, we don't need extra constraints
		// P_i,j >= 0
		double[] con = lp.getEmptyConstraint();
		con[pNum(i,j)] = 1;
		lp.addConstraintGE(con, 0);
		// P_i,j <= 1
		con = lp.getEmptyConstraint();
		con[pNum(i,j)] = -1;
		lp.addConstraintGE(con, -1);
		*/
	    }
	}
    }

    private void constrainOneTilePerNode(LinearProgram lp) {
	for (int j=0; j<numTiles; j++) {
	    // dummy start node is not assigned to a tile
	    double[] con = lp.getEmptyConstraint();
	    con[pNum(0,j)] = 1;
	    lp.addConstraintEQ(con, 0);
	    // dummy end node is not assigned to a tile
	    con = lp.getEmptyConstraint();
	    con[pNum(nodes.size()-1,j)] = 1;
	    lp.addConstraintEQ(con, 0);
	}
	// filters/joiners have one tile per node
	for (int i=1; i<nodes.size()-1; i++) {
	     double[] con = lp.getEmptyConstraint();
	     for (int j=0; j<numTiles; j++) {
		 con[pNum(i,j)] = 1;
	     }
	     lp.addConstraintEQ(con, 1);
	}
    }

    private void constrainWMax(LinearProgram lp) {
	for (int i=0; i<numTiles; i++) {
	    double[] con = lp.getEmptyConstraint();
	    con[wmaxNum()] = 1;
	    for (int j=0; j<nodes.size(); j++) {
		Object node = nodes.get(j);
		if (node instanceof SIRFilter) {
		    // for filter, add work estimate of filter
		    con[pNum(j, i)] = -1 * work.getWork((SIRFilter)node);
		} else if (node instanceof SIRJoiner) {
		    // for joiners, add JOINER_WORK_ESTIMATE
		    con[pNum(j, i)] = -1 * ILPPartitioner.JOINER_WORK_ESTIMATE;
		} else {
		    // otherwise we should have a dummy node
		    assert (node instanceof DummyNode) : "Didn't expect node " + node + " of type " + node.getClass();
		}
	    }
	    lp.addConstraintGE(con, 0);
	}
    }

    private void constrainConnectedPartitions(LinearProgram lp) {
	// introduce <d> variables and set them to be greater than
	// diffs of adjacent indicator vars
	for (int i=0; i<numTiles; i++) {
	    for (int j=0; j<nodes.size()-1; j++) {
		// d_j,i >= (P_j,i - P_j+1,i)
		double[] con = lp.getEmptyConstraint();
		con[dNum(j,i)] = 1;
		con[pNum(j,i)] = -1;
		con[pNum(j+1,i)] = 1;
		lp.addConstraintGE(con, 0);
		// d_j,i >= -1 * (P_L[j],i - P_L[j+1],i)
		con = lp.getEmptyConstraint();
		con[dNum(j,i)] = 1;
		con[pNum(j,i)] = 1;
		con[pNum(j+1,i)] = -1;
		lp.addConstraintGE(con, 0);
	    }
	}
	// forall i in [0,numTiles-1], sum_{j=0}^{|L|-2} d_j,i <= 2
	for (int i=0; i<numTiles; i++) {
	    double[] con = lp.getEmptyConstraint();
	    for (int j=0; j<nodes.size()-1; j++) {
		con[dNum(j,i)] = -1;
	    }
	    lp.addConstraintGE(con, -2);
	}
    }

    private void constrainHierarchicalPartitions(LinearProgram lp) {
	for (Iterator it = first.keySet().iterator(); it.hasNext(); ) {
	    SIRStream s = (SIRStream)it.next();
	    // ignore filters
	    if (!(s instanceof SIRFilter)) {
		int begin = ((Integer)first.get(s)).intValue();
		int end = ((Integer)last.get(s)).intValue();
		for (int t=0; t<numTiles; t++) {
		    // forall s, forall t, P_L[first(s)],t = P_L[first(s)-1],t = 1 ==> P_L[last(s)],t = 1
		    addEqualImplication(lp, pNum(begin,t), pNum(begin-1,t), pNum(end,t));
		    // forall s, forall t, P_L[last(s)],t = P_L[last(s)+1],t =1 ==> P_L[first(s)],t = 1
		    addEqualImplication(lp, pNum(end, t), pNum(end+1, t), pNum(begin,t));
		}
	    }
	}
    }

    private void constrainSeparateJoiners(LinearProgram lp) {
	for (Iterator it = first.keySet().iterator(); it.hasNext(); ) {
	    SIRStream s = (SIRStream)it.next();
	    if (s instanceof SIRSplitJoin) {
		SIRSplitJoin sj = (SIRSplitJoin)s;
		int join = ((Integer)last.get(sj)).intValue();
		// forall t, forall i in [0, sj.size()], P_last(s_i),t != P_last(s_{i+1}) ==> P_join,t = 0
		for (int i=0; i<sj.size()-1; i++) {
		    assert (last.containsKey(sj.get(i))) : "Item missing from last: " + sj.get(i);
		    assert (last.containsKey(sj.get(i+1))) : "Item missing from last: " + sj.get(i+1);
		    int last1 = ((Integer)last.get(sj.get(i))).intValue();
		    int last2 = ((Integer)last.get(sj.get(i+1))).intValue();
		    for (int t=0; t<numTiles; t++) {
			addNotEqualImplication(lp, pNum(last1,t), pNum(last2,t), pNum(join,t));
		    }
		}
	    } else if (s instanceof SIRFeedbackLoop) {
		SIRFeedbackLoop fl = (SIRFeedbackLoop)s;
		int join = ((Integer)first.get(fl)).intValue();
		int lastLoop = ((Integer)last.get(fl.getLoop())).intValue();
		for (int t=0; t<numTiles; t++) {
		    addNotEqualImplication(lp, pNum(lastLoop,t), pNum(join-1,t), pNum(join,t));
		}
	    }
	}
    }

    /**
     * Constrain the partition numbers to be assigned in order of the
     * nodes array, just to constrain the search space.
     */
    private void constrainLinearOrder(LinearProgram lp) {
	double[] con;
	// constrain the forward-looking direction -----------------
	// constrain first node to be on first partition
	con = lp.getEmptyConstraint();
	con[pNum(1, 0)] = 1;
	lp.addConstraintEQ(con, 1);
	// constrain other nodes to be in increasing order
	for (int i=1; i<nodes.size()-2; i++) {
	    for (int j=0; j<numTiles-1; j++) {
		con = lp.getEmptyConstraint();
		con[pNum(i, j)] = -1;
		con[pNum(i+1, j)] = 1;
		con[pNum(i+1, j+1)] = 1;
		lp.addConstraintGE(con, 0);
	    }
	}

	// constrain the backward-looking direction -----------------
	// constrain last node to be on last partition
	/* don't do this because it disallows having < 16 partitions
	con = lp.getEmptyConstraint();
	con[pNum(nodes.size()-2, numTiles-1)] = 1;
	lp.addConstraintEQ(con, 1);
	 */
	// constrain other nodes to be in increasing order
	for (int i=2; i<nodes.size(); i++) {
	    for (int j=1; j<numTiles; j++) {
		con = lp.getEmptyConstraint();
		con[pNum(i, j)] = -1;
		con[pNum(i-1, j)] = 1;
		con[pNum(i-1, j-1)] = 1;
		lp.addConstraintGE(con, 0);
	    }
	}

	// further constrain that we can't wrap-around to have the end
	// tile next to the beginning tile.  that means that you can
	// NOT have P(i, MAX) and P(i+1, 0) both equal to 1.  So the
	// sum must be less than or equal to 1.
	for (int i=1; i<nodes.size()-2; i++) {
	    con = lp.getEmptyConstraint();
	    con[pNum(i, numTiles-1)] = -1;
	    con[pNum(i+1, 0)] = -1;
	    lp.addConstraintGE(con, -1);
	}
    }

    private void constrainSymmetry(LinearProgram lp) {
	// need to find parallel streams with same amount of work.
	// start by looking for splitjoins, then compare adjacent
	// children.
	for (Iterator it = first.keySet().iterator(); it.hasNext(); ) {
	    SIRStream str = (SIRStream)it.next();
	    // ignore filters
	    if (str instanceof SIRSplitJoin) {
		SIRSplitJoin sj = (SIRSplitJoin)str;
		// look for pairwise adjacent children that are the
		// same.  keep track if they're all the same also.
		boolean allEquiv = true;
		for (int i=0; i<sj.size()-1; i++) {
		    SIRStream child1 = sj.get(i);
		    SIRStream child2 = sj.get(i+1);
		    if (equivStructure(child1, child2)) {
			constrainSymmetricalChildren(lp, child1, child2);
		    } else {
			allEquiv = false;
		    }
		}
		// if all were equivalent, then constrain an even
		// split between them
		if (allEquiv) {
		    constrainEvenSplit(sj, lp);
		}
	    }
	}
    }

    private void constrainEvenSplit(SIRSplitJoin sj, LinearProgram lp) {
	System.err.println("Constraining even split in " + sj.getName() + ".");
	// for each tile, constrain the sum of that tile over the
	// children of <sj> to be less than and greater than zMin and
	// zMax, respectively.  Note that zMin and zMax (local to this
	// constraint) are indexed by last(sj).  (Shouldn't be
	// first(sj) since this could cause collisions.)
	int z = ((Integer)last.get(sj)).intValue();
	// constrain bounds of zmin and zmax
	for (int i=0; i<numTiles; i++) {
	    double[] con1 = lp.getEmptyConstraint();
	    double[] con2 = lp.getEmptyConstraint();
	    for (int j=0; j<sj.size(); j++) {
		int childIndex = ((Integer)last.get(sj.get(j))).intValue();
		// constrain zmin
		con1[pNum(childIndex, i)] = 1;
		// constrain zmax
		con2[pNum(childIndex, i)] = -1;
	    }
	    con1[zMin(z)] = -1;
	    con2[zMax(z)] = 1;
	    lp.addConstraintGE(con1, 0);
	    lp.addConstraintGE(con2, 0);
	}
	// constrain zmax - zmin <= 1
	double[] con = lp.getEmptyConstraint();
	con[zMin(z)] = 1;
	con[zMax(z)] = -1;
	lp.addConstraintGE(con, -1);
    }

    private void constrainSymmetricalChildren(LinearProgram lp, SIRStream child1, SIRStream child2) {
	System.err.println("Detected symmetry between " + child1.getName() + " and " + child2.getName());
	// get beginning index
	int first1 = ((Integer)first.get(child1)).intValue();
	int first2 = ((Integer)first.get(child2)).intValue();
	// get size
	int size =  ((Integer)last.get(child1)).intValue() - first1;

	// for all pairs of internal nodes in <child1> and <child2>
	for (int i=0; i<size-1; i++) {
	    double[] con = lp.getEmptyConstraint();
	    // f_L[first2+i+1],L[first2+i] = f_L[first1+i+1],L[first1+i]
	    // f_L[first2+i+1],L[first2+i] - f_L[first1+i+1],L[first1+i] = 0
	    // which means
	    //    sum_j=1^num_t j * P(first2+i+1,j)                    (1)
	    //  + sum_j=1^num_t j * P(first1+i,j)                      (2)
	    //  - sum_j=1^num_t j * P(first2+i,j)                      (3)
	    //  - sum_j=1^num_t j * P(first1+i+1,j) >= 0               (4)
	    for (int j=0; j<numTiles; j++) {
		con[pNum(first2+i+1,j)] = j;
		con[pNum(first1+i,j)] = j;
		con[pNum(first2+i,j)] = -j;
		con[pNum(first1+i+1,j)] = -j;
	    }
	    lp.addConstraintEQ(con, 0);
	}
    }

    /**
     * Adds to <lp> a constraint that guarantees the following:
     *
     *  var[<lhs1>] = var[<lhs2>] = 1 ==> var[rhs] = 1
     *
     * Requires that var[<lhs1>], var[<lhs2>], and var[<rhs>] are
     * elsewhere constrainted to be zero-one vars in <lp>.
     */
    private void addEqualImplication(LinearProgram lp, int lhs1, int lhs2, int rhs) {
	// lhs1 + lhs2 - 1 <= rhs
	double[] con = lp.getEmptyConstraint();
	con[lhs1] = -1;
	con[lhs2] = -1;
	con[rhs] = 1;
	lp.addConstraintGE(con, -1);
    }

    /**
     * Adds to <lp> a constraint that guarantees the following:
     *
     *  var[<lhs1>] != var[<lhs2>] ==> var[rhs] = 0
     *
     * Requires that var[<lhs1>], var[<lhs2>], and var[<rhs>] are
     * elsewhere constrainted to be zero-one vars in <lp>.
     */
    private void addNotEqualImplication(LinearProgram lp, int lhs1, int lhs2, int rhs) {
	// 1 - rhs >= (lhs1 - lhs2)
	double[] con = lp.getEmptyConstraint();
	con[lhs1] = -1;
	con[lhs2] = 1;
	con[rhs] = -1;
	lp.addConstraintGE(con, -1);
	// 1 - rhs >= - (lhs1 - lhs2)
	con = lp.getEmptyConstraint();
	con[lhs1] = 1;
	con[lhs2] = -1;
	con[rhs] = -1;
	lp.addConstraintGE(con, -1);
    }
}
