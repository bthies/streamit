/**
 * Runs the compiler with the options we want to check
 * before a cvs checkin turned on if we fooled with raw.
 * $Id: TestCheckinRaw.java,v 1.1 2002-06-28 22:18:38 aalamb Exp $
 **/
package streamittest;

import junit.framework.*;

public class TestCheckinRaw extends TestCase {

    public TestCheckinRaw(String name) {
	super (name);
    }


    public static Test suite() {
	// simply return a test suite with a single set of options
	//return TestAll.makeTestSuite(CompilerInterface.NONE);
	return TestAll.makeTestSuite(CompilerInterface.NONE |
				     CompilerInterface.RAW4);
    }

    
}
