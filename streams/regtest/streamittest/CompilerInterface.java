package streamittest;

import java.util.*;

/**
 * Interface for compiling streamIT programs 
 * programatically from the regression testing framework, and
 * automatically comparing output from the two files
 * $Id: CompilerInterface.java,v 1.13 2002-11-07 22:57:42 dmaze Exp $
 **/
public class CompilerInterface {
    // flags for the various compiler options
    public static final int NONE               =    0x0;
    //public static final int RAW              =    0x1;
    public static final int CONSTPROP          =    0x2;
    public static final int UNROLL             =    0x4; // sets unroll limit to 100000
    public static final int FUSION             =    0x8;
    public static final int PARTITION          =   0x10;
    public static final int[] RAW              = { 0x00,   // ignore the 0 case
						   0x20,   // RAW[1]
						   0x40,   // RAW[2]
						   0x80,   // RAW[3]
						  0x100,  // RAW[4]
						  0x200,  // RAW[5]
						  0x400,  // RAW[6]
						  0x800,  // RAW[7]
						 0x1000};// RAW[8]
    public static final int LINEAR_ANALYSIS    = 0x2000;
    public static final int LINEAR_REPLACEMENT = 0x4000;
    public static final int DEBUG              = 0x8000;
    public static final int POPTOPEEK          =0x10000;
    


    // Options
    public static final String OPTION_STREAMIT           = "--streamit";
    public static final String OPTION_CONSTPROP          = "--constprop";
    public static final String OPTION_UNROLL             = "--unroll 100000";
    public static final String OPTION_FUSION             = "--fusion";
    public static final String OPTION_PARTITION          = "--partition";

    public static final String OPTION_RAW                = "--raw";

    public static final String OPTION_LINEAR_ANALYSIS    = "--linearanalysis";
    public static final String OPTION_LINEAR_REPLACEMENT = "--linearreplacement";

    public static final String OPTION_DEBUG              = "--debug";
    public static final String OPTION_POPTOPEEK          = "--poptopeek";
       
    
    // suffix to add to the various pieces of compilation
    public static final String SUFFIX_C    = ".c";
    public static final String SUFFIX_EXE  = ".exe";
    public static final String SUFFIX_DATA = ".data";
    public static final String SUFFIX_DATA_RAW = ".raw";
    

    
    // fields
    /** option mask that was passed in **/
    int compilerFlags;
    /** the options to pass to the streamit compiler **/
    String[] compilerOptions;

    
    /**
     * Create a new Compiler interface (always created using
     * factor method createCompilerInterface).
     **/
    private CompilerInterface(int flags, String[] options) {
	super();
	this.compilerFlags = flags;
	this.compilerOptions = options;
    }

    /**
     * Runs the streamit syntax converter from the input file
     * to the output file, passing the return value from the
     * compiler back to the caller.
     */
    boolean streamITConvert(String root, String filein, String fileout)
    {
        return CompilerHarness.streamITConvert(root,
                                               root + filein,
                                               root + fileout);
    }
    
    /**
     * Runs the streamit compiler on the filename provided,
     * passing the return value from the compiler back to
     * the caller.
     **/
    boolean streamITCompile(String root, String filename) {


	boolean streamITResult;
	boolean targetResult;


	streamITResult = CompilerHarness.streamITCompile(this.compilerOptions,
							 root,
							 root + filename,                // input file(s)
							 root + filename + SUFFIX_C);    // output c file

	// if we didn't correctly compile for streamit, abort here
	if (streamITResult == false) {
	    return false;
	}

	// if we are executing on raw
	if (rawTarget(this.compilerFlags)) {
	    // run the raw compile (via make)
	    targetResult = CompilerHarness.rawCompile(root,
						      at.dms.kjc.raw.MakefileGenerator.MAKEFILE_NAME);
	} else {
	    // run uniprocessor compile
	    targetResult =  CompilerHarness.gccCompile(root + filename + SUFFIX_C,    // output c file
						       root + filename + SUFFIX_EXE); // executable
	}

	// return true if we passed both tests
	return (streamITResult && targetResult);
    }


    /**
     * Runs the last compiled streamit program. Returns true if execution goes well,
     * false if something bad happened.
     **/
    boolean streamITRun(String root, String filename, int initOutput, int ssOutput) {
	if (rawTarget(this.compilerFlags)) {
	    // set up the execution of the program via the raw simulator
	    boolean result;
	    result = RuntimeHarness.rawExecute(root,
					       at.dms.kjc.raw.MakefileGenerator.MAKEFILE_NAME,
					       root + filename + SUFFIX_DATA_RAW);
	    // if we got a good result, add a line to the performance data
	    if (result) {
		ResultPrinter.printPerformance(root, filename,
					       getOptionsString(),
					       initOutput, ssOutput);
	    }
	    return result;
	} else {
	    // execute the program directly on the uniprocessor
	    return RuntimeHarness.uniExecute(root + filename + SUFFIX_EXE,
					     root + filename + SUFFIX_DATA);
	}
    }

    /**
     * compares the output file with
     * the data file.
     **/
    boolean streamITCompare(String root, String filename, String datafile) {
	if (rawTarget(this.compilerFlags)) {
	    // set up the verification process via 
	    return RuntimeHarness.rawCompare(root + filename + SUFFIX_DATA_RAW,
					     root + datafile,
					     root,
					     filename,
					     this.compilerOptions);
	} else {
	    return RuntimeHarness.uniCompare(root + filename + SUFFIX_DATA,
					     root + datafile);
	}
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

	// if we are compiling to 4 raw tiles 
	for (int i=1; i<=8; i++) {
	    if ((flags & RAW[i]) == RAW[i]) {
		options[numOptions] = OPTION_RAW;
		numOptions++;
		options[numOptions] = "" + i;
		numOptions++;
	    }
	}

	// if we are running linear analysis
	if ((flags & LINEAR_ANALYSIS) == LINEAR_ANALYSIS) {
	    options[numOptions] = OPTION_LINEAR_ANALYSIS;
	    numOptions++;
	}

	// if we are running linear replacement
	if ((flags & LINEAR_REPLACEMENT) == LINEAR_REPLACEMENT) {
	    options[numOptions] = OPTION_LINEAR_REPLACEMENT;
	    numOptions++;
	}

	// if we want debugging output
	if ((flags & DEBUG) == DEBUG) {
	    options[numOptions] = OPTION_DEBUG;
	    numOptions++;
	}

	// if we want to convert all pops to peeks
	if ((flags & POPTOPEEK) == POPTOPEEK) {
	    options[numOptions] = OPTION_POPTOPEEK;
	    numOptions++;
	}

	
	
	// copy over the options that were used into an options
	// array that is the correct size
	String[] optionsToReturn = new String[numOptions];
	for (int i=0; i<numOptions; i++) {
	    optionsToReturn[i] = options[i];
	}

	// create a new interface with these options	
	return new CompilerInterface(flags, optionsToReturn);
    }

    /**
     * Executes the make command in the specified directory.
     **/
    public boolean runMake(String root) {
	return RuntimeHarness.make(root, "");
    }

    /**
     * Executes the make command in the specified directory
     * for the specified target .
     **/
    public boolean runMake(String root, String target) {
	return RuntimeHarness.make(root, target);
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

    /**
     * returns true if the flags passed include any of the raw options.
     **/
    public static boolean rawTarget(int flags) {
	for (int i=1; i<=8; i++) {
	    if ((flags & RAW[i]) == RAW[i]) {
		return true;
	    }
	}
	return false;
    }

}
