package at.dms.kjc.sir.linear;

/**
 * This exception is thrown when a filter is determined to be non-linear.
 **/
public class NonLinearException extends RuntimeException {
    public NonLinearException(String m) {
	super(m);
    }
}

