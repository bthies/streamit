/**
 * Class which runs all of the test suites
 * $Id: TestAll.java,v 1.21 2003-10-05 00:39:04 thies Exp $
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
	// try with just standard options
	allTests.addTest(makeTestSuite(CompilerInterface.NONE));

	// standard and fusion
	allTests.addTest(makeTestSuite(CompilerInterface.NONE |
				       CompilerInterface.FUSION));
    }
    
    /**
     * add the raw tests to the test suite framework.
     **/
    public static void addRawTests(TestSuite allTests) {
	// try one without partitioning just in case we want to
	// compare with original performance
	allTests.addTest(makeTestSuite(CompilerInterface.NONE |
				       CompilerInterface.RAW[8]));

	// try one without partitioning just in case we want to
	// compare with original performance
	allTests.addTest(makeTestSuite(CompilerInterface.NONE |
				       CompilerInterface.RAW[4] | 
				       CompilerInterface.DPPARTITION));

	// try all configurations of raw with constprop and partition
	// This was causing the regtest to go crazy, so I am removing
	// all but 4
 	for (int i=4; i<=8; i+=4) {
	    allTests.addTest(makeTestSuite(CompilerInterface.NONE |
					   CompilerInterface.RAW[i] |
					   CompilerInterface.PARTITION));
	}

    }


}
