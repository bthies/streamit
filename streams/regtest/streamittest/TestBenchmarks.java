/**
 * For running the 
 *
 * You can then use the CompilerInterface compiler to run compiler sessions.
 * $Id: TestBenchmarks.java,v 1.26 2003-09-13 18:58:24 thies Exp $
 **/
package streamittest;

import junit.framework.*;


public class TestBenchmarks extends StreamITTestCase {
    static String STREAM_ROOT = null;
    static String BENCH_ROOT = null;
    /**
     * Creates a new TestBenchmarks which will use the compiler options
     * specified by flags (defined in CompilerInterface.java).
     **/
    public TestBenchmarks(String name, int flags) {
	super (name,flags);
	if (STREAM_ROOT == null) {
	    STREAM_ROOT = Harness.getStreamITRoot();
	    BENCH_ROOT = STREAM_ROOT + BENCH_PATH;
	}
    }

    public TestBenchmarks(String name) {
	this (name,
              CompilerInterface.NONE |
              CompilerInterface.RAW[4] |
              CompilerInterface.PARTITION);
    }

    public static Test suite(int flags) {
	TestSuite suite = new TestSuite();
        suite.addTest(new TestBenchmarks("testBitonicSortRecursive", flags));
	suite.addTest(new TestBenchmarks("testBitonicSort", flags));
	// can't fit on raw 8 without partition
	if (!flagsContainRaw(flags) || 
	    flagsContainPartition(flags) ||
	    flagsContainFusion(flags)) {
	    suite.addTest(new TestBenchmarks("testFir", flags));
	    suite.addTest(new TestBenchmarks("testVocoder", flags));
	    suite.addTest(new TestBenchmarks("testBeamFormer", flags));
	}
	suite.addTest(new TestBenchmarks("testSimple", flags));
        suite.addTest(new TestBenchmarks("testFft", flags));
        suite.addTest(new TestBenchmarks("testFilterbank", flags));
        suite.addTest(new TestBenchmarks("testFm", flags));
        // has a feedback loop, doesn't fuse:
        if (!flagsContainFusion(flags))
            suite.addTest(new TestBenchmarks("testGsm", flags));
        suite.addTest(new TestBenchmarks("testNokia", flags));
        suite.addTest(new TestBenchmarks("testMatMulBlock", flags));
        suite.addTest(new TestBenchmarks("testCFAR", flags));
        suite.addTest(new TestBenchmarks("testPerftest4", flags));

	return suite;
    }
    
    public void testSimple() {
	assertTrue("was true", true);
    }

    public void testBeamFormer() 
    {
        String root = BENCH_ROOT + "beamformer/streamit/";
        doMake(root);
	// first test java versions as hand-written
	testBeamFormerJava(root);
	// then generate java from new syntax and test those, too
        doSyntaxConvertTest(root, "BeamFormer.str", "BeamFormer.java");
        doSyntaxConvertTest(root, "CoarseBeamFormer.str", "CoarseBeamFormer.java");
        doSyntaxConvertTest(root, "SerializedBeamFormer.str", "SerializedBeamFormer.java");
        doSyntaxConvertTest(root, "CoarseSerializedBeamFormer.str", "CoarseSerializedBeamFormer.java");
	// run java tests again
	testBeamFormerJava(root);
    }

    // only tests the java versions of the programs
    private void testBeamFormerJava(String root) {
	// plain beamformer
        doCompileTest(root, "BeamFormer.java");
        doRunTest(root, "BeamFormer.java", 0, 4);
	// coarse-grained beamformer
        doCompileTest(root, "CoarseBeamFormer.java");
        doRunTest(root, "CoarseBeamFormer.java", 0, 128);
        // serialized versions, for output checking
	doCompileRunVerifyTest(root, "SerializedBeamFormer.java", "SerializedBeamFormer.out", 0, 4);
	doCompileRunVerifyTest(root, "CoarseSerializedBeamFormer.java", "CoarseSerializedBeamFormer.out", 0, 128);
    }

    // iterative version of bitonic sort
    public void testBitonicSort() 
    {
        String root = BENCH_ROOT + "bitonic-sort/streamit/";
        doCompileTest(root, "BitonicSort.java");
        doRunTest(root, "BitonicSort.java", 0, 32);
	doCompareTest(root, "BitonicSort.java", "BitonicSort.out");
    }

    // recursive version of bitonic sort
    public void testBitonicSortRecursive() 
    {
        String root = BENCH_ROOT + "bitonic-sort/streamit/";
	// recursive version
        doCompileTest(root, "BitonicSortRecursive.java");
        doRunTest(root, "BitonicSortRecursive.java", 0, 32);
	doCompareTest(root, "BitonicSortRecursive.java", "BitonicSort.out");
    }

    public void testFft()
    {
        String root = BENCH_ROOT + "fft/streamit/";
        doMake(root);
        doCompileTest(root, "LinkedFFT2.java");
        doRunTest(root, "LinkedFFT2.java", 0, 256);
	// do the comparison test
	doCompareTest(root,
		      "LinkedFFT2.java",
		      "LinkedFFT2.out");
    }

    public void testFilterbank()
    {
        String root = BENCH_ROOT + "filterbank/streamit/";
        doMake(root);
        doCompileTest(root, "LinkedFBtest.java");
        doRunTest(root, "LinkedFBtest.java", 0, 256);
	doCompareTest(root,
		      "LinkedFBtest.java",
		      "LinkedFBtest.out");    
    }

    public void testFir()
    {
        String root = BENCH_ROOT + "fir/streamit/";
        doCompileRunVerifyTest(root, "FIRfine.java", "FIRfine.out", 0, 6);
    }

    public void testFm() {
	String root = BENCH_ROOT + "fm/streamit/"; 
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

    public void testGsm() 
    {
        String root = BENCH_ROOT + "gsm/streamit/";
        doMake(root);
        doCompileTest(root, "Gsm.java");
        doMake(root, "more-imem");
        doRunTest(root, "Gsm.java", 0, 20);
    }

    public void testNokia()
    {
        String root = BENCH_ROOT + "nokia/streamit/";
        doMake(root);
        doCompileRunVerifyTest(root, "Linkeddcalc.java", "Linkeddcalc.out", 0, 4);
    }

    public void testVocoder()
    {
        String root = BENCH_ROOT + "vocoder/streamit/";
        doMake(root);
        doCompileTest(root, "LinkedVocoderToplevel.java");
        doMake(root, "more-imem"); 
        doRunTest(root, "LinkedVocoderToplevel.java", 0, 1);
        doCompareTest(root, "LinkedVocoderToplevel.java", "LinkedVocoderToplevel.out");
   }

    public void testMatMulBlock()
    {
        String root = BENCH_ROOT + "matmul-block/streamit/";
        doSyntaxConvertTest(root, "MatrixMultBlock.str", "MatrixMultBlock.java");
        doCompileTest(root, "MatrixMultBlock.java");
        doRunTest(root, "MatrixMultBlock.java", 0, 108);
        doCompareTest(root, "MatrixMultBlock.java", "MatrixMultBlock.out");
    }

    public void testCFAR()
    {
        String root = BENCH_ROOT + "cfar/streamit/";
        doSyntaxConvertTest(root, "CFARtest.str", "CFARtest.java");
	doCompileRunVerifyTest(root, "CFARtest.java", "CFARtest.out", 0, 64);
    }

    public void testPerftest4()
    {
        String root = BENCH_ROOT + "perftest4/streamit/";
	doMake(root);
	doCompileRunVerifyTest(root, "Linkedperftest4.java", "Linkedperftest4.out", 0, 4);
    }
}

