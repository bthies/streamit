package streamittest;

import junit.framework.*;

/**
 * StreamITTestCase is the base class for all streamit
 * test cases. This class provides some useful methods.
 * $Id: StreamITTestCase.java,v 1.1 2002-06-20 21:19:56 aalamb Exp $
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
    public String getStreamITRoot() {
	return System.getProperty("streamit_home"); // imported using the -D command line
    }

    /**
     * Performs streamit compile, gcc compile, execution, and comparison.
     * root is root directory path.
     * filename is the streamit program file.
     * datafile is the file with known correct data.
     **/
    public void doTests(String root,
			String filename,
			String datafile) {
	
    	assertTrue("Compile " + filename,
		   compiler.streamITCompile(root,
					    filename));
	// test execution
	assertTrue("Run FieldInit",
		   compiler.streamITRun(root,
					filename));
	// test output
	assertTrue("Verify FieldInit",
		   compiler.streamITCompare(root,
					    filename,
					    datafile));
    }

}
	    
