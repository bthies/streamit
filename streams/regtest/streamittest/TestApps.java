/**
 * Test the programs in the apps/applications directory
 * $Id: TestApps.java,v 1.12 2003-07-11 14:25:03 dmaze Exp $
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
	
	// this one doesn't fit on raw 8 without partitioning,
        // and doesn't fuse (it has a filereader).
        if (!flagsContainFusion(flags) &&
            (!flagsContainRaw(8, flags) ||
             flagsContainPartition(flags)))
	    suite.addTest(new TestApps("testMP3Simple", flags));

	suite.addTest(new TestApps("testNokiaFine", flags));
	
	return suite;
    }

    public void testCrc() {
        String root = APPS_ROOT + "crc/streamit/";
        doCompileTest(root, "CrcEncoder32Test.java");
        doRunTest(root, "CrcEncoder32Test.java", 0, 1);
        doCompareTest(root, "CrcEncoder32Test.java", "CrcEncoder32Test.out");
    }

    public void testMP3Simple()
    {
        String root = APPS_ROOT + "mp3decoder/";
	doMake(root);
	doCompileRunVerifyTest(root, "LinkedMP3Simple.java", "MP3Simple.out", 0, 1152);
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
