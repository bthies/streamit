/**
 * Interface for the PLDI 02 benchmarks in the 
 * $Id: TestPldi03Benchmarks.java,v 1.1 2003-01-29 22:13:42 aalamb Exp $
 **/
package streamittest;

import junit.framework.*;


public class TestPldi03Benchmarks extends StreamITTestCase {
    static String STREAM_ROOT = null;
    static String PLDI_ROOT = null;

    /**
     * Creates a new TestPldi03Benchmarks which will use the compiler options
     * specified by flags (defined in CompilerInterface.java).
     **/
    public TestPldi03Benchmarks(String name, int flags) {
	super (name,flags);
	if (STREAM_ROOT == null) {
	    STREAM_ROOT = Harness.getStreamITRoot();
	    PLDI_ROOT = STREAM_ROOT + PLDI_PATH;
	}
    }


    /**
     * Overide default action for suite with no flags.
     * Eg this method gets called if TestPldo03Benchmarks is used as
     * the base class
     **/
    public static Test suite() {
	TestSuite suite = new TestSuite();
	suite.addTest(TestPldi03Benchmarks.suite(CompilerInterface.NONE));
	// linear replacement
	suite.addTest(TestPldi03Benchmarks.suite(CompilerInterface.NONE | CompilerInterface.DEBUG | CompilerInterface.UNROLL | CompilerInterface.LINEAR_REPLACEMENT));
	// frequency replacement
	suite.addTest(TestPldi03Benchmarks.suite(CompilerInterface.NONE | CompilerInterface.DEBUG | CompilerInterface.UNROLL | CompilerInterface.FREQ_REPLACEMENT));
	return suite;
    }


    
    /**
     * Main interface for running just this set of benchmarks.
     **/
    public static Test suite(int flags) {
	TestSuite suite = new TestSuite();
	suite.addTest(new TestPldi03Benchmarks("testSimple", flags));
	suite.addTest(new TestPldi03Benchmarks("testFIR", flags));
	suite.addTest(new TestPldi03Benchmarks("testTarget", flags));
	suite.addTest(new TestPldi03Benchmarks("testFilterBank", flags));
	suite.addTest(new TestPldi03Benchmarks("testSamplingRate", flags));
	suite.addTest(new TestPldi03Benchmarks("testFM", flags));
	suite.addTest(new TestPldi03Benchmarks("testBeamformer", flags));
	return suite;
    }

    

    
    public void testSimple() {
	assertTrue("was true", true);
    }

    
    public void testFIR()
    {
        String root     = PLDI_ROOT;
	String source   = "FIRProgram.java";
	String expected = "FIRProgram.out";
        doMake(root, "benchmarks");
        doCompileRunVerifyTest(root, source, expected);
    }

    public void testTarget()
    {
        String root     = PLDI_ROOT;
	String source   = "TargetDetect.java";
	String expected = "TargetDetect.out";
        doMake(root, "benchmarks");
        doCompileRunVerifyTest(root, source, expected);
    }
    
    public void testFilterBank()
    {
        String root     = PLDI_ROOT;
	String source   = "FilterBank.java";
	String expected = "FilterBank.out";
        doMake(root, "benchmarks");
        doCompileRunVerifyTest(root, source, expected);
    }

    public void testSamplingRate()
    {
        String root     = PLDI_ROOT;
	String source   = "SamplingRateConverter.java";
	String expected = "SamplingRateConverter.out";
        doMake(root, "benchmarks");
        doCompileRunVerifyTest(root, source, expected);
    }
    
    public void testFM()
    {
        String root     = PLDI_ROOT;
	String source   = "LinkedFMTest.java";
	String expected = "LinkedFMTest.out";
        doMake(root, "benchmarks");
        doCompileRunVerifyTest(root, source, expected);
    }

    public void testBeamformer()
    {
        String root     = PLDI_ROOT;
	String source   = "CoarseSerializedBeamFormer.java";
	String expected = "CoarseSerializedBeamFormer.out";
        doMake(root, "benchmarks");
        doCompileRunVerifyTest(root, source, expected);
    }



}
