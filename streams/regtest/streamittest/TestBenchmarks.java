/**
 * For running the 
 *
 * You can then use the CompilerInterface compiler to run compiler sessions.
 * $Id: TestBenchmarks.java,v 1.38 2003-10-06 21:02:04 dmaze Exp $
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

	// this one doesn't fit on raw 8 without partitioning,
        // and doesn't fuse (it has a filereader).
        if (!flagsContainFusion(flags) &&
            (!flagsContainRaw(8, flags) ||
             flagsContainPartition(flags)))
	    suite.addTest(new TestApps("testMP3Simple", flags));

	return suite;
    }
    
    public void testSimple() {
	assertTrue("was true", true);
    }

    public void testBeamFormer() 
    {
        String root = BENCH_ROOT + "beamformer/streamit/";
        doMake(root);
	// plain beamformer
        doSyntaxConvertTest(root, "BeamFormer.str", "BeamFormer.java");
        doCompileTest(root, "BeamFormer.java");
        doRunTest(root, "BeamFormer.java", 0, 4);
	// coarse-grained beamformer
        doSyntaxConvertTest(root, "CoarseBeamFormer.str", "CoarseBeamFormer.java");
        doCompileTest(root, "CoarseBeamFormer.java");
        doRunTest(root, "CoarseBeamFormer.java", 0, 128);
        // serialized versions, for output checking
        doSyntaxConvertTest(root, "SerializedBeamFormer.str", "SerializedBeamFormer.java");
	doCompileRunVerifyTest(root, "SerializedBeamFormer.java", "SerializedBeamFormer.out", 0, 4);
        doSyntaxConvertTest(root, "CoarseSerializedBeamFormer.str", "CoarseSerializedBeamFormer.java");
	doCompileRunVerifyTest(root, "CoarseSerializedBeamFormer.java", "CoarseSerializedBeamFormer.out", 0, 128);
    }

    // iterative version of bitonic sort
    public void testBitonicSort() 
    {
        String root = BENCH_ROOT + "bitonic-sort/streamit/";
        doSyntaxConvertTest(root, "BitonicSort.str", "BitonicSort.java");
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
        doSyntaxConvertTest(root, "FFT2.str", "FFT2.java");
        doCompileTest(root, "FFT2.java");
        doRunTest(root, "FFT2.java", 0, 256);
        doCompareTest(root, "FFT2.java", "LinkedFFT2.out");
    }

    public void testFilterbank()
    {
        String root = BENCH_ROOT + "filterbank/streamit/";
        doSyntaxConvertTest(root, "FilterBankNew.str", "FilterBankNew.java");
        doCompileRunVerifyTest(root, "FilterBankNew.java",
                               "LinkedFBtest.out", 0, 256);
    }

    public void testFir()
    {
        String root = BENCH_ROOT + "fir/streamit/";
        doSyntaxConvertTest(root, "FIR.str", "FIR.java");
        doCompileRunVerifyTest(root, "FIR.java", "FIRfine.out", 0, 6);
    }

    public void testFm() {
	String root = BENCH_ROOT + "fm/streamit/"; 
        // new syntax:
        doSyntaxConvertTest(root, "FMRadio.str", "FMRadio.java");
        doCompileRunVerifyTest(root, "FMRadio.java", "FMRadio.out",
                               0, 1);
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

        doSyntaxConvertTest(root, "perftest.str", "perftest.java");
        doCompileRunVerifyTest(root, "perftest.str", "Linkedperftest4.out", 0, 4);
    }

    public void testMP3Simple()
    {
        String root = BENCH_ROOT + "mp3decoder/";
	doMake(root);
	doCompileRunVerifyTest(root, "LinkedMP3Simple.java", "MP3Simple.out", 0, 1152);

        doSyntaxConvertTest(root, "MP3.str", "MP3.java");
        doCompileRunVerifyTest(root, "MP3.java", "MP3Simple.out", 0, 1152);
    }
    
}

