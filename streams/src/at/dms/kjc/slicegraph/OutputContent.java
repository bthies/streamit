package at.dms.kjc.slicegraph;

import at.dms.kjc.CType;
import at.dms.kjc.sir.*;
import at.dms.kjc.*;
import java.util.*;
import at.dms.kjc.sir.linear.*;

/**
 * Predefined FilterContent for output. Could be restructured to be
 * interface but it was easier to extend PredefinedContent this way
 * and can hold code common for predefined output.
 * @author jasperln
 */
public abstract class OutputContent extends PredefinedContent {
    /**
     * Copy constructor for OutputContent.
     * @param content The OutputContent to copy.
     */
    public OutputContent(OutputContent content) {
        super(content);
    }

    /**
     * Construct OutputContent from SIRPredefinedFilter
     * @param filter The SIRPredefinedFilter used to contruct the OutputContent.
     */
    public OutputContent(SIRPredefinedFilter filter) {
        super(filter);
    }

    /**
     * Construct OutputContent from UnflatFilter.
     * @param unflat The UnflatFilter used to contruct the OutputContent.
     */
    public OutputContent(UnflatFilter unflat) {
        super(unflat);
    }
    
    public abstract void createContent();
}
