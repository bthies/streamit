package at.dms.kjc;

import at.dms.kjc.sir.SIRCreatePortal;
import at.dms.kjc.sir.SIRDynamicToken;
import at.dms.kjc.sir.SIRInterfaceTable;
import at.dms.kjc.sir.SIRPeekExpression;
import at.dms.kjc.sir.SIRPopExpression;
import at.dms.kjc.sir.SIRPortal;
import at.dms.kjc.sir.SIRPushExpression;
import at.dms.kjc.sir.SIRRangeExpression;
import at.dms.kjc.sir.lowering.JVectorLiteral;

/**
 * Visitor only defined on subtypes of JExpression,
 * Takes arbitrary data, returns arbitrary data.
 * <br/>$Id$
 * @author Allyn Dimock
 *
 */
public interface ExpressionVisitor<S,T> {

    S visitArrayAccess(JArrayAccessExpression self, T otherData);
    S visitArrayInitializer(JArrayInitializer self, T otherData);
    S visitArrayLength(JArrayLengthExpression self, T otherData);
    S visitBinary(JBinaryExpression self, T otherData); // could redirect all binary expressions here
    // Subtypes of Binary
    S visitAssignment(JAssignmentExpression self, T otherData);
    S visitCompoundAssignment(JCompoundAssignmentExpression self, T otherData);
    S visitBinaryArithmetic(JBinaryArithmeticExpression self, T otherData); // could redirect all binary arithmetic here
    //  Subtypes of BinaryArithmetic
    S visitAdd(JAddExpression self, T otherData);
    S visitBitwise(JBitwiseExpression self, T otherData);
    S visitDivide(JDivideExpression self, T otherData);
    S visitMinus(JMinusExpression self, T otherData);
    S visitModulo(JModuloExpression self, T otherData);
    S visitMult(JMultExpression self, T otherData);
    S visitShift(JShiftExpression self, T otherData);
    //  end subtypes of BinaryArithmetic
    S visitConditionalAnd(JConditionalAndExpression self, T otherData);
    S visitConditionalOr(JConditionalOrExpression self, T otherData);
    S visitEquality(JEqualityExpression self, T otherData);
    S visitRelational(JRelationalExpression self, T otherData);
    // end subtypes of Binary
    S visitCast(JCastExpression self, T otherData);
    S visitChecked(JCheckedExpression self, T otherData);
    S visitClass(JClassExpression self, T otherData);
    S visitConditional(JConditionalExpression self, T otherData);
    S visitConstructorCall(JConstructorCall self, T otherData);
    S visitEmittedText(JEmittedTextExpression self, T otherData);
    S visitFieldAccess(JFieldAccessExpression self, T otherData);
    S visitInstanceof(JInstanceofExpression self, T otherData);
    S visitLiteral(JLiteral self, T otherData); // could redirect individual literals here.
    // Subtypes of Literal
    S visitBooleanLiteral(JBooleanLiteral self, T otherData);
    S visitByteLiteral(JByteLiteral self, T otherData);
    S visitCharLiteral(JCharLiteral self, T otherData);
    S visitDoubleLiteral(JDoubleLiteral self, T otherData);
    S visitFloatLiteral(JFloatLiteral self, T otherData);
    S visitIntLiteral(JIntLiteral self, T otherData);
    S visitLongLiteral(JLongLiteral self, T otherData);
    S visitNullLiteral(JNullLiteral self, T otherData);
    S visitShortLiteral(JShortLiteral self, T otherData);
    S visitStringLiteral(JStringLiteral self, T otherData);
    S visitPortal(SIRPortal self, T otherData); // yet another literal
    // end subtypes of Literal
    S visitLocalVariable(JLocalVariableExpression self, T otherData);
    S visitMethodCall(JMethodCallExpression self, T otherData);
    S visitName(JNameExpression self, T otherData);
    S visitNewArray(JNewArrayExpression self, T otherData);
    S visitParenthesed(JParenthesedExpression self, T otherData);
    S visitPostfix(JPostfixExpression self, T otherData);
    S visitPrefix(JPrefixExpression self, T otherData);
    S visitQualifiedAnonymousCreation(JQualifiedAnonymousCreation self, T otherData);
    S visitQualifiedInstanceCreation(JQualifiedInstanceCreation self, T otherData);
    S visitSuper(JSuperExpression self, T otherData);
    S visitThis(JThisExpression self, T otherData);
    S visitTypeName(JTypeNameExpression self, T otherData);
    S visitUnary(JUnaryExpression self, T otherData);  // could redirect all unary expressions here
    // Subtypes of Unary
    S visitBitwiseComplement(JBitwiseComplementExpression self, T otherData);
    S visitLogicalComplement(JLogicalComplementExpression self, T otherData);
    S visitUnaryMinus(JUnaryMinusExpression self, T otherData);
    S visitUnaryPlus(JUnaryPlusExpression self, T otherData);
    // end subtypes of Unary
    S visitUnaryPromote(JUnaryPromote self, T otherData);
    S visitUnqualifiedAnonymousCreation(JUnqualifiedAnonymousCreation self, T otherData);
    S visitUnqualifiedInstanceCreation(JUnqualifiedInstanceCreation self, T otherData);
    S visitCreatePortal(SIRCreatePortal self, T otherData);
    S visitDynamicToken(SIRDynamicToken self, T otherData);
    S visitInterfaceTable(SIRInterfaceTable self, T otherData);
    S visitPeek(SIRPeekExpression self, T otherData);
    S visitPop(SIRPopExpression self, T otherData);
    S visitPush(SIRPushExpression self, T otherData);
    S visitRange(SIRRangeExpression self, T otherData);
    S visitVectorLiteral(JVectorLiteral self, T otherData);
}
