package at.dms.kjc.sir.statespace;

/**
 * This exception is thrown when a filter is determined to be non-linear.
 * The exception mechanism is used to short circuit the analysis of filters
 * once we determine that they are non-linear.
 **/
public class NonLinearException extends RuntimeException {
    public NonLinearException(String m) {
	super(m);
    }
}

