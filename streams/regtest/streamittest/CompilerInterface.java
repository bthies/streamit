package streamittest;

import java.util.*;

/**
 * Interface for compiling streamIT programs 
 * programatically from the regression testing framework, and
 * automatically comparing output from the two files
 * $Id: CompilerInterface.java,v 1.1 2002-06-20 21:19:56 aalamb Exp $
 **/
public class CompilerInterface {
    // flags for the various compiler options
    public static final int NONE         = 0x0;
    public static final int RAW          = 0x1;
    public static final int CONSTPROP    = 0x2;

    // Options
    public static final String OPTION_STREAMIT  = "-s";
    public static final String OPTION_CONSTPROP = "-c";
    public static final String OPTION_RAW       = "-raw";

    // suffix to add to the various pieces of compilation
    public static final String SUFFIX_C    = ".c";
    public static final String SUFFIX_EXE  = ".exe";
    public static final String SUFFIX_DATA = ".data";
    

    
    // fields
    /** the options to pass to the streamit compiler **/
    String[] compilerOptions;
    
    /**
     * Create a new Compiler interface (always created using
     * factor method createCompilerInterface).
     **/
    private CompilerInterface(String[] options) {
	super();
	this.compilerOptions = options;
    }
    
    /**
     * Runs the streamit compiler on the filename provided,
     * passing the return value from the compiler back to
     * the caller.
     **/
    boolean streamITCompile(String root, String filename) {
	return CompilerHarness.compile(this.compilerOptions,
				       root + filename,        // input file
				       root + filename + SUFFIX_C,    // output c file
				       root + filename + SUFFIX_EXE); // executable
    }


    /**
     * Runs the last compiled streamit program. Returns true if execution goes well,
     * false if something bad happened.
     **/
    boolean streamITRun(String root, String filename) {
	return RuntimeHarness.execute(root + filename + SUFFIX_EXE,
				      root + filename + SUFFIX_DATA);
    }

    /**
     * compares the output file with
     * the data file.
     **/
    boolean streamITCompare(String root, String filename, String datafile) {
	return RuntimeHarness.compare(root + filename + SUFFIX_DATA,
				      root + datafile);
    }
    

    
    /**
     * Creates a new CompilerInterface with the
     * options based on the flags
     **/
    public static CompilerInterface createCompilerInterface(int flags) {
	int numOptions = 0;
	String[] options = new String[100]; // resize array we return at the end

	// always compiling to streamit
	options[numOptions] = OPTION_STREAMIT;
	numOptions++;
	
	// if we are compiling to raw 
	if ((flags & RAW) == RAW) {
	    options[numOptions] = OPTION_RAW;
	    numOptions++;
	}

	// if we want to turn on constant prop
	if ((flags & CONSTPROP) == CONSTPROP) {
	    options[numOptions] = OPTION_CONSTPROP;
	    numOptions++;
	}

	// copy over the options that were used into an options
	// array that is the correct size
	String[] optionsToReturn = new String[numOptions];
	for (int i=0; i<numOptions; i++) {
	    optionsToReturn[i] = options[i];
	}

	// create a new interface with these options	
	return new CompilerInterface(optionsToReturn);
    }



}
