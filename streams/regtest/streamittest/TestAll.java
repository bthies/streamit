/**
 * Class which runs all of the test suites
 * $Id: TestAll.java,v 1.17 2002-11-27 01:31:38 thies Exp $
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
	/*
	addRawScaleTests(allTests);
	*/

	return allTests;
    }

    /**
     * add the uniprocessor tests to the test suite framework.
     **/
    public static void addUniprocessorTests(TestSuite allTests) {
	// try with just standard options
	allTests.addTest(makeTestSuite(CompilerInterface.NONE));

	// standard and fusion
	allTests.addTest(makeTestSuite(CompilerInterface.NONE |
				       CompilerInterface.FUSION));
    }
    
    /**
     * For testing scalability performance on RAW.
     */
    public static void addRawScaleTests(TestSuite allTests) {
	for (int i=2; i<=8; i+=2) {
 	    allTests.addTest(makeTestSuite(CompilerInterface.NONE |
 					   CompilerInterface.RAW[i] |
 					   CompilerInterface.DPPARTITION));
	}
    }

    /**
     * add the raw tests to the test suite framework.
     **/
    public static void addRawTests(TestSuite allTests) {
	// try one without partitioning just in case we want to
	// compare with original performance
	allTests.addTest(makeTestSuite(CompilerInterface.NONE |
				       CompilerInterface.RAW[8]));

	// try all configurations of raw with constprop and partition
	// This was causing the regtest to go crazy, so I am removing
	// all but 4
	for (int i=4; i<=8; i+=4) {
	    allTests.addTest(makeTestSuite(CompilerInterface.NONE |
					   CompilerInterface.RAW[4] |
					   CompilerInterface.PARTITION));
	}

	// try linear replacement (replace linear filters with a direct implementation).
// 	allTests.addTest(makeTestSuite(CompilerInterface.NONE |
// 				       CompilerInterface.CONSTPROP |
// 				       CompilerInterface.LINEAR_REPLACEMENT));
	
    }


}
