/**
 * Provides Java interface to the main StreamIT compiler, allowing
 * for easy regression testing.
 * $Id: CompilerHarness.java,v 1.2 2002-06-21 20:03:54 aalamb Exp $
 **/
package streamittest;

import at.dms.kjc.*;
import at.dms.kjc.sir.*;
import at.dms.kjc.sir.lowering.*;
import at.dms.kjc.lir.*;
import at.dms.kjc.raw.*;

import java.io.*;

public class CompilerHarness extends Harness {
    static final boolean DEBUG = false;
    
    // command lines    
    static final String GCC_COMMAND = "gcc";
    static final String JAVA_COMMAND = "java";

    // main compiler java class
    static final String JAVA_MAIN = "at.dms.kjc.Main";
    // java memory option
    static final String JAVA_OPTION_MEM = "-Xmx256M";

    // location of streamit c library files
    static final String C_LIBRARY_PATH = "library/c/";
    static final String C_LIBRARY_FILES = C_LIBRARY_PATH + "stream*.c";
    
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

	// expand input streamit files
	String[] expandedFileNames = expandFileName(inFileName);
	
	// new array for options and for filename
	String[] cmdLineArgs = getJavaCommandArray(options, expandedFileNames);

	try {

	    // set up a java file i/o stream so we can save
	    // the output of the streamit compiler into a file (which we
	    // can then compile with gcc)
	    FileOutputStream fileOut = new FileOutputStream(outFileName);
	    
	    // execute natively
	    compilerResult = executeNative(cmdLineArgs, fileOut);

	    // close file descriptor
	    fileOut.close();
	    
	} catch (Exception e) {
	    ResultPrinter.printError("Caught exception compiling with streamit : " + e.getMessage());
	    e.printStackTrace();
	}

	// try and compile the resulting c file with gcc
	try {

	    gccResult = executeNative(getGccCommandArray(outFileName,
							 exeFileName));
	} catch (Exception e) {
	    ResultPrinter.printError("gcc execution caused exception (?): " + e);
	    e.printStackTrace();
	}

	// things are only ok if both the streamit compiler and
	// gcc correctly compiled
	return (compilerResult && gccResult);
    }	


    public static String[] getJavaCommandArray(String[] options,
					       String[] expandedFileNames) {
	// expand the filename that was passed in to multiple filenames
	// if that is necessary
	
	String[] cmdLineArgs = new String[(1 + // java
					   1 + // -Xmx256M
					   1 + // at....Main
					   options.length + // streamit options
					   expandedFileNames.length)]; // filenames
	
	// copy over the java command
	cmdLineArgs[0] = JAVA_COMMAND;
	// copy over the expression for the memory use
	cmdLineArgs[1] = JAVA_OPTION_MEM;
	// copy over the main class file
	cmdLineArgs[2] = JAVA_MAIN;
	
	
	// copy over the options
	for (int i=0; i<options.length;i++) {
	    cmdLineArgs[i+3] = options[i];
	}

	// copy over the filenames
	for (int i=0; i<expandedFileNames.length; i++) {
	    cmdLineArgs[3 + options.length + i] = expandedFileNames[i];
	}
	
	return cmdLineArgs;
    }
    

    /**
     * Set up an array of commands that will start up
     * gcc to compile inputFileName and write executable to exeFileName.
     **/
    public static String[] getGccCommandArray(String inputFileName,
					      String exeFileName) {

	String streamit_root = StreamITTestCase.getStreamITRoot();
	// expand out the library files path
	String[] libFiles = expandFileName(streamit_root + C_LIBRARY_FILES);

	String[] opts = new String[(6 + // set up args
				    libFiles.length)];
	
	opts[0] = GCC_COMMAND;
	opts[1] = "-O2";
	opts[2] = "-lm";
	opts[3] = "-I" + streamit_root + C_LIBRARY_PATH;
	opts[4] = "-o" + exeFileName;
	opts[5] = inputFileName;
	
	// copy over the stream library files
	for (int i=0; i<libFiles.length; i++) {
	    opts[6+i] = libFiles[i];
	}

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
