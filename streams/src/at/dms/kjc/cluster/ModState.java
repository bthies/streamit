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
 * Determines if a method modifies the state of a stream node!
 */

public class ModState extends SLIREmptyVisitor {

    boolean mod;

    ModState() {
        mod = false;
    }

    public static boolean methodModsState(JMethodDeclaration self) {
        ModState m = new ModState();

        if (ClusterBackend.debugPrint)
            System.out.println("=========== ModState: "+self.getName()+" ===========");
        m.visitBlockStatement(self.getBody(), null);
        if (ClusterBackend.debugPrint)
            System.out.println("============================================");

        return m.mod;
    }

    

    public void visitPrefixExpression(JPrefixExpression self,
                                      int oper,
                                      JExpression expr) {

        if (expr instanceof JFieldAccessExpression) {
            JFieldAccessExpression f_expr = (JFieldAccessExpression)expr;
            if (ClusterBackend.debugPrint)
                System.out.println("ModState: field "+f_expr.getIdent()+" changed by a prefix expression");
            mod = true;
        }
    }

    public void visitPostfixExpression(JPostfixExpression self,
                                       int oper,
                                       JExpression expr) {

        if (expr instanceof JFieldAccessExpression) {
            JFieldAccessExpression f_expr = (JFieldAccessExpression)expr;
            //System.out.println("ModState: field "+f_expr.getIdent()+" changed by a postfix expression");
            mod = true;     
        }
    }


    public void visitAssignmentExpression(JAssignmentExpression self,
                                          JExpression left,
                                          JExpression right) {

        if (left instanceof JFieldAccessExpression) {
            JFieldAccessExpression f_expr = (JFieldAccessExpression)left;
            if (ClusterBackend.debugPrint)
                System.out.println("ModState: field "+f_expr.getIdent()+" changed by an assignement expression");
            mod = true;
        }

        if (left instanceof JArrayAccessExpression) {
            JArrayAccessExpression a_expr = (JArrayAccessExpression)left;

            if (a_expr.getPrefix() instanceof JFieldAccessExpression) {
                JFieldAccessExpression f_expr = (JFieldAccessExpression)a_expr.getPrefix();
                if (ClusterBackend.debugPrint)
                    System.out.println("ModState: field "+f_expr.getIdent()+" changed by an assignement expression");
                mod = true;
            }
        }
    }


    public void visitCompoundAssignmentExpression(JCompoundAssignmentExpression self,
                                                  int oper,
                                                  JExpression left,
                                                  JExpression right) {

        if (left instanceof JFieldAccessExpression) {
            JFieldAccessExpression f_expr = (JFieldAccessExpression)left;
            if (ClusterBackend.debugPrint)
                System.out.println("ModState: field "+f_expr.getIdent()+" changed by an assignement expression");
            mod = true;
        }

        if (left instanceof JArrayAccessExpression) {
            JArrayAccessExpression a_expr = (JArrayAccessExpression)left;

            if (a_expr.getPrefix() instanceof JFieldAccessExpression) {
                JFieldAccessExpression f_expr = (JFieldAccessExpression)a_expr.getPrefix();
                if (ClusterBackend.debugPrint)
                    System.out.println("ModState: field "+f_expr.getIdent()+" changed by an assignement expression");
                mod = true;
            }
        }
    }

}
