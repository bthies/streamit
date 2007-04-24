package at.dms.kjc.slicegraph;

import at.dms.kjc.CType;
import at.dms.kjc.sir.*;
import at.dms.kjc.*;
import java.util.*;
import at.dms.kjc.sir.linear.*;

/**
 * Predefined FilterContent. This is for filters that represent
 * built-in functionality like FileReaders and FileWriters. Could be
 * restructured to be interface but it was easier to extend
 * FilterContent this way and can hold code common for predefined
 * filters.
 * @author jasperln
 */
public abstract class PredefinedContent extends FilterContent {
    /**
     * Copy constructor for PredefinedContent.
     * @param content The PredefinedContent to copy.
     */
    public PredefinedContent(PredefinedContent content) {
        super(content);
    }

    /**
     * Construct PredefinedContent from SIRPredefinedFilter
     * @param filter The SIRPredefinedFilter used to contruct the PredefinedContent.
     */
    public PredefinedContent(SIRPredefinedFilter filter) {
        super(filter);
    }

    /**
     * Construct PredefinedContent from UnflatFilter.
     * @param unflat The UnflatFilter used to contruct the PredefinedContent.
     */
    public PredefinedContent(UnflatFilter unflat) {
        super(unflat);
    }
    
    /**
     * Subclasses of {@link  PredefinedContent} must be able to create their content.
     * (fields, methods, work function if appropriate, multiplicities, rates)
     */
    public abstract void createContent();
}
