package at.dms.kjc.sir.linear;

import java.util.*;

/**
 * A LinearFilterRepresentation represents the computations performed by a filter
 * on its input values as a matrix and a vector. The matrix represents
 * the combinations of inputs used to create various outputs. The vector corresponds
 * to constants that are added to the combination of inputs to produce the outputs.
 *
 * This class holds the A and b in the equation y = Ax+b which calculates the output
 * vector y using the input vector x. A is a matrix, b is a vector.
 *
 * While this is not the clearest of descriptions, as this class is fleshed out
 * I hope to make the description more concise.
 *
 * $Id: LinearFilterRepresentation.java,v 1.2 2002-09-06 17:19:42 aalamb Exp $
 **/
public class LinearFilterRepresentation {
    /** the A in y=Ax+b. **/
    private FilterMatrix A;
    /** the b in y=Ax+b. **/
    private FilterVector b;

    /**
     * Create a new linear filter representation with matrix A and vector b.
     * Note that we use a copy of both matrix A and vector b so that we don't end up with
     * an aliasing problem.
     **/
    public LinearFilterRepresentation(FilterMatrix matrixA,
				      FilterVector vectorb) {
	this.A = matrixA.copy();
	this.b = (FilterVector)vectorb.copy();
    }
    
}
