/**
 * This should help you test your bed. Next version.
 * Currently used as scratch space for testing a small
 * subset of the total test cases.
 * $Id: TestBed.java,v 1.3 2002-07-01 21:55:57 aalamb Exp $
 **/
package streamittest;

import junit.framework.*;

public class TestBed extends StreamITTestCase {


    public TestBed(String name, int flags) {
	super (name, flags);
    }




    public static Test suite() {
	//int flags = (CompilerInterface.NONE |
	//     CompilerInterface.RAW4);
	int flags = CompilerInterface.NONE;

	TestSuite suite = new TestSuite();

	//suite.addTest(new TestExamples("testArrayTest", flags));
	
	//suite.addTest(new TestExamples("testFir", flags));
	//suite.addTest(new TestExamples("testHello6", flags));

	suite.addTest(new TestBed("testStatic", flags));
	//suite.addTest(new TestExamples("testFm", flags));
	
	return suite;
	//return TestAll.makeTestSuite(flags);
    }


    public void testStatic() {
	doCompileTest(Harness.getStreamITRoot() + "../test",
 		      "StaticTest.java");
    }

    
    
}
