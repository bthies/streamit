package at.dms.kjc.sir.linear;

import java.util.*;
import at.dms.kjc.*;
import at.dms.kjc.sir.*;
import at.dms.kjc.sir.linear.*;
import at.dms.kjc.iterator.*;


/**
 * This class represents a field access expression. The field access expressions
 * provided by KOPI don't seem to do the right thing for equals.
 * Basically, JFieldAccessExpressions are compared using object equality
 * and I want them to be compared using value equality (eg two wrappers that
 * represent the same field should be equal).<p>
 *
 * As an interesting note on implementation in KOPI, it seems as though there are
 * two cases that we will accept as being valid "prefix" part of a JFieldAccessExpression.
 * The first is a JThisExpression if the field is a local field.
 * The second is a JLocalVariableExpression if the field is from a class
 * that is being used as a structure.<p>
 *
 * If the prefix is not one of the above things, then we give up and don't
 * wrap it.
 *
 * $Id: FieldAccessWrapper.java,v 1.1 2002-09-03 21:28:24 aalamb Exp $
 **/

class FieldAccessWrapper {
    /** space to hold the field name **/
    String fieldIdentifier;

    /**
     * Create a new access wrapper for the specified field name.
     **/
    FieldAccessWrapper(String ident) {
	this.fieldIdentifier = ident;
    }

    // the this and local var versions are subclasses
    
    /**
     * Factory method for creating FeildAccessWrappers. Returns null
     * if the field can not be wrapper (see rules in main class description)
     * otherwise returns a wrapper for this field.
     **/
    public static FieldAccessWrapper wrapFieldAccess(JFieldAccessExpression expr) {
	if (expr == null) {throw new RuntimeException("null arg passed to wrapFieldAccess");}
	// get the prefix of the field access (eg the structure/object that it belongs to)
	JExpression prefix = expr.getPrefix();

	// dispatch on type (bad OO, so sue me).
	if (prefix instanceof JThisExpression) {
	    return new ThisFieldAccessWrapper(expr.getIdent());
	} else if (prefix instanceof JLocalVariableExpression) {
	    return new LocalVariableFieldAccessWrapper(expr.getIdent(),
						       ((JLocalVariableExpression)prefix).getVariable());
	} else {
	    System.err.println("Warning: can't wrap a FieldAccessExpression with prefix : " + prefix);
	    return null;
	}
    }

    /**
     * Returns true if the specified object represents the same
     * field access expression as this.
     **/
    public boolean equals(Object o) {
	if (o == null) {return false;}
	FieldAccessWrapper other = (FieldAccessWrapper)o;
	if (other.fieldIdentifier.equals(this.fieldIdentifier)) {
	    return true;
	} else {
	    return false;
	}
    }

    /**
     * returns the hashcode from the identity string.
     * simple way to preserve the semantics of equals --
     * if two objects are equal, then their hashCode()
     * methods return the same value.
     **/
    public int hashCode() {
	return this.fieldIdentifier.hashCode();
    }
}

/**
 * Represents a local field (eg of the form this.fieldname).
 **/
class ThisFieldAccessWrapper extends FieldAccessWrapper {
    public ThisFieldAccessWrapper(String ident) {
	super(ident);
    }
    public String toString() {
	return ("<this." +this.fieldIdentifier + ">");
    }
}

/**
 * Represents a field that is accessed via a local variable.
 **/
class LocalVariableFieldAccessWrapper extends FieldAccessWrapper {
    JLocalVariable localVariable;
    
    public LocalVariableFieldAccessWrapper(String ident,
					   JLocalVariable var) {
	super(ident);
	this.localVariable = var;
    }

    /** update the equals method to also check that the vars are the same. **/
    public boolean equals(Object o) {
	// if the types of object and the field names don't match
	// then we are done.
	if (!super.equals(o)) {
	    return false;
	}

	// check for the correct type
	if (!(o instanceof LocalVariableFieldAccessWrapper)) {
	    return false;
	}
	// casting magic
	LocalVariableFieldAccessWrapper other = (LocalVariableFieldAccessWrapper)o;
	// remember the super class already checked the field name for us,
	// all we have to do is to check the variable.
	if (other.localVariable.getIdent().equals(this.localVariable.getIdent())) {
	    return true;
	} else {
	    return false;
	}
    }
    public String toString() {
	return ("<" + this.localVariable + "." +this.fieldIdentifier + ">");
    }

}
	    


