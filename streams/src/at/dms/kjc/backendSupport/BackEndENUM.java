/**
 * 
 */
package at.dms.kjc.backendSupport;


/**
 * Used by BackEndAbsFactory to determine which back end to create.
 * Needs to be extended for any new back end.
 * @author dimock
 * @see BackEndAbsFactory
 */
public enum BackEndENUM {
    /** Indicates spacetime back end for RAW processors */
    RAW,
    /** Indicates spacetime back end for uniprocessor */
    UNI;
}
