/**
 * Template framework for writing test cases using JUnit.
 * 1. Copy to new java file, and find/replace TestFieldInit with new name.
 * 2. Add an entry in AllTests.java for this new suite
 * 3. Add test code in void methods like testSimple
 * 4. Add a line in suite() with the new test method name
 * $Id: TestFieldInit.java,v 1.1 2002-06-20 21:19:56 aalamb Exp $
 **/
package streamittest;

import junit.framework.*;


public class TestFieldInit extends StreamITTestCase {

    static String CODE_ROOT = null;

    /**
     * Creates a new TestFieldInit which will use the compiler options
     * specified by flags (defined in CompilerInterface.java).
     **/
    public TestFieldInit(String name, int flags) {
	super (name,flags);
	if (CODE_ROOT == null) {
	    CODE_ROOT = getStreamITRoot() + EXAMPLE_PATH + "field-init/";
	}
    }

    public static Test suite(int flags) {
	TestSuite suite = new TestSuite();
	suite.addTest(new TestFieldInit("testSimple", flags));
	suite.addTest(new TestFieldInit("testFieldInit", flags));
	suite.addTest(new TestFieldInit("testFieldInit2", flags));
	
	return suite;
    }
    
    public void testSimple() {
	assertTrue("was true", true);
    }

    public void testFieldInit() {
	doTests(CODE_ROOT,
		"FieldInit.java",
		"FieldInit.out");
				       
    }
    public void testFieldInit2() {
	doTests(CODE_ROOT,
		"FieldInit2.java",
		"FieldInit2.out");
				       
	
    }

	
}
