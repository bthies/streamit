package at.dms.kjc.sir.lowering;

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
import at.dms.kjc.sir.lowering.fusion.*;
import at.dms.kjc.sir.lowering.fission.*;

public class ILPPartitioner {
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
	    partitionAndFuse();
	}

	// dump the final graph
	StreamItDot.printGraph(str, "after.dot");
	System.err.println("Done with ILP Partitioner.");
    }
    
    private void partitionAndFuse() {
	HashMap partitions = CalcPartitions.doit(str, numTiles);
	str.accept(new ILPFuser(partitions));
    }

}

/*
  This is the class that performs the fusion dictated by the
  partitioner.  The general strategy is this:

  1. make copy of children so you can look them up later
  
  2. visit each of children and replace any of them with what they returned
  
  3. fuse yourself (or pieces of yourself) according to hashmap for your original kids

  4. return the new version of yourself
*/
class ILPFuser extends EmptyAttributeStreamVisitor {
    /**
     * The partition mapping.
     */
    private HashMap partitions;

    public ILPFuser(HashMap partitions) {
	this.partitions = partitions;
    }

    /******************************************************************/
    // local methods for the ILPFuser

    /**
     * Visits/replaces the children of <cont>
     */
    private void replaceChildren(SIRContainer cont) {
	// visit children
	for (int i=0; i<cont.size(); i++) {
	    SIRStream newChild = (SIRStream)cont.get(i).accept(this);
	    cont.set(i, newChild);
	    // if we got a pipeline, try lifting it.  note that this
	    // will mutate the children array and the init function of
	    // <self>
	    if (newChild instanceof SIRPipeline) {
		Lifter.eliminatePipe((SIRPipeline)newChild);
	    }
	}
    }

    /**
     * Returns an array suitable for the fusers that indicates the
     * groupings of children into partitions, according to
     * this.partitions.  For instance, if input is:
     *
     *  <children> = {0, 0, 5, 7, 7, 7}
     *
     * then output is {2, 1, 3}
     */
    private int[] calcChildPartitions(List children) {
	List resultList = new LinkedList();
	int pos = 0;
	while (pos<children.size()) {
	    int count = 0;
	    int cur = getPartition(children.get(pos));
	    do {
		pos++;
		count++;
	    } while (pos<children.size() && 
		     getPartition(children.get(pos))==cur && 
		     // don't conglomerate -1 children, as they are
		     // containers with differing tile content
		     cur!=-1);
	    resultList.add(new Integer(count));
	}
	// copy results into int array
	int[] result = new int[resultList.size()];
	for (int i=0; i<result.length; i++) {
	    result[i] = ((Integer)resultList.get(i)).intValue();
	}
	return result;
    }

    /******************************************************************/
    // these are methods of empty attribute visitor

    /* visit a pipeline */
    public Object visitPipeline(SIRPipeline self,
			 JFieldDeclaration[] fields,
			 JMethodDeclaration[] methods,
			 JMethodDeclaration init) {
	//System.err.println("visiting " + self);
	// build partition array based on orig children
	int[] childPart = calcChildPartitions(self.getChildren());
	// replace children
	replaceChildren(self);
	// fuse children internally
	FusePipe.fuse(self, childPart);
	return self;
    }

    /* visit a splitjoin */
    public Object visitSplitJoin(SIRSplitJoin self,
			  JFieldDeclaration[] fields,
			  JMethodDeclaration[] methods,
			  JMethodDeclaration init,
			  SIRSplitter splitter,
			  SIRJoiner joiner) {
	//System.err.println("visiting " + self);
	// build partition array based on orig children
	int[] childPart = calcChildPartitions(self.getParallelStreams());
	// replace children
	replaceChildren(self);
	// fuse
	return FuseSplit.fuse(self, childPart);
    }

    /* visit a feedbackloop */
    public Object visitFeedbackLoop(SIRFeedbackLoop self,
			     JFieldDeclaration[] fields,
			     JMethodDeclaration[] methods,
			     JMethodDeclaration init,
			     JMethodDeclaration initPath) {
	//System.err.println("visiting " + self);
	// fusing a whole feedback loop isn't supported yet
	Utils.assert(getPartition(self)==-1);
	// replace children
	replaceChildren(self);
	return self;
    }

    /******************************************************************/

    /**
     * Returns int partition for <str>
     */
    private int getPartition(Object str) {
	Utils.assert(partitions.containsKey(str), 
		     "No partition recorded for: " + str);
	return ((Integer)partitions.get(str)).intValue();
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
	HashMap result = buildPartitionMap(sol);
	printTileWork(result);
	PartitionDot.printGraph(str, "partitions.dot", result);
	return result;
    }

    private void printTileWork(HashMap partitions) {
	int[] tileWork = new int[numTiles];

	String[] tileContents = new String[numTiles];
	for (int i=0; i<numTiles; i++) {
	    tileContents[i] = "";
	}

	int maxWork = -1;
	for (int i=1; i<nodes.size()-1; i++) {
	    Object node = nodes.get(i);
	    int tile = ((Integer)partitions.get(node)).intValue();
	    tileContents[tile] += "  " + ((SIROperator)node).getName() + "\n";
	    if (node instanceof SIRFilter) {
		// for filter, add work estimate of filter
		tileWork[tile] += work.getWork((SIRFilter)node);
	    } else if (node instanceof SIRJoiner) {
		// for joiners, add JOINER_WORK_ESTIMATE
		tileWork[tile] += ILPPartitioner.JOINER_WORK_ESTIMATE;
	    }
	    // keep track of max work
	    if (tileWork[tile]>maxWork) {
		maxWork = tileWork[tile];
	    }
	}

	// print each tile's work
	double totalUtil = 0;
	for (int i=0; i<tileWork.length; i++) {
	    double util = ((double)tileWork[i]) / ((double)maxWork);
	    totalUtil += util / ((double)tileWork.length);
	    System.err.println("tile " + i + " has work:\t" + tileWork[i] 
			       + "\t Estimated utilization:\t" + Utils.asPercent(util));
	    System.err.print(tileContents[i]);
	}

	System.err.println("Estimated total utilization: " + Utils.asPercent(totalUtil));
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
		    Utils.assert(tile==-1, 
				 "This node is assigned to both tile #" + tile + " and tile #" + j + 
				 " (and possibly others): " + nodes.get(i));
		    //System.err.println("  assigned to tile " + j);
		    tile = j;
		}
	    }
	    Utils.assert(tile!=-1, "This node is without a tile assigment: " + nodes.get(i));
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
		    last.put(self, new Integer(nodes.size()-1));
		}

		public void visitFilter(SIRFilter self,
					SIRFilterIter iter) {
		    preVisitStream(self, iter);
		    nodes.add(self);
		    postVisitStream(self, iter);
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
	return pNum(nodes.size()-1, numTiles) + numTiles * n + t;
    }

    /**
     * Returns an array holding the values of the variables in the
     * optimum of the partitioning problem.
     */
    private double[] calcSolution() {
	// make the linear program...
	int numVars = dNum(nodes.size()-1, numTiles);
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
	Utils.assert(sol!=null, "Got a null value back from solver.");
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
		    Utils.assert(node instanceof DummyNode);
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
		    Utils.assert(last.containsKey(sj.get(i)), "Item missing from last: " + sj.get(i));
		    Utils.assert(last.containsKey(sj.get(i+1)), "Item missing from last: " + sj.get(i+1));
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

/**
 * This class extends the main streamit dot printer to annotate the
 * dot graphs with partitioning information. 
 **/
class PartitionDot extends StreamItDot {
    private HashMap partitions;

    public PartitionDot(PrintStream outputstream,
			HashMap partitions) {
	super(outputstream);
	this.partitions = partitions;
    }

    /* visit a filter */
    public Object visitFilter(SIRFilter self,
                              JFieldDeclaration[] fields,
                              JMethodDeclaration[] methods,
                              JMethodDeclaration init,
                              JMethodDeclaration work,
                              CType inputType, CType outputType)
    {
        // Return a name pair with both ends pointing to this.
	//        return new NamePair(makeLabelledNode(self.getRelativeName()));
	String label = self.getName();
	label += "\\ntile=" + ((Integer)partitions.get(self)).intValue();
	return new NamePair(makeLabelledNode(label));
    }

    /* visit a joiner */
    public Object visitJoiner(SIRJoiner self,
                              SIRJoinType type,
                              JExpression[] expWeights)
    {
	String label = type.toString();
	// try to add weights to label
	try {
	    int[] weights = self.getWeights();
	    label += "(";
	    for (int i=0; i<weights.length; i++) {
		label += weights[i];
		if (i!=weights.length-1) {
		    label+=",";
		}
	    }
	    label += ")";
	} catch (Exception e) {}
	label += "\\ntile=" + ((Integer)partitions.get(self)).intValue();
        return new NamePair(makeLabelledNode(label));
    }
    
    /**
     * Override to show partitions.
     */
    public String getClusterString(SIRStream self) {
	// if we have a linear rep of this object, color the resulting dot graph rose.
	Utils.assert(partitions.containsKey(self), "No partition for " + self);
	int tile = ((Integer)partitions.get(self)).intValue();
	if (tile!=-1) {
	    return "subgraph cluster_" + getName() + " {" + 
		"\n label=\"" + self.getIdent() + "\\n tile=" + tile + "\";\n";
	} else {
	    // otherwise, return boring white
	    return "subgraph cluster_" + getName() + " {" + 
		"\n label=\"" + self.getIdent() + "\";\n";
	}
    }

    /**
     * Prints dot graph of <str> to <filename>.
     */
    public static void printGraph(SIRStream str, String filename,
				  HashMap partitions) {
	try {
	    FileOutputStream out = new FileOutputStream(filename);
	    StreamItDot dot = new PartitionDot(new PrintStream(out), partitions);
	    dot.print("digraph streamit {\n");
	    str.accept(dot);
	    dot.print("}\n");
	    out.flush();
	    out.close();
	} catch (IOException e) {
	    e.printStackTrace();
	}
    }
}
