/**
 * This should help you test your bed. Next version.
 * Currently used as scratch space for testing a small
 * subset of the total test cases.
 * $Id: TestBed.java,v 1.12 2002-08-30 20:11:20 aalamb Exp $
 **/
package streamittest;

import junit.framework.*;

public class TestBed extends StreamITTestCase {


    public TestBed(String name, int flags) {
	super (name, flags);
    }




    public static Test suite() {

 	int flags = (CompilerInterface.NONE |
 		     CompilerInterface.RAW4 |
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
