package at.dms.kjc.sir;

import at.dms.kjc.*;
import at.dms.util.Utils;
import java.io.Serializable;

/**
 * This class enumerates the types of splitters.
 */
public class SIRSplitType implements Serializable {
    /**
     * A duplicating splitter.
     */
    public static final SIRSplitType DUPLICATE 
	= new SIRSplitType("DUPLICATE");
    /**
     * An equal-weight round robing splitter.
     */
    public static final SIRSplitType ROUND_ROBIN 
	= new SIRSplitType("ROUND_ROBIN");
    /**
     * A round robin splitter with individual weights for each tape.
     */
    public static final SIRSplitType WEIGHTED_RR 
	= new SIRSplitType("WEIGHTED_ROUND_ROBIN");
    /**
     * A null splitter, providing no tokens on its output.
     */
    public static final SIRSplitType NULL 
	= new SIRSplitType("NULL_SJ");
    /**
     * The name of this type.
     */
    private String name;

    /**
     * No argument constructor, FOR AUTOMATIC CLONING ONLY.
     */
    private SIRSplitType() {
	super();
    }
    
    /**
     * Constructs a split type with name <name>.
     */
    private SIRSplitType(String name) {
	this.name = name;
    }

    private Object readResolve() throws Exception {
	if (this.name.equals("DUPLICATE"))
	    return this.DUPLICATE;
	if (this.name.equals("ROUND_ROBIN"))
	    return this.ROUND_ROBIN;
	if (this.name.equals("WEIGHTED_ROUND_ROBIN"))
	    return this.WEIGHTED_RR;
	if (this.name.equals("NULL_SJ"))
	    return this.NULL;
	else 
	    throw new Exception();
    }
    
    public String toString() {
	return name;
    }
}
