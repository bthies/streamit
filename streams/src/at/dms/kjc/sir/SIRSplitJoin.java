package at.dms.kjc.sir;

import at.dms.kjc.*;

/**
 * This represents a SplitJoin construct.
 */
public class SIRSplitJoin extends SIRStream {
    /**
     * The splitter at the top of this.
     */
    private SIRSplitter splitter;
    /**
     * The joiner at the bottom of this.
     */
    private SIRJoiner joiner;
    /**
     * The stream components in this.  The i'th element of this array
     * corresponds to the i'th tape in the splitter and joiner.  
     */
    private SIRStream elements[];

}
