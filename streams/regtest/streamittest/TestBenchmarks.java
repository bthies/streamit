/**
 * For running the 
 *
 * You can then use the CompilerInterface compiler to run compiler sessions.
 * $Id: TestBenchmarks.java,v 1.7 2002-10-04 14:35:45 dmaze Exp $
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
              CompilerInterface.CONSTPROP |
              CompilerInterface.PARTITION);
    }

    public static Test suite(int flags) {
	TestSuite suite = new TestSuite();
	suite.addTest(new TestBenchmarks("testSimple", flags));
        suite.addTest(new TestBenchmarks("testBeamFormer", flags));
        suite.addTest(new TestBenchmarks("testBitonicSort", flags));
        suite.addTest(new TestBenchmarks("testFft", flags));
        suite.addTest(new TestBenchmarks("testFilterbank", flags));
        suite.addTest(new TestBenchmarks("testFir", flags));
        suite.addTest(new TestBenchmarks("testFm", flags));
        suite.addTest(new TestBenchmarks("testGsm", flags));
        suite.addTest(new TestBenchmarks("testNokia", flags));
        suite.addTest(new TestBenchmarks("testVocoder", flags));

	return suite;
    }
    
    public void testSimple() {
	assertTrue("was true", true);
    }

    public void testBeamFormer() 
    {
        String root = BENCH_ROOT + "beamformer/streamit/";
        doCompileTest(root, "BeamFormer.java");
        doRunTest(root, "BeamFormer.java", 0, 4);
    }

    public void testBitonicSort() 
    {
        String root = BENCH_ROOT + "bitonic-sort/streamit/";
        doCompileTest(root, "BitonicSort.java");
        doRunTest(root, "BitonicSort.java", 0, 32);
    }

    public void testFft()
    {
        String root = BENCH_ROOT + "fft/streamit/";
        doMake(root);
        doCompileTest(root, "LinkedFFT2.java");
        doRunTest(root, "LinkedFFT2.java", 0, 256);
    }

    public void testFilterbank()
    {
        String root = BENCH_ROOT + "filterbank/streamit/";
        doMake(root);
        doCompileTest(root, "LinkedFBtest.java");
        doRunTest(root, "LinkedFBtest.java", 0, 256);
    }

    public void testFir()
    {
        String root = BENCH_ROOT + "fir/streamit/";
        doCompileTest(root, "FIRfine.java");
        doRunTest(root, "FIRfine.java", 0, 6);
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
        doRunTest(root, "Gsm.java", 0, 20);
    }

    public void testNokia()
    {
        String root = BENCH_ROOT + "nokia/streamit/";
        doMake(root);
        doCompileTest(root, "Linkeddcalc.java");
        doRunTest(root, "Linkeddcalc.java", 0, 4);
    }

    public void testVocoder()
    {
        String root = BENCH_ROOT + "vocoder/streamit/";
        doMake(root);
        doCompileTest(root, "LinkedVocoder.java");
        doMake(root, "more-imem");
        doRunTest(root, "LinkedVocoder.java", 0, 1);
    }
}

