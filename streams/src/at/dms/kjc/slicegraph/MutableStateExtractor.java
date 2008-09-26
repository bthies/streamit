package at.dms.kjc.slicegraph;

import java.util.HashSet;
import java.util.List;

import at.dms.kjc.JAssignmentExpression;
import at.dms.kjc.JBlock;
import at.dms.kjc.JCompoundAssignmentExpression;
import at.dms.kjc.JExpression;
import at.dms.kjc.JExpressionStatement;
import at.dms.kjc.JFieldAccessExpression;
import at.dms.kjc.JFieldDeclaration;
import at.dms.kjc.JIntLiteral;
import at.dms.kjc.JMethodDeclaration;
import at.dms.kjc.JPostfixExpression;
import at.dms.kjc.JPrefixExpression;
import at.dms.kjc.JStatement;
import at.dms.kjc.JVariableDefinition;
import at.dms.kjc.slicegraph.FilterContent;
import at.dms.kjc.SLIREmptyVisitor;
import at.dms.util.GetSteadyMethods;

/**
 * This class extracts the mutable state from FilterContent.  Most of these
 * methods were pulled out of at.dms.kjc.sir.lowering.fission.StatelessDuplicate
 */
public class MutableStateExtractor {

    /**
     * Returns whether or not <filter> has mutable state.  This is
     * equivalent to checking if there are any assignments to fields
     * outside of the init function.
     */

    public static boolean hasMutableState(final FilterContent filter) {
        return sizeOfMutableState(filter) > 0;
    }

    public static HashSet<String> getMutableState(final FilterContent filter) {
        // visit all methods except <init>
        List<JMethodDeclaration> methods = GetSteadyMethods.getSteadyMethods(filter);
        
        return doGetMutableState(methods);
    }

    private static HashSet<String> doGetMutableState(List<JMethodDeclaration> methods) {
        // whether or not we are on the LHS of an assignment
        final boolean[] inAssignment = { false };
        // names of fields that are mutatable state
        final HashSet<String> mutatedFields = new HashSet<String>();
        
        for (JMethodDeclaration method : methods)
            method.accept(new SLIREmptyVisitor() {
                    // wrap visit to <left> with some book-keeping
                    // to indicate that it is on the LHS of an assignment
                    private void wrapVisit(JExpression left) {
                        // in case of nested assignment
                        // expressions, remember the old value of <inAssignment>
                        boolean old = inAssignment[0];
                        inAssignment[0] = true;
                        left.accept(this);
                        inAssignment[0] = old;
                    }

                    public void visitCompoundAssignmentExpression(JCompoundAssignmentExpression self,
                                                                  int oper,
                                                                  JExpression left,
                                                                  JExpression right) {
                        wrapVisit(left);
                        right.accept(this);
                    }

                    public void visitAssignmentExpression(JAssignmentExpression self,
                                                          JExpression left,
                                                          JExpression right) {
                        wrapVisit(left);
                        right.accept(this);
                    }

                    // need to count i++ as mutated state if i is a field
                    public void visitPostfixExpression(JPostfixExpression self,
                                                       int oper,
                                                       JExpression expr) {
                        wrapVisit(expr);
                    }

                    // need to count ++i as mutated state if i is a field
                    public void visitPrefixExpression(JPrefixExpression self,
                                                      int oper,
                                                      JExpression expr) {
                        wrapVisit(expr);
                    }



                    public void visitFieldExpression(JFieldAccessExpression self,
                                                     JExpression left,
                                                     String ident) {
                        // if we are in assignment, mark that there is mutable state
                        if (inAssignment[0]) {
                            mutatedFields.add(self.getIdent());
                        }

                        super.visitFieldExpression(self, left, ident);
                    }
                        
                });
        return mutatedFields;
    }

    public static int sizeOfMutableState(final FilterContent filter) {
        HashSet<String> mutatedFields = getMutableState(filter);
        // tally up the size of all the fields found
        int mutableSizeInC = 0;
        JFieldDeclaration fields[] = filter.getFields();
        for (int i=0; i<fields.length; i++) {
            JVariableDefinition var = fields[i].getVariable();
            if (mutatedFields.contains(var.getIdent())) {
                if (var.getType() == null) {
                    // this should never happen
                    System.err.println("Warning: found null type of variable in JFieldDeclaration.");
                    mutableSizeInC++;  // increment size just in case
                } else {
                    int size = var.getType().getSizeInC();
                    // fields should always have non-zero size
                    assert size > 0;
                    mutableSizeInC += size;
                }
            }
        }

        return mutableSizeInC;
    }    
}