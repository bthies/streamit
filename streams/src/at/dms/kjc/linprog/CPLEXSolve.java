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
public class CPLEXSolve extends SimpleLinearProgram implements LinearProgramSolver, Serializable {
    /**
     * The optimal and gap timeouts for this.
     */
    private long optTimeout;
    private long gapTimeout;
    /**
     * The maximum allowable gap (for times between optTimeout and
     * gapTimeout)
     */
    private double gapTolerance;
    /**
     * Maximum possible timeout value.
     */
    private static final long MAX_TIMEOUT = Long.MAX_VALUE;

    /**
     * Create one of these with <numVars> variables with a timeout of
     * <timeout> for the solving process.
     */
    public CPLEXSolve(int numVars, 
                      long optTimeout,
                      double gapTolerance,
                      long gapTimeout) {
        super(numVars);
        this.optTimeout = optTimeout;
        this.gapTolerance = gapTolerance;
        this.gapTimeout = gapTimeout;
    }

    /**
     * Create one of these with <numVars> variables with no timeout.
     */
    public CPLEXSolve(int numVars) {
        this(numVars, MAX_TIMEOUT, 0, MAX_TIMEOUT);
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
        if (this.optTimeout!=MAX_TIMEOUT) {
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
        Constraint[] con = constraints.toArray(new Constraint[0]);
        for (int i=0; i<con.length; i++) {
            // use .equals instead of object equality because of serialization issues
            if (con[i].type.equals(ConstraintType.GE)) {
                model.add(model.addGe(model.scalProd(x, con[i].lhs), con[i].rhs));
            } else if (con[i].type.equals(ConstraintType.EQ)) {
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
        TimeoutCallback tc = new TimeoutCallback(this.optTimeout, this.gapTolerance, this.gapTimeout, x);
        try {
            model.use(tc);
            // export for sake of debugging
            model.exportModel("partitions.lp");

            System.err.println("trying to solve model");
            if (model.solve()) {
                System.err.println("done (true)");
                result = model.getValues(x);
                System.err.println("Objective function in optimum: " + model.getObjValue());
            } else {
                System.err.println("done (false)");
                throw new LPSolverFailedException("CPLEX returned false from IloCplex.solve()");
            }
        } catch (IloException e) {
            System.err.println("done (exception)");
            // we end up here if we aborted the solution for a
            // timeout.  See if this is the case and fail if it isn't.
            result = tc.getBestSolution();
            System.err.println("Objective function of best solution: " + tc.getBestObjective());
            // if we hadn't found one yet, then there's still a problem
            if (result==null) {
                throw new LPSolverFailedException("CPLEX solver failed due to its own exception: " + e);
            }
        }

        model.end();
        return result;
    }

    static class ModelAndVars {
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

    static class TimeoutCallback extends IloCplex.MIPCallback {
        /**
         * Optimal  and  Absolute  timeout  for this  (in  millis).
         */
        private long optTimeoutMillis;
        private long gapTimeoutMillis;
        /**
         * Gap tolerance. (see ILPPartitioner)
         */
        private double gapTolerance;
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
        /**
         * The best value of the objective function that we've found so
         * far.
         */
        private double obj;

        public TimeoutCallback(long optTimeout, double gapTolerance, long gapTimeout, IloNumVar[] x) {
            this.optTimeoutMillis = 1000*optTimeout;
            this.gapTolerance = gapTolerance;
            this.gapTimeoutMillis = 1000*gapTimeout;
            this.x = x;
            this.startMillis = -1;
            this.sol = null;
            this.obj = -1;
        }

        protected void main() {
            // if we haven't found a solution, keep looking
            if (!hasIncumbent()) {
                return;
            }
            // otherwise, see if we're satisfied. if so, keep track of the
            // best solution and abort.
            if (satisfied()) {
                sol = getIncumbentValues(x);
                obj = getIncumbentObjValue();
                abort();
            }
        }

        /**
         * Given that this has some solution, returns whether or not this
         * has a "satisfactory" solution.  This will be the case if
         * either:
         *
         * 1) Elapsed time exceeds optTimeout and the gap is better than
         * gap_tolerance
         *
         * 2) Elapsed time exceeds gapTimeout.
         *
         */
        private boolean satisfied() {
            long elapsed = getElapsedMillis();
            if (elapsed < optTimeoutMillis) {
                return false;
            } else if (getGap() < gapTolerance) {
                System.err.println("Stopping search because GAP of " + Utils.asPercent(getGap()) + 
                                   " is less than tolerance of " + Utils.asPercent(gapTolerance));
                return true;
            } else if (elapsed > gapTimeoutMillis) {
                System.err.println("Stopping search because elapsed time exceeded limit of " + 
                                   (gapTimeoutMillis/1000) + " secs.");
                return true;
            } else {
                return false;
            }
        }

        /**
         * Returns current gap between best integer solution and solver's
         * lower bound on the best possible solution.
         */
        private double getGap() {
            double best = getBestObjValue();
            double cur  = getIncumbentObjValue();
            return (cur - best) / cur;
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

        /**
         * Returns best objective value found in solving process, or -1 if
         * none was found.
         */
        public double getBestObjective() {
            return obj;
        }

    }
}

