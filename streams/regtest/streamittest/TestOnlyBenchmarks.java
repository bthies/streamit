/**
 * Class which runs defines which tests are run using the nightly regtest.
 * $Id: TestOnlyBenchmarks.java,v 1.2 2003-10-15 15:04:04 dmaze Exp $
 **/
package streamittest;

import junit.framework.*;

public class TestOnlyBenchmarks extends TestCase {

    public TestOnlyBenchmarks(String name) {
	super (name);
    }


    /**
     * creates a test suite with all of the tests so far,
     * each of which is run using the specified options.
     **/
    public static Test makeTestSuite(int flags) {
        CompilerInterface cif =
            CompilerInterface.createCompilerInterface(flags);
	TestSuite suite = new TestSuite("Benchmarks " + cif.getOptionsString());
	suite.addTest(TestBenchmarks.suite(flags));
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
	// note : we only run with raw 4 and partition now.
	
	allTests.addTest(makeTestSuite(CompilerInterface.NONE |
				       CompilerInterface.RAW[4]));

        // -O1
        allTests.addTest(makeTestSuite(CompilerInterface.NONE |
				       CompilerInterface.RAW[4] |
                                       CompilerInterface.PARTITION_DP |
                                       CompilerInterface.ALTCODEGEN |
                                       CompilerInterface.DESTROYFIELDARRAY |
                                       CompilerInterface.RATEMATCH |
                                       CompilerInterface.WBS));

        // -O2
        allTests.addTest(makeTestSuite(CompilerInterface.NONE |
				       CompilerInterface.RAW[4] |
                                       CompilerInterface.PARTITION_DP |
                                       CompilerInterface.UNROLL |
                                       CompilerInterface.ALTCODEGEN |
                                       CompilerInterface.DESTROYFIELDARRAY |
                                       CompilerInterface.RATEMATCH |
                                       CompilerInterface.REMOVE_GLOBALS |
                                       CompilerInterface.SIMULATEWORK |
                                       CompilerInterface.WBS));
    }


}
