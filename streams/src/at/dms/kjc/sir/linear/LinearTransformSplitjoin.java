package at.dms.kjc.sir.linear;

import java.util.*;
//import at.dms.kjc.*;
//mport at.dms.kjc.sir.*;
//import at.dms.kjc.sir.linear.*;
//import at.dms.kjc.iterator.*;

/**
 * Contains the code for merging all the filters from a split join
 * into a single monolithic matrix.
 * $Id: LinearTransformSplitjoin.java,v 1.1 2002-09-20 18:09:37 aalamb Exp $
 **/
public class LinearTransformSplitjoin {
    private LinearTransformSplitjoin() {
    }

    /**
     * Implement the actual transformation.
     **/
    public  LinearFilterRepresentation transform() throws NoTransformPossibleException {
	throw new NoTransformPossibleException("Not yet implemented!");
    }


    /**
     * Calculate the necessary information to combine the splitjoin when the splitter is
     * a duplicate.
     **/
    public static LinearTransform calculateDuplicate(List representationList,
						     int[] joinerWeights) {
	return new LinearTransformNull("nope.");
    }

    /**
     * Calculate the necessary information to combine the splitjoin when the splitter is
     * a roundrobin.
     **/
    public static LinearTransform calculateRoundRobin(List representationList,
					       int[] joinerWeights,
					       int[] splitterWeights) {
	return new LinearTransformNull ("nope.");
    }
    
}




 
