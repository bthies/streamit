/**
 * Template framework for writing test cases using JUnit.
 * 1. Copy to new java file, and find/replace TestExamples with new name.
 * 2. Add an entry in AllTests.java for this new suite
 * 3. Add test code in void methods like testSimple
 * 4. Add a line in suite() with the new test method name
 *
 * You can then use the CompilerInterface compiler to run compiler sessions.
 * $Id: TestSorts.java,v 1.1 2003-10-15 18:59:01 dmaze Exp $
 **/
package streamittest;

import junit.framework.*;


public class TestSorts extends StreamITTestCase {
    static String STREAM_ROOT = null;
    static String SORT_ROOT = null;

    public TestSorts(String name) {
	this(name, DEFAULT_FLAGS);
    }
    
    /**
     * Creates a new TestSorts which will use the compiler options
     * specified by flags (defined in CompilerInterface.java).
     **/
    public TestSorts(String name, int flags) {
	super (name,flags);
	if (STREAM_ROOT == null) {
	    STREAM_ROOT = Harness.getStreamITRoot();
	    SORT_ROOT = STREAM_ROOT + SORT_PATH;	    
	}
    }

    /**
     * Creates a suite containing test compilatons and runs for
     * (most of) the examples in the docs/examples/hand directory.
     **/
    public static Test suite(int flags) {
        CompilerInterface cif =
            CompilerInterface.createCompilerInterface(flags);
	TestSuite suite = new TestSuite("Sorts " + cif.getOptionsString());
	
	suite.addTest(new TestExamples("testBatcherSort", flags));
	suite.addTest(new TestExamples("testRadixSort", flags));
	suite.addTest(new TestExamples("testComparisonCounting", flags));
	suite.addTest(new TestExamples("testAutoBatcherSort", flags));
	suite.addTest(new TestExamples("testBubbleSort", flags));
	suite.addTest(new TestExamples("testInsertionSort", flags));
	suite.addTest(new TestExamples("testMergeSort", flags));
	
	return suite;
    }
    
    public void testMergeSort() 
    {
	String root = SORT_ROOT + "MergeSort/";
	doConvertCompileRunVerifyTest(root, "MergeSort", 0, 16);
    }

    public void testInsertionSort() 
    {
	String root = SORT_ROOT + "InsertionSort/";
	doConvertCompileRunVerifyTest(root, "InsertionSort", 0, 16);
    }

    public void testBubbleSort() 
    {
	String root = SORT_ROOT + "BubbleSort/";
	doConvertCompileRunVerifyTest(root, "BubbleSort", 0, 1);
    }
    
    public void testBatcherSort() 
    {
	String root = SORT_ROOT + "BatcherSort/";
	doConvertCompileRunVerifyTest(root, "BatcherSort", 0, 16);
    }
    
    public void testRadixSort() 
    {
	String root = SORT_ROOT + "RadixSort/";
	doConvertCompileRunVerifyTest(root, "RadixSort", 0, 16);
    }
    
    public void testComparisonCounting() 
    {
	String root = SORT_ROOT + "ComparisonCounting/";
	doConvertCompileRunVerifyTest(root, "ComparisonCounting", 0, 16);
    }
    
    public void testAutoBatcherSort() 
    {
	String root = SORT_ROOT + "BatcherSort/";
	doConvertCompileRunVerifyTest(root, "AutoBatcherSort", 0, 16);
    }
}
