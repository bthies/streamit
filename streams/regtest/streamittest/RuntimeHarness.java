package streamittest;

import java.io.*;

/**
 * Class which contains code to execute a compiled StreamIT program, and compares
 * its output with an expected output file.
 **/

public class RuntimeHarness extends Harness {
    public static final int ITER_COUNT = 1000;
    public static final String ITER_OPTION     = "-i";

    public static final String CMP_COMMAND     = "cmp";

    public static final String MAKE_COMMAND    = "make";
    public static final String MAKE_DIR_OPTION = "-C";
    
    /**
     * executes the specified program streamit program
     * for ITER_COUNT iterations, and redirects its output to the
     * file named FILE_STREAMIT_OUTPUT. Returns true if successful execution
     * false otherwise with information going through ResultPrinter.
     * Used for executing programs generated via the uniprocessor path
     **/ 
    static boolean uniExecute(String program,
			      String outfile) {
	try {
	    boolean result;
	    
	    // set up file to dump results to
	    FileOutputStream fout =  new FileOutputStream(outfile);	    

	    // execute streamit program
	    result =  executeNative(getUniRuntimeCommandArray(program),
				    fout);

	    // close the output stream
	    fout.close();

	    return result;
	} catch (Exception e) {
	    ResultPrinter.printError("Caught an exception while executing streamit program: " +
				     e.getMessage());
	    e.printStackTrace();
	    return false;
	}
    }

    /**
     * executes the specified program streamit program
     * for ITER_COUNT iterations, and redirects its output to the
     * file named FILE_STREAMIT_OUTPUT. Returns true if successful execution
     * false otherwise with information going through ResultPrinter.
     * Used for executing programs generated via the raw path
     **/
    static boolean rawExecute(String path,
			      String makefileName,
			      String outfile) {
	try {
	    boolean result;
	    
	    // set up file to dump results to
	    FileOutputStream fout =  new FileOutputStream(outfile);	    

	    // execute raw program on simulator
	    result =  executeNative(getRawRuntimeCommandArray(path, makefileName),
				    fout);

	    // close the output stream
	    fout.close();

	    return result;
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
	    return executeNative(getCompareCommandArray(file1, file2));
	} catch (Exception e) {
	    ResultPrinter.printError("Caught an exception while comparing output: " +
				     e.getMessage());
	    e.printStackTrace();
	    return false;
	}
    }


    /**
     * Runs make in the root directory specified.
     **/
    static boolean make(String root) {
	try {
	    return executeNative(getMakeCommandArray(root));
	} catch (Exception e) {
	    ResultPrinter.printError("Caught an exception while running make: " +
				     e.getMessage());
	    e.printStackTrace();
	    return false;
	}
    }
    
    /**
     * Get a command line argument array for running the
     * compiled streamit program.
     **/
    public static String[] getUniRuntimeCommandArray(String program) {
	String[] cmdLineArgs = new String[2];
	cmdLineArgs[0] = program;
	cmdLineArgs[1] = ITER_OPTION + " " + ITER_COUNT;
	return cmdLineArgs;
    }

    /**
     * Get a command line argument array for running the
     * compiled raw program on simulator.
     **/
    public static String[] getRawRuntimeCommandArray(String rootPath,
						     String makefileName) {
	String opts[] = new String[3];
	
	// run via csh (which seems crazy, but necessary)
	opts[0] = "csh";
	opts[1] = "-c";
	opts[2] = ("cd " + rootPath + ";" + // cd to the root path
		   "make -f " + makefileName + " run"); // make run
		   
	return opts;
    }

    /** Get a command line argument array for comparing two files with cmp **/
    public static String[] getCompareCommandArray(String file1, String file2) {
	String[] cmdLineArgs = new String[3];
	cmdLineArgs[0] = CMP_COMMAND;
	cmdLineArgs[1] = file1;
	cmdLineArgs[2] = file2;
	return cmdLineArgs;
    }

    /** Get a command line argument array for running make **/
    public static String[] getMakeCommandArray(String root) {
	String[] cmdLineArgs = new String[3];
	cmdLineArgs[0] = MAKE_COMMAND;
	cmdLineArgs[1] = MAKE_DIR_OPTION;
	cmdLineArgs[2] = root;
	return cmdLineArgs;
    }

    
	
	
}
