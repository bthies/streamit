/**
 * Test the programs in the apps/applications directory
 * $Id: TestApps.java,v 1.14 2003-10-13 23:12:47 mgordon Exp $
 **/
package streamittest;

import junit.framework.*;


public class TestApps extends StreamITTestCase {
    static String STREAM_ROOT = null;
    static String APPS_ROOT = null;
    
    public TestApps(String name) {
	this(name, DEFAULT_FLAGS);
    }
    
    /**
     * Creates a new TestApps which will use the compiler options
     * specified by flags (defined in CompilerInterface.java).
     **/
    public TestApps(String name, int flags) {
	super (name,flags);
	if (STREAM_ROOT == null) {
	    STREAM_ROOT = Harness.getStreamITRoot();
	    APPS_ROOT = STREAM_ROOT + APPS_PATH;
	    
	}
    }

    public static Test suite() {
	return suite(DEFAULT_FLAGS);
    }
    
    public static Test suite(int flags) {
	TestSuite suite = new TestSuite();
	
        // this one doesn't fuse to a single tile:
        if (!flagsContainFusion(flags))
            suite.addTest(new TestApps("testCrc", flags));
	
	suite.addTest(new TestApps("testNokiaFine", flags));
	
	suite.addTest(new TestApps("testDCT", flags));
	suite.addTest(new TestApps("testIDCT", flags));
	suite.addTest(new TestApps("testDCT2D", flags));
	suite.addTest(new TestApps("testIDCT2D", flags));
	//	suite.addTest(new TestApps("testFAT", flags));
	suite.addTest(new TestApps("testforw", flags));
	return suite;
    }
    
    //cannot be called at this time
    public void TestFAT() 
    {
	String root = APPS_ROOT + "FAT-new";
	doConvertCompileRunVerifyTest(root, "", 0, 1);
    }
    
    public void testforw() 
    {
	String root = APPS_ROOT + "video";
	doConvertCompileRunVerifyTest(root, "forw", 0, 8);
    }

    public void testDCT() 
    {
	String root = APPS_ROOT + "DCT";
	doConvertCompileRunVerifyTest(root, "DCT", 0, 8);
    }
    
    public void testDCT2D() 
    {
	String root = APPS_ROOT + "DCT";
	doConvertCompileRunVerifyTest(root, "DCT2D", 0, 4);
    }
    
    public void testIDCT() 
    {
	String root = APPS_ROOT + "DCT";
	doConvertCompileRunVerifyTest(root, "IDCT", 0, 4);
    }

    public void testIDCT2D() 
    {
	String root = APPS_ROOT + "DCT";
	doConvertCompileRunVerifyTest(root, "IDCT2D", 0, 4);
    }

    public void testCrc() {
        String root = APPS_ROOT + "crc/streamit/";
        doCompileTest(root, "CrcEncoder32Test.java");
        doRunTest(root, "CrcEncoder32Test.java", 0, 1);
        doCompareTest(root, "CrcEncoder32Test.java", "CrcEncoder32Test.out");
    }

    public void testNokiaFine() {
	String nokiaRoot = APPS_ROOT + "nokia-fine/";
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
