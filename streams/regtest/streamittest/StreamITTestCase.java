package streamittest;

import junit.framework.*;

/**
 * StreamITTestCase is the base class for all streamit
 * test cases. This class provides some useful methods.
 * $Id: StreamITTestCase.java,v 1.6 2002-07-01 19:17:46 aalamb Exp $
 **/
class StreamITTestCase extends TestCase {
    static final String EXAMPLE_PATH  = "docs/examples/hand/";
    static final String APPS_PATH     = "apps/";

    static final int    DEFAULT_FLAGS = CompilerInterface.NONE;
    
    /** Compiler interface for this test to use **/
    CompilerInterface compiler;

    /**
     * Create a new StreamITTestCase with the default compiler options.
     **/
    public StreamITTestCase(String name) {
	this(name, DEFAULT_FLAGS);
    }
    
    /**
     * Create a new StreamItTestCase with the
     * specified compiler flags.
     **/
    public StreamITTestCase(String name, int flags) {
	super(name);
	// create a compiler interface for the test case to use (set up the compiler options)
	this.compiler = CompilerInterface.createCompilerInterface(flags);
    }

    /**
     * Performs streamit compile, gcc compile.
     **/
    public void doCompileTest(String root,
			      String filename) {
	
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
	assertTrue("Executing " + root + filename + "(" + compiler.getOptionsString() + ")",
		   compiler.streamITRun(root,
					filename));

	// test output
	assertTrue("Verify output " + root + filename + "(" + compiler.getOptionsString() + ")",
		   compiler.streamITCompare(root,
					    filename,
					    datafile));
    }

    public void doMake(String root) {
	assertTrue("make for " + root,
		   compiler.runMake(root));
    }
		 
    
}
	    
