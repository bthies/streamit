##
## Copyright (C) 1990-2001 DMS Decision Management Systems Ges.m.b.H.
##
## This program is free software; you can redistribute it and/or modify
## it under the terms of the GNU General Public License as published by
## the Free Software Foundation; either version 2 of the License, or
## (at your option) any later version.
##
## This program is distributed in the hope that it will be useful,
## but WITHOUT ANY WARRANTY; without even the implied warranty of
## MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
## GNU General Public License for more details.
##
## You should have received a copy of the GNU General Public License
## along with this program; if not, write to the Free Software
## Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
##
## $Id: Makefile,v 1.4 2001-10-02 19:15:08 thies Exp $
##

TOPDIR =	..

PACKAGE =	at/dms/kjc

SUBDIRS =	sir lir

## ----------------------------------------------------------------------
## TOOLS

## ----------------------------------------------------------------------
## FILES

GRM1VOC =	Kjc
GRM1DEP =
SCANNER1 =	Kjc

# Compiler classes
JAVAFILES +=	BytecodeOptimizer					\
		CArrayType CBadClass					\
		CBinaryClass CBinaryField CBinaryMethod CBlockContext	\
		CBlockError CBodyContext CBooleanType			\
		CByteType CCatchContext CCharType CClass		\
		CClassContext CClassNameType CClassType			\
		CCompilationUnit CCompilationUnitContext		\
		CConstructorContext CContext				\
		CDoubleType CExpressionContext				\
		CExpressionError CField					\
		CFloatType CInitializerContext				\
		CIntType CInterfaceContext CLabeledContext		\
		CLineError CLongType CLoopContext CMember		\
		CMethod CMethodContext					\
		CMethodNotFoundError CModifier CNullType		\
		CNumericType CParseClassContext				\
		CParseCompilationUnitContext CShortType			\
		CSimpleBodyContext CSourceClass CSourceField		\
		CSourceMethod CStdType					\
		CSwitchBodyContext CSwitchGroupContext			\
		CThrowableInfo CTopLevel CTryContext			\
		CTryFinallyContext CType CVariableInfo CVoidType	\
		CodeLabel CodeSequence Constants			\
		DefaultFilter JCheckedExpression KjcPrettyPrinter	\
		KjcScanner KjcVisitor KjcEmptyVisitor Main 		\
		MethodSignatureParser SLIRVisitor

# Syntax tree classes
JAVAFILES +=	JAddExpression JArrayAccessExpression			\
		JArrayInitializer JArrayLengthExpression		\
		JAssignmentExpression JBinaryArithmeticExpression	\
		JBinaryExpression JBitwiseComplementExpression		\
		JBitwiseExpression JBlock				\
		JBooleanLiteral JBreakStatement JByteLiteral		\
		JCastExpression JCatchClause JCharLiteral		\
		JClassBlock JClassDeclaration JClassExpression		\
		JClassFieldDeclarator					\
		JClassImport JCompilationUnit				\
		JCompoundAssignmentExpression JCompoundStatement	\
		JConditionalAndExpression JConditionalExpression	\
		JConditionalOrExpression JConstructorBlock		\
		JConstructorCall					\
		JConstructorDeclaration JContinueStatement		\
		JDivideExpression JDoStatement				\
		JDoubleLiteral JEmptyStatement				\
		JEqualityExpression					\
		JExpression JExpressionListStatement			\
		JExpressionStatement					\
		JFieldAccessExpression JFieldDeclaration		\
		JFloatLiteral JForStatement JFormalParameter		\
		JGeneratedLocalVariable JIfStatement			\
		JInitializerDeclaration JInstanceofExpression		\
		JIntLiteral						\
		JInterfaceDeclaration JLabeledStatement JLiteral	\
		JLocalVariable JLocalVariableExpression			\
		JLogicalComplementExpression				\
		JLongLiteral JLoopStatement				\
		JMemberDeclaration JMethodCallExpression		\
		JMethodDeclaration JMinusExpression JModuloExpression	\
		JMultExpression JNameExpression				\
		JNewArrayExpression					\
		JNullLiteral JOuterLocalVariableExpression		\
		JPackageImport JPackageName JParenthesedExpression	\
		JPhylum JPostfixExpression JPrefixExpression		\
		JQualifiedAnonymousCreation				\
		JQualifiedInstanceCreation				\
		JRelationalExpression					\
		JReturnStatement JShiftExpression			\
		JShortLiteral JStatement				\
		JStringLiteral JSuperExpression JSwitchGroup		\
		JSwitchLabel JSwitchStatement JSynchronizedStatement	\
		JThisExpression JThrowStatement JTryCatchStatement	\
		JTryFinallyStatement JTypeDeclaration			\
		JTypeDeclarationStatement JTypeNameExpression		\
		JUnaryExpression JUnaryMinusExpression			\
		JUnaryPlusExpression JUnaryPromote			\
		JUnqualifiedAnonymousCreation				\
		JUnqualifiedInstanceCreation				\
		JVariableDeclarationStatement JVariableDefinition	\
		JWhileStatement

MESSAGES =	KjcMessages

OPTIONS =	KjcOptions

## ----------------------------------------------------------------------
## GOALS

default:	build

## ----------------------------------------------------------------------
## GLOBAL DEFINITIONS

include $(TOPDIR)/Make.Defs
