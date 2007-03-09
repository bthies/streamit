/**
 * 
 */
package at.dms.kjc.backendSupport;

import at.dms.kjc.spacetime.RawBackEndFactory;
import at.dms.kjc.vanillaSlice.UniBackEndFactory;

/**
 * Abstract Factory pattern for creating BackEndFactory classes.
 * 
 * To generate a new back end extend BackEndENUM and extend
 * the <b>switch</b> statement in <b>getBackEnd</b>.
 * @author dimock
 * @see BackEndEnum
 */
public class BackEndAbsFactory {
    private static BackEndFactory theBackEnd = null;

    /**
     * 
     * @param which specifies kind of factory to create.
     * @return the BackEndFactory specified by <b>which</b>
     */
    public static BackEndFactory getBackEnd(BackEndENUM which) {
        switch (which) {
        case RAW: 
            theBackEnd = new RawBackEndFactory();
            break;
        case UNI:
            theBackEnd = new UniBackEndFactory();
        }
        return theBackEnd;
    }
    public static BackEndFactory reaccessBackEnd() {
        return theBackEnd;
    }
}
