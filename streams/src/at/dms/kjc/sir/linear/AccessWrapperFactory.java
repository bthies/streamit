package at.dms.kjc.sir.linear;

import java.util.*;
import at.dms.kjc.*;
import at.dms.kjc.sir.*;
import at.dms.kjc.sir.linear.*;
import at.dms.kjc.iterator.*;

/**
 * Class for creating access wrappers.
 * $Id: AccessWrapperFactory.java,v 1.1 2002-09-09 21:52:06 aalamb Exp $
 **/
class AccessWrapperFactory {
    /**
     * Factory method for creating AccessWrappers. Returns
     * null if we don't know how to wrap an expression.
     **/
    public static AccessWrapper wrapAccess(JExpression expr) {
	if (expr == null) {throw new RuntimeException("null arg passed to wrapAccess");}
	
	// dispatch on type (bad OO, so sue me).
	if (expr instanceof JIntLiteral) {
	    return new IntegerAccessWrapper((JIntLiteral)expr);
	} else if (expr instanceof JThisExpression) {
	    return new ThisAccessWrapper();
	} else if (expr instanceof JLocalVariableExpression) {
	    JLocalVariableExpression lve = (JLocalVariableExpression)expr;
	    return new LocalVariableAccessWrapper(lve.getVariable());
	} else if (expr instanceof JFieldAccessExpression) {
	    JFieldAccessExpression accessExpr = (JFieldAccessExpression)expr;
	    AccessWrapper prefixWrapper = AccessWrapperFactory.wrapAccess(accessExpr.getPrefix());
	    // if we successfully wrapped the prefix, return a wrapper for the field
	    if (prefixWrapper != null) {
		return new FieldAccessWrapper(prefixWrapper, accessExpr.getIdent());
	    } else {return null;}
	} else if (expr instanceof JArrayAccessExpression) {
	    JArrayAccessExpression aae = (JArrayAccessExpression)expr;
	    // see if we can figure out if the index expression is a constant.
	    if (!(aae.getAccessor() instanceof JIntLiteral)) {
		return null;
	    }
	    int index = ((JIntLiteral)aae.getAccessor()).intValue();
	    // try and wrap the prefix
	    AccessWrapper prefixWrapper = AccessWrapperFactory.wrapAccess(aae.getPrefix());
	    if (prefixWrapper != null) {
		return new ArrayAccessWrapper(prefixWrapper, index);
	    } else {return null;}
	} else {
	    System.err.println("Warning: can't wrap a : " + expr);
	    return null;
	}
    }


}


///////////////////////////////////////////
//////// Inner Classes
///////////////////////////////////////////    

/** Wraps an integer. **/
class IntegerAccessWrapper extends AccessWrapper {
    JIntLiteral value;
    public IntegerAccessWrapper(JIntLiteral val) {
	super("integer"); this.value=val;
    }
    public boolean equals(Object o) {
	if (!super.equals(o)) {return false;}
	if (!(o instanceof IntegerAccessWrapper)) {return false;}
	IntegerAccessWrapper other = (IntegerAccessWrapper)o;
	return (other.value.equals(this.value));
    }
}

/** Wraps a this expression **/
class ThisAccessWrapper extends AccessWrapper {
    public ThisAccessWrapper() {
	super("this");
    }
    public boolean equals(Object o) {
	if (!super.equals(o)) {return false;}
	if (!(o instanceof ThisAccessWrapper)) {return false;}
	return true;
    }
}

/** Wraps a local variable. **/
class LocalVariableAccessWrapper extends AccessWrapper {
    JLocalVariable variable;
    public LocalVariableAccessWrapper(JLocalVariable var) {
	super(var.getIdent());
	this.variable = var;
    }
    // the local variable is completely described by its name, so just
    // use the equals method from AccessWrapper.
}

/** Wraps a field access expression. **/
class FieldAccessWrapper extends AccessWrapper {
    AccessWrapper prefix;
    public FieldAccessWrapper(AccessWrapper pre, String f) {
	super(pre.getIdent() + "." + f);
	this.prefix = pre;
    }
    public boolean equals(Object o) {
	if (!super.equals(o)) {return false;}
	if (!(o instanceof FieldAccessWrapper)) {return false;}
	FieldAccessWrapper other = (FieldAccessWrapper)o;
	return (other.prefix.equals(this.prefix));
    }
}

/** Wraps an array access. Index must be a compile time known constant. **/
class ArrayAccessWrapper extends AccessWrapper {
    AccessWrapper base;
    int index;
    public ArrayAccessWrapper(AccessWrapper b, int i) {
	super(b.getIdent() + "[" + i + "]");
	this.base = b;
	this.index = i;
    }
    public boolean equals(Object o) {
	if (!super.equals(o)) {return false;}
	if (!(o instanceof ArrayAccessWrapper)) {return false;}
	ArrayAccessWrapper other = (ArrayAccessWrapper)o;
	return (other.base.equals(this.base) &&
		(other.index == this.index));
    }
}
