package at.dms.kjc.sir.linear;

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
}




 
