package at.dms.kjc.sir.linear;

/**
 * Exception that is throws when we can't compute a transform for whatever reason.
 * This is a checked exception to ensure that the impossible transformation is
 * explicity checked for.
 **/
class NoTransformPossibleException extends Exception {
    public NoTransformPossibleException(String message) {
	super(message);
    }
}
