/**
 * This should help you test your bed. Next version.
 * Currently used as scratch space for testing a small
 * subset of the total test cases.
 * $Id: TestBed.java,v 1.14 2002-11-20 15:23:01 aalamb Exp $
 **/
package streamittest;

import junit.framework.*;

public class TestBed extends StreamITTestCase {

    public TestBed(String name, int flags) {
	super (name, flags);
    }

    public static Test suite() {
	TestSuite suite = new TestSuite();;
	int flags = 0;

	suite.addTest(new TestExamples("testMergeSort",
				       CompilerInterface.NONE));


	suite.addTest(new TestExamples("testMergeSort",
				       CompilerInterface.FUSION));

	suite.addTest(new TestExamples("testMergeSort",
				       CompilerInterface.PARTITION    |
				       CompilerInterface.RAW[4]));


	return suite;
	//return TestAll.makeTestSuite(flags);
    }


    public void testStatic() {
	doCompileTest(Harness.getStreamITRoot() + "../test",
 		      "StaticTest.java");
    }

    
    
}
