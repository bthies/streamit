package at.dms.kjc.sir;

import at.dms.kjc.*;
import at.dms.kjc.iterator.*;
import at.dms.kjc.sir.*;
import at.dms.util.Utils;
import java.util.*;

/**
 * This generates unique names for stream structures.
 */
class Namer extends at.dms.util.Utils {
    /**
     * Mapping from stream structure (SIROperator) to name (String) of
     * structure in target program.
     */
    private static HashMap names = new HashMap();

    /**
     * Mapping from name (string) to an integer which is the highest
     * number currently appended to that string in any existing name.
     */
    private static HashMap version = new HashMap();

    /**
     * Clear all names.
     */
    public static void clearNames() {
	names.clear();
	version.clear();
    }
    
    /**
     * Returns the stream associated with name <name>.  This takes
     * linear time instead of constant (could optimize by maintaining
     * a reverse map if desired.)  Requires that this contains a
     * stream by the name of <name>.
     */
    public static SIROperator getStream(String name) {
	for (Iterator it = names.entrySet().iterator(); it.hasNext(); ) {
	    Map.Entry entry = (Map.Entry)it.next();
	    if (entry.getValue().equals(name)) {
		return (SIROperator)entry.getKey();
	    }
	}
	Utils.fail("Couldn't find stream by name of \"" + name + "\"");
	// stupid compiler
	return null;
    }

    /**
     * Returns the name for <str>.  If a name has not yet been
     * assigned to <str>, creates a unique name and remembers it for
     * future references.
     */
    public static String getName(SIROperator str) {
	// if we've already made a name for <str>, then just return
	// it.
	if (names.containsKey(str)) {
	    return (String)names.get(str);
	}
	// otherwise, see if we've assigned a name with the same
	// identifier as <str>, and get next version number
	String ident = str.getIdent();
	int ver;
	if (version.containsKey(ident)) {
	    ver = 1 + ((Integer)version.get(ident)).intValue();
	} else {
	    ver = 1;
	}
	// build result
	String result = ident + "_" + ver;
	// update the version and name lists
	version.put(ident, new Integer(ver));
	names.put(str, result);
	return result;
    }
}
