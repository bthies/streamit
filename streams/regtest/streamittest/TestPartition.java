/**
 * Runs the compiler on the all of the tests with no optimizations
 * turned on.
 * $Id: TestPartition.java,v 1.2 2002-12-13 23:17:58 thies Exp $
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
			 CompilerInterface.REMOVE_GLOBALS);

	// generate dp_scaling.txt files for theoretical scaling results
	
	suite.addTest(new TestBenchmarks("testFir", 
					 baseFlags | CompilerInterface.DPPARTITION | CompilerInterface.DPSCALE | 
					 CompilerInterface.RAW[2]));

	suite.addTest(new TestBenchmarks("testFm", 
					 baseFlags | CompilerInterface.DPPARTITION | CompilerInterface.DPSCALE | 
					 CompilerInterface.RAW[2]));

	suite.addTest(new TestBenchmarks("testBitonicSort", 
					 baseFlags | CompilerInterface.DPPARTITION | CompilerInterface.DPSCALE | 
					 CompilerInterface.RAW[2]));

	suite.addTest(new TestBenchmarks("testBeamFormer", 
					 baseFlags | CompilerInterface.DPPARTITION | CompilerInterface.DPSCALE | 
					 CompilerInterface.RAW[2]));

	suite.addTest(new TestBenchmarks("testFft", 
					 baseFlags | CompilerInterface.DPPARTITION | CompilerInterface.DPSCALE | 
					 CompilerInterface.RAW[2]));

	// generate the rest for both greedy and dp partition

	for (int i=2; i<=8; i+=2) {
		suite.addTest(new TestBenchmarks("testFir", 
						 baseFlags | CompilerInterface.PARTITION | CompilerInterface.RAW[i]));
	}
	for (int i=4; i<=8; i+=2) {
		suite.addTest(new TestBenchmarks("testFir", 
						 baseFlags | CompilerInterface.DPPARTITION | CompilerInterface.RAW[i]));
	}
	
	for (int i=2; i<=8; i+=2) {
	    suite.addTest(new TestBenchmarks("testFm", 
					     baseFlags | CompilerInterface.PARTITION | CompilerInterface.RAW[i]));
	}
	for (int i=4; i<=8; i+=2) {
	    suite.addTest(new TestBenchmarks("testFm", 
					     baseFlags | CompilerInterface.DPPARTITION | CompilerInterface.RAW[i]));
	}
	
	for (int i=2; i<=8; i+=2) {
	    suite.addTest(new TestBenchmarks("testBitonicSort", 
					     baseFlags | CompilerInterface.PARTITION | CompilerInterface.RAW[i]));
	}
	for (int i=4; i<=8; i+=2) {
	    suite.addTest(new TestBenchmarks("testBitonicSort", 
					     baseFlags | CompilerInterface.DPPARTITION | CompilerInterface.RAW[i]));
	}
	
	for (int i=2; i<=8; i+=2) {
	    suite.addTest(new TestBenchmarks("testBeamFormer", 
					     baseFlags | CompilerInterface.PARTITION | CompilerInterface.RAW[i]));
	}
	for (int i=4; i<=8; i+=2) {
	    suite.addTest(new TestBenchmarks("testBeamFormer", 
					     baseFlags | CompilerInterface.DPPARTITION | CompilerInterface.RAW[i]));
	}
	
	for (int i=2; i<=8; i+=2) {
	    suite.addTest(new TestBenchmarks("testFft", 
					     baseFlags | CompilerInterface.PARTITION | CompilerInterface.RAW[i]));
	}
	for (int i=4; i<=8; i+=2) {
	    suite.addTest(new TestBenchmarks("testFft", 
					     baseFlags | CompilerInterface.DPPARTITION | CompilerInterface.RAW[i]));
	}
	
	return suite;	
    }
}
