/**
 * Class which runs defines which tests are run using the nightly regtest.
 * $Id: TestNightly.java,v 1.6 2003-09-24 14:23:07 dmaze Exp $
 **/
package streamittest;

import junit.framework.*;

public class TestNightly extends TestCase {

    public TestNightly(String name) {
	super (name);
    }


    /**
     * creates a test suite with all of the tests so far,
     * each of which is run using the specified options.
     **/
    public static Test makeTestSuite(int flags) {
        CompilerInterface cif =
            CompilerInterface.createCompilerInterface(flags);
	TestSuite suite = new TestSuite("Nightly " + cif.getOptionsString());
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

        // -O1
        allTests.addTest(makeTestSuite(CompilerInterface.NONE |
                                       CompilerInterface.ALTCODEGEN |
                                       CompilerInterface.DESTROYFIELDARRAY |
                                       CompilerInterface.RATEMATCH |
                                       CompilerInterface.WBS));

        // -O2
        allTests.addTest(makeTestSuite(CompilerInterface.NONE |
                                       CompilerInterface.UNROLL |
                                       CompilerInterface.ALTCODEGEN |
                                       CompilerInterface.DESTROYFIELDARRAY |
                                       CompilerInterface.RATEMATCH |
                                       CompilerInterface.REMOVE_GLOBALS |
                                       CompilerInterface.SIMULATEWORK |
                                       CompilerInterface.WBS));
    }
    
    /**
     * add the raw tests to the test suite framework.
     **/
    public static void addRawTests(TestSuite allTests) {
	// note : we only run with raw 4 and partition now.
	
	allTests.addTest(makeTestSuite(CompilerInterface.NONE |
				       CompilerInterface.RAW[4] |
				       CompilerInterface.PARTITION));

        // -O1
        allTests.addTest(makeTestSuite(CompilerInterface.NONE |
				       CompilerInterface.RAW[4] |
                                       CompilerInterface.PARTITION |
                                       CompilerInterface.ALTCODEGEN |
                                       CompilerInterface.DESTROYFIELDARRAY |
                                       CompilerInterface.RATEMATCH |
                                       CompilerInterface.WBS));

        // -O2
        allTests.addTest(makeTestSuite(CompilerInterface.NONE |
				       CompilerInterface.RAW[4] |
                                       CompilerInterface.PARTITION |
                                       CompilerInterface.UNROLL |
                                       CompilerInterface.ALTCODEGEN |
                                       CompilerInterface.DESTROYFIELDARRAY |
                                       CompilerInterface.RATEMATCH |
                                       CompilerInterface.REMOVE_GLOBALS |
                                       CompilerInterface.SIMULATEWORK |
                                       CompilerInterface.WBS));
    }


}
