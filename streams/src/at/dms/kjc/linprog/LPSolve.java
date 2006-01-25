package at.dms.kjc.linprog;

import lpsolve.lprec;
import lpsolve.constant;
import lpsolve.solve;

import at.dms.util.*;

/**
 * This is an integer linear program solver based on the Java port of
 * lp_solve 2.0, available from:
 * 
 * http://www.cs.wustl.edu/~javagrp/help/LinearProgramming.html
 *
 */
public class LPSolve implements LinearProgramSolver {
    private final int numVars;
    private final int numConstraints;
    private final lprec lp;
    private final solve solver;

    /**
     * Construct an LP with <numConstraints> constraints and <numVars>
     * variables.
     */
    public LPSolve(int numConstraints, int numVars) {
        this.numConstraints = numConstraints;
        this.numVars = numVars;
        this.lp = new lprec(numConstraints, numVars);
        this.solver = new solve();
    }

    /**
     * Returns an array with a zero-entry for each variable in the
     * linear program (which the client can then fill in with
     * coefficients before checking in as a new constraint.)
     */
    public double[] getEmptyConstraint() {
        return new double[wrap(numVars)];
    }

    /**
     * Sets the objective function to be <obj>.
     */
    public void setObjective(double[] obj) {
        solver.set_obj_fn(lp, wrap(obj));
    }

    /**
     * Constrains the i'th variable of this to be an integer.
     */
    public void setBoolVar(int i) {
        solver.set_int(lp, wrap(i), constant.TRUE);
    }

    /**
     * Adds a greater-than-or-equal constraint between the variables
     * with coefficients <constraint> and the right-hand-side <rhs>.
     *
     * That is, <constraint> <dot> <variables> >= <rhs>. 
     */
    public void addConstraintGE(double[] constraint, double rhs) {
        solver.add_constraint(lp, wrap(constraint), constant.GE, rhs);
    }

    /**
     * Adds an equality constraint between the variables with
     * coefficients <constraint> and the right-hand-side <rhs>.
     */
    public void addConstraintEQ(double[] constraint, double rhs) {
        solver.add_constraint(lp, wrap(constraint), constant.EQ, rhs);
    }
    
    /**
     * Solve the program and return the value of the the variables
     * (indices 0...numVars-1) in the optimum.
     */
    public double[] solve() {
        solver.dosolve(lp);
        double[] result = new double[numVars];
        for (int i=0; i<numVars; i++) {
            result[i] = lp.getBestSolution(lp.getRows()+i+1);
        }
        return result;
    }

    /**
     * Wraps the index <i> to interface with lp_solve, since it seems
     * to be off by one in the way it expects some indices.
     */
    private int wrap(int i) {
        return i+1;
    }
    
    /**
     * Shifts everything up by one in <arr> to interface to lp_solve.
     * Mutates the argument and also returns it.
     */
    private double[] wrap(double[] arr) {
        // make sure user isn't expecting arr[arr.length-1] to be
        // preserved
        if (arr[arr.length-1]!=0) {
            System.err.println("Warning: user set a coefficient for a variable that doesn't exist.");
        }
        // shift everything up
        for (int i=arr.length-1; i>0; i--) {
            arr[i] = arr[i-1];
        }
        arr[0] = 0;
        return arr;
    }
    
}
