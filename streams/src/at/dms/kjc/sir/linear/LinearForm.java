package at.dms.kjc.sir.linear;

import java.util.*;

/**
 * A LinearForm is the representation of a variable inside
 * the linear dataflow analysis. It is comprised of a vector and a value.
 * The vector corresponds to the combinations of inputs used to compute the value,
 * and the value corresponds to a constant that is added to compute the value.
 *
 * The basic gist of the dataflow analysis is to determine for all variables 
 * what the corresponding LinearForm is. Armed with this knowledge, we can
 * propage LinearForm information throughout the body of the filter and
 * hopefully construct a LinearFilterRepresentation from the filter.
 *
 * $Id: LinearForm.java,v 1.1 2002-08-14 18:13:19 aalamb Exp $
 **/
public class LinearForm {
    /** weights of inputs **/
    private FilterVector weights;
    /** offset that is added later **/
    private double offset;

    /** Construct a new LinearForm with vector size size with all elements zero. **/
    public LinearForm(int size) {
	this.weights = new FilterVector(size);
	this.offset = 0;
    }

    
    
}


