/**
 * For running the 
 *
 * You can then use the CompilerInterface compiler to run compiler sessions.
 * $Id: TestTests.java,v 1.4 2002-09-26 00:15:24 thies Exp $
 **/
package streamittest;

import junit.framework.*;


public class TestTests extends StreamITTestCase {
    static String STREAM_ROOT = null;
    static String TESTS_ROOT = null;

    public TestTests(String name) {
	super(name, DEFAULT_FLAGS);
    }
    
    /**
     * Creates a new TestTests which will use the compiler options
     * specified by flags (defined in CompilerInterface.java).
     **/
    public TestTests(String name, int flags) {
	super (name,flags);
	if (STREAM_ROOT == null) {
	    STREAM_ROOT = Harness.getStreamITRoot();
	    TESTS_ROOT = STREAM_ROOT + TESTS_PATH;
	}
    }

    /** return a suite with the default flags **/
    public static Test suite() {
	return suite(DEFAULT_FLAGS);
    }

    
    public static Test suite(int flags) {
	TestSuite suite = new TestSuite();
	suite.addTest(new TestTests("testSimple", flags));

	//suite.addTest(new TestTests("testArrayTest", flags));

	suite.addTest(new TestTests("testFir", flags));
	suite.addTest(new TestTests("testFuse", flags));
	suite.addTest(new TestTests("testFuseTest", flags));

	suite.addTest(new TestTests("testFlybit", flags));
	suite.addTest(new TestTests("testHelloSeparate", flags));	
	suite.addTest(new TestTests("testPeekPipe", flags));
	suite.addTest(new TestTests("testSimpleSplit", flags));
	suite.addTest(new TestTests("testUnroll", flags));

	suite.addTest(new TestTests("testFieldProp", flags));
	suite.addTest(new TestTests("testFieldProp2", flags));
	suite.addTest(new TestTests("testFieldInit", flags));
	suite.addTest(new TestTests("testFieldInit2", flags));
	suite.addTest(new TestTests("testFieldInit3", flags));
	suite.addTest(new TestTests("testFieldInit4", flags));

	suite.addTest(new TestTests("testWeightedRR", flags));
	suite.addTest(new TestTests("testTwoWeightedRR", flags));
	
	return suite;
    }
    
    public void testSimple() {
	assertTrue("was true", true);
    }

    public void testArrayTest() {
	doCompileRunVerifyTest(TESTS_ROOT + "arraytest/",
			       "ArrayTest.java",
			       "ArrayTest.out");
    }

    public void testFuse() {
	doCompileRunVerifyTest(TESTS_ROOT + "fuse/",
			       "FusionTest.java",
			       "FusionTest.out",
			       0,1);
    }

    public void testFuseTest() {
	doCompileRunVerifyTest(TESTS_ROOT + "fuse-test/",
			       "FuseTest.java",
			       "FuseTest.out",
			       0,3);
    }

    public void testFlybit() {
	doCompileRunVerifyTest(TESTS_ROOT + "flybit/",
			       "Flybit.java",
			       "Flybit.out",
			       0,4);
	
    }

    public void testHelloSeparate() {
	doCompileRunVerifyTest(TESTS_ROOT + "hello-separate/",
			       "HelloSeparate.java",
			       "HelloSeparate.out",
			       0,1);
    }

    public void testPeekPipe() {
	doCompileRunVerifyTest(TESTS_ROOT + "peek-pipe/",
			       "PeekPipe.java",
			       "PeekPipe.out",
			       0,1);
    }

    public void testSimpleSplit() {
	doCompileRunVerifyTest(TESTS_ROOT + "simple-split/",
			       "SimpleSplit.java",
			       "SimpleSplit.out",
			       0,4);
    }

    public void testUnroll() {
	doCompileRunVerifyTest(TESTS_ROOT + "unroll/",
			       "Unroll.java",
			       "Unroll.out",
			       0,1);
    }

    public void testFir() {
	String root = TESTS_ROOT + "fir-test/";
	doMake(root);
	doCompileRunVerifyTest(root,
			       "LinkedFirTest.java",
			       "LinkedFirTest.out",
			       0,1);
    }

    public void testFieldProp() {
	doCompileRunVerifyTest(TESTS_ROOT + "field-prop/",
			       "FieldPropTest.java",
			       "FieldPropTest.out",
			       0,2);
    }

    public void testFieldProp2() {
	doCompileRunVerifyTest(TESTS_ROOT + "field-prop/",
			       "FieldPropTest2.java",
			       "FieldPropTest2.out",
			       0,2);
    }

    public void testFieldInit() {
	doCompileRunVerifyTest(TESTS_ROOT + "field-init/",
			       "FieldInit.java",
			       "FieldInit.out",
			       0,1);
    }

    public void testFieldInit2() {
	doCompileRunVerifyTest(TESTS_ROOT + "field-init/",
			       "FieldInit2.java",
			       "FieldInit2.out",
			       0,1);
    }

    public void testFieldInit3() {
	doCompileRunVerifyTest(TESTS_ROOT + "field-init/",
			       "FieldInit3.java",
			       "FieldInit3.out",
			       0,1);
    }

    public void testFieldInit4() {
	doCompileRunVerifyTest(TESTS_ROOT + "field-init/",
			       "FieldInit4.java",
			       "FieldInit4.out",
			       0,1);
    }
    public void testUpDown() {
	// who knows what this is supposed to do?
	doCompileRunVerifyTest(TESTS_ROOT + "updown/",
			       "UpDown.java",
			       "UpDown.out");
    }

    public void testWeightedRR() {
	doCompileRunVerifyTest(TESTS_ROOT + "weighted-rr/",
			       "WeightedRR.java",
			       "WeightedRR.out",
			       0,6);
    }

    public void testTwoWeightedRR() {
	doCompileRunVerifyTest(TESTS_ROOT + "weighted-rr/",
			       "TwoWeightedRR.java",
			       "TwoWeightedRR.out",
			       0,6);
    }
    
}
