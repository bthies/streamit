package at.dms.kjc.cluster;

import at.dms.kjc.flatgraph.FlatNode;
import at.dms.kjc.flatgraph.FlatVisitor;
import at.dms.kjc.*;
import at.dms.kjc.sir.*;
import at.dms.kjc.iterator.*;
import at.dms.util.Utils;
import java.util.List;
import java.util.ListIterator;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.HashMap;
import java.util.HashSet;
import java.io.*;
import at.dms.compiler.*;
import at.dms.kjc.sir.lowering.*;
import java.util.Hashtable;
import java.util.Vector;

import at.dms.kjc.raw.*;

/**
 * This class dumps the tile code for each filter into a file based 
 * on the tile number assigned 
 */
public class DetectConst extends SLIREmptyVisitor {

    private static HashMap instances = new HashMap();

    private HashMap methodsToVisit;
    private HashSet fieldIsModified;

    public static void detect(FlatNode node) 
    {
	DetectConst dc = new DetectConst((SIRFilter)node.contents);
	dc.visitFilter((SIRFilter)node.contents);
    }

    public static DetectConst getInstance(SIRFilter filter) {
	if (!instances.containsKey(filter)) return null;
	return (DetectConst)instances.get(filter);
    }
    


    public DetectConst(SIRFilter filter) 
    {
	instances.put(filter, this);
    }
       
    public boolean isConst(String field) {
	if (fieldIsModified.contains(field)) return false;
	return true;
    }

    public void visitFilter(SIRFilter self) {

	System.out.println();
	System.out.println("DetectConst: visiting filter "+self.getIdent());

	JMethodDeclaration work = self.getWork();
	JMethodDeclaration init = self.getInit();

	//visit methods of filter, print the declaration first

	JMethodDeclaration[] methods = self.getMethods();

	methodsToVisit = new HashMap();
	fieldIsModified = new HashSet();
	int old_size = 0;

	methodsToVisit.put("__CLUSTERMAIN__", new Boolean(false));

	while (methodsToVisit.size() != old_size) {
	    old_size = methodsToVisit.size();
	    for (int i = 0; i < methods.length; i++) {
		String currMethod = methods[i].getName();
		if (methodsToVisit.containsKey(currMethod)) {
		    Boolean done = (Boolean)methodsToVisit.get(currMethod);
		    if (!done.booleanValue()) {
			methods[i].accept(this);
			methodsToVisit.put(currMethod, new Boolean(true));
		    }
		}
	    }
	}
    }

    public void visitPhasedFilter(SIRPhasedFilter self,
                                  SIRPhasedFilterIter iter) {
        // This is a stub; it'll get filled in once we figure out how phased
        // filters should actually work.
    }


    public void visitMethodDeclaration(JMethodDeclaration self,
                                       int modifiers,
                                       CType returnType,
                                       String ident,
                                       JFormalParameter[] parameters,
                                       CClassType[] exceptions,
                                       JBlock body) {

	System.out.println("DetectConst: visiting method "+ident);
	this.visitBlockStatement(body, null);
    }

    public void visitMethodCallExpression(JMethodCallExpression self,
                                          JExpression prefix,
                                          String ident,
                                          JExpression[] args) {
    
	if (!methodsToVisit.containsKey(ident)) {
	    methodsToVisit.put(ident, new Boolean(false));
	}

	//System.out.println("Method call expression: "+ident);
    }



    public void visitPrefixExpression(JPrefixExpression self,
                                      int oper,
                                      JExpression expr) {

	if (expr instanceof JFieldAccessExpression) {
	    JFieldAccessExpression f_expr = (JFieldAccessExpression)expr;
	    System.out.println("DetectConst: field "+f_expr.getIdent()+" changed by a prefix expression");
	    fieldIsModified.add(f_expr.getIdent());
	}
    }

    public void visitPostfixExpression(JPostfixExpression self,
                                       int oper,
                                       JExpression expr) {

	if (expr instanceof JFieldAccessExpression) {
	    JFieldAccessExpression f_expr = (JFieldAccessExpression)expr;
	    System.out.println("DetectConst: field "+f_expr.getIdent()+" changed by a postfix expression");
	    fieldIsModified.add(f_expr.getIdent());	    
	}
    }


    public void visitAssignmentExpression(JAssignmentExpression self,
                                          JExpression left,
                                          JExpression right) {

	if (left instanceof JFieldAccessExpression) {
	    JFieldAccessExpression f_expr = (JFieldAccessExpression)left;
	    System.out.println("DetectConst: field "+f_expr.getIdent()+" changed by an assignement expression");
	    fieldIsModified.add(f_expr.getIdent());
	}

	if (left instanceof JArrayAccessExpression) {
	    JArrayAccessExpression a_expr = (JArrayAccessExpression)left;

	    if (a_expr.getPrefix() instanceof JFieldAccessExpression) {
		JFieldAccessExpression f_expr = (JFieldAccessExpression)a_expr.getPrefix();
		System.out.println("DetectConst: field "+f_expr.getIdent()+" changed by an assignement expression");
		fieldIsModified.add(f_expr.getIdent());
	    }
	}
    }



    public void visitCompoundAssignmentExpression(JCompoundAssignmentExpression self,
                                                  int oper,
                                                  JExpression left,
                                                  JExpression right) {

	if (left instanceof JFieldAccessExpression) {
	    JFieldAccessExpression f_expr = (JFieldAccessExpression)left;
	    System.out.println("DetectConst: field "+f_expr.getIdent()+" changed by a compound assignement expression");
	    fieldIsModified.add(f_expr.getIdent());
	}

	if (left instanceof JArrayAccessExpression) {
	    JArrayAccessExpression a_expr = (JArrayAccessExpression)left;

	    if (a_expr.getPrefix() instanceof JFieldAccessExpression) {
		JFieldAccessExpression f_expr = (JFieldAccessExpression)a_expr.getPrefix();
		System.out.println("DetectConst: field "+f_expr.getIdent()+" changed by a compound assignement expression");
		fieldIsModified.add(f_expr.getIdent());
	    }
	}
    }

}




