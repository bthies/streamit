package streamittest;

import java.io.*;

/**
 * Class which contains code to execute a compiled StreamIT program, and compares
 * its output with an expected output file.
 **/

public class RuntimeHarness {
    public static final int ITER_COUNT = 1000;
    public static final String ITER_OPTION = "-i";

    public static final String CMP_COMMAND = "cmp";

    /**
     * executes the specified program streamit program
     * for ITER_COUNT iterations, and redirects its output to the
     * file named FILE_STREAMIT_OUTPUT. Returns true if successful execution
     * false otherwise with information going through ResultPrinter.
     **/ 
    static boolean execute(String program,
			   String outfile) {
	try {
	    Process sProcess;
	    // execute the stream program for ITER count iterations
	    sProcess = Runtime.getRuntime().exec(program + " " + 
						 ITER_OPTION + " " + ITER_COUNT);

	    // get hooks into the output that is generated
	    BufferedReader sOutStream = new BufferedReader(new InputStreamReader(sProcess.getInputStream()));
	    BufferedReader sErrorStream =  new BufferedReader(new InputStreamReader(sProcess.getErrorStream()));
	    
	    // wait for the process to complete
	    sProcess.waitFor();

	    // if the return value was not 0, or if 
	    // there is anything in the error stream, something bad happened
	    if ((sProcess.exitValue() != 0) || (sErrorStream.ready())) {
		ResultPrinter.printError("Runtime Error for running streamit program: stderr");
		while (sErrorStream.ready()) {
		    ResultPrinter.printError(":" + sErrorStream.readLine());
		}
		return false;
	    } else {
		// things seem to have terminated normally, so dump the stream output into a
		// file so that we can compare later
		BufferedWriter fout = new BufferedWriter(new FileWriter(outfile));
		while(sOutStream.ready()) {
		    fout.write(sOutStream.read());
		}
		fout.close();
		return true;
	    }
	} catch (Exception e) {
	    ResultPrinter.printError("Caught an exception while executing streamit program: " +
				     e.getMessage());
	    e.printStackTrace();
	    return false;
	}
    }

    /**
     * Compares the contents of two files using
     * the "cmp" unix utility. Returns true if files are identical
     * returns false if not.
     **/
    static boolean compare(String file1, String file2) {
	try {
	    Process cmpProcess;
	    // execute the cmp program on the two files
	    cmpProcess = Runtime.getRuntime().exec(CMP_COMMAND + " " +
						 file1 + " " +
						 file2);

	    // get hooks into the output that is generated
	    BufferedReader cmpOutStream = new BufferedReader(new InputStreamReader(cmpProcess.getInputStream()));
	    BufferedReader cmpErrorStream =  new BufferedReader(new InputStreamReader(cmpProcess.getErrorStream()));
	    
	    // wait for the process to complete
	    cmpProcess.waitFor();

	    // if the return value was not 0, or if 
	    // there is anything in the error stream, something bad happened
	    if ((cmpProcess.exitValue() != 0) || (cmpErrorStream.ready())) {
		ResultPrinter.printError("Results not the same: ");
		while (cmpErrorStream.ready()) {
		    ResultPrinter.printError(":" + cmpErrorStream.readLine());
		}
		return false;
	    } else {
		return true;
	    }
	} catch (Exception e) {
	    ResultPrinter.printError("Caught an exception while comparing output: " +
				     e.getMessage());
	    e.printStackTrace();
	    return false;
	}
    }
}
