package streamittest;

import junit.framework.*;

/**
 * StreamITTestCase is the base class for all streamit
 * test cases. This class provides some useful methods.
 * $Id: StreamITTestCase.java,v 1.3 2002-06-24 21:25:36 aalamb Exp $
 **/
class StreamITTestCase extends TestCase {
    static final String EXAMPLE_PATH  = "docs/examples/hand/";
    static final String APPS_PATH     = "apps/";

    /** Compiler interface for this test to use **/
    CompilerInterface compiler;
    
    public StreamITTestCase(String name, int flags) {
	super(name);
	this.compiler = CompilerInterface.createCompilerInterface(flags);
    }


    /**
     * Reads out the environment variable STREAMIT_HOME
     **/
    public static String getStreamITRoot() {
	return System.getProperty("streamit_home"); // imported using the -D command line
    }

    /**
     * Performs streamit compile, gcc compile.
     **/
    public void doCompileTest(String root,
			      String filename) {

	// print out test details for verbose output
	ResultPrinter.printTest(this.getClass().getName(),
				this.getName() + " compile ",
				"compiling " + root + filename + " with " + compiler.getOptionsString());

    	assertTrue("Compile " + root + filename + "(" + compiler.getOptionsString() + ")",
		   compiler.streamITCompile(root,
					    filename));
    }

    /**
     * Performs streamit compile, gcc compile, execution, and comparison.
     * root is root directory path.
     * filename is the streamit program file.
     * datafile is the file with known correct data.
     **/
    public void doCompileRunVerifyTest(String root,
				       String filename,
				       String datafile) {

	// run the compilation tests
	doCompileTest(root, filename);



	// test execution

	// print out test details for verbose output
	ResultPrinter.printTest(this.getClass().getName(),
				this.getName() + " run ",
				" running " + root + filename + ", redirecting output to " + root + datafile);
	assertTrue("Executing " + root + filename,
		   compiler.streamITRun(root,
					filename));

	// test output
	
	// print out test details for verbose output
	ResultPrinter.printTest(this.getClass().getName(),
				this.getName() + " verify ",
				" verifying output of " + root + filename);	
	assertTrue("Verify output " + root + filename,
		   compiler.streamITCompare(root,
					    filename,
					    datafile));
    }

    public void doMake(String root) {
	assertTrue("make for " + root,
		   compiler.runMake(root));
    }
		 
    
}
	    
