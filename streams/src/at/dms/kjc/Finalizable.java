package at.dms.kjc;

/**
 * This represents an object that can be finalized, after which point
 * no mutators should be able to adjust its state.
 */

public interface Finalizable {
    /**
     * Asserts that this is mutable.  If this has been finalized,
     * throws a runtime exception.  This should be done with a lookup
     * to a third-party class that is keeping track of the
     * immutability.  (We don't let the class itself keep track of the
     * immutability, since the immutability can only be modified in
     * batches by a trusted party that iterates over part of the tree.)
     */
    void assertMutable();

}
