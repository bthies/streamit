package at.dms.kjc.linprog;

import java.io.*;

import at.dms.util.*;
import lpsolve.lprec;
import lpsolve.constant;
import lpsolve.solve;

/**
 * This is an integer linear program solver based on GLPK.
 *
 */
public class GLPKSolve extends MPSWriter implements LinearProgramSolver {
    private static final String PROBLEM_FILE = "partitions.mps";
    private static final String SOLUTION_FILE = "partitions.sol";

    /**
     * Construct an LP with <numVars> variables.
     */
    public GLPKSolve(int numVars) {
        super(numVars);
    }

    /**
     * Solve the program and return the value of the the variables
     * (indices 0...numVars-1) in the optimum.
     */
    public double[] solve() {
        // write mps to file
        printMPSToFile("partitions.mps");
        // run the solver
        double[] result = null;
        try {
            int status = Runtime.getRuntime().exec("glpsol partitions.mps --wlpt partitions.lp -o partitions.sol").waitFor();
            if (status!=0) {
                Utils.fail("Error running ILP solver.");
            }
            // parse results
            result = parseResults();
        } catch (InterruptedException e) {
            e.printStackTrace();
            Utils.fail("Linear programming solver was interrupted.");
        } catch (IOException e) {
            e.printStackTrace();
            Utils.fail("Failed to read results back from ILP solver.");
        }
        return result;
    }

    /**
     * Return results in format required by <solve> from
     * <SOLUTION_FILE>.  Returns null if it finds status of
     * "UNDEFINED".
     */
    private double[] parseResults() throws IOException {
        BufferedReader in = new BufferedReader(new FileReader(SOLUTION_FILE));
    
        // find variable output
        String line;
        do {
            line = in.readLine();
            // return null if undefined
            if (line.indexOf("UNDEFINED")>0) {
                return null;
            }
        } while (line.indexOf("Column name")==-1);
        // read one more dummy line
        in.readLine();
    
        double[] result = new double[numVars];
        while (!(line = in.readLine()).equals("")) {
            String index = line.substring(8, 13).trim();
            String val = line.substring(23, 36).trim();
            int i = Integer.valueOf(index).intValue();
            double v = (double)Double.valueOf(val).doubleValue();
            result[i] = v;
        }
    
        return result;
    }
}
