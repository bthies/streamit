/**
 * Template framework for writing test cases using JUnit.
 * 1. Copy to new java file, and find/replace TestTemplate with new name.
 * 2. Add an entry in AllTests.java for this new suite
 * 3. Add test code in void methods like testSimple
 * 4. Add a line in suite() with the new test method name
 *
 * You can then use the CompilerInterface compiler to run compiler sessions.
 * $Id: TestTemplate.java,v 1.4 2003-01-29 22:12:31 aalamb Exp $
 **/
package streamittest;

import junit.framework.*;


public class TestTemplate extends StreamITTestCase {
    static String STREAM_ROOT = null;
    /**
     * Creates a new TestTemplate which will use the compiler options
     * specified by flags (defined in CompilerInterface.java).
     **/
    public TestTemplate(String name, int flags) {
	super (name,flags);
	if (STREAM_ROOT == null) {
	    STREAM_ROOT = Harness.getStreamITRoot();
	}
    }


    /**
     * The main entry point for running alone. Eg if we run
     * java junit.textui.TestRunner streamittest.TestTemplate
     * this method will be called to determine which tests to run.
     * If this method is not here, then the default action of the
     * test runner is to execute all public void methods that start
     * with "test" which it determines by reflection. Note that this
     * method does not have the flags argument
     **/
    public static Test suite() {
	int testFlags = CompilerInterface.NONE;
	TestSuite suite = new TestSuite();
	suite.addTest(TestTemplate.suite(testFlags));
	
	return suite;
    }

    
    /** Returns the full suite of tests for this particular set of apps. **/
    public static Test suite(int flags) {
	TestSuite suite = new TestSuite();
	suite.addTest(new TestTemplate("testSimple", flags));
	
	return suite;
    }    
    
    public void testSimple() {
	assertTrue("was true", true);
    }

}
