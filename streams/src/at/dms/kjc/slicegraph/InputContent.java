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
public class InputContent extends PredefinedContent {
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
}
