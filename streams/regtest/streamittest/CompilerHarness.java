/**
 * Provides Java interface to the main StreamIT compiler, allowing
 * for easy regression testing.
 * $Id: CompilerHarness.java,v 1.1 2002-06-20 21:19:56 aalamb Exp $
 **/
package streamittest;

import at.dms.kjc.*;
import at.dms.kjc.sir.*;
import at.dms.kjc.sir.lowering.*;
import at.dms.kjc.lir.*;
import at.dms.kjc.raw.*;

import java.io.*;

public class CompilerHarness {
    static final boolean DEBUG = false;
    
    // temp files used by the harness
    
    static final String GCC_COMMAND = "gcc";
    


    /**
     * Run the compiler with the options specified in the 
     * passed array. Returns true if compliation is successful
     * false otherwise.
     **/
    static boolean compile(String[] options,
			   String inFileName,
			   String outFileName,
			   String exeFileName) {
	if (DEBUG) {
	    printOptions(options);
	}

	// result of running the streamit compiler
	boolean compilerResult = false;
	// result of running gcc
	boolean gccResult = false;

	// new array for options and for filename
	String[] cmdLineArgs = new String[1 + options.length];

	// copy over the options
	for (int i=0; i<options.length;i++) {
	    cmdLineArgs[i] = options[i];
	}

	// copy over the filename
	cmdLineArgs[options.length] = inFileName;

	// save stdout so we can reset System when we are done
	PrintStream stdout = System.out;

	try {
	    
	    // set up a java file i/o stream so we can save
	    // the output of the streamit compiler into a file (which we
	    // can then compile with gcc)
	    FileOutputStream fileOut = new FileOutputStream(outFileName);

	    // set System to use the file as the output stream
	    System.setOut(new PrintStream(fileOut));
	    
	    // actually run the compiler
	    compilerResult = Main.compile(cmdLineArgs);
	} catch (Exception e) {
	    stdout.println("Caught exception : " + e.getMessage());
	}


	// restore standard out
	System.setOut(stdout);

	
	// try and compile the resulting c file with gcc
	try {
	    Process gcc;
	    gcc = Runtime.getRuntime().exec(getGccCommandArray(outFileName,
							       exeFileName));
	    BufferedReader gccOutStream = new BufferedReader(new InputStreamReader(gcc.getInputStream()));
	    BufferedReader gccErrorStream =  new BufferedReader(new InputStreamReader(gcc.getErrorStream()));
	    gcc.waitFor();
	    

	    // figure out what the result was based on the exit value of gcc
	    // (if not 0, then error compiling
	    if ((gcc.exitValue() != 0) || (gccErrorStream.ready())) {
		// print output from gcc so that we know what is going on
		ResultPrinter.printInfo("----- Done compiling in gcc -----");
		ResultPrinter.printError("Error compiling with gcc");
		ResultPrinter.printError("gcc return value: " + gcc.exitValue());
		ResultPrinter.printError("stdout from gcc: ");
		while (gccOutStream.ready()) {
		    ResultPrinter.printError(":" + gccOutStream.readLine());
		}
		ResultPrinter.printError("stderr from gcc: ");
		while (gccErrorStream.ready()) {
		    ResultPrinter.printError(":" + gccErrorStream.readLine());
		}
		
		gccResult = false;
	    } else {
		gccResult = true;
	    }
	    
	} catch (Exception e) {
	    ResultPrinter.printError("gcc caused exception (?): " + e);
	    e.printStackTrace();
	}

	// things are only ok if both the streamit compiler and
	// gcc correctly compiled
	return (compilerResult && gccResult);
    }	


    /**
     * Set up an array of commands that will start up
     * gcc to compile inputFileName and write executable to exeFileName.
     **/
    public static String[] getGccCommandArray(String inputFileName,
					      String exeFileName) {
	String[] opts = new String[12];

	opts[0] = GCC_COMMAND;
	opts[1] = "-O2";
	opts[2] = "-lm";
	opts[3] = "-I/u/aalamb/streams/library/c";
	opts[4] = "-o" + exeFileName;
	opts[5] = "/u/aalamb/streams/library/c/stream_context.c";
	opts[6] = "/u/aalamb/streams/library/c/streamit_message.c";
	opts[7] = "/u/aalamb/streams/library/c/streamit_splitjoin.c";
	opts[8] = "/u/aalamb/streams/library/c/streamit_io.c";
	opts[9] = "/u/aalamb/streams/library/c/streamit_run.c";
	opts[10] = "/u/aalamb/streams/library/c/streamit_tape.c";
	
	opts[11] = inputFileName;

	return opts;
    }

    

    /**
     * Print out the options in the passed array.
     **/
    public static void printOptions(String[] opts) {
	String s = "Compiling with options: ";
	for (int i=0; i<opts.length; i++) {
	    s += (opts[i] + " ");
	}
	ResultPrinter.printInfo(s);
    }

}
