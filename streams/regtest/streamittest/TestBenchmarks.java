/**
 * For running the 
 *
 * You can then use the CompilerInterface compiler to run compiler sessions.
 * $Id: TestBenchmarks.java,v 1.2 2002-09-26 00:15:24 thies Exp $
 **/
package streamittest;

import junit.framework.*;


public class TestBenchmarks extends StreamITTestCase {
    static String STREAM_ROOT = null;
    static String BENCH_ROOT = null;
    /**
     * Creates a new TestBenchmarks which will use the compiler options
     * specified by flags (defined in CompilerInterface.java).
     **/
    public TestBenchmarks(String name, int flags) {
	super (name,flags);
	if (STREAM_ROOT == null) {
	    STREAM_ROOT = Harness.getStreamITRoot();
	    BENCH_ROOT = STREAM_ROOT + BENCH_PATH;
	}
    }

    public static Test suite(int flags) {
	TestSuite suite = new TestSuite();
	suite.addTest(new TestBenchmarks("testSimple", flags));
	// can't fit on raw 4 without partition
	if (!(flagsContainRaw4(flags) && !flagsContainPartition(flags))) {
	    suite.addTest(new TestBenchmarks("testFm", flags));
	}

	
	return suite;
    }
    
    public void testSimple() {
	assertTrue("was true", true);
    }

    public void testFm() {
	String root = BENCH_ROOT + "fm/streamit/"; 
	doMake(root);
	doCompileTest(root,
		      "LinkedFMTest.java");
	// run make, this time with the target extra-run
	// which changes the streamit makefile so the simulator runs
	// for more cycles
	doMake(root, "more-cycles");
	doRunTest(root,
		  "LinkedFMTest.java",
		  0,1);
	// do the comparison test
	doCompareTest(root,
		      "LinkedFMTest.java",
		      "LinkedFMTest.out");
    }
    
}
