package at.dms.kjc.slicegraph;

//import at.dms.kjc.CType;
import at.dms.kjc.sir.*;
//import at.dms.kjc.*;
//import java.util.*;
//import at.dms.kjc.sir.linear.*;

/**
 * Predefined FilterContent for input. Could be restructured to be
 * interface but it was easier to extend PredefinedContent this way
 * and can hold code common for predefined input.
 * @author jasperln
 */
public abstract class InputContent extends PredefinedContent implements at.dms.kjc.DeepCloneable {
    /**
     * No argument constructor, FOR AUTOMATIC CLONING ONLY.
     */
    protected InputContent() {
        super();
    }

    /**
     * Copy constructor for InputContent.
     * @param content The InputContent to copy.
     */
    public InputContent(InputContent content) {
        super(content);
    }

    /**
     * Construct InputContent from SIRPredefinedFilter
     * @param filter The SIRPredefinedFilter used to contruct the InputContent.
     */
    public InputContent(SIRPredefinedFilter filter) {
        super(filter);
    }

    /**
     * Construct InputContent from UnflatFilter.
     * @param unflat The UnflatFilter used to contruct the InputContent.
     */
    public InputContent(UnflatFilter unflat) {
        super(unflat);
    }
    
    public abstract void createContent();

    /** THE FOLLOWING SECTION IS AUTO-GENERATED CLONING CODE - DO NOT MODIFY! */

    /** Returns a deep clone of this object. */
    public Object deepClone() { at.dms.util.Utils.fail("Error in auto-generated cloning methods - deepClone was called on an abstract class."); return null; }

    /** Clones all fields of this into <pre>other</pre> */
    protected void deepCloneInto(at.dms.kjc.slicegraph.InputContent other) {
        super.deepCloneInto(other);
    }

    /** THE PRECEDING SECTION IS AUTO-GENERATED CLONING CODE - DO NOT MODIFY! */
}
