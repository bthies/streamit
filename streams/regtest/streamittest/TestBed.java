/**
 * This should help you test your bed. Next version.
 * Currently used as scratch space for testing a small
 * subset of the total test cases.
 * $Id: TestBed.java,v 1.6 2002-07-17 18:35:38 aalamb Exp $
 **/
package streamittest;

import junit.framework.*;

public class TestBed extends StreamITTestCase {


    public TestBed(String name, int flags) {
	super (name, flags);
    }




    public static Test suite() {
	int flags = (CompilerInterface.NONE |
		     CompilerInterface.RAW4,
		     CompilerInterface.PARTITION);
	//int flags = CompilerInterface.NONE;

	TestSuite suite = new TestSuite();

	//suite.addTest(new TestApps("testAppsFM", flags));
	//suite.addTest(new TestExamples("testNokiaFine", flags));

	//suite.addTest(new TestExamples("testFib", flags));
	//suite.addTest(new TestExamples("testFib2", flags));
	//suite.addTest(new TestExamples("testFir", flags));
	//suite.addTest(new TestExamples("testFm", flags));

	suite.addTest(TestExamples.suite(flags));
	
	//suite.addTest(new TestExamples("testFFT3", flags));
	//suite.addTest(new TestExamples("testFFT_inlined", flags));
		      
	//suite.addTest(new TestBed("testStatic", flags));
	
	return suite;
	//return TestAll.makeTestSuite(flags);
    }


    public void testStatic() {
	doCompileTest(Harness.getStreamITRoot() + "../test",
 		      "StaticTest.java");
    }

    
    
}
