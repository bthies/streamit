package at.dms.kjc.lir;

import at.dms.kjc.*;
import at.dms.compiler.*;

/**
 * This represents the types of special work functions that are
 * defined by the backend.
 */
public class LIRSpecialWorkType {

    /**
     * A file reader.
     */
    public static final LIRSpecialWorkType 
	FILE_READER = new LIRSpecialWorkType("File Reader");
    /**
     * A file writer.
     */
    public static final LIRSpecialWorkType 
	FILE_WRITER = new LIRSpecialWorkType("File Writer");

    /**
     * The name of this type.
     */
    private final String name;
    
    // ----------------------------------------------------------------------
    // CONSTRUCTORS
    // ----------------------------------------------------------------------

    /**
     * Construct a node.
     */
    private LIRSpecialWorkType(String name) {
	this.name = name;
    }

    /**
     * Return a string representation of this.
     */
    public String toString(){
	return "LIRSpecialWorkType \"" + name + "\"";
    }
}
