package at.dms.kjc.lir;

import at.dms.kjc.*;
import at.dms.compiler.*;

/**
 * This represents a function pointer.  (Should we include the
 * signature of the function in here, too?
 */
public class LIRFunctionPointer {

    /**
     * The name of the function.
     */
    private String name;

    // ----------------------------------------------------------------------
    // CONSTRUCTORS
    // ----------------------------------------------------------------------

    /**
     * Construct a node in the parsing tree
     */
    public LIRFunctionPointer(String name) {
        this.name = name;
    }

    /**
     * Construct a node in the parsing tree
     */
    public LIRFunctionPointer(JMethodDeclaration meth) {
        this(meth.getName());
    }

    /**
     * Returns the name of the function pointed to.
     */
    public String getName() {
        return name;
    }

    /**
     * Accepts the specified attribute visitor.
     * @param   p               the visitor
     */
    public Object accept(AttributeVisitor p) {
        if (p instanceof SLIRAttributeVisitor) {
            return ((SLIRAttributeVisitor)p).visitFunctionPointer(this, name);
        } else {
            return this;
        }
    }

    public void accept(SLIRVisitor v)
    {
        v.visitFunctionPointer(this, this.getName());
    }
}
