package streamittest;

import java.util.*;

/**
 * Interface for compiling streamIT programs 
 * programatically from the regression testing framework, and
 * automatically comparing output from the two files
 * $Id: CompilerInterface.java,v 1.2 2002-06-21 20:03:54 aalamb Exp $
 **/
public class CompilerInterface {
    // flags for the various compiler options
    public static final int NONE         = 0x0;
    public static final int RAW          = 0x1;
    public static final int CONSTPROP    = 0x2;
    public static final int UNROLL       = 0x4;
    public static final int FUSION       = 0x10;
    public static final int PARTITION    = 0x20;

    
    // Options
    public static final String OPTION_STREAMIT  = "--streamit";
    public static final String OPTION_CONSTPROP = "--constprop";
    public static final String OPTION_UNROLL    = "--unroll";
    public static final String OPTION_FUSION    = "--fusion";
    public static final String OPTION_PARTITION = "--parition";

    public static final String OPTION_RAW       = "--raw";

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
				       root + filename,        // input file(s)
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

	// if we want to turn on unrolling
	if ((flags & UNROLL) == UNROLL) {
	    options[numOptions] = OPTION_UNROLL;
	    numOptions++;
	}
	
	// if we want to turn on fusion
	if ((flags & FUSION) == FUSION) {
	    options[numOptions] = OPTION_FUSION;
	    numOptions++;
	}

	// if we want to turn on partitioning
	if ((flags & PARTITION) == PARTITION) {
	    options[numOptions] = OPTION_PARTITION;
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

    /**
     * Executes the make command in the specified directory.
     **/
    public boolean runMake(String root) {
	return RuntimeHarness.make(root);
    }
    
    /**
     * Get a nice string which lists the options that the compiler is being run with
     **/
    public String getOptionsString() {
	String returnString = "";
	for (int i=0; i<this.compilerOptions.length; i++) {
	    returnString += this.compilerOptions[i] + " ";
	}
	return returnString;
    }

}
