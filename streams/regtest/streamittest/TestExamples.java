/**
 * Template framework for writing test cases using JUnit.
 * 1. Copy to new java file, and find/replace TestExamples with new name.
 * 2. Add an entry in AllTests.java for this new suite
 * 3. Add test code in void methods like testSimple
 * 4. Add a line in suite() with the new test method name
 *
 * You can then use the CompilerInterface compiler to run compiler sessions.
 * $Id: TestExamples.java,v 1.15 2002-08-09 16:42:22 aalamb Exp $
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
	
	//suite.addTest(new TestExamples("testArrayTest", flags));
	//suite.addTest(new TestExamples("testBitonicSort", flags));


	// can't fit on raw 4 or 8 without partition
	if (!(flagsContainRaw(flags) && !flagsContainPartition(flags))) {
	    suite.addTest(new TestExamples("testFFT3", flags));
	    suite.addTest(new TestExamples("testFFT_inlined", flags));
	}

	suite.addTest(new TestExamples("testFuse", flags));
	suite.addTest(new TestExamples("testFuseTest", flags));
	suite.addTest(new TestExamples("testFib", flags));
	suite.addTest(new TestExamples("testFib2", flags));
	suite.addTest(new TestExamples("testFir", flags));
	suite.addTest(new TestExamples("testFlybit", flags));
	suite.addTest(new TestExamples("testHello6", flags));
	suite.addTest(new TestExamples("testHelloSeparate", flags));
	suite.addTest(new TestExamples("testPeekPipe", flags));
	suite.addTest(new TestExamples("testSimpleSplit", flags));
	suite.addTest(new TestExamples("testUnroll", flags));

	// can't fit on raw 4 without partition
	if (!(flagsContainRaw4(flags) && !flagsContainPartition(flags))) {
	
	    suite.addTest(new TestExamples("testFm", flags));
	}
	
	
	suite.addTest(new TestExamples("testFile", flags));
	suite.addTest(new TestExamples("testFieldProp", flags));
	suite.addTest(new TestExamples("testFieldProp2", flags));
	suite.addTest(new TestExamples("testFieldInit", flags));
	suite.addTest(new TestExamples("testFieldInit2", flags));
	suite.addTest(new TestExamples("testFieldInit3", flags));
	suite.addTest(new TestExamples("testFieldInit4", flags));

	// this one doesn't fit on any raw4
	if (!flagsContainRaw4(flags)) {
	    suite.addTest(new TestExamples("testLattice", flags));
	}
	

	//suite.addTest(new TestExamples("testMergeSort", flags));
	//suite.addTest(new TestExamples("testUpDown", flags));
	suite.addTest(new TestExamples("testVectAdd", flags));
	suite.addTest(new TestExamples("testVectAdd1", flags));
	suite.addTest(new TestExamples("testWeightedRR", flags));
	suite.addTest(new TestExamples("testTwoWeightedRR", flags));

	// this one doesn't fit on any raw4
	if (!flagsContainRaw4(flags)) {
	    suite.addTest(new TestExamples("testNokiaFine", flags));
	}
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

    public void testFuse() {
	doCompileRunVerifyTest(EXAMPLE_ROOT + "fuse/",
			       "FusionTest.java",
			       "FusionTest.out",
			       0,1);
    }

    public void testFuseTest() {
	doCompileRunVerifyTest(EXAMPLE_ROOT + "fuse-test/",
			       "FuseTest.java",
			       "FuseTest.out",
			       0,3);
    }

    public void testFib() {
	doCompileRunVerifyTest(EXAMPLE_ROOT + "fib/",
			       "Fib.java",
			       "Fib.out",
			       0,1);
    }
    
    public void testFib2() {
	doCompileRunVerifyTest(EXAMPLE_ROOT + "fib2/",
			       "Fib2.java",
			       "Fib2.out",
			       0,1);
    }

    public void testFir() {
	String root = EXAMPLE_ROOT + "fir/";
	doMake(root);
	doCompileRunVerifyTest(root,
			       "LinkedFirTest.java",
			       "LinkedFirTest.out",
			       0,1);
    }

    public void testFlybit() {
	doCompileRunVerifyTest(EXAMPLE_ROOT + "flybit/",
			       "Flybit.java",
			       "Flybit.out",
			       0,4);
	
    }

    public void testHello6() {
	doCompileRunVerifyTest(EXAMPLE_ROOT + "hello6/",
			       "HelloWorld6.java",
			       "HelloWorld6.out",
			       0,1);
			
    }

    public void testHelloSeparate() {
	doCompileRunVerifyTest(EXAMPLE_ROOT + "hello-separate/",
			       "HelloSeparate.java",
			       "HelloSeparate.out",
			       0,1);
    }
	
    public void testPeekPipe() {
	doCompileRunVerifyTest(EXAMPLE_ROOT + "peek-pipe/",
			       "PeekPipe.java",
			       "PeekPipe.out",
			       0,1);
    }

    public void testSimpleSplit() {
	doCompileRunVerifyTest(EXAMPLE_ROOT + "simple-split/",
			       "SimpleSplit.java",
			       "SimpleSplit.out",
			       0,4);
    }

    public void testUnroll() {
	doCompileRunVerifyTest(EXAMPLE_ROOT + "unroll/",
			       "Unroll.java",
			       "Unroll.out",
			       0,1);
    }
    
    public void testFm() {
	String root = EXAMPLE_ROOT + "fm/"; 
	doMake(root);
	doCompileTest(root,
		      "LinkedFMTest.java");
	// run make, this time with the target extra-run
	// which changes the streamit makefile so the simulator runs
	// for more cycles
	doMake(root, "more-cycles");
	doRunTest(root,
		  "LinkedFMTest.java",
		  0,1);
	// do the comparison test
	doCompareTest(root,
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
			       "FieldPropTest.out",
			       0,2);
    }

    public void testFieldProp2() {
	doCompileRunVerifyTest(EXAMPLE_ROOT + "field-prop/",
			       "FieldPropTest2.java",
			       "FieldPropTest2.out",
			       0,2);
    }

    public void testFieldInit() {
	doCompileRunVerifyTest(EXAMPLE_ROOT + "field-init/",
			       "FieldInit.java",
			       "FieldInit.out",
			       0,1);
    }

    public void testFieldInit2() {
	doCompileRunVerifyTest(EXAMPLE_ROOT + "field-init/",
			       "FieldInit2.java",
			       "FieldInit2.out",
			       0,1);
    }

    public void testFieldInit3() {
	doCompileRunVerifyTest(EXAMPLE_ROOT + "field-init/",
			       "FieldInit3.java",
			       "FieldInit3.out",
			       0,1);
    }

    public void testFieldInit4() {
	doCompileRunVerifyTest(EXAMPLE_ROOT + "field-init/",
			       "FieldInit4.java",
			       "FieldInit4.out",
			       0,1);
    }

    public void testLattice() {
	doCompileRunVerifyTest(EXAMPLE_ROOT + "lattice/",
			       "Lattice.java",
			       "Lattice.out",
			       0,256);
    }

    public void testMergeSort() {
	doCompileRunVerifyTest(EXAMPLE_ROOT + "mergesort/",
			       "MergeSort.java",
			       "MergeSort.out",
			       0,16);
    }

    public void testUpDown() {
	// who knows what this is supposed to do?
	doCompileRunVerifyTest(EXAMPLE_ROOT + "updown/",
			       "UpDown.java",
			       "UpDown.out");
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

    public void testWeightedRR() {
	doCompileRunVerifyTest(EXAMPLE_ROOT + "weighted-rr/",
			       "WeightedRR.java",
			       "WeightedRR.out",
			       0,6);
    }

    public void testTwoWeightedRR() {
	doCompileRunVerifyTest(EXAMPLE_ROOT + "weighted-rr/",
			       "TwoWeightedRR.java",
			       "TwoWeightedRR.out",
			       0,6);
    }

    public void testNokiaFine() {
	String nokiaRoot = EXAMPLE_ROOT + "nokia-fine/";
	// create a linked version (and also bring in Delay.java)
	doMake(nokiaRoot, "link");
	doCompileTest(nokiaRoot,
		      "Linkeddcalc.java");
	// run the make script to make the app run a bit longer (like 5M cycles)
	doMake(nokiaRoot, "more-cycles");
	doRunTest(nokiaRoot,
		  "Linkeddcalc.java",
		  0,72);
	doCompareTest(nokiaRoot,
		      "Linkeddcalc.java",
		      "Linkeddcalc.out");
    }
}
