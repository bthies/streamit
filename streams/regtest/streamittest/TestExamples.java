/**
 * Template framework for writing test cases using JUnit.
 * 1. Copy to new java file, and find/replace TestExamples with new name.
 * 2. Add an entry in AllTests.java for this new suite
 * 3. Add test code in void methods like testSimple
 * 4. Add a line in suite() with the new test method name
 *
 * You can then use the CompilerInterface compiler to run compiler sessions.
 * $Id: TestExamples.java,v 1.39 2003-10-16 18:07:53 dmaze Exp $
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
        CompilerInterface cif =
            CompilerInterface.createCompilerInterface(flags);
	TestSuite suite = new TestSuite("Examples " + cif.getOptionsString());
	
	// overflows instruction memory on raw if there is fusion
	if (!(flagsContainRaw(1, flags))) {
	    suite.addTest(new TestExamples("testAutoCor", flags));
	}

	// contains mutually recursive definitions, and is only in new syntax
	//suite.addTest(new TestExamples("testChol", flags)); 

        // things that don't fuse:
        if (!flagsContainFusion(flags))
        {
            // feedback loop
            suite.addTest(new TestExamples("testFib", flags));
            suite.addTest(new TestExamples("testFibFeed", flags));
            suite.addTest(new TestExamples("testFib2", flags));
            // file reader
            suite.addTest(new TestExamples("testFile", flags));
        }
        
	suite.addTest(new TestExamples("testHello", flags));

	// test only 8 tile since code size is a problem otherwise
	if (!(flagsContainRaw(flags) && !flagsContainRaw(8, flags))) {
	    suite.addTest(new TestExamples("testMatrixMult", flags));
	}

	suite.addTest(new TestExamples("testmergesort", flags));

	// this one doesn't fit on any raw4, and doesn't work with fusion
	if (!(flagsContainRaw(1, flags) || 
	      flagsContainRaw(2, flags) || 
	      flagsContainRaw(3, flags) || 
	      flagsContainRaw(4, flags) ||
              flagsContainFusion(flags))) {
	    suite.addTest(new TestExamples("testLattice", flags));
	}

	suite.addTest(new TestExamples("testVectAdd", flags));
	suite.addTest(new TestExamples("testVectAdd1", flags));
	
	suite.addTest(new TestExamples("testPhase", flags));
	suite.addTest(new TestExamples("testchol", flags));
	suite.addTest(new TestExamples("testMedian", flags));
	
	return suite;
    }
    
    public void testPhase() 
    {
	String root = EXAMPLE_ROOT + "phase/";
        doSyntaxConvertTest(root, "PhaseTest.str", "PhaseTest.java");
        doCompileTest(root, "PhaseTest.java");
    }
    
    public void testchol() 
    {
	String root = EXAMPLE_ROOT + "chol-para/";
	doConvertCompileRunVerifyTest(root, "chol", 0, 5050);
    }
    
    public void testMedian() 
    {
	String root = EXAMPLE_ROOT + "median/";
	doConvertCompileRunVerifyTest(root, "Median", 0, 160);
    }
    
    
    public void testAutoCor() {
        doSyntaxConvertTest(EXAMPLE_ROOT + "autocor/",
                            "AutoCor.str", "AutoCor.java");
	doCompileRunVerifyTest(EXAMPLE_ROOT + "autocor/",
			       "AutoCor.java",
			       "AutoCor.out",
			       0, 8);
    }

    public void testChol() {
        doSyntaxConvertTest(EXAMPLE_ROOT + "chol-para/",
                            "chol.str", "chol.java");
	doCompileRunVerifyTest(EXAMPLE_ROOT + "chol-para/",
			       "chol.java",
			       "chol.out",
			       0, 5050);
    }

    public void testFib() {
	String root = EXAMPLE_ROOT + "fib/";
        doSyntaxConvertTest(root, "Fib.str", "Fib.java");
	doCompileRunVerifyTest(root,
			       "Fib.java",
			       "Fib.out",
			       0,1);
    }
    
    public void testFibFeed() {
	String root = EXAMPLE_ROOT + "fib/";
        doSyntaxConvertTest(root, "FibFeed.str", "FibFeed.java");
	doCompileRunVerifyTest(root,
			       "FibFeed.java",
			       "Fib.out",
			       0,1);
    }
    
    public void testFib2() {
        doSyntaxConvertTest(EXAMPLE_ROOT + "fib/",
                            "Fib2.str", "Fib2.java");
	doCompileRunVerifyTest(EXAMPLE_ROOT + "fib/",
			       "Fib2.java",
			       "Fib2.out",
			       0,1);
    }

    public void testFile() {
        doSyntaxConvertTest(EXAMPLE_ROOT + "file/",
                            "FileTest.str", "FileTest.java");
	doCompileTest(EXAMPLE_ROOT + "file/",
		      "FileTest.java");
    }

    public void testHello() {
        doSyntaxConvertTest(EXAMPLE_ROOT + "hello/",
                            "HelloWorld6.str", "HelloWorld6.java");
	doCompileRunVerifyTest(EXAMPLE_ROOT + "hello/",
			       "HelloWorld6.java",
			       "HelloWorld6.out",
			       0,1);
			
    }

    public void testLattice() {
        doSyntaxConvertTest(EXAMPLE_ROOT + "lattice/",
                            "Lattice.str", "Lattice.java");
	doCompileRunVerifyTest(EXAMPLE_ROOT + "lattice/",
			       "Lattice.java",
			       "Lattice.out",
			       0,256);
    }

    public void testMatrixMult() {
        doSyntaxConvertTest(EXAMPLE_ROOT + "matrixmult/",
                            "MatrixMult.str", "MatrixMult.java");
	doCompileRunVerifyTest(EXAMPLE_ROOT + "matrixmult/",
			       "MatrixMult.java",
			       "MatrixMult.out",
			       0,256);
    }

    

    public void testmergesort() {
	String root = EXAMPLE_ROOT + "mergesort/";
        doSyntaxConvertTest(root, "MergeSort.str", "MergeSort.java");
	doCompileRunVerifyTest(root,
			       "MergeSort.java",
			       "MergeSort.out",
			       0,16);
    }
    
    public void testVectAdd() {
        doSyntaxConvertTest(EXAMPLE_ROOT + "vectadd/",
                            "VectAdd.str", "VectAdd.java");
	doCompileRunVerifyTest(EXAMPLE_ROOT + "vectadd/",
			       "VectAdd.java",
			       "VectAdd.out",
			       0,1);
    }

    public void testVectAdd1() {
        doSyntaxConvertTest(EXAMPLE_ROOT + "vectadd/",
                            "VectAdd1.str", "VectAdd1.java");
	doCompileRunVerifyTest(EXAMPLE_ROOT + "vectadd/",
			       "VectAdd1.java",
			       "VectAdd1.out",
			       0,1);
    }

    
}
