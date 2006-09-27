/**
 * 
 */
package at.dms.kjc;

import at.dms.kjc.sir.SIRCreatePortal;
import at.dms.kjc.sir.SIRDynamicToken;
import at.dms.kjc.sir.SIRInterfaceTable;
import at.dms.kjc.sir.SIRPeekExpression;
import at.dms.kjc.sir.SIRPopExpression;
import at.dms.kjc.sir.SIRPortal;
import at.dms.kjc.sir.SIRPushExpression;
import at.dms.kjc.sir.SIRRangeExpression;

/**
 * Implementation of ExpressionVisitor, implements all methods by asserting false.
 * Can be set to redispatch to combined visit for superclass before failing.
 * @author dimock
 *
 */
public class ExpressionVisitorBase implements ExpressionVisitor {

    private boolean redispatchBinary = false;
    private boolean redispatchBinaryArithmetic = false;
    private boolean redispatchLiteral = false;
    private boolean redispatchUnary = false;

    /**
     * Collect visits to subtypes of JBinaryExpression into visitBinary.
     * @param tf  true to use visitBinary for subtypes of JBinaryExpression
     */
    public void collectBinary(boolean tf) {
	redispatchBinary = tf;
    }

    /**
     * Collect visits to subtypes of JBinaryArithmeticExpression into visitBinaryArithmetic.
     * calling collectBinary(true) will cause use of visitBinary instead.
     * @param tf  true to use visitBinary for subtypes of JBinaryArithmeticExpression
     */
    public void collectBinaryArithetic(boolean tf) {
	redispatchBinaryArithmetic = tf;
    }

    /**
     * Collect visits to subtypes of JLiteral into visitLiteral
     * @param tf  true to use visitLiteral for subtypes of JLiteral
     */
   public void collectLiteral(boolean tf) {
	redispatchLiteral = tf;
    }

   /**
    * Collect visits to subtypes of JUnary into visitUnary
    * @param tf  true to use visitUnary for subtypes of JUnary
    */
    public void collectUnary(boolean tf) {
	redispatchUnary = tf;
    }

    /* (non-Javadoc)
     * @see at.dms.kjc.ExpressionVisitor#visitAdd(at.dms.kjc.JAddExpression, java.lang.Object)
     */
    public Object visitAdd(JAddExpression self, Object otherData) {
	if (redispatchBinary) return visitBinary(self,otherData);
	if (redispatchBinaryArithmetic) return visitBinaryArithmetic(self,otherData);
        assert false : "Unexpected Expression";
        return null;
    }

    /* (non-Javadoc)
     * @see at.dms.kjc.ExpressionVisitor#visitArrayAccess(at.dms.kjc.JArrayAccessExpression, java.lang.Object)
     */
    public Object visitArrayAccess(JArrayAccessExpression self, Object otherData) {
        assert false : "Unexpected Expression";
        return null;
    }

    /* (non-Javadoc)
     * @see at.dms.kjc.ExpressionVisitor#visitArrayInitializer(at.dms.kjc.JArrayInitializer, java.lang.Object)
     */
    public Object visitArrayInitializer(JArrayInitializer self, Object otherData) {
        assert false : "Unexpected Expression";
        return null;
    }

    /* (non-Javadoc)
     * @see at.dms.kjc.ExpressionVisitor#visitArrayLength(at.dms.kjc.JArrayLengthExpression, java.lang.Object)
     */
    public Object visitArrayLength(JArrayLengthExpression self, Object otherData) {
        assert false : "Unexpected Expression";
        return null;
    }

    /* (non-Javadoc)
     * @see at.dms.kjc.ExpressionVisitor#visitAssignment(at.dms.kjc.JAssignmentExpression, java.lang.Object)
     */
    public Object visitAssignment(JAssignmentExpression self, Object otherData) {
	if (redispatchBinary) return visitBinary(self,otherData);
        assert false : "Unexpected Expression";
        return null;
    }

    /* (non-Javadoc)
     * @see at.dms.kjc.ExpressionVisitor#visitBinary(at.dms.kjc.JBinaryExpression, java.lang.Object)
     */
    public Object visitBinary(JBinaryExpression self, Object otherData) {
        assert false : "Unexpected Expression";
        return null;
    }

    /* (non-Javadoc)
     * @see at.dms.kjc.ExpressionVisitor#visitBinaryArithmetic(at.dms.kjc.JBinaryArithmeticExpression, java.lang.Object)
     */
    public Object visitBinaryArithmetic(JBinaryArithmeticExpression self,
            Object otherData) {
	if (redispatchBinary) return visitBinary(self,otherData);
        assert false : "Unexpected Expression";
        return null;
    }

    /* (non-Javadoc)
     * @see at.dms.kjc.ExpressionVisitor#visitBitwise(at.dms.kjc.JBitwiseExpression, java.lang.Object)
     */
    public Object visitBitwise(JBitwiseExpression self, Object otherData) {
	if (redispatchBinary) return visitBinary(self,otherData);
	if (redispatchBinaryArithmetic) return visitBinaryArithmetic(self,otherData);
        assert false : "Unexpected Expression";
        return null;
    }

    /* (non-Javadoc)
     * @see at.dms.kjc.ExpressionVisitor#visitBitwiseComplement(at.dms.kjc.JBitwiseComplementExpression, java.lang.Object)
     */
    public Object visitBitwiseComplement(JBitwiseComplementExpression self,
            Object otherData) {
	if (redispatchUnary) return visitUnary(self,otherData);
        assert false : "Unexpected Expression";
        return null;
    }

    /* (non-Javadoc)
     * @see at.dms.kjc.ExpressionVisitor#visitBooleanLiteral(at.dms.kjc.JBooleanLiteral, java.lang.Object)
     */
    public Object visitBooleanLiteral(JBooleanLiteral self, Object otherData) {
	if (redispatchLiteral) return visitLiteral(self,otherData);
        assert false : "Unexpected Expression";
        return null;
    }

    /* (non-Javadoc)
     * @see at.dms.kjc.ExpressionVisitor#visitByteLiteral(at.dms.kjc.JByteLiteral, java.lang.Object)
     */
    public Object visitByteLiteral(JByteLiteral self, Object otherData) {
	if (redispatchLiteral) return visitLiteral(self,otherData);
        assert false : "Unexpected Expression";
        return null;
    }

    /* (non-Javadoc)
     * @see at.dms.kjc.ExpressionVisitor#visitCast(at.dms.kjc.JCastExpression, java.lang.Object)
     */
    public Object visitCast(JCastExpression self, Object otherData) {
        assert false : "Unexpected Expression";
        return null;
    }

    /* (non-Javadoc)
     * @see at.dms.kjc.ExpressionVisitor#visitCharLiteral(at.dms.kjc.JCharLiteral, java.lang.Object)
     */
    public Object visitCharLiteral(JCharLiteral self, Object otherData) {
	if (redispatchLiteral) return visitLiteral(self,otherData);
        assert false : "Unexpected Expression";
        return null;
    }

    /* (non-Javadoc)
     * @see at.dms.kjc.ExpressionVisitor#visitChecked(at.dms.kjc.JCheckedExpression, java.lang.Object)
     */
    public Object visitChecked(JCheckedExpression self, Object otherData) {
        assert false : "Unexpected Expression";
        return null;
    }

    /* (non-Javadoc)
     * @see at.dms.kjc.ExpressionVisitor#visitClass(at.dms.kjc.JClassExpression, java.lang.Object)
     */
    public Object visitClass(JClassExpression self, Object otherData) {
        assert false : "Unexpected Expression";
        return null;
    }

    /* (non-Javadoc)
     * @see at.dms.kjc.ExpressionVisitor#visitCompoundAssignment(at.dms.kjc.JCompoundAssignmentExpression, java.lang.Object)
     */
    public Object visitCompoundAssignment(JCompoundAssignmentExpression self,
            Object otherData) {
	if (redispatchBinary) return visitBinary(self,otherData);
        assert false : "Unexpected Expression";
        return null;
    }

    /* (non-Javadoc)
     * @see at.dms.kjc.ExpressionVisitor#visitConditional(at.dms.kjc.JConditionalExpression, java.lang.Object)
     */
    public Object visitConditional(JConditionalExpression self, Object otherData) {
        assert false : "Unexpected Expression";
        return null;
    }

    /* (non-Javadoc)
     * @see at.dms.kjc.ExpressionVisitor#visitConditionalAnd(at.dms.kjc.JConditionalAndExpression, java.lang.Object)
     */
    public Object visitConditionalAnd(JConditionalAndExpression self,
            Object otherData) {
	if (redispatchBinary) return visitBinary(self,otherData);
        assert false : "Unexpected Expression";
        return null;
    }

    /* (non-Javadoc)
     * @see at.dms.kjc.ExpressionVisitor#visitConditionalOr(at.dms.kjc.JConditionalOrExpression, java.lang.Object)
     */
    public Object visitConditionalOr(JConditionalOrExpression self,
            Object otherData) {
	if (redispatchBinary) return visitBinary(self,otherData);
        assert false : "Unexpected Expression";
        return null;
    }

    /* (non-Javadoc)
     * @see at.dms.kjc.ExpressionVisitor#visitConstructorCall(at.dms.kjc.JConstructorCall, java.lang.Object)
     */
    public Object visitConstructorCall(JConstructorCall self, Object otherData) {
        assert false : "Unexpected Expression";
        return null;
    }

    /* (non-Javadoc)
     * @see at.dms.kjc.ExpressionVisitor#visitCreatePortal(at.dms.kjc.sir.SIRCreatePortal, java.lang.Object)
     */
    public Object visitCreatePortal(SIRCreatePortal self, Object otherData) {
        assert false : "Unexpected Expression";
        return null;
    }

    /* (non-Javadoc)
     * @see at.dms.kjc.ExpressionVisitor#visitDivide(at.dms.kjc.JDivideExpression, java.lang.Object)
     */
    public Object visitDivide(JDivideExpression self, Object otherData) {
	if (redispatchBinary) return visitBinary(self,otherData);
	if (redispatchBinaryArithmetic) return visitBinaryArithmetic(self,otherData);
        assert false : "Unexpected Expression";
        return null;
    }

    /* (non-Javadoc)
     * @see at.dms.kjc.ExpressionVisitor#visitDoubleLiteral(at.dms.kjc.JDoubleLiteral, java.lang.Object)
     */
    public Object visitDoubleLiteral(JDoubleLiteral self, Object otherData) {
	if (redispatchLiteral) return visitLiteral(self,otherData);
        assert false : "Unexpected Expression";
        return null;
    }

    /* (non-Javadoc)
     * @see at.dms.kjc.ExpressionVisitor#visitDynamicToken(at.dms.kjc.sir.SIRDynamicToken, java.lang.Object)
     */
    public Object visitDynamicToken(SIRDynamicToken self, Object otherData) {
        assert false : "Unexpected Expression";
        return null;
    }

    /* (non-Javadoc)
     * @see at.dms.kjc.ExpressionVisitor#visitEquality(at.dms.kjc.JEqualityExpression, java.lang.Object)
     */
    public Object visitEquality(JEqualityExpression self, Object otherData) {
	if (redispatchBinary) return visitBinary(self,otherData);
        assert false : "Unexpected Expression";
        return null;
    }

    /* (non-Javadoc)
     * @see at.dms.kjc.ExpressionVisitor#visitFieldAccess(at.dms.kjc.JFieldAccessExpression, java.lang.Object)
     */
    public Object visitFieldAccess(JFieldAccessExpression self, Object otherData) {
        assert false : "Unexpected Expression";
        return null;
    }

    /* (non-Javadoc)
     * @see at.dms.kjc.ExpressionVisitor#visitFloatLiteral(at.dms.kjc.JFloatLiteral, java.lang.Object)
     */
    public Object visitFloatLiteral(JFloatLiteral self, Object otherData) {
	if (redispatchLiteral) return visitLiteral(self,otherData);
        assert false : "Unexpected Expression";
        return null;
    }

    /* (non-Javadoc)
     * @see at.dms.kjc.ExpressionVisitor#visitInstanceof(at.dms.kjc.JInstanceofExpression, java.lang.Object)
     */
    public Object visitInstanceof(JInstanceofExpression self, Object otherData) {
        assert false : "Unexpected Expression";
        return null;
    }

    /* (non-Javadoc)
     * @see at.dms.kjc.ExpressionVisitor#visitIntLiteral(at.dms.kjc.JIntLiteral, java.lang.Object)
     */
    public Object visitIntLiteral(JIntLiteral self, Object otherData) {
	if (redispatchLiteral) return visitLiteral(self,otherData);
        assert false : "Unexpected Expression";
        return null;
    }

    /* (non-Javadoc)
     * @see at.dms.kjc.ExpressionVisitor#visitInterfaceTable(at.dms.kjc.sir.SIRInterfaceTable, java.lang.Object)
     */
    public Object visitInterfaceTable(SIRInterfaceTable self, Object otherData) {
        assert false : "Unexpected Expression";
        return null;
    }

    /* (non-Javadoc)
     * @see at.dms.kjc.ExpressionVisitor#visitLiteral(at.dms.kjc.JLiteral, java.lang.Object)
     */
    public Object visitLiteral(JLiteral self, Object otherData) {
        assert false : "Unexpected Expression";
        return null;
    }

    /* (non-Javadoc)
     * @see at.dms.kjc.ExpressionVisitor#visitLocalVariable(at.dms.kjc.JLocalVariableExpression, java.lang.Object)
     */
    public Object visitLocalVariable(JLocalVariableExpression self,
            Object otherData) {
        assert false : "Unexpected Expression";
        return null;
    }

    /* (non-Javadoc)
     * @see at.dms.kjc.ExpressionVisitor#visitLogicalComplement(at.dms.kjc.JLogicalComplementExpression, java.lang.Object)
     */
    public Object visitLogicalComplement(JLogicalComplementExpression self,
            Object otherData) {
	if (redispatchUnary) return visitUnary(self,otherData);
        assert false : "Unexpected Expression";
        return null;
    }

    /* (non-Javadoc)
     * @see at.dms.kjc.ExpressionVisitor#visitLongLiteral(at.dms.kjc.JLongLiteral, java.lang.Object)
     */
    public Object visitLongLiteral(JLongLiteral self, Object otherData) {
	if (redispatchLiteral) return visitLiteral(self,otherData);
        assert false : "Unexpected Expression";
        return null;
    }

    /* (non-Javadoc)
     * @see at.dms.kjc.ExpressionVisitor#visitMethodCall(at.dms.kjc.JMethodCallExpression, java.lang.Object)
     */
    public Object visitMethodCall(JMethodCallExpression self, Object otherData) {
        assert false : "Unexpected Expression";
        return null;
    }

    /* (non-Javadoc)
     * @see at.dms.kjc.ExpressionVisitor#visitMinus(at.dms.kjc.JMinusExpression, java.lang.Object)
     */
    public Object visitMinus(JMinusExpression self, Object otherData) {
	if (redispatchBinary) return visitBinary(self,otherData);
	if (redispatchBinaryArithmetic) return visitBinaryArithmetic(self,otherData);
        assert false : "Unexpected Expression";
        return null;
    }

    /* (non-Javadoc)
     * @see at.dms.kjc.ExpressionVisitor#visitModulo(at.dms.kjc.JModuloExpression, java.lang.Object)
     */
    public Object visitModulo(JModuloExpression self, Object otherData) {
	if (redispatchBinary) return visitBinary(self,otherData);
	if (redispatchBinaryArithmetic) return visitBinaryArithmetic(self,otherData);
        assert false : "Unexpected Expression";
        return null;
    }

    /* (non-Javadoc)
     * @see at.dms.kjc.ExpressionVisitor#visitMult(at.dms.kjc.JMultExpression, java.lang.Object)
     */
    public Object visitMult(JMultExpression self, Object otherData) {
	if (redispatchBinary) return visitBinary(self,otherData);
	if (redispatchBinaryArithmetic) return visitBinaryArithmetic(self,otherData);
        assert false : "Unexpected Expression";
        return null;
    }

    /* (non-Javadoc)
     * @see at.dms.kjc.ExpressionVisitor#visitName(at.dms.kjc.JNameExpression, java.lang.Object)
     */
    public Object visitName(JNameExpression self, Object otherData) {
        assert false : "Unexpected Expression";
        return null;
    }

    /* (non-Javadoc)
     * @see at.dms.kjc.ExpressionVisitor#visitNewArray(at.dms.kjc.JNewArrayExpression, java.lang.Object)
     */
    public Object visitNewArray(JNewArrayExpression self, Object otherData) {
        assert false : "Unexpected Expression";
        return null;
    }

    /* (non-Javadoc)
     * @see at.dms.kjc.ExpressionVisitor#visitNullLiteral(at.dms.kjc.JNullLiteral, java.lang.Object)
     */
    public Object visitNullLiteral(JNullLiteral self, Object otherData) {
	if (redispatchLiteral) return visitLiteral(self,otherData);
        assert false : "Unexpected Expression";
        return null;
    }

    /* (non-Javadoc)
     * @see at.dms.kjc.ExpressionVisitor#visitParenthesed(at.dms.kjc.JParenthesedExpression, java.lang.Object)
     */
    public Object visitParenthesed(JParenthesedExpression self, Object otherData) {
        assert false : "Unexpected Expression";
        return null;
    }

    /* (non-Javadoc)
     * @see at.dms.kjc.ExpressionVisitor#visitPeek(at.dms.kjc.sir.SIRPeekExpression, java.lang.Object)
     */
    public Object visitPeek(SIRPeekExpression self, Object otherData) {
        assert false : "Unexpected Expression";
        return null;
    }

    /* (non-Javadoc)
     * @see at.dms.kjc.ExpressionVisitor#visitPop(at.dms.kjc.sir.SIRPopExpression, java.lang.Object)
     */
    public Object visitPop(SIRPopExpression self, Object otherData) {
        assert false : "Unexpected Expression";
        return null;
    }

    /* (non-Javadoc)
     * @see at.dms.kjc.ExpressionVisitor#visitPortal(at.dms.kjc.sir.SIRPortal, java.lang.Object)
     */
    public Object visitPortal(SIRPortal self, Object otherData) {
        assert false : "Unexpected Expression";
        return null;
    }

    /* (non-Javadoc)
     * @see at.dms.kjc.ExpressionVisitor#visitPostfix(at.dms.kjc.JPostfixExpression, java.lang.Object)
     */
    public Object visitPostfix(JPostfixExpression self, Object otherData) {
        assert false : "Unexpected Expression";
        return null;
    }

    /* (non-Javadoc)
     * @see at.dms.kjc.ExpressionVisitor#visitPrefix(at.dms.kjc.JPrefixExpression, java.lang.Object)
     */
    public Object visitPrefix(JPrefixExpression self, Object otherData) {
        assert false : "Unexpected Expression";
        return null;
    }

    /* (non-Javadoc)
     * @see at.dms.kjc.ExpressionVisitor#visitPush(at.dms.kjc.sir.SIRPushExpression, java.lang.Object)
     */
    public Object visitPush(SIRPushExpression self, Object otherData) {
        assert false : "Unexpected Expression";
        return null;
    }

    /* (non-Javadoc)
     * @see at.dms.kjc.ExpressionVisitor#visitQualifiedAnonymousCreation(at.dms.kjc.JQualifiedAnonymousCreation, java.lang.Object)
     */
    public Object visitQualifiedAnonymousCreation(
            JQualifiedAnonymousCreation self, Object otherData) {
        assert false : "Unexpected Expression";
        return null;
    }

    /* (non-Javadoc)
     * @see at.dms.kjc.ExpressionVisitor#visitQualifiedInstanceCreation(at.dms.kjc.JQualifiedInstanceCreation, java.lang.Object)
     */
    public Object visitQualifiedInstanceCreation(
            JQualifiedInstanceCreation self, Object otherData) {
        assert false : "Unexpected Expression";
        return null;
    }

    /* (non-Javadoc)
     * @see at.dms.kjc.ExpressionVisitor#visitRange(at.dms.kjc.sir.SIRRangeExpression, java.lang.Object)
     */
    public Object visitRange(SIRRangeExpression self, Object otherData) {
        assert false : "Unexpected Expression";
        return null;
    }

    /* (non-Javadoc)
     * @see at.dms.kjc.ExpressionVisitor#visitRelational(at.dms.kjc.JRelationalExpression, java.lang.Object)
     */
    public Object visitRelational(JRelationalExpression self, Object otherData) {
	if (redispatchBinary) return visitBinary(self,otherData);
        assert false : "Unexpected Expression";
        return null;
    }

    /* (non-Javadoc)
     * @see at.dms.kjc.ExpressionVisitor#visitShift(at.dms.kjc.JShiftExpression, java.lang.Object)
     */
    public Object visitShift(JShiftExpression self, Object otherData) {
	if (redispatchBinary) return visitBinary(self,otherData);
	if (redispatchBinaryArithmetic) return visitBinaryArithmetic(self,otherData);
        assert false : "Unexpected Expression";
        return null;
    }

    /* (non-Javadoc)
     * @see at.dms.kjc.ExpressionVisitor#visitShortLiteral(at.dms.kjc.JShortLiteral, java.lang.Object)
     */
    public Object visitShortLiteral(JShortLiteral self, Object otherData) {
	if (redispatchLiteral) return visitLiteral(self,otherData);
        assert false : "Unexpected Expression";
        return null;
    }

    /* (non-Javadoc)
     * @see at.dms.kjc.ExpressionVisitor#visitStringLiteral(at.dms.kjc.JStringLiteral, java.lang.Object)
     */
    public Object visitStringLiteral(JStringLiteral self, Object otherData) {
	if (redispatchLiteral) return visitLiteral(self,otherData);
        assert false : "Unexpected Expression";
        return null;
    }

    /* (non-Javadoc)
     * @see at.dms.kjc.ExpressionVisitor#visitSuper(at.dms.kjc.JSuperExpression, java.lang.Object)
     */
    public Object visitSuper(JSuperExpression self, Object otherData) {
        assert false : "Unexpected Expression";
        return null;
    }

    /* (non-Javadoc)
     * @see at.dms.kjc.ExpressionVisitor#visitThis(at.dms.kjc.JThisExpression, java.lang.Object)
     */
    public Object visitThis(JThisExpression self, Object otherData) {
        assert false : "Unexpected Expression";
        return null;
    }

    /* (non-Javadoc)
     * @see at.dms.kjc.ExpressionVisitor#visitTypeName(at.dms.kjc.JTypeNameExpression, java.lang.Object)
     */
    public Object visitTypeName(JTypeNameExpression self, Object otherData) {
        assert false : "Unexpected Expression";
        return null;
    }

    /* (non-Javadoc)
     * @see at.dms.kjc.ExpressionVisitor#visitUnary(at.dms.kjc.JUnaryExpression, java.lang.Object)
     */
    public Object visitUnary(JUnaryExpression self, Object otherData) {
        assert false : "Unexpected Expression";
        return null;
    }

    /* (non-Javadoc)
     * @see at.dms.kjc.ExpressionVisitor#visitUnaryMinus(at.dms.kjc.JUnaryMinusExpression, java.lang.Object)
     */
    public Object visitUnaryMinus(JUnaryMinusExpression self, Object otherData) {
	if (redispatchUnary) return visitUnary(self,otherData);
        assert false : "Unexpected Expression";
        return null;
    }

    /* (non-Javadoc)
     * @see at.dms.kjc.ExpressionVisitor#visitUnaryPlus(at.dms.kjc.JUnaryPlusExpression, java.lang.Object)
     */
    public Object visitUnaryPlus(JUnaryPlusExpression self, Object otherData) {
	if (redispatchUnary) return visitUnary(self,otherData);
        assert false : "Unexpected Expression";
        return null;
    }

    /* (non-Javadoc)
     * @see at.dms.kjc.ExpressionVisitor#visitUnaryPromote(at.dms.kjc.JUnaryPromote, java.lang.Object)
     */
    public Object visitUnaryPromote(JUnaryPromote self, Object otherData) {
        assert false : "Unexpected Expression";
        return null;
    }

    /* (non-Javadoc)
     * @see at.dms.kjc.ExpressionVisitor#visitUnqualifiedAnonymousCreation(at.dms.kjc.JUnqualifiedAnonymousCreation, java.lang.Object)
     */
    public Object visitUnqualifiedAnonymousCreation(
            JUnqualifiedAnonymousCreation self, Object otherData) {
        assert false : "Unexpected Expression";
        return null;
    }

    /* (non-Javadoc)
     * @see at.dms.kjc.ExpressionVisitor#visitUnqualifiedInstanceCreation(at.dms.kjc.JUnqualifiedInstanceCreation, java.lang.Object)
     */
    public Object visitUnqualifiedInstanceCreation(
            JUnqualifiedInstanceCreation self, Object otherData) {
        assert false : "Unexpected Expression";
        return null;
    }
}
