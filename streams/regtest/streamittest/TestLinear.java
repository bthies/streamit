package streamittest;

import junit.framework.*;
import at.dms.kjc.linear.FilterMatrix;
import at.dms.kjc.linear.ComplexNumber;

/**
 * Regression test for linear filter extraction and
 * manipulation framework.
 * $Id: TestLinear.java,v 1.2 2002-08-12 19:07:57 aalamb Exp $
 **/

public class TestLinear extends TestCase {
    public TestLinear(String name) {
	super(name);
    }

    public static Test suite() {
	TestSuite suite = new TestSuite();
	suite.addTest(new TestLinear("testSimple"));
	suite.addTest(new TestLinear("testComplexNumberCreationAndAccess"));
	suite.addTest(new TestLinear("testComplexNumberEquality"));
	suite.addTest(new TestLinear("testFilterMatrixConstructor"));
	suite.addTest(new TestLinear("testFilterMatrixAccessor"));
	suite.addTest(new TestLinear("testFilterMatrixModifier"));
	
	return suite;
    }
    
    public void testSimple() {
	assertTrue("was true", true);
    }

    /** Simple complex number tests **/
    public void testComplexNumberCreationAndAccess() {
	ComplexNumber cn;

	double MAX = 5;
	double STEP = .1;
	
	
	cn = new ComplexNumber(0,0);
	cn = new ComplexNumber(0,1);
	cn = new ComplexNumber(1,0);
	cn = new ComplexNumber(5,4);
	cn = new ComplexNumber(1.2,-94.43);

	// test access
	for (double i=-MAX; i<MAX; i+= STEP) {
	    for (double j=-MAX; j<MAX; j+= STEP) {
		cn = new ComplexNumber(i,j);
		assertTrue("real part", cn.getReal() == i);
		assertTrue("im part", cn.getImaginary() == j);
	    }
	}
    }
    
    /** Simple tests for equality **/
    public void testComplexNumberEquality() {
	ComplexNumber cn1;
	ComplexNumber cn2;

	cn1 = new ComplexNumber(.32453252342142523, .78881282845324);
	cn2 = new ComplexNumber(.32453252342142523, .78881282845324);

	// test for self equality
	assertTrue("self", cn1.equals(cn1));
	assertTrue("self", cn2.equals(cn2));
	
	// test for numerical equality
	assertTrue("numerical equality", cn1.equals(cn2));
	assertTrue("numerical equality", cn2.equals(cn1));

	// make sure we aren't just always doing true
	cn2 = new ComplexNumber(.8572983475029834, .78881282845324);
	assertTrue("not always equal", !cn1.equals(cn2));
	assertTrue("not always equal", !cn2.equals(cn1));

	cn2 = new ComplexNumber(-.32453252342142523, .78881282845324);
	assertTrue("not always equal", !cn1.equals(cn2));
	assertTrue("not always equal", !cn2.equals(cn1));

    }

    
    /** FilterMatrix tests **/
    public void testFilterMatrixConstructor() {
	int MAX = 10;
	FilterMatrix fm;
	// basically, just make a bunch of these matricies of various sizes
	// and make sure nothing bad happens, and that all elements are initialized
	// to zero
	for (int i=1; i<MAX; i++) {
	    for (int j=1; j<MAX; j++) {
		fm = new FilterMatrix(i,j);
		// check that the rows and cols are as expected
		assertTrue("row size matches", i == fm.getRows());
		assertTrue("col size matches", j == fm.getCols());
		for (int a=0; a<i; a++) {
		    for (int b=0; b<j; b++) {
			// check that the elt is 0
			ComplexNumber zero = new ComplexNumber(0,0);
			assertTrue(("(" + a + "," + b + ")=0"),
				   zero.equals(fm.getElement(a,b)));
		    }
		}
	    }
	}
	// also make sure that we can't do stupid things like instantiate matricies
	// with negative dimensions
	for (int i=0; i>-MAX; i--) {
	    for (int j=0; j>-MAX; j--) {
		try {
		    fm = new FilterMatrix(i,j);
		    // if we get here, it let us instantiate a matrix
		    // with negative dimensions
		    fail("can't instantiate a matrix with dimensions (" +
			 i + "," + j + ")");
		} catch (IllegalArgumentException e) {
		    // this is the case that we expect, so no worries
		}
	    }
	}
	
	
    }
    /**
     * Test the accessors a little bit more to make
     * sure that they catch errors as they are supposed to.
     **/
    public void testFilterMatrixAccessor() {
	int ROW_MAX = 3;
	int COL_MAX = 4;
	int MAX = 10;
	// make a matrix, and then try to get a bunch of elements
	// Make sure that they are valid ones
	FilterMatrix fm = new FilterMatrix(ROW_MAX,COL_MAX);
	assertTrue("right size row", fm.getRows() == ROW_MAX);
	assertTrue("right size col", fm.getCols() == COL_MAX);

	for (int i=0; i<MAX; i++) {
	    for (int j=0; j<MAX; j++) {
		// if we are within the bounds, expect no error
		if ((i<ROW_MAX) && (j<COL_MAX)) {
		    ComplexNumber zero = new ComplexNumber(0,0);
		    assertTrue("elt is 0", zero.equals(fm.getElement(i,j)));
		} else {
		    // we expect an exception
		    try {fm.getElement(i,j); fail("no exception on illegal access");}
		    catch (IllegalArgumentException e) {/*no worries*/}
		}
	    }
	}
    }


    /** Test setting the values within the matrix **/
    public void testFilterMatrixModifier() {
	// for this test, it is rather simple. We will just create a
	// matrix, set its elements to be particular values and then
	// verify that the appropriate numbers were stored.
	FilterMatrix fm;
	int MAX = 10;
	for (int i=0; i<MAX; i++) {
	    for (int j=0; j<MAX;j++) {
		fm = new FilterMatrix(i+1,j+1);
		for (int a=0; a<=i; a++) {
		    for (int b=0; b<=j; b++) {
			double re_val = Math.random();
			double im_val = Math.random();
			fm.setElement(a,b,new ComplexNumber(re_val, im_val));
			ComplexNumber value = new ComplexNumber(re_val, im_val);
			assertTrue("value matches", value.equals(fm.getElement(a,b)));
		    }
		}
	    }
	}

    }
	




    
}
