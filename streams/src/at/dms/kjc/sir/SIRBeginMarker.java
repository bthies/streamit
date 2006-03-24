package at.dms.kjc.sir;

import at.dms.kjc.JStatement;

/**
 * This statement is an annotation that marks the beginning of a given
 * function or stream so that it can be followed through fusion, etc.
 */
public class SIRBeginMarker extends SIRMarker {
    /**
     * The name of the stream, function, etc. that is beginning at
     * this statement.
     */
    private String name;

    /**
     * For cloner only.
     */
    private SIRBeginMarker() {
        super();
    }

    /**
     * Create an SIRBeginMarker.
     *
     * @param name   The name of the stream, function, etc.
     */
    public SIRBeginMarker(String name) {
        super();
        this.name = name;
    }

    /**
     * @return the name of the stream being marked.
     */
    public String getName() {
        return name;
    }

    /** THE FOLLOWING SECTION IS AUTO-GENERATED CLONING CODE - DO NOT MODIFY! */

    /** Returns a deep clone of this object. */
    public Object deepClone() {
        at.dms.kjc.sir.SIRBeginMarker other = new at.dms.kjc.sir.SIRBeginMarker();
        at.dms.kjc.AutoCloner.register(this, other);
        deepCloneInto(other);
        return other;
    }

    /** Clones all fields of this into <pre>other</pre> */
    protected void deepCloneInto(at.dms.kjc.sir.SIRBeginMarker other) {
        super.deepCloneInto(other);
        other.name = (java.lang.String)at.dms.kjc.AutoCloner.cloneToplevel(this.name);
    }

    /** THE PRECEDING SECTION IS AUTO-GENERATED CLONING CODE - DO NOT MODIFY! */
}
