/**
 * Template framework for writing test cases using JUnit.
 * 1. Copy to new java file, and find/replace TestExamples with new name.
 * 2. Add an entry in AllTests.java for this new suite
 * 3. Add test code in void methods like testSimple
 * 4. Add a line in suite() with the new test method name
 *
 * You can then use the CompilerInterface compiler to run compiler sessions.
 * $Id: TestExamples.java,v 1.17 2002-08-09 21:11:32 aalamb Exp $
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

    /**
     * Creates a suite containing test compilatons and runs for
     * (most of) the examples in the docs/examples/hand directory.
     **/
    public static Test suite(int flags) {
	TestSuite suite = new TestSuite();
	
	// can't fit on raw 4 or 8 without partition
	if (!(flagsContainRaw(flags) && !flagsContainPartition(flags))) {
	    suite.addTest(new TestExamples("testFFT3", flags));
	    suite.addTest(new TestExamples("testFFT_inlined", flags));
	}

	suite.addTest(new TestExamples("testAutoCor", flags));
	suite.addTest(new TestExamples("testChol", flags));

	suite.addTest(new TestExamples("testFib", flags));
	suite.addTest(new TestExamples("testFib2", flags));
	suite.addTest(new TestExamples("testFile", flags));
	suite.addTest(new TestExamples("testHello", flags));

	//suite.addTest(new TestExamples("testMatrixMult", flags));
	
	
	// this one doesn't fit on any raw4
	if (!flagsContainRaw4(flags)) {
	    suite.addTest(new TestExamples("testLattice", flags));
	}

	suite.addTest(new TestExamples("testVectAdd", flags));
	suite.addTest(new TestExamples("testVectAdd1", flags));

	
	return suite;
    }


    public void testAutoCor() {
	doCompileRunVerifyTest(EXAMPLE_ROOT + "autocor/",
			       "AutoCor.java",
			       "AutoCor.out",
			       0, 8);
    }

    public void testChol() {
	doCompileRunVerifyTest(EXAMPLE_ROOT + "chol-para/",
			       "chol.java",
			       "chol.out",
			       0, 5050);
    }


    
    public void testFFT3() {
	doCompileRunVerifyTest(EXAMPLE_ROOT + "fft/",
			       "FFT3.java",
			       "FFT3.out",
			       0, 64);
    }

    public void testFFT_inlined() {
	doCompileRunVerifyTest(EXAMPLE_ROOT + "fft/",
			       "FFT_inlined.java",
			       "FFT_inlined.out",
			       0, 32);
    }

    public void testFib() {
	doCompileRunVerifyTest(EXAMPLE_ROOT + "fib/",
			       "Fib.java",
			       "Fib.out",
			       0,1);
    }
    
    public void testFib2() {
	doCompileRunVerifyTest(EXAMPLE_ROOT + "fib/",
			       "Fib2.java",
			       "Fib2.out",
			       0,1);
    }

    public void testFile() {
	doCompileTest(EXAMPLE_ROOT + "file/",
		      "FileTest.java");
    }

    public void testHello() {
	doCompileRunVerifyTest(EXAMPLE_ROOT + "hello/",
			       "HelloWorld6.java",
			       "HelloWorld6.out",
			       0,1);
			
    }

    public void testLattice() {
	doCompileRunVerifyTest(EXAMPLE_ROOT + "lattice/",
			       "Lattice.java",
			       "Lattice.out",
			       0,256);
    }

    public void testMatrixMult() {
	doCompileRunVerifyTest(EXAMPLE_ROOT + "matrixmult/",
			       "MatrixMult.java",
			       "MatrixMult.out",
			       0,256);
    }

    

    public void testMergeSort() {
	doCompileRunVerifyTest(EXAMPLE_ROOT + "mergesort/",
			       "MergeSort.java",
			       "MergeSort.out",
			       0,16);
    }

    public void testVectAdd() {
	doCompileRunVerifyTest(EXAMPLE_ROOT + "vectadd/",
			       "VectAdd.java",
			       "VectAdd.out",
			       0,1);
    }

    public void testVectAdd1() {
	doCompileRunVerifyTest(EXAMPLE_ROOT + "vectadd/",
			       "VectAdd1.java",
			       "VectAdd1.out",
			       0,1);
    }

    
}
