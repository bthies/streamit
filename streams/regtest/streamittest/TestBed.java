/**
 * This should help you test your bed. Next version.
 * Currently used as scratch space for testing a small
 * subset of the total test cases.
 * $Id: TestBed.java,v 1.13 2002-10-04 00:35:34 thies Exp $
 **/
package streamittest;

import junit.framework.*;

public class TestBed extends StreamITTestCase {


    public TestBed(String name, int flags) {
	super (name, flags);
    }




    public static Test suite() {

 	int flags = (CompilerInterface.NONE |
 		     CompilerInterface.RAW[4] |
		     CompilerInterface.PARTITION);
	//flags = CompilerInterface.NONE;

	TestSuite suite = new TestSuite();

	suite.addTest(new TestExamples("testAutoCor", flags));

	
	return suite;
	//return TestAll.makeTestSuite(flags);
    }


    public void testStatic() {
	doCompileTest(Harness.getStreamITRoot() + "../test",
 		      "StaticTest.java");
    }

    
    
}
