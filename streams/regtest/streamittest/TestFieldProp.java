/**
 * Class to test the workings of FieldProp.
 * $Id: TestFieldProp.java,v 1.1 2002-06-20 21:19:56 aalamb Exp $
 **/
package streamittest;

import junit.framework.*;

public class TestFieldProp extends StreamITTestCase {
    /**
     * Creates a new TestFieldProp which will use the compiler options
     * specified by flags (defined in CompilerInterface.java).
     **/
    public TestFieldProp(String name, int flags) {
	super (name,flags);
    }

    /** Create a test suite with the specified flags **/
    public static Test suite(int flags) {
	TestSuite suite = new TestSuite();
	suite.addTest(new TestFieldProp("testSimple", flags));

	return suite;
    }
    
    public void testSimple() {
	assertTrue("was true", true);
    }

}



