/**
 * Runs the compiler on the all of the tests with no optimizations
 * turned on.
 * $Id: TestPartition.java,v 1.6 2003-09-29 20:41:05 thies Exp $
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
			 CompilerInterface.NUMBERS);

	int[] opts1 = { baseFlags | CompilerInterface.DPPARTITION | CompilerInterface.DPSCALE };

	int[] opts2 = {baseFlags | CompilerInterface.DPPARTITION | CompilerInterface.RAW[2],
		       baseFlags | CompilerInterface.DPPARTITION | CompilerInterface.RAW[3],
		       baseFlags | CompilerInterface.DPPARTITION | CompilerInterface.RAW[4],
		       baseFlags | CompilerInterface.DPPARTITION | CompilerInterface.RAW[5],
		       baseFlags | CompilerInterface.DPPARTITION | CompilerInterface.RAW[6],
		       baseFlags | CompilerInterface.DPPARTITION | CompilerInterface.RAW[7],
		       baseFlags | CompilerInterface.DPPARTITION | CompilerInterface.RAW[8],
		       baseFlags | CompilerInterface.PARTITION | CompilerInterface.RAW[2],
		       baseFlags | CompilerInterface.PARTITION | CompilerInterface.RAW[3],
		       baseFlags | CompilerInterface.PARTITION | CompilerInterface.RAW[4],
		       baseFlags | CompilerInterface.PARTITION | CompilerInterface.RAW[5],
		       baseFlags | CompilerInterface.PARTITION | CompilerInterface.RAW[6],
		       baseFlags | CompilerInterface.PARTITION | CompilerInterface.RAW[7],
		       baseFlags | CompilerInterface.PARTITION | CompilerInterface.RAW[8]};

	int[] opts3 = {baseFlags | CompilerInterface.DPPARTITION | CompilerInterface.RAW[2],
		       baseFlags | CompilerInterface.DPPARTITION | CompilerInterface.RAW[4],
		       baseFlags | CompilerInterface.DPPARTITION | CompilerInterface.RAW[6],
		       baseFlags | CompilerInterface.DPPARTITION | CompilerInterface.RAW[8],
		       baseFlags | CompilerInterface.PARTITION | CompilerInterface.RAW[2],
		       baseFlags | CompilerInterface.PARTITION | CompilerInterface.RAW[4],
		       baseFlags | CompilerInterface.PARTITION | CompilerInterface.RAW[6],
		       baseFlags | CompilerInterface.PARTITION | CompilerInterface.RAW[8]};

	// first get theoretical scaling results for everything
	for (int i=0; i<opts1.length; i++) {
	    addHighPriority(suite, opts1[i]);
	    addLowPriority(suite, opts1[i]);
	}
	
	// then fine-grained execution numbers for high priority stuff
	for (int i=0; i<opts2.length; i++) {
	    addHighPriority(suite, opts2[i]);
	}

	// then coarse numbers for low priority stuff
	for (int i=0; i<opts3.length; i++) {
	    addLowPriority(suite, opts3[i]);
	}

	return suite;
    }
    
    private static void addHighPriority(TestSuite suite, int opts) {
	suite.addTest(new TestBenchmarks("testBeamFormer", opts));
	suite.addTest(new TestBenchmarks("testBitonicSort", opts));
	suite.addTest(new TestBenchmarks("testFir", opts));
    }
    
    private static void addLowPriority(TestSuite suite, int opts) {
	suite.addTest(new TestBenchmarks("testFm", opts));
	suite.addTest(new TestBenchmarks("testFft", opts));
	suite.addTest(new TestBenchmarks("testVocoder", opts));
	suite.addTest(new TestBenchmarks("testFilterbank", opts));
	suite.addTest(new TestBenchmarks("testNokia", opts));
	suite.addTest(new TestBenchmarks("testMatMulBlock", opts));
	suite.addTest(new TestBenchmarks("testCFAR", opts));
	suite.addTest(new TestBenchmarks("testPerftest4", opts));
	suite.addTest(new TestBenchmarks("testMP3Simple", opts));
	
	suite.addTest(new TestApps("testCrc", opts));
	suite.addTest(new TestApps("testNokiaFine", opts));
	
	suite.addTest(new TestExamples("testMatrixMult", opts));
	suite.addTest(new TestExamples("testMergeSort", opts));
	suite.addTest(new TestExamples("testLattice", opts));	
    }

}
