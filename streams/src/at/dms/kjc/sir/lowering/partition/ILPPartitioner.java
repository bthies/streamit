package at.dms.kjc.sir.lowering;

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
	    fuseAll();
	}

	// dump the final graph
	StreamItDot.printGraph(str, "after.dot");
	System.err.println("Done with ILP Partitioner.");
    }
    
    private void fuseAll() {
	CalcPartitions.doit(str, numTiles);
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
	}
	for (int i=0; i<tileWork.length; i++) {
	    System.err.println("tile " + i + " has work:\t" + tileWork[i]);
	    System.err.print(tileContents[i]);
	}
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
	    for (int j=0; j<numTiles; j++) {
		if (sol[pNum(i,j)]==1) {
		    //System.err.println("Node " + nodes.get(i) + " assigned to tile " + j);
		    tile = j;
		    break;
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
	System.err.println("numVars = " + numVars);
	System.err.println("numConstraints <= " + numConstraints);
	LinearProgramSolver lp = new GLPKSolve(numVars);
	
	setupObjective(lp);
	setupConstraints(lp);

	// get solution, including objective function
	System.err.print("Solving integer linear program...");
	double[] sol = lp.solve();
	System.err.println("done.");
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
	// set the indicator variables to be integral, and to be
	// between zero and one (we need extra constraints since there
	// isn't a "boolean" var type in the interace.)
	for (int i=0; i<nodes.size(); i++) {
	    for (int j=0; j<numTiles; j++) {
		lp.setBoolVar(pNum(i, j));
		// P_i,j >= 0
		double[] con = lp.getEmptyConstraint();
		con[pNum(i,j)] = 1;
		lp.addConstraintGE(con, 0);
		// P_i,j <= 1
		con = lp.getEmptyConstraint();
		con[pNum(i,j)] = -1;
		lp.addConstraintGE(con, -1);
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
		int end = ((Integer)first.get(s)).intValue();
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

    /**
     * Override to color filters.
     */
    public String makeLabelledNode(String label)
    {
        String name = getName();
        if (label == null) label = name;
        print(name + " [ color=blue, style=filled, label=\"" + label + "\" ]\n");
        return name;
    }
    

    /**
     * Override to color partitions.
     */
    public String getClusterString(SIRStream self) {
	// if we have a linear rep of this object, color the resulting dot graph rose.
	Utils.assert(partitions.containsKey(self), "No partition for " + self);
	if (((Integer)partitions.get(self)).intValue()!=-1) {
	    return "subgraph cluster_" + getName() + " {" + 
		"\n color=blue;\n style=filled;" + 
		"\n label=\"" + self.getIdent() + "\";\n";
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
