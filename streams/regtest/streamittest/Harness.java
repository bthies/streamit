package streamittest;

import java.io.*;
import java.util.Vector;
import java.util.StringTokenizer;


/**
 * This class contains code for executing commands while inside the java
 * runtime system.
 **/
public class Harness {


    /** run command natively, ignoring stdout (eg for gcc, make) **/
    public static boolean executeNative(String[] cmdArray) throws Exception {
	return executeNative(cmdArray, null);
    }

    /**
     * runs a command natively, returns true if command
     * successfully executes and returns 0 exit status. Otherwise
     * returns false and prints error messages and return status
     * via ResultPrinter.printError.
     *
     * Writes standard output from program to outStream.
     **/
    public static boolean executeNative(String[] cmdArray, OutputStream outStream) throws Exception {

	// start the process executing
	Process jProcess = Runtime.getRuntime().exec(cmdArray);
	
	// get hooks into the output and error streams
	BufferedReader jOutStream = new BufferedReader(new InputStreamReader(jProcess.getInputStream()));
	BufferedReader jErrorStream =  new BufferedReader(new InputStreamReader(jProcess.getErrorStream()));
	jProcess.waitFor();
	
	
	// figure out what the result was based on the exit value of gcc
	// (if not 0, then error compiling
	if ((jProcess.exitValue() != 0) || (jErrorStream.ready())) {
	    // print output from gcc so that we know what is going on
	    ResultPrinter.printError("Error running: " + cmdArray[0]);
	    ResultPrinter.printError("return value: " + jProcess.exitValue());
	    ResultPrinter.printError("stdout: ");
	    while (jOutStream.ready()) {
		ResultPrinter.printError(":" + jOutStream.readLine());
	    }
	    ResultPrinter.printError("stderr: ");
	    while (jErrorStream.ready()) {
		ResultPrinter.printError(":" + jErrorStream.readLine());
	    }
	    return false;
	} else {
	    // if we have an output stream to write to,
	    // write out stdout there.
	    if (outStream != null) {
		// copy the data in the output stream to the file
		while(jOutStream.ready()) {
		    outStream.write(jOutStream.read());
		}
	    }
	    // if we get here, streamit compilation went fine
	    return true;
	}
    }

    /**
     * Expand a filename with a wildcard in it to an array of file named.
     **/
    public static String[] expandFileName(String fileName) {
	Vector filenames = new Vector(); // expanded filenames

	try {
	    // we are going to use ls to expand the filenames for us
	    ByteArrayOutputStream lsBuff = new ByteArrayOutputStream();
	    executeNative(getLsCommandOpts(fileName), lsBuff);
	    
	    // parse ls output:
	    StringTokenizer st = new StringTokenizer(lsBuff.toString(), // output from ls
						     "\n"); // split on newline

	    while(st.hasMoreTokens()) {
		String currentToken = st.nextToken();
		if (!currentToken.equals("")) {
		    filenames.add(currentToken);
		}
	    }
	} catch (Exception e) {
	    throw new RuntimeException("Caught exception expanding filenames: " + e.getMessage());
	}

	// copy from vector to array
	String[] sourceFiles = new String[filenames.size()];
	for (int i=0; i<sourceFiles.length; i++) {
	    sourceFiles[i] = (String)filenames.elementAt(i);
	    //System.out.println("expanded: " + sourceFiles[i]);
	}
	return sourceFiles;
    }
    
    public static String[] getLsCommandOpts(String path) {
	String[] args = new String[3];
	args[0] = "bash";
	args[1] = "-c";
	args[2] = "ls " + path;
	return args;
    }


    
    
}
