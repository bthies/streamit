/**
 * Class which runs all of the test suites
 * $Id: TestAll.java,v 1.4 2002-06-25 20:02:31 aalamb Exp $
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
	suite.addTest(TestExamples.suite(flags));
	suite.addTest(TestApps.suite(flags));
	suite.addTest(TestFieldInit.suite(flags));
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

	// const prop, unrolling, fusion
	allTests.addTest(makeTestSuite(CompilerInterface.NONE |
				       CompilerInterface.CONSTPROP |
				       CompilerInterface.FUSION));

	// const prop, unrolling, fusion, partitioning
	allTests.addTest(makeTestSuite(CompilerInterface.NONE |
				       CompilerInterface.CONSTPROP |
				       CompilerInterface.UNROLL |
				       CompilerInterface.FUSION |
				       CompilerInterface.PARTITION));
    }

    /**
     * add the raw tests to the test suite framework.
     **/
    public static void addRawTests(TestSuite allTests) {
	// raw with 4 tiles, 8 tiles
	// raw with 4 partitioning
	// const prop on/off for all three

	// raw with 4 tiles
	allTests.addTest(makeTestSuite(CompilerInterface.NONE |
				       CompilerInterface.RAW4));

	// raw with 4 tiles, constprop
	allTests.addTest(makeTestSuite(CompilerInterface.NONE |
				       CompilerInterface.RAW4 |
				       CompilerInterface.CONSTPROP));
				       
	// raw with 8 tiles
	allTests.addTest(makeTestSuite(CompilerInterface.NONE |
				       CompilerInterface.RAW8));

	// raw with 8 tiles, constprop
	allTests.addTest(makeTestSuite(CompilerInterface.NONE |
				       CompilerInterface.RAW8 |
				       CompilerInterface.CONSTPROP));
    }


}
