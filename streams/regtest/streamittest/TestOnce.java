/**
 * Runs the compiler on the all of the tests with no optimizations
 * turned on.
 * $Id: TestOnce.java,v 1.1 2002-06-28 22:18:38 aalamb Exp $
 **/
package streamittest;

import junit.framework.*;

public class TestOnce extends TestCase {

    public TestOnce(String name) {
	super (name);
    }


    public static Test suite() {
	// simply return a test suite with a single set of options
	//return TestAll.makeTestSuite(CompilerInterface.NONE);
	return TestAll.makeTestSuite(CompilerInterface.NONE |
				     CompilerInterface.RAW4);
    }

    
}
