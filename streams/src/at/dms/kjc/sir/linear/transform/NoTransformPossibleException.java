package at.dms.kjc.sir.linear.transform;

import at.dms.kjc.sir.linear.*;

/**
 * Exception that is thrown when we can't compute a transform for some reason.
 * This is a checked exception to ensure that the case of an impossible transformation is
 * explicity checked for and so that the compiler doesn't die (this optimization just
 * stops where it is).<br>
 *
 * $Id: NoTransformPossibleException.java,v 1.3 2003-05-30 14:05:04 aalamb Exp $
 **/
public class NoTransformPossibleException extends Exception {
    public NoTransformPossibleException(String message) {
	super(message);
    }
}
