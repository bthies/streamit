package at.dms.kjc.sir;

public interface Finalizer {

    /**
     * Given an Object <o> which may or may not be mutable, returns an
     * immutable object that is "equivalent" to <o>.
     */
    Object finalize(Object o);

    /**
     * Returns whether or not <o> can be mutated.  This should be
     * checked by mutator methods in <o> before they try to modify the
     * fields of <o>.
     */
    boolean isMutable(Object o);
}
