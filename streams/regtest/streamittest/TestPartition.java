/**
 * Runs the compiler on the all of the tests with no optimizations
 * turned on.
 * $Id: TestPartition.java,v 1.1 2002-12-10 05:36:05 thies Exp $
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
	
	for (int i=2; i<=8; i+=2) {
	    suite.addTest(new TestBenchmarks("testFir", 
					     baseFlags | CompilerInterface.PARTITION | CompilerInterface.RAW[i]));
	    suite.addTest(new TestBenchmarks("testFir", 
					     baseFlags | CompilerInterface.DPPARTITION | CompilerInterface.RAW[i]));
	}
	
	for (int i=2; i<=8; i+=2) {
	    suite.addTest(new TestBenchmarks("testFm", 
					     baseFlags | CompilerInterface.PARTITION | CompilerInterface.RAW[i]));
	    suite.addTest(new TestBenchmarks("testFm", 
					     baseFlags | CompilerInterface.DPPARTITION | CompilerInterface.RAW[i]));
	}
	
	for (int i=2; i<=8; i+=2) {
	    suite.addTest(new TestBenchmarks("testBitonicSort", 
					     baseFlags | CompilerInterface.PARTITION | CompilerInterface.RAW[i]));
	    suite.addTest(new TestBenchmarks("testBitonicSort", 
					     baseFlags | CompilerInterface.DPPARTITION | CompilerInterface.RAW[i]));
	}
	
	for (int i=2; i<=8; i+=2) {
	    suite.addTest(new TestBenchmarks("testBeamFormer", 
					     baseFlags | CompilerInterface.PARTITION | CompilerInterface.RAW[i]));
	    suite.addTest(new TestBenchmarks("testBeamFormer", 
					     baseFlags | CompilerInterface.DPPARTITION | CompilerInterface.RAW[i]));
	}
	
	for (int i=2; i<=8; i+=2) {
	    suite.addTest(new TestBenchmarks("testFft", 
					     baseFlags | CompilerInterface.PARTITION | CompilerInterface.RAW[i]));
	    suite.addTest(new TestBenchmarks("testFft", 
					     baseFlags | CompilerInterface.DPPARTITION | CompilerInterface.RAW[i]));
	}
	
	return suite;	
    }
}
