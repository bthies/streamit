/**
 * Class which runs all of the test suites
 * $Id: TestAll.java,v 1.14 2002-10-29 01:11:56 thies Exp $
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
	suite.addTest(TestTests.suite(flags));
	suite.addTest(TestExamples.suite(flags));
	suite.addTest(TestApps.suite(flags));
	suite.addTest(TestBenchmarks.suite(flags));
	suite.addTest(TestTemplate.suite(flags));
	return suite;	
    }


    public static Test suite() {
	TestSuite allTests = new TestSuite();

	addUniprocessorTests(allTests);
	addRawTests(allTests);

	return allTests;
    }

    /**
     * add the uniprocessor tests to the test suite framework.
     **/
    public static void addUniprocessorTests(TestSuite allTests) {
	// try with no optimizations running
	allTests.addTest(makeTestSuite(CompilerInterface.NONE));
	
	// try with const prop
	allTests.addTest(makeTestSuite(CompilerInterface.NONE |
				       CompilerInterface.CONSTPROP));

	// fusion
	allTests.addTest(makeTestSuite(CompilerInterface.NONE |
				       CompilerInterface.FUSION));

	// const prop and fusion
	allTests.addTest(makeTestSuite(CompilerInterface.NONE |
				       CompilerInterface.CONSTPROP |
				       CompilerInterface.FUSION));
    }
    
    /**
     * add the raw tests to the test suite framework.
     **/
    public static void addRawTests(TestSuite allTests) {
	// test on raw 4 and raw 8 without fieldprop, just to see if a
	// fieldprop problem (or performance issue) is local to these
	allTests.addTest(makeTestSuite(CompilerInterface.NONE |
				       CompilerInterface.RAW[4] |
				       CompilerInterface.PARTITION));

	allTests.addTest(makeTestSuite(CompilerInterface.NONE |
				       CompilerInterface.RAW[8] |
				       CompilerInterface.PARTITION));

	// try one without partitioning just in case we want to
	// compare with original performance
	allTests.addTest(makeTestSuite(CompilerInterface.NONE |
				       CompilerInterface.RAW[8]));

	// try all configurations of raw with constprop and partition
	// This was causing the regtest to go crazy, so I am removing
	// all but 4
	// 	for (int i=1; i<=8; i++) {
 	    allTests.addTest(makeTestSuite(CompilerInterface.NONE |
 					   CompilerInterface.RAW[4] |
 					   CompilerInterface.PARTITION |
 					   CompilerInterface.CONSTPROP));
	    // 	}

	// try linear replacement (replace linear filters with a direct implementation).
// 	allTests.addTest(makeTestSuite(CompilerInterface.NONE |
// 				       CompilerInterface.CONSTPROP |
// 				       CompilerInterface.LINEAR_REPLACEMENT));
	
    }


}
