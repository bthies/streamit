package at.dms.kjc.linprog;

import java.io.*;
import java.util.*;

/**
 * A simple implementation of the LinearProgram interface; keeps track
 * of all the information that has been passed in with local fields.
 */
public class SimpleLinearProgram implements LinearProgram, Serializable {
    /**
     * Number of variables.
     */
    protected int numVars;
    /**
     * Rows of Constraints.
     */
    protected List<Constraint> constraints; 
    /**
     * The objective function.
     */
    protected double[] obj;
    /**
     * An array indicating whether or not a given variable should be
     * constrained to be zero-one.
     */
    protected boolean[] boolVar;
   
    /**
     * Create one of these with <numVars> variables.
     */
    public SimpleLinearProgram(int numVars) {
        this.numVars = numVars;
        this.boolVar = new boolean[numVars];
        this.constraints = new LinkedList<Constraint>();
    }

    /**
     * Returns an array with a zero-entry for each variable in the
     * linear program (which the client can then fill in with
     * coefficients before checking in as a new constraint.)
     */
    public double[] getEmptyConstraint() {
        return new double[numVars];
    }

    /**
     * Sets the objective function to be <obj>.
     */
    public void setObjective(double[] obj) {
        this.obj = obj;
    }

    /**
     * Constrains the n'th variable of this to be a boolean variable
     * (zero or one).
     */
    public void setBoolVar(int n) {
        this.boolVar[n] = true;
    }

    /**
     * Adds a greater-than-or-equal constraint between the variables
     * with coefficients <constraint> and the right-hand-side <rhs>.
     *
     * That is, <constraint> <dot> <variables> >= <rhs>. 
     */
    public void addConstraintGE(double[] constraint, double rhs) {
        constraints.add(new Constraint(ConstraintType.GE, constraint, rhs));
    }

    /**
     * Adds an equality constraint between the variables with
     * coefficients <constraint> and the right-hand-side <rhs>.
     */
    public void addConstraintEQ(double[] constraint, double rhs) {
        constraints.add(new Constraint(ConstraintType.EQ, constraint, rhs));
    }
}
