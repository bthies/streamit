package streamittest;

import junit.framework.*;
import at.dms.kjc.sir.linear.ComplexNumber;
import at.dms.kjc.sir.linear.FilterMatrix;
import at.dms.kjc.sir.linear.FilterVector;
import at.dms.kjc.sir.linear.LinearForm;


/**
 * Regression test for linear filter extraction and
 * manipulation framework.
 * $Id: TestLinear.java,v 1.4 2002-08-15 20:36:13 aalamb Exp $
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
	suite.addTest(new TestLinear("testComplexNumberNegation"));
	suite.addTest(new TestLinear("testComplexNumberAddition"));
	suite.addTest(new TestLinear("testFilterMatrixConstructor"));
	suite.addTest(new TestLinear("testFilterMatrixAccessor"));
	suite.addTest(new TestLinear("testFilterMatrixModifier"));
	suite.addTest(new TestLinear("testFilterVector"));
	suite.addTest(new TestLinear("testLinearForm"));
	suite.addTest(new TestLinear("testLinearFormAddition"));
	
	
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

    /** test that negating a complex number works **/
    public void testComplexNumberNegation() {
	ComplexNumber cn1;
	ComplexNumber cn2;

	//make sure that nothing screwy wiht zero is happening
	assertTrue("zero is fine", ComplexNumber.ZERO.negate().equals(ComplexNumber.ZERO));
	
	cn1 = new ComplexNumber(1.34879759847, 21324);
	cn2 = cn1.negate();
	assertTrue("negation not equal", !cn1.equals(cn2));

	// we should also be able to negate a number twice and have it be equal
	assertTrue("double negation", cn1.equals(cn1.negate().negate()));
	assertTrue("double negation", cn2.equals(cn2.negate().negate()));
	
    }

    /** test adding complex numbers together **/
    public void testComplexNumberAddition() {
	ComplexNumber cn1;
	ComplexNumber cn2;
	ComplexNumber cn3;

	ComplexNumber zero = ComplexNumber.ZERO;
	ComplexNumber one = ComplexNumber.ONE;
	ComplexNumber negativeOne = one.negate();

	// make sure that adding zero to itself is still zero
	assertTrue("0+0", zero.plus(zero).equals(zero));
	// make sure that one is fine too
	assertTrue("1+0", one.plus(zero).equals(one));	
	assertTrue("0+1", zero.plus(one).equals(one));

	// make sure that adding the negative of a number to itself is zero
	assertTrue("1+-1", one.plus(negativeOne).equals(zero));
	
	// test some random numbers
	cn1 = new ComplexNumber(1.2, 3.2);
	cn2 = new ComplexNumber(-1.2, -3);
	cn3 = new ComplexNumber(.4, -.3);
	assertTrue(cn1.plus(cn2).equals(new ComplexNumber(0, .2)));
	assertTrue(cn2.plus(cn1).equals(new ComplexNumber(0, .2)));
	assertTrue(cn1.plus(cn3).equals(new ComplexNumber(1.6, 2.9)));
	assertTrue(cn3.plus(cn1).equals(new ComplexNumber(1.6, 2.9))); 
	assertTrue(cn2.plus(cn3).equals(new ComplexNumber(-.8, -3.3)));
	assertTrue(cn3.plus(cn2).equals(new ComplexNumber(-.8, -3.3))); 
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
	
    /** Test the FilterVector class **/
    public void testFilterVector() {
	// there are fewer tests here because most of the functionality is inhereted from
	// FilterMatrix
	double D_INCREMENT = .1;
	double D_MAX = 10;
	int MAX = 10;
	FilterVector fv;
	
	// make sure that we can create vectors and that all elts are 0
	for (int i=1; i<=MAX; i++) {
	    fv = new FilterVector(i);
	    for (int a=0; a<i; a++) {
		assertTrue("init to zero", ComplexNumber.ZERO.equals(fv.getElement(a)));
	    }
	}

	// also, ensure that we can't use the two argument form of set element or get element
	fv = new FilterVector(MAX);
	try{fv.getElement(1,1);fail("used two arg form of get");} catch (RuntimeException e) {/*no prob*/}
	try{fv.setElement(1,1,new ComplexNumber(1,2));fail("used two arg form of set");} catch (RuntimeException e) {/*no prob*/}


	// do a little exercise in getting elements from the vector
	for (int i=1; i<=MAX; i++) {
	    fv = new FilterVector(i);
	    // for each element in the vector
	    for (int j=0; j<i; j++) {
		// make a lot of complex numbers
		for (double a=-D_MAX; a<D_MAX; a+=D_INCREMENT) {
		    for (double b=-D_MAX; b<D_MAX; b+=D_INCREMENT) {
			ComplexNumber cn = new ComplexNumber(a,b);
			fv.setElement(j, cn);
			// make sure that this element is the same
			assertTrue("elt in vector is same",
				   fv.getElement(j).equals(new ComplexNumber(a,b)));
			// make sure that the size is correct
			assertTrue("Correct size", fv.getSize() == i);
		    }
		}
	    }
	}
    }

    public void testLinearForm() {
	LinearForm lf;
	// make sure that we can't make negative sized linear forms
	try {lf = new LinearForm(-1); fail("no error!");} catch(RuntimeException e){}
	try {lf = new LinearForm(0); fail("no error!");} catch(RuntimeException e){}

	// now, make sure that if we make a linear form with an integer offset,
	// isIntegerOffset works correctly
	lf = new LinearForm(1);
	lf.setOffset(10);
	assertTrue("int is actually an int", lf.isIntegerOffset());

	// make sure that the actual offset is the same
	assertTrue("Offset matches", lf.getIntegerOffset() == 10);

	// make sure that we can't get an integer offset of a offset
	// that is non integer
	lf.setOffset(3.2);
	try{lf.getIntegerOffset(); fail("should die");} catch(RuntimeException e) {}

	// now, make sure that the isOnlyOffset method works
	lf = new LinearForm(5);
	assertTrue("is only offset", lf.isOnlyOffset());
	// set the offset, and try again
	lf.setOffset(1);
	assertTrue("is only offset", lf.isOnlyOffset());
	lf.setOffset(-83.42);
	assertTrue("is only offset", lf.isOnlyOffset());
	
	
	// set an element and make sure that it is false
	lf.setWeight(3, new ComplexNumber(4,-1.4322));
	assertTrue("is not only offset", !(lf.isOnlyOffset()));	
    }

    /** test the additon and negation of linear forms **/
    public void testLinearFormAddition() {
	LinearForm lf1;
	LinearForm lf2;

	int MAX=10;
	
	lf1 = new LinearForm(MAX);
	lf2 = new LinearForm(MAX);
	// manually construct lf1 and lf2 to be its negative
	for (int i=0; i<MAX; i++) {
	    lf1.setWeight(i, new ComplexNumber(i,i));
	    lf2.setWeight(i, new ComplexNumber(-i,-i));
	}
	lf1.setOffset(new ComplexNumber(-32.423,0));
	lf2.setOffset(new ComplexNumber(32.423,0));

	LinearForm negativelf1 = lf1.negate();

	// check offsets
	assertTrue("Same offsets", negativelf1.getOffset().equals(lf2.getOffset()));
	assertTrue("Same offsets", lf2.getOffset().equals(negativelf1.getOffset()));
	
	// make sure that the element of negating lf1 are the same as lf2
	for (int i=0; i<MAX; i++) {
	    assertTrue("element " + i,
		       negativelf1.getWeight(i).equals(lf2.getWeight(i)));
	}

	// check to see if they sum to zero
	LinearForm lfzero = lf1.plus(lf2);
	assertTrue("Zero Offset", lfzero.getOffset().equals(ComplexNumber.ZERO));
	for (int i=0; i<MAX; i++) {
	    assertTrue("element " + i + " is zero",
		       lfzero.getWeight(i).equals(ComplexNumber.ZERO));
	}
	lfzero = lf2.plus(lf1);
	assertTrue("Zero Offset", lfzero.getOffset().equals(ComplexNumber.ZERO));
	for (int i=0; i<MAX; i++) {
	    assertTrue("element " + i + " is zero",
		       lfzero.getWeight(i).equals(ComplexNumber.ZERO));
	}
	
	// make sure that we can't add linear forms of different sizes
	lf1 = new LinearForm(10);
	lf2 = new LinearForm(11);
	try{lf1.plus(lf2); fail("diff linear form size");} catch (RuntimeException e) {}

    }
}
