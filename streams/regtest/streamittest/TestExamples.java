/**
 * Template framework for writing test cases using JUnit.
 * 1. Copy to new java file, and find/replace TestExamples with new name.
 * 2. Add an entry in AllTests.java for this new suite
 * 3. Add test code in void methods like testSimple
 * 4. Add a line in suite() with the new test method name
 *
 * You can then use the CompilerInterface compiler to run compiler sessions.
 * $Id: TestExamples.java,v 1.5 2002-07-01 21:55:57 aalamb Exp $
 **/
package streamittest;

import junit.framework.*;


public class TestExamples extends StreamITTestCase {
    static String STREAM_ROOT = null;
    static String EXAMPLE_ROOT = null;

    public TestExamples(String name) {
	this(name, DEFAULT_FLAGS);
    }
    
    /**
     * Creates a new TestExamples which will use the compiler options
     * specified by flags (defined in CompilerInterface.java).
     **/
    public TestExamples(String name, int flags) {
	super (name,flags);
	if (STREAM_ROOT == null) {
	    STREAM_ROOT = Harness.getStreamITRoot();
	    EXAMPLE_ROOT = STREAM_ROOT + EXAMPLE_PATH;
	    
	}
    }

    public static Test suite(int flags) {
	TestSuite suite = new TestSuite();
	
	//suite.addTest(new TestExamples("testArrayTest", flags));
	//suite.addTest(new TestExamples("testBitonicSort", flags));
	suite.addTest(new TestExamples("testFFT", flags));
	suite.addTest(new TestExamples("testFusion", flags));
	suite.addTest(new TestExamples("testFib", flags));
	suite.addTest(new TestExamples("testFib2", flags));
	suite.addTest(new TestExamples("testFir", flags));
	suite.addTest(new TestExamples("testFlybit", flags));
	suite.addTest(new TestExamples("testHello6", flags));
	suite.addTest(new TestExamples("testHelloSeparate", flags));
	suite.addTest(new TestExamples("testPeekPipe", flags));
	suite.addTest(new TestExamples("testSimpleSplit", flags));
	suite.addTest(new TestExamples("testUnroll", flags));
	suite.addTest(new TestExamples("testFm", flags));
	suite.addTest(new TestExamples("testFile", flags));
	suite.addTest(new TestExamples("testFieldProp", flags));
	suite.addTest(new TestExamples("testFieldProp2", flags));
	suite.addTest(new TestExamples("testLattice", flags));
	//suite.addTest(new TestExamples("testMergeSort", flags));
	//suite.addTest(new TestExamples("testUpDown", flags));
	//suite.addTest(new TestExamples("testVectAdd", flags));
	suite.addTest(new TestExamples("testWeightedRR", flags));
	return suite;
    }

    public void testArrayTest() {
	doCompileRunVerifyTest(EXAMPLE_ROOT + "arraytest/",
			       "ArrayTest.java",
			       "ArrayTest.out");
    }

    public void testBitonicSort() {
	doCompileRunVerifyTest(EXAMPLE_ROOT + "bitonic-sort/",
			       "BitonicSort.java",
			       "BitonicSort.out");
    }

    public void testFFT() {
	doCompileRunVerifyTest(EXAMPLE_ROOT + "fft/",
			       "FFT_inlined.java",
			       "FFT.out");
    }

    public void testFusion() {
	doCompileRunVerifyTest(EXAMPLE_ROOT + "fuse-test/",
			       "FuseTest.java",
			       "FuseTest.out");
    }
    
    public void testFib() {
	doCompileTest(EXAMPLE_ROOT + "fib/",
		      "Fib.java");
    }
    
    public void testFib2() {
	doCompileTest(EXAMPLE_ROOT + "fib2/",
		      "Fib2.java");
    }

    public void testFir() {
	String root = EXAMPLE_ROOT + "fir/";
	doMake(root);
	doCompileRunVerifyTest(root,
			       "LinkedFirTest.java",
			       "LinkedFirTest.out");
    }

    public void testFlybit() {
	doCompileTest(EXAMPLE_ROOT + "flybit/",
		      "Flybit.java");
    }

    public void testHello6() {
	doCompileRunVerifyTest(EXAMPLE_ROOT + "hello6/",
			       "HelloWorld6.java",
			       "HelloWorld6.out");
			
    }

    public void testHelloSeparate() {
	doCompileTest(EXAMPLE_ROOT + "hello-separate/",
		      "HelloSeparate.java");
    }
	
    public void testPeekPipe() {
	doCompileTest(EXAMPLE_ROOT + "peek-pipe/",
		      "PeekPipe.java");
    }

    public void testSimpleSplit() {
	doCompileTest(EXAMPLE_ROOT + "simple-split/",
		      "SimpleSplit.java");
    }

    public void testUnroll() {
	doCompileTest(EXAMPLE_ROOT + "unroll/",
		      "Unroll.java");
    }
    
    public void testFm() {
	String root = EXAMPLE_ROOT + "fm/"; 
	doMake(root);
	doCompileRunVerifyTest(root,
			       "LinkedFMTest.java",
			       "LinkedFMTest.out");
    }

    public void testFile() {
	doCompileTest(EXAMPLE_ROOT + "file/",
		      "FileTest.java");
    }

    public void testFieldProp() {
	doCompileRunVerifyTest(EXAMPLE_ROOT + "field-prop/",
			       "FieldPropTest.java",
			       "FieldPropTest.out");
    }

    public void testFieldProp2() {
	doCompileRunVerifyTest(EXAMPLE_ROOT + "field-prop/",
			       "FieldPropTest2.java",
			       "FieldPropTest2.out");
    }

    public void testLattice() {
	doCompileRunVerifyTest(EXAMPLE_ROOT + "lattice/",
			       "Lattice.java",
			       "Lattice.out");
    }

    public void testMergeSort() {
	doCompileRunVerifyTest(EXAMPLE_ROOT + "mergesort/",
			       "MergeSort.java",
			       "MergeSort.out");
    }

    public void testUpDown() {
	doCompileRunVerifyTest(EXAMPLE_ROOT + "updown/",
			       "UpDown.java",
			       "UpDown.out");
    }

    public void testVectAdd() {
	doCompileRunVerifyTest(EXAMPLE_ROOT + "vectadd/",
			       "VectAdd.java",
			       "VectAdd.out");
    }

    public void testWeightedRR() {
	doCompileRunVerifyTest(EXAMPLE_ROOT + "weighted-rr/",
			       "WeightedRR.java",
			       "WeightedRR.out");
    }

    

    

}
