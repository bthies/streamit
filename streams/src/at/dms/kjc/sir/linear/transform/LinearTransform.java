package at.dms.kjc.sir.linear.transform;

import at.dms.kjc.sir.linear.*;
import java.util.*;
//import at.dms.kjc.*;
//mport at.dms.kjc.sir.*;
//import at.dms.kjc.sir.linear.*;
//import at.dms.kjc.iterator.*;

/**
 * A LinearTransform communicates information
 * about how to transform one or more linear transforms into
 * a new linear representation. For example, for pipeline combinations,
 * the LRT contains information about what factor to use for expansion
 * for both filters trying to be combined.
 **/
public abstract class LinearTransform {

    /**
     * Actually implements the transform. Returns
     * a LinearFilterRepresentation that results from
     * applying the appropriate transformation.
     **/
    public abstract LinearFilterRepresentation transform() throws NoTransformPossibleException;




    ///////////// Utility functions.


    
    /** Calculates the least common multiple of two integers. **/
    public static int lcm(int a, int b) {
	return (a*b)/gcd(a,b);
    }

    /** calculates the least common multiple of all the integers contained in the array. **/
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
     * Return the greatest factor that evenly divids both m and n.
     * Valid for 0 < n < m.
     * From http://www.brent.worden.org/algorithm/mathematics/greatestCommonDenominator.html.
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

    /** gets the gcd of an array of numbers **/
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
    
}




 
