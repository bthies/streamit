package at.dms.kjc.linprog;

public interface LinearProgramSolver extends LinearProgram {    
    /**
     * Solve the program and return the value of the the variables
     * (indices 0...numVars-1) in the optimum.
     */
    double[] solve() throws LPSolverFailedException;
}
