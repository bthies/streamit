package streamittest;

import java.util.*;

/**
 * Interface for compiling streamIT programs 
 * programatically from the regression testing framework, and
 * automatically comparing output from the two files
 * $Id: CompilerInterface.java,v 1.5 2002-06-28 22:18:38 aalamb Exp $
 **/
public class CompilerInterface {
    // flags for the various compiler options
    public static final int NONE         = 0x0;
    //public static final int RAW          = 0x1;
    public static final int CONSTPROP    = 0x2;
    public static final int UNROLL       = 0x4;
    public static final int FUSION       = 0x8;
    public static final int PARTITION    = 0x10;
    public static final int RAW4         = 0x20;
    public static final int RAW8         = 0x40;
    
    // Options
    public static final String OPTION_STREAMIT  = "--streamit";
    public static final String OPTION_CONSTPROP = "--constprop";
    public static final String OPTION_UNROLL    = "--unroll";
    public static final String OPTION_FUSION    = "--fusion";
    public static final String OPTION_PARTITION = "--partition";

    public static final String OPTION_RAW        = "--raw";
    public static final String OPTION_FOUR       = "4";
    public static final String OPTION_EIGHT      = "8";
    
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
    boolean streamITRun(String root, String filename) {
	if (rawTarget(this.compilerFlags)) {
	    // set up the execution of the program via the raw simulator
	    return RuntimeHarness.rawExecute(root,
					     at.dms.kjc.raw.MakefileGenerator.MAKEFILE_NAME,
					     root + filename + SUFFIX_DATA_RAW);
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
	if ((flags & RAW4) == RAW4) {
	    options[numOptions] = OPTION_RAW;
	    numOptions++;
	    options[numOptions] = OPTION_FOUR;
	    numOptions++;
	}

	// if we are compiling to 8 raw tiles 
	if ((flags & RAW8) == RAW8) {
	    options[numOptions] = OPTION_RAW;
	    numOptions++;
	    options[numOptions] = OPTION_EIGHT;
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

    /**
     * returns true if the flags passed include any of the raw options.
     **/
    public static boolean rawTarget(int flags) {
	boolean raw4 = ((flags & RAW4) == RAW4);
	boolean raw8 = ((flags & RAW8) == RAW8);

	return (raw4 || raw8);
    }
}
