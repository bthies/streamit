package at.dms.kjc.linprog;

/**
 * A generic inteface to a linear programming package.  Constraints
 * are all specified in terms of arrays of variables; it is up to the
 * user to keep track of which index of the array corresponds to which
 * variable.  In functions taking an index, the indices start from 0.
 */
public interface LinearProgram {

    /**
     * Returns an array with a zero-entry for each variable in the
     * linear program (which the client can then fill in with
     * coefficients before checking in as a new constraint.)
     */
    double[] getEmptyConstraint();

    /**
     * Sets the objective function to be <obj>.
     */
    void setObjective(double[] obj);

    /**
     * Constrains the n'th variable of this to be an integer.
     */
    void setIntVar(int n);

    /**
     * Adds a greater-than-or-equal constraint between the variables
     * with coefficients <constraint> and the right-hand-side <rhs>.
     *
     * That is, <constraint> <dot> <variables> >= <rhs>. 
     */
    void addConstraintGE(double[] constraint, double rhs);

    /**
     * Adds an equality constraint between the variables with
     * coefficients <constraint> and the right-hand-side <rhs>.
     */
    void addConstraintEQ(double[] constraint, double rhs);
    
    /**
     * Solve the program and return the value of the objective
     * function (at index 0), and the variables (indices 1...numVars)
     * in the optimum.
     */
    double[] solve();
}
