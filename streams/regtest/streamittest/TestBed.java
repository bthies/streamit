/**
 * This should help you test your bed. Next version.
 * Currently used as scratch space for testing a small
 * subset of the total test cases.
 * $Id: TestBed.java,v 1.1 2002-06-28 22:18:38 aalamb Exp $
 **/
package streamittest;

import junit.framework.*;

public class TestBed extends TestCase {

    public TestBed(String name) {
	super (name);
    }




    public static Test suite() {
	int flags = (CompilerInterface.NONE |
		     CompilerInterface.RAW4);

	TestSuite suite = new TestSuite();

	suite.addTest(new TestExamples("testArrayTest", flags));
	
	//suite.addTest(new TestExamples("testFir", flags));
	//suite.addTest(new TestExamples("testHello6", flags));
	
	return suite;
	//return TestAll.makeTestSuite(flags);
    }

    
}
