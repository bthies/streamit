package at.dms.kjc.sir.statespace.transform;

import at.dms.kjc.sir.statespace.*;
import java.util.*;

/**
 * A LinearTransform communicates information
 * about how to transform one or more linear representations into
 * a new linear representation (eg combination rules).
 * For example, for pipeline combinations,
 * the Linear Transform contains information about what expansion factors to use
 * for the filters to be combined.<br>
 *
 * The interface to a LinearTransform is simple the transform()
 * method, which will return the overall linear representation
 * that the transform calculates. To create a LinearTransform,
 * you use one of the static methods in the subclasses to pre-compute
 * whatever information is necessary, and then you call transform()
 * on the returned LinearTransform object.<br>
 *
 * $Id: LinearTransform.java,v 1.1 2004-02-09 17:55:22 thies Exp $
 **/
public abstract class LinearTransform {
    /**
     * Actually implements the specific transform. Returns
     * a LinearFilterRepresentation that results from
     * applying the appropriate transformation.
     **/
    public abstract LinearFilterRepresentation transform() throws NoTransformPossibleException;




    ///////////// Utility functions.


    
    /** Calculates the least common multiple of two integers. **/
    public static int lcm(int a, int b) {
	return (a*b)/gcd(a,b);
    }


    /** Calculates the least common multiple of all the integers contained in the array. **/
    public static int lcm(int[] numbers) {
	// if we don't have any numbers, complain.
	if (!(numbers.length > 0)) {
	    throw new IllegalArgumentException("no integers were found in the array in lcm(int[])");
	}
	int currentLcm = numbers[0];
	for (int i=1; i<numbers.length; i++) {
	    currentLcm = lcm(currentLcm, numbers[i]);
	}
	return currentLcm;
    }
							 

    /**
     * Return the greatest factor that evenly divids both m and n (ie the
     * greatest common denominator).
     * Valid for 0 < n < m.
     * From
     * <a href="http://www.brent.worden.org/algorithm/mathematics/greatestCommonDenominator.html">
     * http://www.brent.worden.org/algorithm/mathematics/greatestCommonDenominator.html.</a>
     **/
    public static int gcd(int a, int b) {
	if ((a <= 0) || (b <= 0)) {
	    throw new IllegalArgumentException("Bad arguments to lcm: " +
					       "(" + a + "," + b + ")");
	}
	int m, n;
	if (a < b) {
	    n = a; m = b;
	} else {
	    n = b; m = a;
	}
	int d = m;
	int r = n;
	
	while( r != 0 ){ 
	    d = r;
	    r = m % r;
	}
	return d;
    }

    /** Gets the gcd of an array of numbers. **/
    public static int gcd(int[] arr) {
	if (arr.length < 1) {
	    throw new IllegalArgumentException("No numbers passed to gcd");
	}
	int currentGcd = arr[0];
	for (int i=1; i<arr.length; i++) {
	    currentGcd = gcd(currentGcd, arr[i]);
	}
	return currentGcd;
    }

    /**
     * Gets the ceiling of the division of two integers. Eg
     * divCeil(5,3) = ceil(5/3) = 2
     **/
    public static int divCeiling(int a, int b) {
	int dividend = a/b;
	int remainder = a%b;
	if (remainder != 0) {
	    dividend++;
	}
	return dividend;
    }
}




 
