/**
 * Template framework for writing test cases using JUnit.
 * 1. Copy to new java file, and find/replace TestExamples with new name.
 * 2. Add an entry in AllTests.java for this new suite
 * 3. Add test code in void methods like testSimple
 * 4. Add a line in suite() with the new test method name
 *
 * You can then use the CompilerInterface compiler to run compiler sessions.
 * $Id: TestApps.java,v 1.1 2002-06-21 20:03:54 aalamb Exp $
 **/
package streamittest;

import junit.framework.*;


public class TestApps extends StreamITTestCase {
    static String STREAM_ROOT = null;
    static String APPS_ROOT = null;

    /**
     * Creates a new TestApps which will use the compiler options
     * specified by flags (defined in CompilerInterface.java).
     **/
    public TestApps(String name, int flags) {
	super (name,flags);
	if (STREAM_ROOT == null) {
	    STREAM_ROOT = getStreamITRoot();
	    APPS_ROOT = STREAM_ROOT + APPS_PATH;
	    
	}
    }

    public static Test suite(int flags) {
	TestSuite suite = new TestSuite();

	suite.addTest(new TestApps("testMatrixMult", flags));
	//suite.addTest(new TestApps("testGsm", flags));

	return suite;
    }
    

    public void testMatrixMult() {
	doCompileRunVerifyTest(APPS_ROOT + "matrixmult/",
			       "*.java",
			       "MatrixMult.out");
    }

    public void testGsm() {
	String root = APPS_ROOT + "gsm/";
	doMake(root);
	doCompileTest(root,
		      "Gsm.java");
    }

}
