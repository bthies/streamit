package streamittest;

import java.io.*;
import java.util.*;



/**
 * This class contains code for executing commands while inside the java
 * runtime system.
 **/
public class Harness {
    /** The maximum time (in minutes) to execute a command **/
    public static final long TIME_LIMIT = 40;
    /** the path to the texec program to limit execution time **/
    public static final String TEXEC = "regtest/tools/texec/texec";

    /** The maximum number of output lines to put in the error file. **/
    public static final int MAX_OUTPUT_LINES = 500;
    
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
    public static boolean executeNative(String[] cmdArray, String cwd) throws Exception {
        File cwdf = null;
        if (cwd != null) cwdf = new File(cwd);
	return executeNative(cmdArray, null, cwdf);
    }

    /**
     * runs a command natively, returns true if command
     * successfully executes and returns 0 exit status. Otherwise
     * returns false and prints error messages and return status
     * via ResultPrinter.printError.
     *
     * Writes standard output from program to outStream.
     **/
    public static boolean executeNative(String[] cmdArray, OutputStream outStream, File cwd) throws Exception {
	// add a hook to the command array that will limit the execution time
	cmdArray = addTimeLimit(cmdArray);
	
	// start the process executing
	Process jProcess = Runtime.getRuntime().exec(cmdArray, null, cwd);
	
	// get hooks into the output and error streams
	InputStreamReader outReader=new InputStreamReader(jProcess.getInputStream());
	BufferedReader jOutStream = new BufferedReader(outReader);
	BufferedReader jErrorStream =  new BufferedReader(new InputStreamReader(jProcess.getErrorStream()));

        System.out.println("In directory: " + cwd.getPath());
	System.out.println("Starting:\n" + flattenCommandArray(cmdArray));

	// block until the child process is done (time limiting is done outside the JVM).
	jProcess.waitFor();

	System.out.println("done.");
	
	// if 0 is not returned, print up to MAX_OUTPUT_LINES of stdout and stderr to result printer 
	if (jProcess.exitValue() != 0) {
	    // print output from so that we know what is going on
	    ResultPrinter.printError("Error running: " + flattenCommandArray(cmdArray));
	    ResultPrinter.printError("return value: " + jProcess.exitValue());
	    ResultPrinter.printError("stdout: ");
	    int lineCounter = 0;
	    while (jOutStream.ready() && (lineCounter < MAX_OUTPUT_LINES)) {
		ResultPrinter.printError("out:" + jOutStream.readLine());
		lineCounter++;
	    }
	    // flush errors to disk
	    ResultPrinter.flushFileWriter();
	}

	if (jErrorStream.ready()) {
	    ResultPrinter.printError("Error messages while running: " + flattenCommandArray(cmdArray));
	    ResultPrinter.printError("err: ");
	    int lineCounter = 0;
	    while (jErrorStream.ready() && (lineCounter < MAX_OUTPUT_LINES)) {
		ResultPrinter.printError(":" + jErrorStream.readLine());
		lineCounter++;
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
	// if we get here, execution succeeded
	return true;
    }

    /**
     * Appends the contents of <srcFile> to <dstFile>, first appending
     * <label> (surrounded by newlines) to <dstFile>.
     */
    public static boolean appendFile(String label, 
				     String srcFile,
				     String dstFile) {
	String[] cmdArray = new String[3];
	// run via csh (following other examples)
	try {
            FileOutputStream fos = new FileOutputStream(dstFile, true);
            OutputStreamWriter osw = new OutputStreamWriter(fos);
            BufferedWriter bw = new BufferedWriter(osw);
            PrintWriter pw = new PrintWriter(bw);
            
            pw.println();
            pw.println();
            pw.println(label);
            pw.println();
            
            FileInputStream fis = new FileInputStream(srcFile);
            InputStreamReader isr = new InputStreamReader(fis);
            BufferedReader br = new BufferedReader(isr);
            
            while (true) {
                String line = br.readLine();
                if (line == null) break;
                pw.println(line);
            }

            // The end.
            br.close();
            pw.close();
            return true;
	} catch (Exception e) {
	    ResultPrinter.printError("appending file caused exception (?): " + e);
	    e.printStackTrace();
	    return false;
	}
    }

    /**
     * Deletes <filename>.
     */
    public static boolean deleteFile(String filename) {
        return new File(filename).delete();
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
	    executeNative(getLsCommandOpts(fileName), lsBuff, null);
	    
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
