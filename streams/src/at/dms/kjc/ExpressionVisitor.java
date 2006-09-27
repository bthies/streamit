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
 * Visitor only defined on subtypes of JExpression,
 * Takes arbitrary data, returns arbitrary data.
 * @author dimock
 *
 */
public interface ExpressionVisitor {

    Object visitArrayAccess(JArrayAccessExpression self, Object otherData);
    Object visitArrayInitializer(JArrayInitializer self, Object otherData);
    Object visitArrayLength(JArrayLengthExpression self, Object otherData);
    Object visitBinary(JBinaryExpression self, Object otherData); // could redirect all binary expressions here
    // Subtypes of Binary
    Object visitAssignment(JAssignmentExpression self, Object otherData);
    Object visitCompoundAssignment(JCompoundAssignmentExpression self, Object otherData);
    Object visitBinaryArithmetic(JBinaryArithmeticExpression self, Object otherData); // could redirect all binary arithmetic here
    //  Subtypes of BinaryArithmetic
    Object visitAdd(JAddExpression self, Object otherData);
    Object visitBitwise(JBitwiseExpression self, Object otherData);
    Object visitDivide(JDivideExpression self, Object otherData);
    Object visitMinus(JMinusExpression self, Object otherData);
    Object visitModulo(JModuloExpression self, Object otherData);
    Object visitMult(JMultExpression self, Object otherData);
    Object visitShift(JShiftExpression self, Object otherData);
    //  end subtypes of BinaryArithmetic
    Object visitConditionalAnd(JConditionalAndExpression self, Object otherData);
    Object visitConditionalOr(JConditionalOrExpression self, Object otherData);
    Object visitEquality(JEqualityExpression self, Object otherData);
    Object visitRelational(JRelationalExpression self, Object otherData);
    // end subtypes of Binary
    Object visitCast(JCastExpression self, Object otherData);
    Object visitChecked(JCheckedExpression self, Object otherData);
    Object visitClass(JClassExpression self, Object otherData);
    Object visitConditional(JConditionalExpression self, Object otherData);
    Object visitConstructorCall(JConstructorCall self, Object otherData);
    Object visitFieldAccess(JFieldAccessExpression self, Object otherData);
    Object visitInstanceof(JInstanceofExpression self, Object otherData);
    Object visitLiteral(JLiteral self, Object otherData); // could redirect individual literals here.
    // Subtypes of Literal
    Object visitBooleanLiteral(JBooleanLiteral self, Object otherData);
    Object visitByteLiteral(JByteLiteral self, Object otherData);
    Object visitCharLiteral(JCharLiteral self, Object otherData);
    Object visitDoubleLiteral(JDoubleLiteral self, Object otherData);
    Object visitFloatLiteral(JFloatLiteral self, Object otherData);
    Object visitIntLiteral(JIntLiteral self, Object otherData);
    Object visitLongLiteral(JLongLiteral self, Object otherData);
    Object visitNullLiteral(JNullLiteral self, Object otherData);
    Object visitShortLiteral(JShortLiteral self, Object otherData);
    Object visitStringLiteral(JStringLiteral self, Object otherData);
    Object visitPortal(SIRPortal self, Object otherData); // yet another literal
    // end subtypes of Literal
    Object visitLocalVariable(JLocalVariableExpression self, Object otherData);
    Object visitMethodCall(JMethodCallExpression self, Object otherData);
    Object visitName(JNameExpression self, Object otherData);
    Object visitNewArray(JNewArrayExpression self, Object otherData);
    Object visitParenthesed(JParenthesedExpression self, Object otherData);
    Object visitPostfix(JPostfixExpression self, Object otherData);
    Object visitPrefix(JPrefixExpression self, Object otherData);
    Object visitQualifiedAnonymousCreation(JQualifiedAnonymousCreation self, Object otherData);
    Object visitQualifiedInstanceCreation(JQualifiedInstanceCreation self, Object otherData);
    Object visitSuper(JSuperExpression self, Object otherData);
    Object visitThis(JThisExpression self, Object otherData);
    Object visitTypeName(JTypeNameExpression self, Object otherData);
    Object visitUnary(JUnaryExpression self, Object otherData);  // could redirect all unary expressions here
    // Subtypes of Unary
    Object visitBitwiseComplement(JBitwiseComplementExpression self, Object otherData);
    Object visitLogicalComplement(JLogicalComplementExpression self, Object otherData);
    Object visitUnaryMinus(JUnaryMinusExpression self, Object otherData);
    Object visitUnaryPlus(JUnaryPlusExpression self, Object otherData);
    // end subtypes of Unary
    Object visitUnaryPromote(JUnaryPromote self, Object otherData);
    Object visitUnqualifiedAnonymousCreation(JUnqualifiedAnonymousCreation self, Object otherData);
    Object visitUnqualifiedInstanceCreation(JUnqualifiedInstanceCreation self, Object otherData);
    Object visitCreatePortal(SIRCreatePortal self, Object otherData);
    Object visitDynamicToken(SIRDynamicToken self, Object otherData);
    Object visitInterfaceTable(SIRInterfaceTable self, Object otherData);
    Object visitPeek(SIRPeekExpression self, Object otherData);
    Object visitPop(SIRPopExpression self, Object otherData);
    Object visitPush(SIRPushExpression self, Object otherData);
    Object visitRange(SIRRangeExpression self, Object otherData);
}
