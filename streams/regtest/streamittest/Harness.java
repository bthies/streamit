package streamittest;

import java.io.*;
import java.util.*;



/**
 * This class contains code for executing commands while inside the java
 * runtime system.
 **/
public class Harness {
    /** The maximum time (in minutes) to execute a command **/
    public static final long TIME_LIMIT = 10;
    /** the path to the texec program to limit execution time **/
    public static final String TEXEC = "regtest/tools/texec/texec"; 
    
    /**
     * Reads out the environment variable STREAMIT_HOME
     * (which gets set via calling java with -Dstreamit_home=$STREAMIT_HOME
     * command line argument).
     **/
    public static String getStreamITRoot() {
	String home = System.getProperty("streamit_home"); // imported using the -D command line
	if (home == null) {
	    throw new RuntimeException("null streamit root property");
	}
	if (!home.endsWith("/"))
	    home = home + "/";
	return home;
    }

    
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
	// add a hook to the command array that will limit the execution time
	cmdArray = addTimeLimit(cmdArray);
	
	// start the process executing
	Process jProcess = Runtime.getRuntime().exec(cmdArray);
	
	// get hooks into the output and error streams
	InputStreamReader outReader=new InputStreamReader(jProcess.getInputStream());
	BufferedReader jOutStream = new BufferedReader(outReader);
	BufferedReader jErrorStream =  new BufferedReader(new InputStreamReader(jProcess.getErrorStream()));

	// block until the child process is done (time limiting is done outside the JVM).
	jProcess.waitFor();

	// if 0 is not returned, print stdout to result printer for debugging
	if (jProcess.exitValue() != 0) {
	    // print output from so that we know what is going on
	    ResultPrinter.printError("Error running: " + flattenCommandArray(cmdArray));
	    ResultPrinter.printError("return value: " + jProcess.exitValue());
	    ResultPrinter.printError("stdout: ");
	    while (jOutStream.ready()) {
		ResultPrinter.printError("out:" + jOutStream.readLine());
	    }
	    // flush errors to disk
	    ResultPrinter.flushFileWriter();
	}

	if (jErrorStream.ready()) {
	    ResultPrinter.printError("Error messages while running: " + flattenCommandArray(cmdArray));
	    ResultPrinter.printError("err: ");
	    while (jErrorStream.ready()) {
		ResultPrinter.printError(":" + jErrorStream.readLine());
	    }
	    // flush errors to disk
	    ResultPrinter.flushFileWriter();
	    
	}

	if (jProcess.exitValue() != 0) {
	    return false;
	}

	// if we have an output stream to write to,
	// write out stdout there.
	if (outStream != null) {
	    // copy the data in the output stream to the file
	    int i=0;
	    while(i<10) {
		i++;
		while(jOutStream.ready()) {
		    i=10;
		    outStream.write(jOutStream.read());
		}
		if(i<10)
		    Thread.currentThread().sleep(10);
	    }
	}
	// if we get here, streamit compilation succeeded
	return true;
    }

    /**
     * Prepend the texec command to the command array to limit
     * its execution time to TIME_LIMIT minutes.
     **/
    public static String[] addTimeLimit(String[] cmdArray) {
	String[] newCmdArray = new String[cmdArray.length + 3];
	newCmdArray[0] = getStreamITRoot() + TEXEC;
	newCmdArray[1] = "-m";
	newCmdArray[2] = "" + TIME_LIMIT;
	for (int i=0; i<cmdArray.length; i++) {
	    newCmdArray[i+3] = cmdArray[i];
	}
	return newCmdArray;
    }

    
    /**
     * Expand a filename with a wildcard in it to an array of file names.
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
	}
	return sourceFiles;
    }
    
    public static String[] getLsCommandOpts(String path) {
	String[] args = new String[3];
	args[0] = "bash";
	args[1] = "-c";
	args[2] = "ls "+ path;
	return args;
    }

    /**
     * Convertes a command array (of strings) into a
     * single string for display purposes.
     **/
    public static String flattenCommandArray(String[] arr) {
	String returnString = "";
	for (int i=0; i<arr.length; i++) {
	    returnString += arr[i] + " ";
	}
	return returnString;
    }
    
    
}
