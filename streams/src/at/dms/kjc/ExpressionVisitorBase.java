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
 * <br/>
 * $Id$
 * @param S : return type of visitor.
 * @param T : type of second argument to visitor.
 * @author Allyn Dimock
 *
 */
public class ExpressionVisitorBase<S,T> implements ExpressionVisitor<S,T> {

    protected boolean redispatchBinary = false;
    protected boolean redispatchBinaryArithmetic = false;
    protected boolean redispatchLiteral = false;
    protected boolean redispatchUnary = false;

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
    public S visitAdd(JAddExpression self, T otherData) {
	if (redispatchBinary) return visitBinary(self,otherData);
	if (redispatchBinaryArithmetic) return visitBinaryArithmetic(self,otherData);
        assert false : "Unexpected Expression";
        return null;
    }

    /* (non-Javadoc)
     * @see at.dms.kjc.ExpressionVisitor#visitArrayAccess(at.dms.kjc.JArrayAccessExpression, java.lang.Object)
     */
    public S visitArrayAccess(JArrayAccessExpression self, T otherData) {
        assert false : "Unexpected Expression";
        return null;
    }

    /* (non-Javadoc)
     * @see at.dms.kjc.ExpressionVisitor#visitArrayInitializer(at.dms.kjc.JArrayInitializer, java.lang.Object)
     */
    public S visitArrayInitializer(JArrayInitializer self, T otherData) {
        assert false : "Unexpected Expression";
        return null;
    }

    /* (non-Javadoc)
     * @see at.dms.kjc.ExpressionVisitor#visitArrayLength(at.dms.kjc.JArrayLengthExpression, java.lang.Object)
     */
    public S visitArrayLength(JArrayLengthExpression self, T otherData) {
        assert false : "Unexpected Expression";
        return null;
    }

    /* (non-Javadoc)
     * @see at.dms.kjc.ExpressionVisitor#visitAssignment(at.dms.kjc.JAssignmentExpression, java.lang.Object)
     */
    public S visitAssignment(JAssignmentExpression self, T otherData) {
	if (redispatchBinary) return visitBinary(self,otherData);
        assert false : "Unexpected Expression";
        return null;
    }

    /* (non-Javadoc)
     * @see at.dms.kjc.ExpressionVisitor#visitBinary(at.dms.kjc.JBinaryExpression, java.lang.Object)
     */
    public S visitBinary(JBinaryExpression self, T otherData) {
        assert false : "Unexpected Expression";
        return null;
    }

    /* (non-Javadoc)
     * @see at.dms.kjc.ExpressionVisitor#visitBinaryArithmetic(at.dms.kjc.JBinaryArithmeticExpression, java.lang.Object)
     */
    public S visitBinaryArithmetic(JBinaryArithmeticExpression self,
            T otherData) {
	if (redispatchBinary) return visitBinary(self,otherData);
        assert false : "Unexpected Expression";
        return null;
    }

    /* (non-Javadoc)
     * @see at.dms.kjc.ExpressionVisitor#visitBitwise(at.dms.kjc.JBitwiseExpression, java.lang.Object)
     */
    public S visitBitwise(JBitwiseExpression self, T otherData) {
	if (redispatchBinary) return visitBinary(self,otherData);
	if (redispatchBinaryArithmetic) return visitBinaryArithmetic(self,otherData);
        assert false : "Unexpected Expression";
        return null;
    }

    /* (non-Javadoc)
     * @see at.dms.kjc.ExpressionVisitor#visitBitwiseComplement(at.dms.kjc.JBitwiseComplementExpression, java.lang.Object)
     */
    public S visitBitwiseComplement(JBitwiseComplementExpression self,
            T otherData) {
	if (redispatchUnary) return visitUnary(self,otherData);
        assert false : "Unexpected Expression";
        return null;
    }

    /* (non-Javadoc)
     * @see at.dms.kjc.ExpressionVisitor#visitBooleanLiteral(at.dms.kjc.JBooleanLiteral, java.lang.Object)
     */
    public S visitBooleanLiteral(JBooleanLiteral self, T otherData) {
	if (redispatchLiteral) return visitLiteral(self,otherData);
        assert false : "Unexpected Expression";
        return null;
    }

    /* (non-Javadoc)
     * @see at.dms.kjc.ExpressionVisitor#visitByteLiteral(at.dms.kjc.JByteLiteral, java.lang.Object)
     */
    public S visitByteLiteral(JByteLiteral self, T otherData) {
	if (redispatchLiteral) return visitLiteral(self,otherData);
        assert false : "Unexpected Expression";
        return null;
    }

    /* (non-Javadoc)
     * @see at.dms.kjc.ExpressionVisitor#visitCast(at.dms.kjc.JCastExpression, java.lang.Object)
     */
    public S visitCast(JCastExpression self, T otherData) {
        assert false : "Unexpected Expression";
        return null;
    }

    /* (non-Javadoc)
     * @see at.dms.kjc.ExpressionVisitor#visitCharLiteral(at.dms.kjc.JCharLiteral, java.lang.Object)
     */
    public S visitCharLiteral(JCharLiteral self, T otherData) {
	if (redispatchLiteral) return visitLiteral(self,otherData);
        assert false : "Unexpected Expression";
        return null;
    }

    /* (non-Javadoc)
     * @see at.dms.kjc.ExpressionVisitor#visitChecked(at.dms.kjc.JCheckedExpression, java.lang.Object)
     */
    public S visitChecked(JCheckedExpression self, T otherData) {
        assert false : "Unexpected Expression";
        return null;
    }

    /* (non-Javadoc)
     * @see at.dms.kjc.ExpressionVisitor#visitClass(at.dms.kjc.JClassExpression, java.lang.Object)
     */
    public S visitClass(JClassExpression self, T otherData) {
        assert false : "Unexpected Expression";
        return null;
    }

    /* (non-Javadoc)
     * @see at.dms.kjc.ExpressionVisitor#visitCompoundAssignment(at.dms.kjc.JCompoundAssignmentExpression, java.lang.Object)
     */
    public S visitCompoundAssignment(JCompoundAssignmentExpression self,
            T otherData) {
	if (redispatchBinary) return visitBinary(self,otherData);
        assert false : "Unexpected Expression";
        return null;
    }

    /* (non-Javadoc)
     * @see at.dms.kjc.ExpressionVisitor#visitConditional(at.dms.kjc.JConditionalExpression, java.lang.Object)
     */
    public S visitConditional(JConditionalExpression self, T otherData) {
        assert false : "Unexpected Expression";
        return null;
    }

    /* (non-Javadoc)
     * @see at.dms.kjc.ExpressionVisitor#visitConditionalAnd(at.dms.kjc.JConditionalAndExpression, java.lang.Object)
     */
    public S visitConditionalAnd(JConditionalAndExpression self,
            T otherData) {
	if (redispatchBinary) return visitBinary(self,otherData);
        assert false : "Unexpected Expression";
        return null;
    }

    /* (non-Javadoc)
     * @see at.dms.kjc.ExpressionVisitor#visitConditionalOr(at.dms.kjc.JConditionalOrExpression, java.lang.Object)
     */
    public S visitConditionalOr(JConditionalOrExpression self,
            T otherData) {
	if (redispatchBinary) return visitBinary(self,otherData);
        assert false : "Unexpected Expression";
        return null;
    }

    /* (non-Javadoc)
     * @see at.dms.kjc.ExpressionVisitor#visitConstructorCall(at.dms.kjc.JConstructorCall, java.lang.Object)
     */
    public S visitConstructorCall(JConstructorCall self, T otherData) {
        assert false : "Unexpected Expression";
        return null;
    }

    /* (non-Javadoc)
     * @see at.dms.kjc.ExpressionVisitor#visitCreatePortal(at.dms.kjc.sir.SIRCreatePortal, java.lang.Object)
     */
    public S visitCreatePortal(SIRCreatePortal self, T otherData) {
        assert false : "Unexpected Expression";
        return null;
    }

    /* (non-Javadoc)
     * @see at.dms.kjc.ExpressionVisitor#visitDivide(at.dms.kjc.JDivideExpression, java.lang.Object)
     */
    public S visitDivide(JDivideExpression self, T otherData) {
	if (redispatchBinary) return visitBinary(self,otherData);
	if (redispatchBinaryArithmetic) return visitBinaryArithmetic(self,otherData);
        assert false : "Unexpected Expression";
        return null;
    }

    /* (non-Javadoc)
     * @see at.dms.kjc.ExpressionVisitor#visitDoubleLiteral(at.dms.kjc.JDoubleLiteral, java.lang.Object)
     */
    public S visitDoubleLiteral(JDoubleLiteral self, T otherData) {
	if (redispatchLiteral) return visitLiteral(self,otherData);
        assert false : "Unexpected Expression";
        return null;
    }

    /* (non-Javadoc)
     * @see at.dms.kjc.ExpressionVisitor#visitDynamicToken(at.dms.kjc.sir.SIRDynamicToken, java.lang.Object)
     */
    public S visitDynamicToken(SIRDynamicToken self, T otherData) {
        assert false : "Unexpected Expression";
        return null;
    }

    /* (non-Javadoc)
     * @see at.dms.kjc.ExpressionVisitor#visitEquality(at.dms.kjc.JEqualityExpression, java.lang.Object)
     */
    public S visitEquality(JEqualityExpression self, T otherData) {
	if (redispatchBinary) return visitBinary(self,otherData);
        assert false : "Unexpected Expression";
        return null;
    }

    /* (non-Javadoc)
     * @see at.dms.kjc.ExpressionVisitor#visitFieldAccess(at.dms.kjc.JFieldAccessExpression, java.lang.Object)
     */
    public S visitFieldAccess(JFieldAccessExpression self, T otherData) {
        assert false : "Unexpected Expression";
        return null;
    }

    /* (non-Javadoc)
     * @see at.dms.kjc.ExpressionVisitor#visitFloatLiteral(at.dms.kjc.JFloatLiteral, java.lang.Object)
     */
    public S visitFloatLiteral(JFloatLiteral self, T otherData) {
	if (redispatchLiteral) return visitLiteral(self,otherData);
        assert false : "Unexpected Expression";
        return null;
    }

    /* (non-Javadoc)
     * @see at.dms.kjc.ExpressionVisitor#visitInstanceof(at.dms.kjc.JInstanceofExpression, java.lang.Object)
     */
    public S visitInstanceof(JInstanceofExpression self, T otherData) {
        assert false : "Unexpected Expression";
        return null;
    }

    /* (non-Javadoc)
     * @see at.dms.kjc.ExpressionVisitor#visitIntLiteral(at.dms.kjc.JIntLiteral, java.lang.Object)
     */
    public S visitIntLiteral(JIntLiteral self, T otherData) {
	if (redispatchLiteral) return visitLiteral(self,otherData);
        assert false : "Unexpected Expression";
        return null;
    }

    /* (non-Javadoc)
     * @see at.dms.kjc.ExpressionVisitor#visitInterfaceTable(at.dms.kjc.sir.SIRInterfaceTable, java.lang.Object)
     */
    public S visitInterfaceTable(SIRInterfaceTable self, T otherData) {
        assert false : "Unexpected Expression";
        return null;
    }

    /* (non-Javadoc)
     * @see at.dms.kjc.ExpressionVisitor#visitLiteral(at.dms.kjc.JLiteral, java.lang.Object)
     */
    public S visitLiteral(JLiteral self, T otherData) {
        assert false : "Unexpected Expression";
        return null;
    }

    /* (non-Javadoc)
     * @see at.dms.kjc.ExpressionVisitor#visitLocalVariable(at.dms.kjc.JLocalVariableExpression, java.lang.Object)
     */
    public S visitLocalVariable(JLocalVariableExpression self,
            T otherData) {
        assert false : "Unexpected Expression";
        return null;
    }

    /* (non-Javadoc)
     * @see at.dms.kjc.ExpressionVisitor#visitLogicalComplement(at.dms.kjc.JLogicalComplementExpression, java.lang.Object)
     */
    public S visitLogicalComplement(JLogicalComplementExpression self,
            T otherData) {
	if (redispatchUnary) return visitUnary(self,otherData);
        assert false : "Unexpected Expression";
        return null;
    }

    /* (non-Javadoc)
     * @see at.dms.kjc.ExpressionVisitor#visitLongLiteral(at.dms.kjc.JLongLiteral, java.lang.Object)
     */
    public S visitLongLiteral(JLongLiteral self, T otherData) {
	if (redispatchLiteral) return visitLiteral(self,otherData);
        assert false : "Unexpected Expression";
        return null;
    }

    /* (non-Javadoc)
     * @see at.dms.kjc.ExpressionVisitor#visitMethodCall(at.dms.kjc.JMethodCallExpression, java.lang.Object)
     */
    public S visitMethodCall(JMethodCallExpression self, T otherData) {
        assert false : "Unexpected Expression";
        return null;
    }

    /* (non-Javadoc)
     * @see at.dms.kjc.ExpressionVisitor#visitMinus(at.dms.kjc.JMinusExpression, java.lang.Object)
     */
    public S visitMinus(JMinusExpression self, T otherData) {
	if (redispatchBinary) return visitBinary(self,otherData);
	if (redispatchBinaryArithmetic) return visitBinaryArithmetic(self,otherData);
        assert false : "Unexpected Expression";
        return null;
    }

    /* (non-Javadoc)
     * @see at.dms.kjc.ExpressionVisitor#visitModulo(at.dms.kjc.JModuloExpression, java.lang.Object)
     */
    public S visitModulo(JModuloExpression self, T otherData) {
	if (redispatchBinary) return visitBinary(self,otherData);
	if (redispatchBinaryArithmetic) return visitBinaryArithmetic(self,otherData);
        assert false : "Unexpected Expression";
        return null;
    }

    /* (non-Javadoc)
     * @see at.dms.kjc.ExpressionVisitor#visitMult(at.dms.kjc.JMultExpression, java.lang.Object)
     */
    public S visitMult(JMultExpression self, T otherData) {
	if (redispatchBinary) return visitBinary(self,otherData);
	if (redispatchBinaryArithmetic) return visitBinaryArithmetic(self,otherData);
        assert false : "Unexpected Expression";
        return null;
    }

    /* (non-Javadoc)
     * @see at.dms.kjc.ExpressionVisitor#visitName(at.dms.kjc.JNameExpression, java.lang.Object)
     */
    public S visitName(JNameExpression self, T otherData) {
        assert false : "Unexpected Expression";
        return null;
    }

    /* (non-Javadoc)
     * @see at.dms.kjc.ExpressionVisitor#visitNewArray(at.dms.kjc.JNewArrayExpression, java.lang.Object)
     */
    public S visitNewArray(JNewArrayExpression self, T otherData) {
        assert false : "Unexpected Expression";
        return null;
    }

    /* (non-Javadoc)
     * @see at.dms.kjc.ExpressionVisitor#visitNullLiteral(at.dms.kjc.JNullLiteral, java.lang.Object)
     */
    public S visitNullLiteral(JNullLiteral self, T otherData) {
	if (redispatchLiteral) return visitLiteral(self,otherData);
        assert false : "Unexpected Expression";
        return null;
    }

    /* (non-Javadoc)
     * @see at.dms.kjc.ExpressionVisitor#visitParenthesed(at.dms.kjc.JParenthesedExpression, java.lang.Object)
     */
    public S visitParenthesed(JParenthesedExpression self, T otherData) {
        assert false : "Unexpected Expression";
        return null;
    }

    /* (non-Javadoc)
     * @see at.dms.kjc.ExpressionVisitor#visitPeek(at.dms.kjc.sir.SIRPeekExpression, java.lang.Object)
     */
    public S visitPeek(SIRPeekExpression self, T otherData) {
        assert false : "Unexpected Expression";
        return null;
    }

    /* (non-Javadoc)
     * @see at.dms.kjc.ExpressionVisitor#visitPop(at.dms.kjc.sir.SIRPopExpression, java.lang.Object)
     */
    public S visitPop(SIRPopExpression self, T otherData) {
        assert false : "Unexpected Expression";
        return null;
    }

    /* (non-Javadoc)
     * @see at.dms.kjc.ExpressionVisitor#visitPortal(at.dms.kjc.sir.SIRPortal, java.lang.Object)
     */
    public S visitPortal(SIRPortal self, T otherData) {
        assert false : "Unexpected Expression";
        return null;
    }

    /* (non-Javadoc)
     * @see at.dms.kjc.ExpressionVisitor#visitPostfix(at.dms.kjc.JPostfixExpression, java.lang.Object)
     */
    public S visitPostfix(JPostfixExpression self, T otherData) {
        assert false : "Unexpected Expression";
        return null;
    }

    /* (non-Javadoc)
     * @see at.dms.kjc.ExpressionVisitor#visitPrefix(at.dms.kjc.JPrefixExpression, java.lang.Object)
     */
    public S visitPrefix(JPrefixExpression self, T otherData) {
        assert false : "Unexpected Expression";
        return null;
    }

    /* (non-Javadoc)
     * @see at.dms.kjc.ExpressionVisitor#visitPush(at.dms.kjc.sir.SIRPushExpression, java.lang.Object)
     */
    public S visitPush(SIRPushExpression self, T otherData) {
        assert false : "Unexpected Expression";
        return null;
    }

    /* (non-Javadoc)
     * @see at.dms.kjc.ExpressionVisitor#visitQualifiedAnonymousCreation(at.dms.kjc.JQualifiedAnonymousCreation, java.lang.Object)
     */
    public S visitQualifiedAnonymousCreation(
            JQualifiedAnonymousCreation self, T otherData) {
        assert false : "Unexpected Expression";
        return null;
    }

    /* (non-Javadoc)
     * @see at.dms.kjc.ExpressionVisitor#visitQualifiedInstanceCreation(at.dms.kjc.JQualifiedInstanceCreation, java.lang.Object)
     */
    public S visitQualifiedInstanceCreation(
            JQualifiedInstanceCreation self, T otherData) {
        assert false : "Unexpected Expression";
        return null;
    }

    /* (non-Javadoc)
     * @see at.dms.kjc.ExpressionVisitor#visitRange(at.dms.kjc.sir.SIRRangeExpression, java.lang.Object)
     */
    public S visitRange(SIRRangeExpression self, T otherData) {
        assert false : "Unexpected Expression";
        return null;
    }

    /* (non-Javadoc)
     * @see at.dms.kjc.ExpressionVisitor#visitRelational(at.dms.kjc.JRelationalExpression, java.lang.Object)
     */
    public S visitRelational(JRelationalExpression self, T otherData) {
	if (redispatchBinary) return visitBinary(self,otherData);
        assert false : "Unexpected Expression";
        return null;
    }

    /* (non-Javadoc)
     * @see at.dms.kjc.ExpressionVisitor#visitShift(at.dms.kjc.JShiftExpression, java.lang.Object)
     */
    public S visitShift(JShiftExpression self, T otherData) {
	if (redispatchBinary) return visitBinary(self,otherData);
	if (redispatchBinaryArithmetic) return visitBinaryArithmetic(self,otherData);
        assert false : "Unexpected Expression";
        return null;
    }

    /* (non-Javadoc)
     * @see at.dms.kjc.ExpressionVisitor#visitShortLiteral(at.dms.kjc.JShortLiteral, java.lang.Object)
     */
    public S visitShortLiteral(JShortLiteral self, T otherData) {
	if (redispatchLiteral) return visitLiteral(self,otherData);
        assert false : "Unexpected Expression";
        return null;
    }

    /* (non-Javadoc)
     * @see at.dms.kjc.ExpressionVisitor#visitStringLiteral(at.dms.kjc.JStringLiteral, java.lang.Object)
     */
    public S visitStringLiteral(JStringLiteral self, T otherData) {
	if (redispatchLiteral) return visitLiteral(self,otherData);
        assert false : "Unexpected Expression";
        return null;
    }

    /* (non-Javadoc)
     * @see at.dms.kjc.ExpressionVisitor#visitSuper(at.dms.kjc.JSuperExpression, java.lang.Object)
     */
    public S visitSuper(JSuperExpression self, T otherData) {
        assert false : "Unexpected Expression";
        return null;
    }

    /* (non-Javadoc)
     * @see at.dms.kjc.ExpressionVisitor#visitThis(at.dms.kjc.JThisExpression, java.lang.Object)
     */
    public S visitThis(JThisExpression self, T otherData) {
        assert false : "Unexpected Expression";
        return null;
    }

    /* (non-Javadoc)
     * @see at.dms.kjc.ExpressionVisitor#visitTypeName(at.dms.kjc.JTypeNameExpression, java.lang.Object)
     */
    public S visitTypeName(JTypeNameExpression self, T otherData) {
        assert false : "Unexpected Expression";
        return null;
    }

    /* (non-Javadoc)
     * @see at.dms.kjc.ExpressionVisitor#visitUnary(at.dms.kjc.JUnaryExpression, java.lang.Object)
     */
    public S visitUnary(JUnaryExpression self, T otherData) {
        assert false : "Unexpected Expression";
        return null;
    }

    /* (non-Javadoc)
     * @see at.dms.kjc.ExpressionVisitor#visitUnaryMinus(at.dms.kjc.JUnaryMinusExpression, java.lang.Object)
     */
    public S visitUnaryMinus(JUnaryMinusExpression self, T otherData) {
	if (redispatchUnary) return visitUnary(self,otherData);
        assert false : "Unexpected Expression";
        return null;
    }

    /* (non-Javadoc)
     * @see at.dms.kjc.ExpressionVisitor#visitUnaryPlus(at.dms.kjc.JUnaryPlusExpression, java.lang.Object)
     */
    public S visitUnaryPlus(JUnaryPlusExpression self, T otherData) {
	if (redispatchUnary) return visitUnary(self,otherData);
        assert false : "Unexpected Expression";
        return null;
    }

    /* (non-Javadoc)
     * @see at.dms.kjc.ExpressionVisitor#visitUnaryPromote(at.dms.kjc.JUnaryPromote, java.lang.Object)
     */
    public S visitUnaryPromote(JUnaryPromote self, T otherData) {
        assert false : "Unexpected Expression";
        return null;
    }

    /* (non-Javadoc)
     * @see at.dms.kjc.ExpressionVisitor#visitUnqualifiedAnonymousCreation(at.dms.kjc.JUnqualifiedAnonymousCreation, java.lang.Object)
     */
    public S visitUnqualifiedAnonymousCreation(
            JUnqualifiedAnonymousCreation self, T otherData) {
        assert false : "Unexpected Expression";
        return null;
    }

    /* (non-Javadoc)
     * @see at.dms.kjc.ExpressionVisitor#visitUnqualifiedInstanceCreation(at.dms.kjc.JUnqualifiedInstanceCreation, java.lang.Object)
     */
    public S visitUnqualifiedInstanceCreation(
            JUnqualifiedInstanceCreation self, T otherData) {
        assert false : "Unexpected Expression";
        return null;
    }
}
