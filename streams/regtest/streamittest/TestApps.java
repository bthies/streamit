/**
 * Test the programs in the apps/applications directory
 * $Id: TestApps.java,v 1.13 2003-09-29 20:41:05 thies Exp $
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
	
	return suite;
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
