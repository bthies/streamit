/**
 * Class which runs all of the test suites
 * $Id: TestAll.java,v 1.1 2002-06-20 21:19:56 aalamb Exp $
 **/
package streamittest;

import junit.framework.*;

public class TestAll extends TestCase {

    public TestAll(String name) {
	super (name);
    }


    /**
     * creates a test suite with all of the tests so far,
     * each of which is run using the specified options.
     **/
    public static Test makeTestSuite(int flags) {
	TestSuite suite = new TestSuite();
	suite.addTest(TestTemplate.suite(flags));
	suite.addTest(TestFieldProp.suite(flags));
	suite.addTest(TestFieldInit.suite(flags));
	return suite;	
    }


    public static Test suite() {
	TestSuite allTests = new TestSuite();
	    
	// try with no optimizations running
	allTests.addTest(makeTestSuite(CompilerInterface.NONE));

	// try with const prop
	allTests.addTest(makeTestSuite(CompilerInterface.NONE |
				       CompilerInterface.CONSTPROP));

	return allTests;
    }


}
