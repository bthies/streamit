package at.dms.kjc.linprog;

import ilog.concert.*;
import ilog.cplex.*;

import at.dms.util.*;

import java.io.*;

/**
 * Solver that uses CPLEX.  Current limitations:
 *
 * 1. Not making use of double variable bounds.  Just saying between
 *     MIN_VALUE, MAX_VALUE
 */
public class CPLEXSolve extends SimpleLinearProgram implements LinearProgramSolver {
    /**
     * The timeout for the solver in this.
     */
    private long timeout;
    /**
     * The max timeout.
     */
    private static final long MAX_TIMEOUT = Long.MAX_VALUE;

    /**
     * Create one of these with <numVars> variables with a timeout of
     * <timeout> for the solving process.
     */
    public CPLEXSolve(int numVars, long timeout) {
	super(numVars);
	this.timeout = timeout;
    }

    /**
     * Create one of these with <numVars> variables with no timeout.
     */
    public CPLEXSolve(int numVars) {
	this(numVars, MAX_TIMEOUT);
    }

    /**
     * Constructs a CPLEX linear program from the state that has been
     * accumulated, to get ready for solving.
     */
    private ModelAndVars setupModel() throws IloException, IOException {
	// make model
	IloCplex model = new IloCplex();

	// if there's any timeout, put emphasis on finding a feasible
	// solution quickly instead of proving the optimality of the
	// solution.
	if (this.timeout!=MAX_TIMEOUT) {
	    model.setParam(IloCplex.IntParam.MIPEmphasis, IloCplex.MIPEmphasis.Feasibility);
	}
	
	// construct types
	IloNumVarType[] types = new IloNumVarType[numVars];
	for (int i=0; i<numVars; i++) {
	    if (boolVar[i]) {
		types[i] = IloNumVarType.Bool;
	    } else {
		types[i] = IloNumVarType.Float;
	    }
	}

	// construct bounds for vars
	double[] lb = new double[numVars];
	double[] ub = new double[numVars];
	for (int i=0; i<numVars; i++) {
	    // give 0-1 range for bool vars solely to avoid warning
	    // from cplex; the important part of the bool var is the
	    // type decided above.
	    if (boolVar[i]) {
		lb[i] = 0.0;
		ub[i] = 1.0;
	    } else {
		lb[i] = Double.MIN_VALUE;
		ub[i] = Double.MAX_VALUE;
	    }
	}

	// construct vars <x>
	IloNumVar[] x = model.numVarArray(numVars, lb, ub, types);

	// construct objective function
	model.addMinimize(model.scalProd(x, obj));

	// add constraints
	Constraint[] con = (Constraint[])constraints.toArray(new Constraint[0]);
	for (int i=0; i<con.length; i++) {
	    if (con[i].type==ConstraintType.GE) {
		model.add(model.addGe(model.scalProd(x, con[i].lhs), con[i].rhs));
	    } else if (con[i].type==ConstraintType.EQ) {
		model.add(model.addEq(model.scalProd(x, con[i].lhs), con[i].rhs));
	    } else {
		Utils.fail("Unrecognized constraint type: " + con[i].type);
	    }
	}

	return new ModelAndVars(model, x);
    }

    /**
     * Solve the program and return the value of the the variables
     * (indices 0...numVars-1) in the optimum.
     */
    public double[] solve() throws LPSolverFailedException {
	// setup model
	ModelAndVars mv = null;
	try {
	    mv = setupModel();
	} catch (IloException e) {
	    throw new LPSolverFailedException("CPLEX setup failed due to its own exception: " + e);
	} catch (IOException e) {
	    throw new LPSolverFailedException("CPLEX setup failed due to I/O error: " + e);
	}
	IloCplex model = mv.model;
	IloNumVar[] x = mv.x;

	// try solving model
	double[] result;
	// add a callback to keep track of timeout
	TimeoutCallback tc = new TimeoutCallback(this.timeout, x);
	try {
	    model.use(tc);

	    if (model.solve()) {
		result = model.getValues(x);
	    } else {
		throw new LPSolverFailedException("CPLEX returned false from IloCplex.solve()");
	    }
	} catch (IloException e) {
	    // we end up here if we aborted the solution for a
	    // timeout.  See if this is the case and fail if it isn't.
	    result = tc.getBestSolution();
	    // if we hadn't found one yet, then there's still a problem
	    if (result==null) {
		throw new LPSolverFailedException("CPLEX solver failed due to its own exception: " + e);
	    }
	}

	model.end();
	return result;
    }
}

class ModelAndVars {
    /**
     * The cplex model.
     */
    public final IloCplex model;
    /**
     * The variables.
     */
    public final IloNumVar[] x;

    public ModelAndVars(IloCplex model, IloNumVar[] x) {
	this.model = model;
	this.x = x;
    }
}

class TimeoutCallback extends IloCplex.MIPCallback {
    /**
     * Timeout for this (in millis).
     */
    private long timeoutMillis;
    /**
     * Starting time of this, in milliseconds.  (Before set, should be
     * -1).
     */
    private long startMillis;
    /**
     * The variables we're solving for.
     */
    private IloNumVar[] x;
    /**
     * The best solution we've found so far.  This will be null before
     * we've found a solution.
     */
    private double[] sol;

    public TimeoutCallback(long timeout, IloNumVar[] x) {
	this.timeoutMillis = 1000*timeout;
	this.x = x;
	this.startMillis = -1;
	this.sol = null;
    }

    protected void main() {
	// if we haven't found a solution, keep looking
	if (!hasIncumbent()) {
	    return;
	}
	// otherwise, see if we've timed out.  if so, keep track of
	// the best solution and abort.
	if (hasTimedOut()) {
	    System.err.println("Found a solution, but will stop looking due to " + 
			       (timeoutMillis/1000) + "-sec timeout.  " +
			       "Spent " + (getElapsedMillis()/1000) + " secs.");
	    sol = getIncumbentValues(x);
	    abort();
	}
    }

    /**
     * Returns whether or not this has timed out.
     */
    private boolean hasTimedOut() {
	return getElapsedMillis() > timeoutMillis;
    }

    private long getElapsedMillis() {
	// check start time
	if (startMillis==-1) {
	    startMillis = System.currentTimeMillis();
	}
	return System.currentTimeMillis() - startMillis;
    }

    /**
     * Returns best solution found in solving process, or null if none
     * was found.
     */
    public double[] getBestSolution() {
	return sol;
    }

}

