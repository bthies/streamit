/**
 * Class which runs all of the test suites
 * $Id: TestAll.java,v 1.2 2002-06-21 20:03:54 aalamb Exp $
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
	suite.addTest(TestApps.suite(flags));
	suite.addTest(TestExamples.suite(flags));
	suite.addTest(TestTemplate.suite(flags));
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

	// try with const prop and unrolling
	allTests.addTest(makeTestSuite(CompilerInterface.NONE |
				       CompilerInterface.CONSTPROP |
				       CompilerInterface.UNROLL));

	// const prop, unrolling, fusion
	allTests.addTest(makeTestSuite(CompilerInterface.NONE |
				       CompilerInterface.CONSTPROP |
				       CompilerInterface.UNROLL |
				       CompilerInterface.FUSION));

	// const prop, unrolling, fusion, partitioning
	allTests.addTest(makeTestSuite(CompilerInterface.NONE |
				       CompilerInterface.CONSTPROP |
				       CompilerInterface.UNROLL |
				       CompilerInterface.FUSION |
				       CompilerInterface.PARTITION));
			 

	return allTests;
    }


}
