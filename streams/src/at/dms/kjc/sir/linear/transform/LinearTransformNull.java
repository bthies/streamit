package at.dms.kjc.sir.linear;

/** Represents a null transform -- eg no transform is applied. **/
class LinearTransformNull extends LinearTransform {
    /** The reason that no transformation could be applied. **/
    String reason;
    /**
     * Creates a new LinearTransformNull with the reason that no
     * transform was possible specified in the string r.
     **/
    public LinearTransformNull(String r) {
	this.reason = r;
    }
    
    public LinearFilterRepresentation transform() throws NoTransformPossibleException {
	throw new NoTransformPossibleException(reason);
    }
}
