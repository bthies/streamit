/**
 * For running the 
 *
 * You can then use the CompilerInterface compiler to run compiler sessions.
 * $Id: TestTests.java,v 1.12 2003-10-14 23:03:10 mgordon Exp $
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

	suite.addTest(new TestTests("testArrayTest", flags));

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

	suite.addTest(new TestTests("testLifter", flags));

	suite.addTest(new TestTests("testWeightedRR", flags));
	suite.addTest(new TestTests("testTwoWeightedRR", flags));
	suite.addTest(new TestTests("testRecursive", flags));
	
	suite.addTest(new TestTests("testComplexIdTest", flags));
	suite.addTest(new TestTests("testIndirectTest", flags));
	suite.addTest(new TestTests("testlinearpartition", flags));
	suite.addTest(new TestTests("testpartition", flags));
	suite.addTest(new TestTests("testscriptratios", flags));

	suite.addTest(new TestTests("testLinearTest1", flags));
	suite.addTest(new TestTests("testLinearTest1", flags));
	suite.addTest(new TestTests("testLinearTest2", flags));
	suite.addTest(new TestTests("testLinearTest3", flags));
	suite.addTest(new TestTests("testLinearTest4", flags));
	suite.addTest(new TestTests("testLinearTest5", flags));
	suite.addTest(new TestTests("testLinearTest6", flags));
	suite.addTest(new TestTests("testLinearTest7", flags));
	suite.addTest(new TestTests("testLinearTest8", flags));


	suite.addTest(new TestTests("testLinearTest9", flags));
	suite.addTest(new TestTests("testLinearTest10", flags));
	suite.addTest(new TestTests("testLinearTest11", flags));
	suite.addTest(new TestTests("testLinearTest12", flags));
	suite.addTest(new TestTests("testLinearTest13", flags));
	suite.addTest(new TestTests("testLinearTest14", flags));
	suite.addTest(new TestTests("testLinearTest15", flags));
	suite.addTest(new TestTests("testLinearTest16", flags));
	suite.addTest(new TestTests("testLinearTest17", flags));
	suite.addTest(new TestTests("testLinearTest18", flags));
	suite.addTest(new TestTests("testLinearTest19", flags));
	suite.addTest(new TestTests("testLinearTest20", flags));
	suite.addTest(new TestTests("testLinearTest21", flags));
	suite.addTest(new TestTests("testLinearTest22", flags));
	suite.addTest(new TestTests("testLinearTest23", flags));
	suite.addTest(new TestTests("testLinearTest24", flags));
	suite.addTest(new TestTests("testLinearTest25", flags));
	suite.addTest(new TestTests("testLinearTest26", flags));
	suite.addTest(new TestTests("testLinearTest27", flags));
	suite.addTest(new TestTests("testLinearTest28", flags));
	suite.addTest(new TestTests("testLinearTest29", flags));
	suite.addTest(new TestTests("testLinearTest30", flags));
	suite.addTest(new TestTests("testLinearTest31", flags));
	suite.addTest(new TestTests("testLinearTest32", flags));
	suite.addTest(new TestTests("testLinearTest33", flags));
	suite.addTest(new TestTests("testLinearTest34", flags));
	suite.addTest(new TestTests("testLinearTest35", flags));
	suite.addTest(new TestTests("testLinearTest36", flags));
	suite.addTest(new TestTests("testLinearTest37", flags));

	return suite;
    }
    
    public void testSimple() {
	assertTrue("was true", true);
    }

    public void testscriptratios() {
	String root = TESTS_ROOT + "script-ratios";
	doMake(root);
	doCompileRunVerifyTest(root,
			       "LinkedScriptRatios.java",
			       "LinkedScriptRatios.out",
			       0,1);
    }

    public void testpartition() 
    {
	String root = TESTS_ROOT + "partition";
	doConvertCompileRunVerifyTest(root, "Partition1", 0, 20);
    }

    public void testlinearpartition() 
    {
	String root = TESTS_ROOT + "linear-partition";
	doConvertCompileRunVerifyTest(root, "Test1", 0, 3);
    }

    public void testIndirectTest() 
    {
	String root = TESTS_ROOT + "indirect";
	doConvertCompileRunVerifyTest(root, "IndirectTest", 0, 12);
    }

    public void testComplexIdTest() 
    {
	String root = TESTS_ROOT + "complexid";
	doConvertCompileRunVerifyTest(root, "ComplexIdTest", 0, 2);
    }

    public void testArrayTest() {
	String root = TESTS_ROOT + "arraytest/";
	doSyntaxConvertTest(root, "ArrayTest.str", "ArrayTest.java");
	doCompileRunVerifyTest(root,
			       "ArrayTest.java",
			       "ArrayTest.out",
			       0,1);
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

    public void testLifter() {
	String root = TESTS_ROOT + "lifter/";
        doSyntaxConvertTest(root, "TestLifter.str", "TestLifter.java");
	doCompileRunVerifyTest(root,
			       "TestLifter.java",
			       "TestLifter.out",
			       0,8);
	
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

    public void testRecursive()
    {
        String root = TESTS_ROOT + "recursive/";
        doSyntaxConvertTest(root, "Recursive.str", "Recursive.java");
	doCompileRunVerifyTest(root, "Recursive.java", "Recursive.out");
    }
    
    public void testLinearTest1() 
    {
	String root = TESTS_ROOT + "lineartest/regtests/";
	doCompileRunVerifyTest(root,
			       "LinearTest1.java",
			       "LinearTest1.expected",
			       0,1);
    }
    
    public void testLinearTest2() 
    {
	String root = TESTS_ROOT + "lineartest/regtests/";
	doCompileRunVerifyTest(root,
			       "LinearTest2.java",
			       "LinearTest2.expected",
			       0,1);
    }

     public void testLinearTest3() 
    {
	String root = TESTS_ROOT + "lineartest/regtests/";
	doCompileRunVerifyTest(root,
			       "LinearTest3.java",
			       "LinearTest3.expected",
			       0,1);
    }
    
    public void testLinearTest4() 
    {
	String root = TESTS_ROOT + "lineartest/regtests/";
	doCompileRunVerifyTest(root,
			       "LinearTest4.java",
			       "LinearTest4.expected",
			       0,1);
    }

    public void testLinearTest5() 
    {
	String root = TESTS_ROOT + "lineartest/regtests/";
	doCompileRunVerifyTest(root,
			       "LinearTest5.java",
			       "LinearTest5.expected",
			       0,1);
    }
    
    public void testLinearTest6() 
    {
	String root = TESTS_ROOT + "lineartest/regtests/";
	doCompileRunVerifyTest(root,
			       "LinearTest6.java",
			       "LinearTest6.expected",
			       0,1);
    }

     public void testLinearTest7() 
    {
	String root = TESTS_ROOT + "lineartest/regtests/";
	doCompileRunVerifyTest(root,
			       "LinearTest7.java",
			       "LinearTest7.expected",
			       0,1);
    }
    
    public void testLinearTest8() 
    {
	String root = TESTS_ROOT + "lineartest/regtests/";
	doCompileRunVerifyTest(root,
			       "LinearTest8.java",
			       "LinearTest8.expected",
			       0,1);
    }

    public void testLinearTest9() 
    {
	String root = TESTS_ROOT + "lineartest/regtests/";
	doSyntaxConvertTest(root, "LinearTest9.str", "LinearTest9.java");
	doCompileRunVerifyTest(root, "LinearTest9.java", "LinearTest9.expected");
    }

    public void testLinearTest10() 
    {
	String root = TESTS_ROOT + "lineartest/regtests/";
	doSyntaxConvertTest(root, "LinearTest10.str", "LinearTest10.java");
	doCompileRunVerifyTest(root, "LinearTest10.java", "LinearTest10.expected");
    }
    
     public void testLinearTest11() 
    {
	String root = TESTS_ROOT + "lineartest/regtests/";
	doSyntaxConvertTest(root, "LinearTest11.str", "LinearTest11.java");
	doCompileRunVerifyTest(root, "LinearTest11.java", "LinearTest11.expected");
    }

    public void testLinearTest12() 
    {
	String root = TESTS_ROOT + "lineartest/regtests/";
	doSyntaxConvertTest(root, "LinearTest12.str", "LinearTest12.java");
	doCompileRunVerifyTest(root, "LinearTest12.java", "LinearTest12.expected");
    }
    
     public void testLinearTest13() 
    {
	String root = TESTS_ROOT + "lineartest/regtests/";
	doSyntaxConvertTest(root, "LinearTest13.str", "LinearTest13.java");
	doCompileRunVerifyTest(root, "LinearTest13.java", "LinearTest13.expected");
    }

    public void testLinearTest14() 
    {
	String root = TESTS_ROOT + "lineartest/regtests/";
	doSyntaxConvertTest(root, "LinearTest14.str", "LinearTest14.java");
	doCompileRunVerifyTest(root, "LinearTest14.java", "LinearTest14.expected");
    }
    
     public void testLinearTest15() 
    {
	String root = TESTS_ROOT + "lineartest/regtests/";
	doSyntaxConvertTest(root, "LinearTest15.str", "LinearTest15.java");
	doCompileRunVerifyTest(root, "LinearTest15.java", "LinearTest15.expected");
    }

    public void testLinearTest16() 
    {
	String root = TESTS_ROOT + "lineartest/regtests/";
	doSyntaxConvertTest(root, "LinearTest16.str", "LinearTest16.java");
	doCompileRunVerifyTest(root, "LinearTest16.java", "LinearTest16.expected");
    }

      public void testLinearTest17() 
    {
	String root = TESTS_ROOT + "lineartest/regtests/";
	doSyntaxConvertTest(root, "LinearTest17.str", "LinearTest17.java");
	doCompileRunVerifyTest(root, "LinearTest17.java", "LinearTest17.expected");
    }

    public void testLinearTest18() 
    {
	String root = TESTS_ROOT + "lineartest/regtests/";
	doSyntaxConvertTest(root, "LinearTest18.str", "LinearTest18.java");
	doCompileRunVerifyTest(root, "LinearTest18.java", "LinearTest18.expected");
    }
    
     public void testLinearTest19() 
    {
	String root = TESTS_ROOT + "lineartest/regtests/";
	doSyntaxConvertTest(root, "LinearTest19.str", "LinearTest19.java");
	doCompileRunVerifyTest(root, "LinearTest19.java", "LinearTest19.expected");
    }

    public void testLinearTest20() 
    {
	String root = TESTS_ROOT + "lineartest/regtests/";
	doSyntaxConvertTest(root, "LinearTest20.str", "LinearTest20.java");
	doCompileRunVerifyTest(root, "LinearTest20.java", "LinearTest20.expected");
    }
    
     public void testLinearTest21() 
    {
	String root = TESTS_ROOT + "lineartest/regtests/";
	doSyntaxConvertTest(root, "LinearTest21.str", "LinearTest21.java");
	doCompileRunVerifyTest(root, "LinearTest21.java", "LinearTest21.expected");
    }

    public void testLinearTest22() 
    {
	String root = TESTS_ROOT + "lineartest/regtests/";
	doSyntaxConvertTest(root, "LinearTest22.str", "LinearTest22.java");
	doCompileRunVerifyTest(root, "LinearTest22.java", "LinearTest22.expected");
    }
    
     public void testLinearTest23() 
    {
	String root = TESTS_ROOT + "lineartest/regtests/";
	doSyntaxConvertTest(root, "LinearTest23.str", "LinearTest23.java");
	doCompileRunVerifyTest(root, "LinearTest23.java", "LinearTest23.expected");
    }

    public void testLinearTest24() 
    {
	String root = TESTS_ROOT + "lineartest/regtests/";
	doSyntaxConvertTest(root, "LinearTest24.str", "LinearTest24.java");
	doCompileRunVerifyTest(root, "LinearTest24.java", "LinearTest24.expected");
    }
    
      public void testLinearTest25() 
    {
	String root = TESTS_ROOT + "lineartest/regtests/";
	doSyntaxConvertTest(root, "LinearTest25.str", "LinearTest25.java");
	doCompileRunVerifyTest(root, "LinearTest25.java", "LinearTest25.expected");
    }

    public void testLinearTest26() 
    {
	String root = TESTS_ROOT + "lineartest/regtests/";
	doSyntaxConvertTest(root, "LinearTest26.str", "LinearTest26.java");
	doCompileRunVerifyTest(root, "LinearTest26.java", "LinearTest26.expected");
    }
    
     public void testLinearTest27() 
    {
	String root = TESTS_ROOT + "lineartest/regtests/";
	doSyntaxConvertTest(root, "LinearTest27.str", "LinearTest27.java");
	doCompileRunVerifyTest(root, "LinearTest27.java", "LinearTest27.expected");
    }

    public void testLinearTest28() 
    {
	String root = TESTS_ROOT + "lineartest/regtests/";
	doSyntaxConvertTest(root, "LinearTest28.str", "LinearTest28.java");
	doCompileRunVerifyTest(root, "LinearTest28.java", "LinearTest28.expected");
    }
    
     public void testLinearTest29() 
    {
	String root = TESTS_ROOT + "lineartest/regtests/";
	doSyntaxConvertTest(root, "LinearTest29.str", "LinearTest29.java");
	doCompileRunVerifyTest(root, "LinearTest29.java", "LinearTest29.expected");
    }

    public void testLinearTest30() 
    {
	String root = TESTS_ROOT + "lineartest/regtests/";
	doSyntaxConvertTest(root, "LinearTest30.str", "LinearTest30.java");
	doCompileRunVerifyTest(root, "LinearTest30.java", "LinearTest30.expected");
    }
    
     public void testLinearTest31() 
    {
	String root = TESTS_ROOT + "lineartest/regtests/";
	doSyntaxConvertTest(root, "LinearTest31.str", "LinearTest31.java");
	doCompileRunVerifyTest(root, "LinearTest31.java", "LinearTest31.expected");
    }

    public void testLinearTest32() 
    {
	String root = TESTS_ROOT + "lineartest/regtests/";
	doSyntaxConvertTest(root, "LinearTest32.str", "LinearTest32.java");
	doCompileRunVerifyTest(root, "LinearTest32.java", "LinearTest32.expected");
    }
    
    public void testLinearTest33() 
    {
	String root = TESTS_ROOT + "lineartest/regtests/";
	doSyntaxConvertTest(root, "LinearTest33.str", "LinearTest33.java");
	doCompileRunVerifyTest(root, "LinearTest33.java", "LinearTest33.expected");
    }

    public void testLinearTest34() 
    {
	String root = TESTS_ROOT + "lineartest/regtests/";
	doSyntaxConvertTest(root, "LinearTest34.str", "LinearTest34.java");
	doCompileRunVerifyTest(root, "LinearTest34.java", "LinearTest34.expected");
    }

   public void testLinearTest35() 
    {
	String root = TESTS_ROOT + "lineartest/regtests/";
	doSyntaxConvertTest(root, "LinearTest35.str", "LinearTest35.java");
	doCompileRunVerifyTest(root, "LinearTest35.java", "LinearTest35.expected");
    }

  public void testLinearTest36() 
    {
	String root = TESTS_ROOT + "lineartest/regtests/";
	doSyntaxConvertTest(root, "LinearTest36.str", "LinearTest36.java");
	doCompileRunVerifyTest(root, "LinearTest36.java", "LinearTest36.expected");
    }

  public void testLinearTest37() 
    {
	String root = TESTS_ROOT + "lineartest/regtests/";
	doSyntaxConvertTest(root, "LinearTest37.str", "LinearTest37.java");
	doCompileRunVerifyTest(root, "LinearTest37.java", "LinearTest37.expected");
    }

}
