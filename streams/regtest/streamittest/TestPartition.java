/**
 * Runs the compiler on the all of the tests with no optimizations
 * turned on.
 * $Id: TestPartition.java,v 1.3 2003-01-26 12:34:23 thies Exp $
 **/
package streamittest;

import junit.framework.*;

public class TestPartition extends StreamITTestCase {

    public TestPartition(String name, int flags) {
	super (name,flags);
    }

    public static Test suite() {
	TestSuite suite = new TestSuite();
	
	int baseFlags = (CompilerInterface.NONE |
			 CompilerInterface.UNROLL | 
			 CompilerInterface.REMOVE_GLOBALS |
			 CompilerInterface.NUMBERS);

	int[] opts = { baseFlags | CompilerInterface.DPPARTITION | CompilerInterface.DPSCALE,
		       baseFlags | CompilerInterface.DPPARTITION | CompilerInterface.RAW[2],
		       baseFlags | CompilerInterface.DPPARTITION | CompilerInterface.RAW[4],
		       baseFlags | CompilerInterface.DPPARTITION | CompilerInterface.RAW[6],
		       baseFlags | CompilerInterface.DPPARTITION | CompilerInterface.RAW[8],
		       baseFlags | CompilerInterface.PARTITION | CompilerInterface.RAW[2],
		       baseFlags | CompilerInterface.PARTITION | CompilerInterface.RAW[4],
		       baseFlags | CompilerInterface.PARTITION | CompilerInterface.RAW[6],
		       baseFlags | CompilerInterface.PARTITION | CompilerInterface.RAW[8]};

	for (int i=0; i<opts.length; i++) {
	    suite.addTest(new TestBenchmarks("testFir", opts[i]));
	    suite.addTest(new TestBenchmarks("testFm", opts[i]));
	    suite.addTest(new TestBenchmarks("testBitonicSort", opts[i]));
	    suite.addTest(new TestBenchmarks("testBeamFormer", opts[i]));
	    suite.addTest(new TestBenchmarks("testFft", opts[i]));
	    suite.addTest(new TestBenchmarks("testVocoder", opts[i]));
	    suite.addTest(new TestBenchmarks("testFilterbank", opts[i]));
	    suite.addTest(new TestBenchmarks("testNokia", opts[i]));
	    suite.addTest(new TestBenchmarks("testMatMulBlock", opts[i]));
	    suite.addTest(new TestBenchmarks("testCFAR", opts[i]));
	    suite.addTest(new TestBenchmarks("testPerftest4", opts[i]));

	    suite.addTest(new TestApps("testCrc", opts[i]));
	    suite.addTest(new TestApps("testMP3Simple", opts[i]));
	    suite.addTest(new TestApps("testNokiaFine", opts[i]));

	    suite.addTest(new TestExamples("testMatrixMult", opts[i]));
	    suite.addTest(new TestExamples("testMergeSort", opts[i]));
	    suite.addTest(new TestExamples("testMergeSort16", opts[i]));
	    suite.addTest(new TestExamples("testLattice", opts[i]));
	}
	
	return suite;	
    }
}
