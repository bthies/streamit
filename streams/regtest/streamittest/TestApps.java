/**
 * Test the programs in the apps/applications directory
 * $Id: TestApps.java,v 1.8 2002-09-26 01:10:08 thies Exp $
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

	// this one doesn't fit on any raw4
	suite.addTest(new TestApps("testNokiaFine", flags));

	return suite;
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
