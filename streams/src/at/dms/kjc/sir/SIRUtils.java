/**
 * 
 */
package at.dms.kjc.sir;

/**
 * This class contains the odd small utility method for use with SIR.
 * 
 * @author dimock
 *
 */
public class SIRUtils {

	/**
	 * Determine whether a filter is one that needs special processing.
	 * 
	 * Returns true if a filter is one of the special pre-defined filters.
	 * else returns false.
	 * 
	 * The predefined filters are currently: SIRFileReader, SIRFileWriter,
	 * and SIRIdentity 
	 *
	 */
	
	static public boolean isSpecialFilter (SIRFilter f) {
		return (f instanceof SIRFileReader
				|| f instanceof SIRFileReader
				|| f instanceof SIRIdentity);
	}
}
