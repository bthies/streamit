/**
 * Runs the compiler with the options we want to check
 * before a cvs checkin turned on.
 * $Id: TestCheckin.java,v 1.1 2002-06-28 22:18:38 aalamb Exp $
 **/
package streamittest;

import junit.framework.*;

public class TestCheckin extends TestCase {

    public TestCheckin(String name) {
	super (name);
    }


    public static Test suite() {
	// simply return a test suite with a single set of options
	return TestAll.makeTestSuite(CompilerInterface.NONE);
    }

    
}
