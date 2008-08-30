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
     * No argument constructor, FOR AUTOMATIC CLONING ONLY.
     */
    protected PredefinedContent() {
        super();
    }

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

    /** THE FOLLOWING SECTION IS AUTO-GENERATED CLONING CODE - DO NOT MODIFY! */

    /** Returns a deep clone of this object. */
    public Object deepClone() { at.dms.util.Utils.fail("Error in auto-generated cloning methods - deepClone was called on an abstract class."); return null; }

    /** Clones all fields of this into <pre>other</pre> */
    protected void deepCloneInto(at.dms.kjc.slicegraph.PredefinedContent other) {
        super.deepCloneInto(other);
    }

    /** THE PRECEDING SECTION IS AUTO-GENERATED CLONING CODE - DO NOT MODIFY! */
}
