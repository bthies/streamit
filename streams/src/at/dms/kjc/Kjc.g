/*
 * Copyright (C) 1990-2001 DMS Decision Management Systems Ges.m.b.H.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 *
 * $Id: Kjc.g,v 1.7 2007-02-22 20:15:01 dimock Exp $
 */

/*
 * This grammar is based on the Java 1.2 Recognizer from ANTLR
 *
 * Contributing authors:
 *		John Mitchell		johnm@non.net
 *		Terence Parr		parrt@magelang.com
 *		John Lilley		jlilley@empathy.com
 *		Scott Stanchfield	thetick@magelang.com
 *		Markus Mohnen           mohnen@informatik.rwth-aachen.de
 *		Peter Williams		pwilliams@netdynamics.com
 */

header { package at.dms.kjc; }
{
  import java.util.Vector;
  import java.util.ArrayList;

  import at.dms.compiler.CWarning;
  import at.dms.compiler.Compiler;
  import at.dms.compiler.JavaStyleComment;
  import at.dms.compiler.JavadocComment;
  import at.dms.compiler.PositionedError;
  import at.dms.compiler.TokenReference;
  import at.dms.compiler.antlr.extra.InputBuffer;
}

// ----------------------------------------------------------------------
// THE PARSER STARTS HERE
// ----------------------------------------------------------------------

class KjcParser extends Parser;
options {
  k = 2;                           // two token lookahead
  importVocab = Kjc;
  exportVocab = Kjc;
  codeGenMakeSwitchThreshold = 2;  // Some optimizations
  codeGenBitsetTestThreshold = 3;
  defaultErrorHandler = false;     // Don't generate parser error handlers
  superClass = "at.dms.compiler.antlr.extra.Parser";
  access = "private";			// Set default rule access
}

{
  public KjcParser(Compiler compiler, InputBuffer buffer) {
    super(compiler, new KjcScanner(compiler, buffer), MAX_LOOKAHEAD);
  }
}

// Compilation Unit: this is the start rule for this parser
public jCompilationUnit []
  returns [JCompilationUnit self = null]
{
  JPackageName			pack;
  CParseCompilationUnitContext	context = CParseCompilationUnitContext.getInstance();
  TokenReference		sourceRef = buildTokenReference();
}
:
  pack = jPackageDeclaration[]
    { context.setPackage(pack); }
  jImportDeclarations[context]
  ( jTypeDefinition[context] )*
  EOF
    {
      self = new JCompilationUnit(sourceRef,
				  pack,
				  context.getPackageImports(),
				  context.getClassImports(),
				  context.getTypeDeclarations());
      context.release();
    }
;

// JLS 7.4 Package Declarations
jPackageDeclaration []
returns [JPackageName self = JPackageName.UNNAMED]
{
  String	name;
}
:
  (
    "package" name = jIdentifier[] SEMI
      { self = new JPackageName(buildTokenReference(), name, getStatementComment()); }
  )?
;


// JLS 7.5 Import Declarations
jImportDeclarations [CParseCompilationUnitContext context]
:
  jAutomaticImports[context]
  ( jImportDeclaration[context] )*
;

// JLS 7.5.3 Automatic Imports
jAutomaticImports [CParseCompilationUnitContext context]
:
{
  context.addPackageImport(new JPackageImport(TokenReference.NO_REF, "java/lang", null));
}
;

jImportDeclaration [CParseCompilationUnitContext context]
{
  StringBuffer	buffer = null;
  boolean	star = false;
  String	name = null;
}
:
  "import" i:IDENT
  (
    DOT j:IDENT
      { (buffer == null ? (buffer = new StringBuffer(i.getText())) : buffer).append('/').append(j.getText()); }
  )*
    { name = buffer == null ? i.getText() : buffer.toString(); }
  ( DOT STAR { star = true; } )?
  SEMI
    {
      if (star) {
	context.addPackageImport(new JPackageImport(buildTokenReference(), name, getStatementComment()));
      } else {
	context.addClassImport(new JClassImport(buildTokenReference(), name, getStatementComment()));
      }
    }
;

// A type definition in a file is either a class or interface definition.
jTypeDefinition [CParseCompilationUnitContext context]
{
  int			mods = 0;
  JTypeDeclaration	decl = null;
  TokenReference	sourceRef = buildTokenReference();
}
:
  mods = jModifiers[]
  (
    decl = jClassDefinition[mods]
  |
    decl = jInterfaceDefinition[mods]
  )
    { context.addTypeDeclaration(decl); }
  |
    SEMI
      { reportTrouble(new CWarning(sourceRef, KjcMessages.STRAY_SEMICOLON, null)); }
;

// JLS 14.4 : Local Variable Declaration Statement
// A declaration is the creation of a reference or primitive-type variable
// Create a separate Type/Var tree for each var in the var list.
jLocalVariableDeclaration [int modifiers]
  returns [JVariableDeclarationStatement self = null]
{
  CType			type;
  JVariableDefinition[] decl;
  TokenReference	sourceRef = buildTokenReference();
}
:
  type = jTypeSpec[] decl = jVariableDefinitions[modifiers, type]
   {
     self = new JVariableDeclarationStatement(sourceRef,
					      decl,
					      getStatementComment());
   }
;

// A list of zero or more modifiers.  We could have used (modifier)* in
// place of a call to modifiers, but I thought it was a good idea to keep
// this rule separate so they can easily be collected in a Vector if
// someone so desires
jModifiers []
  returns [int self = 0]
{
  int		mod;
}
:
  (
    mod = jModifier[]
      {
	if ((mod & self) != 0) {
	  reportTrouble(new PositionedError(buildTokenReference(),
					    KjcMessages.DUPLICATE_MODIFIER,
					    CModifier.getName(mod)));
	}

	if (!CModifier.checkOrder(self, mod)) {
	  reportTrouble(new CWarning(buildTokenReference(),
				     KjcMessages.MODIFIER_ORDER,
				     CModifier.getName(mod)));
	}
	self |= mod;
      }
  )*
    {
      //!!! 010428 move to JXxxDeclaration
      if (CModifier.getSubsetSize(self,
				  at.dms.kjc.Constants.ACC_PUBLIC
				  | at.dms.kjc.Constants.ACC_PROTECTED
				  | at.dms.kjc.Constants.ACC_PRIVATE) > 1) {
	reportTrouble(new PositionedError(buildTokenReference(),
					  KjcMessages.INCOMPATIBLE_MODIFIERS,
					  CModifier.toString(CModifier.getSubsetOf(self,
										   at.dms.kjc.Constants.ACC_PUBLIC
										   | at.dms.kjc.Constants.ACC_PROTECTED
										   | at.dms.kjc.Constants.ACC_PRIVATE))));
      }
    }
;


// A type specification is a type name with possible brackets afterwards
//   (which would make it an array type).
jTypeSpec []
  returns [CType self = null]
:
  self = jClassTypeSpec[]
|
  self = jBuiltInTypeSpec[]
;

// A class type specification is a class type with possible brackets afterwards
//   (which would make it an array type).
jClassTypeSpec []
  returns [CType self = null]
{
  String		name = null;
  int			bounds = 0;
  JExpression expr;
  ArrayList dims = new ArrayList();
}
:
  self = jTypeName[]
  (
    LBRACK (expr = jExpression[]) RBRACK { dims.add(expr); bounds += 1; }
  )*
    {
      if (bounds > 0) {
	self = new CArrayType(self, bounds, (JExpression[])dims.toArray(new JExpression[0]));
      }
    }
;

// A builtin type specification is a builtin type with possible brackets
// afterwards (which would make it an array type).
jBuiltInTypeSpec []
  returns [CType self = null]
{
  int			bounds = 0;
  JExpression expr;
  ArrayList dims = new ArrayList();
}
:
  self = jBuiltInType[]
  (
    LBRACK (expr = jExpression[]) RBRACK { dims.add(expr); bounds += 1; }
  )*
    {
      if (bounds > 0) {
	self = new CArrayType(self, bounds, (JExpression[])dims.toArray(new JExpression[0]));
      }
    }
;

jType []
  returns [CType type = null]
:
  type = jBuiltInType[]
|
  type = jTypeName[]
;

// !!!JLS The primitive types.
jBuiltInType []
  returns [CType self = null]
:
  "void" { self = CStdType.Void; }
|
  "boolean" { self = CStdType.Boolean; }
|
  "byte" { self = CStdType.Byte; }
|
  "char" { self = CStdType.Char; }
|
  "short" { self = CStdType.Short; }
|
  "int" { self = CStdType.Integer; }
|
  "long" { self = CStdType.Long; }
|
  "float" { self = CStdType.Float; }
|
  "double" { self = CStdType.Double; }
;


// A (possibly-qualified) java identifier.  We start with the first IDENT
// and expand its name by adding dots and following IDENTS
jIdentifier []
  returns [String self = null]
{
  StringBuffer buffer = null;
}
:
  i:IDENT
  (
    DOT j:IDENT
      { (buffer == null ? (buffer = new StringBuffer(i.getText())) : buffer).append('/').append(j.getText()); }
  )*
    { self = buffer == null ? i.getText() : buffer.toString(); }
;

// modifiers for Java classes, interfaces, class/instance vars and methods
jModifier []
  returns [int self = 0]
:
  "public" { self = at.dms.kjc.Constants.ACC_PUBLIC; }
|
  "protected" { self = at.dms.kjc.Constants.ACC_PROTECTED; }
|
  "private" { self = at.dms.kjc.Constants.ACC_PRIVATE; }
|
  "static" { self = at.dms.kjc.Constants.ACC_STATIC; }
|
  "abstract" { self = at.dms.kjc.Constants.ACC_ABSTRACT; }
|
  "final" { self = at.dms.kjc.Constants.ACC_FINAL; }
|
  "native" { self = at.dms.kjc.Constants.ACC_NATIVE; }
|
  "strictfp" { self = at.dms.kjc.Constants.ACC_STRICT; }
|
  "synchronized" { self = at.dms.kjc.Constants.ACC_SYNCHRONIZED; }
|
  "transient" { self = at.dms.kjc.Constants.ACC_TRANSIENT; }
|
  "volatile" { self = at.dms.kjc.Constants.ACC_VOLATILE; }
;


// Definition of a Java class
jClassDefinition[int modifiers]
  returns [JClassDeclaration self = null]
{
  Token			ident = null;
  CClassType		superClass = null;
  CClassType[]		interfaces = CClassType.EMPTY;
  CParseClassContext	context = CParseClassContext.getInstance();
  TokenReference	sourceRef = buildTokenReference();
  JavadocComment	javadoc = getJavadocComment();
  JavaStyleComment[]	comments = getStatementComment();
}
:
  (
    "class" ident1:IDENT { ident = ident1; }
    superClass = jSuperClassClause[]
//   |
//     "filter" { superClass = CClassType.lookup("Filter"); }
//     ident2:IDENT { ident = ident2; }
//   |
//     "feedback" { superClass = CClassType.lookup("FeedbackLoop"); }
//     ident3:IDENT { ident = ident3; }
//   |
//     "pipeline" { superClass = CClassType.lookup("Pipeline"); }
//     ident4:IDENT { ident = ident4; }
//   |
//     "splitjoin" { superClass = CClassType.lookup("SplitJoin"); }
//     ident5:IDENT { ident = ident5; }
  )
  interfaces = jImplementsClause[]
  jClassBlock[context]
    {
      self = new JClassDeclaration(sourceRef,
				   modifiers,
				   ident.getText(),
				   superClass,
				   interfaces,
				   context.getFields(),
				   context.getMethods(),
				   context.getInnerClasses(),
				   context.getBody(),
				   javadoc,
				   comments);
      context.release();
    }
;

jSuperClassClause []
  returns [CClassType self = null]
:
  ( "extends" self = jTypeName[] )?
;

// Definition of a Java Interface
jInterfaceDefinition [int modifiers]
  returns [JInterfaceDeclaration self = null]
{
  CClassType[]		interfaces =  CClassType.EMPTY;
  CParseClassContext	context = CParseClassContext.getInstance();
  TokenReference	sourceRef = buildTokenReference();
}
:
  "interface" ident:IDENT
  // it might extend some other interfaces
  interfaces = jInterfaceExtends[]
  // now parse the body of the interface (looks like a class...)
  jClassBlock[context]
    {
      self = new JInterfaceDeclaration(sourceRef,
				       modifiers,
				       ident.getText(),
				       interfaces,
				       context.getFields(),
				       context.getMethods(),
				       context.getInnerClasses(),
				       context.getBody(),
				       getJavadocComment(),
				       getStatementComment());
      context.release();
    }
;


// This is the body of a class.  You can have fields and extra semicolons,
// That's about it (until you see what a field is...)
jClassBlock [CParseClassContext context]
:
  LCURLY
  (
    jMember[context]
  |
    SEMI
      { reportTrouble(new CWarning(buildTokenReference(), KjcMessages.STRAY_SEMICOLON, null)); }
  )*
  RCURLY
;

// An interface can extend several other interfaces...
jInterfaceExtends []
  returns [CClassType[] self = CClassType.EMPTY]
:
  ( "extends" self = jNameList[] )?
;

// A class can implement several interfaces...
jImplementsClause[]
  returns [CClassType[] self = CClassType.EMPTY]
:
  ( "implements" self = jNameList[] )?
;

// Now the various things that can be defined inside a class or interface:
// methods, constructors, or variable declaration
// Note that not all of these are really valid in an interface (constructors,
// for example), and if this grammar were used for a compiler there would
// need to be some semantic checks to make sure we're doing the right thing...
jMember [CParseClassContext context]
{
  int				modifiers = 0;
  CType				type;
  JMethodDeclaration		method;
  JTypeDeclaration		decl;
  JVariableDefinition[]		vars;
  JStatement[]			body = null;
  TokenReference		sourceRef = buildTokenReference();
}
:
  modifiers = jModifiers[]
  (
    decl = jClassDefinition[modifiers]              // inner class
      { context.addInnerDeclaration(decl); }
  |
    decl = jInterfaceDefinition[modifiers]          // inner interface
      { context.addInnerDeclaration(decl); }
  |
    method = jConstructorDefinition[modifiers]
      { context.addMethodDeclaration(method); }
  |
    // method or variable declaration(s)
    type = jTypeSpec[]
    (
      method = jMethodDefinition [modifiers, type]
        { context.addMethodDeclaration(method); }
    |
      vars = jVariableDefinitions[modifiers, type] SEMI
        {
	  for (int i = 0; i < vars.length; i++) {
	    context.addFieldDeclaration(new JFieldDeclaration(sourceRef,
							      vars[i],
							      getJavadocComment(),
							      getStatementComment()));
	  }
	}
    )
  )
|
  // "static { ... }" class initializer
  "static" body = jCompoundStatement[]
    { context.addBlockInitializer(new JClassBlock(sourceRef, true, body)); }
|
  // "{ ... }" instance initializer
  body = jCompoundStatement[]
    { context.addBlockInitializer(new JClassBlock(sourceRef, false, body)); }
;

jConstructorDefinition [int modifiers]
  returns [JConstructorDeclaration self = null]
{
  JFormalParameter[]	parameters;
  CClassType[]		throwsList = CClassType.EMPTY;
  JConstructorCall	constructorCall = null;
  Vector		body = new Vector();
  JStatement		stmt;
  TokenReference	sourceRef = buildTokenReference();
  JavadocComment	javadoc = getJavadocComment();
  JavaStyleComment[]	comments = getStatementComment();
}
:
  name : IDENT
  LPAREN parameters = jParameterDeclarationList[JLocalVariable.DES_PARAMETER] RPAREN
  ( throwsList = jThrowsClause[] )?
  LCURLY
  (
    ( ( "this" | "super") LPAREN ) =>
    constructorCall = jExplicitConstructorInvocation[]
  |
    // nothing
  )
  (
    stmt = jBlockStatement[]
      {
	if (stmt instanceof JEmptyStatement) {
	  reportTrouble(new CWarning(stmt.getTokenReference(), KjcMessages.STRAY_SEMICOLON, null));
	}
	body.addElement(stmt);
      }
  )*
  RCURLY
    {
      self = new JConstructorDeclaration(sourceRef,
					 modifiers,
					 name.getText(),
					 parameters,
					 throwsList,
					 constructorCall,
					 (JStatement[])at.dms.util.Utils.toArray(body, JStatement.class),
					 javadoc,
					 comments);
    }
;

jExplicitConstructorInvocation []
  returns [JConstructorCall self = null]
{
  boolean		functorIsThis = false;
  JExpression[]		args = null;
  JavaStyleComment[]	comments = getStatementComment();
  TokenReference	sourceRef = buildTokenReference();
}
:
  (
    "this"
      { functorIsThis = true; }
  |
    "super"
      { functorIsThis = false; }
  )
  LPAREN args = jArgList[] RPAREN
  SEMI
    { self = new JConstructorCall(sourceRef, functorIsThis, args); }
;

jMethodDefinition [int modifiers, CType type]
  returns [JMethodDeclaration self = null]
{
  JFormalParameter[]	parameters;
  int			bounds = 0;
  CClassType[]		throwsList = CClassType.EMPTY;
  JStatement[]		body = null;
  TokenReference	sourceRef = buildTokenReference();
  JavadocComment	javadoc = getJavadocComment();
  JavaStyleComment[]	comments = getStatementComment();
  JExpression expr;
  ArrayList dims = new ArrayList();
}
:
  name : IDENT
  LPAREN parameters = jParameterDeclarationList[JLocalVariable.DES_PARAMETER] RPAREN
  ( LBRACK (expr = jExpression[]) RBRACK { dims.add(expr); bounds += 1; } )*
    {
      if (bounds > 0) {
	reportTrouble(new CWarning(sourceRef, KjcMessages.OLD_STYLE_ARRAY_BOUNDS, null));
	type = new CArrayType(type, bounds, (JExpression[])dims.toArray(new JExpression[0]));
      }
    }
  (throwsList = jThrowsClause[])?
  (
    body = jCompoundStatement[]
  |
    SEMI
  )
    {
      self = new JMethodDeclaration(sourceRef,
				    modifiers,
				    type,
				    name.getText(),
				    parameters,
				    throwsList,
				    body == null ? null : new JBlock(sourceRef, body, null),
				    javadoc,
				    comments);
    }
;

jVariableDefinitions [int modifiers, CType type]
  returns [JVariableDefinition[] self = null]
{
  Vector		vars = new Vector();
  JVariableDefinition	decl;
}
:
  decl = jVariableDeclarator[modifiers, type]
    { vars.addElement(decl); }
  (
    COMMA decl = jVariableDeclarator[modifiers, type]
      { vars.addElement(decl); }
  )*
    { self = (JVariableDefinition[])at.dms.util.Utils.toArray(vars, JVariableDefinition.class); }
;

// JLS 8.3 Variable Declarator
jVariableDeclarator [int modifiers, CType type]
  returns [JVariableDefinition self = null]
{
  int			bounds = 0;
  JExpression		expr = null;
  TokenReference	sourceRef = buildTokenReference();
  JExpression dimExpr;
  ArrayList dims = new ArrayList();
}
:
  ident : IDENT
  ( LBRACK (dimExpr = jExpression[]) RBRACK { dims.add(expr); bounds += 1; } )*
  ( ASSIGN expr = jVariableInitializer[] )?
    {
      if (bounds > 0) {
	reportTrouble(new CWarning(sourceRef, KjcMessages.OLD_STYLE_ARRAY_BOUNDS, null));
	type = new CArrayType(type, bounds, (JExpression[])dims.toArray(new JExpression[0]));
      }
      self = new JVariableDefinition(sourceRef, modifiers, type, ident.getText(), expr);
    }
;

// JLS 8.3 Variable Initializer
jVariableInitializer []
  returns [JExpression self = null]
:
  self = jExpression[]
|
  self = jArrayInitializer[]
;

// JLS 10.6 Array Initializer
jArrayInitializer[]
  returns [JArrayInitializer self = null]
{
  JExpression		expr = null;
  Vector		vect = new Vector();
  TokenReference	sourceRef = buildTokenReference();
}
:
  LCURLY
  (
    expr = jVariableInitializer[]
      { vect.addElement(expr); }
    (
      // CONFLICT: does a COMMA after an initializer start a new
      //           initializer or start the option ',' at end?
      //           ANTLR generates proper code by matching
      //			 the comma as soon as possible.
      options { warnWhenFollowAmbig = false; } :
      COMMA expr = jVariableInitializer[]
        { vect.addElement(expr); }
    )*
  )?
  (
    COMMA
      { reportTrouble(new CWarning(sourceRef, KjcMessages.STRAY_COMMA, null)); }
  )?
  RCURLY
    {
      self = new JArrayInitializer(sourceRef,
				   (JExpression[])at.dms.util.Utils.toArray(vect, JExpression.class));
    }
;

// This is a list of exception classes that the method is declared to throw
jThrowsClause []
  returns [CClassType[] self]
:
  "throws" self = jNameList[]
;


// A list of formal parameters
jParameterDeclarationList [int desc]
 returns [JFormalParameter[] self = JFormalParameter.EMPTY]
{
  JFormalParameter		elem = null;
  Vector			vect = new Vector();
}
:
  (
    elem = jParameterDeclaration[desc]
      { vect.addElement(elem); }
    (
      COMMA elem = jParameterDeclaration[desc]
        { vect.addElement(elem); }
    )*
      { self = (JFormalParameter[])at.dms.util.Utils.toArray(vect, JFormalParameter.class); }
    )?
;

// A formal parameter.
jParameterDeclaration [int desc]
  returns [JFormalParameter self = null]
{
  boolean	isFinal = false;
  int		bounds = 0;
  CType		type;
  TokenReference	sourceRef = buildTokenReference();
  JExpression expr;
  ArrayList dims = new ArrayList();
}
:
  ( "final" { isFinal = true; } )?
  type = jTypeSpec[]
  ident:IDENT
  ( LBRACK (expr = jExpression[]) RBRACK { dims.add(expr); bounds += 1; } )*
    {
      if (bounds > 0) {
	reportTrouble(new CWarning(sourceRef, KjcMessages.OLD_STYLE_ARRAY_BOUNDS, null));
	type = new CArrayType(type, bounds, (JExpression[])dims.toArray(new JExpression[0]));
      }
      self = new JFormalParameter(sourceRef, desc, type, ident.getText(), isFinal);
    }
;

// Compound statement.  This is used in many contexts:
//   Inside a class definition prefixed with "static":
//      it is a class initializer
//   Inside a class definition without "static":
//      it is an instance initializer
//   As the body of a method
//   As a completely indepdent braced block of code inside a method
//      it starts a new scope for variable definitions

jCompoundStatement[]
  returns [JStatement[] self = null]
{
  Vector		body = new Vector();
  JStatement		stmt;
}
:
  LCURLY
  (
    stmt = jBlockStatement[]
      {
	if (stmt instanceof JEmptyStatement) {
	  reportTrouble(new CWarning(stmt.getTokenReference(), KjcMessages.STRAY_SEMICOLON, null));
	}
	body.addElement(stmt);
      }
  )*
  RCURLY
    { self = (JStatement[])at.dms.util.Utils.toArray(body, JStatement.class); }
;

jBlockStatement []
   returns [JStatement self = null]
{
  JTypeDeclaration	type;
  int			modifiers;
}
:
  ( jModifiers[] "class" ) =>
  modifiers = jModifiers[] type = jClassDefinition[modifiers]
    { self = new JTypeDeclarationStatement(type.getTokenReference(), type); }
|
  // declarations are ambiguous with "ID DOT" relative to expression
  // statements.  Must backtrack to be sure.  Could use a semantic
  // predicate to test symbol table to see what the type was coming
  // up, but that's pretty hard without a symbol table ;-)
  ( jModifiers[] jTypeSpec[] IDENT ) =>
  modifiers = jModifiers[] self = jLocalVariableDeclaration[modifiers] SEMI
|
  self = jStatement[]
;

jStatement []
   returns [JStatement self = null]
{
  JExpression		expr;
  JStatement[]		stmts;
  JavaStyleComment[]	comments = getStatementComment();
  TokenReference	sourceRef = buildTokenReference();
}
:
  // A list of statements in curly braces -- start a new scope!
  stmts = jCompoundStatement[]
    { self = new JBlock(sourceRef, stmts, comments); }
|
  // An expression statement.  This could be a method call,
  // assignment statement, or any other expression evaluated for
  // side-effects.
  expr = jExpression[] SEMI
    { self = new JExpressionStatement(sourceRef, expr, comments); }
|
  self = jLabeledStatement[]
|
  self = jIfStatement[]
|
  self = jForStatement[]
|
  self = jWhileStatement[]
|
  self = jDoStatement[]
|
  self = jBreakStatement[]
|
  self = jContinueStatement[]
|
  self = jReturnStatement[]
|
  self = jSwitchStatement[]
|
  // exception try-catch block
  self = jTryBlock[]
|
  self = jThrowStatement[]
|
  self = jSynchronizedStatement[]
|
  // empty statement
  SEMI
    { self = new JEmptyStatement(sourceRef, comments); }
;

jLabeledStatement []
  returns [JLabeledStatement self = null]
{
  JStatement		stmt;
  TokenReference	sourceRef = buildTokenReference();
}
:
  // Attach a label to the front of a statement
  label:IDENT COLON stmt = jStatement[]
    {
      if (stmt instanceof JEmptyStatement) {
	reportTrouble(new CWarning(sourceRef, KjcMessages.STRAY_SEMICOLON, null));
      }
      self = new JLabeledStatement(sourceRef, label.getText(), stmt, getStatementComment());
    }
;

jIfStatement []
  returns [JIfStatement self = null]
{
  JExpression		cond;
  JStatement		thenClause;
  JStatement		elseClause = null;
  TokenReference	sourceRef = buildTokenReference();
}
:
  "if" LPAREN cond = jExpression[] RPAREN
  thenClause = jStatement[]
  {
    // LOOKAHEAD OF 2 FORBIDDEN
    if (LA(1)==LITERAL_else) {
      match(LITERAL_else);
      elseClause = jStatement();
    }
  }
    {
      if (! (thenClause instanceof JBlock)) {
	reportTrouble(new CWarning(sourceRef, KjcMessages.ENCLOSE_IF_THEN_IN_BLOCK, null));
      }
      if (elseClause != null && !(elseClause instanceof JBlock || elseClause instanceof JIfStatement)) {
	reportTrouble(new CWarning(sourceRef, KjcMessages.ENCLOSE_IF_ELSE_IN_BLOCK, null));
      }
      self = new JIfStatement(sourceRef, cond, thenClause, elseClause, getStatementComment());
    }
;

jWhileStatement []
  returns [JWhileStatement self = null]
{
  JExpression		cond;
  JStatement		body;
  TokenReference	sourceRef = buildTokenReference();
}
:
  "while" LPAREN cond = jExpression[] RPAREN body = jStatement[]
    {
      if (!(body instanceof JBlock || body instanceof JEmptyStatement)) {
	reportTrouble(new CWarning(sourceRef, KjcMessages.ENCLOSE_LOOP_BODY_IN_BLOCK, null));
      }
      self = new JWhileStatement(sourceRef, cond, body, getStatementComment());
    }
;

jDoStatement []
  returns [JDoStatement self = null]
{
  JExpression		cond;
  JStatement		body;
  TokenReference	sourceRef = buildTokenReference();
}
:
  "do" body = jStatement[] "while" LPAREN cond = jExpression[] RPAREN SEMI
    {
      if (! (body instanceof JBlock)) {
	reportTrouble(new CWarning(sourceRef, KjcMessages.ENCLOSE_LOOP_BODY_IN_BLOCK, null));
      }
      self = new JDoStatement(sourceRef, cond, body, getStatementComment());
    }
;

jBreakStatement []
  returns [JBreakStatement self = null]
{
  TokenReference	sourceRef = buildTokenReference();
}
:
  "break" ( label:IDENT )? SEMI
    { self = new JBreakStatement(sourceRef, label == null ? null : label.getText(), getStatementComment()); }
;

jContinueStatement []
  returns [JContinueStatement self = null]
{
  TokenReference	sourceRef = buildTokenReference();
}
:
  "continue" ( label:IDENT )? SEMI
    { self = new JContinueStatement(sourceRef, label == null ? null : label.getText(), getStatementComment()); }
;

jReturnStatement []
  returns [JReturnStatement self = null]
{
  JExpression		expr = null;
  TokenReference	sourceRef = buildTokenReference();
}
:
  "return" ( expr = jExpression[] )? SEMI
    { self = new JReturnStatement(sourceRef, expr, getStatementComment()); }
;

jThrowStatement []
  returns [JThrowStatement self = null]
{
  JExpression		expr;
  TokenReference	sourceRef = buildTokenReference();
}
:
  "throw" expr = jExpression[] SEMI
    { self = new JThrowStatement(sourceRef, expr, getStatementComment()); }
;

jSynchronizedStatement []
  returns [JSynchronizedStatement self = null]
{
  JExpression		expr;
  JStatement[]		body;
  TokenReference	sourceRef = buildTokenReference();
}
:
  "synchronized" LPAREN expr = jExpression[] RPAREN body = jCompoundStatement[]
    {
      self = new JSynchronizedStatement(sourceRef,
					expr,
					new JBlock(sourceRef, body, null),
					getStatementComment());
    }
;

jSwitchStatement []
  returns [JSwitchStatement self = null]
{
  JExpression		expr = null;
  Vector		body = null;
  JSwitchGroup		group;
  TokenReference	sourceRef = buildTokenReference();
}
:
  "switch"
  LPAREN expr = jExpression[] RPAREN
  LCURLY
    { body = new Vector(); }
  (
    group = jCasesGroup[]
      { body.addElement(group); }
  )*
  RCURLY
    {
      self = new JSwitchStatement(sourceRef,
				  expr,
				  (JSwitchGroup[])at.dms.util.Utils.toArray(body, JSwitchGroup.class),
				  getStatementComment());
    }
;

jCasesGroup []
  returns [JSwitchGroup self = null]
{
  Vector		labels = new Vector();
  Vector		stmts = new Vector();

  JSwitchLabel		label;
  JStatement		stmt;
  TokenReference	sourceRef = buildTokenReference();
}
:
  (
    // CONFLICT: to which case group do the statements bind?
    //           ANTLR generates proper code: it groups the
    //           many "case"/"default" labels together then
    //           follows them with the statements
    options { warnWhenFollowAmbig = false; } :
    label = jACase[] { labels.addElement(label); }
  )+
  (
    stmt = jBlockStatement[]
    {
      if (stmt instanceof JEmptyStatement) {
	reportTrouble(new CWarning(stmt.getTokenReference(), KjcMessages.STRAY_SEMICOLON, null));
      }
      stmts.addElement(stmt);
    }
  )*
    {
      self = new JSwitchGroup(sourceRef,
			      (JSwitchLabel[])at.dms.util.Utils.toArray(labels, JSwitchLabel.class),
			      (JStatement[])at.dms.util.Utils.toArray(stmts, JStatement.class));
    }
;

jACase []
  returns [JSwitchLabel self = null]
{
  JExpression		expr = null;
  TokenReference	sourceRef = buildTokenReference();
}
:
  (
    "case" expr = jExpression[]
      { self = new JSwitchLabel(sourceRef, expr); }
  |
    "default"
      { self = new JSwitchLabel(sourceRef, null); }
  )
  COLON
;


jForStatement []
  returns [JForStatement self = null]
{
  JStatement		init;
  JExpression		cond;
  JStatement		iter;
  JStatement		body;
  TokenReference	sourceRef = buildTokenReference();
}
:
  "for"
  LPAREN
  init = jForInit[] SEMI
  cond = jForCond[] SEMI
  iter = jForIter[]
  RPAREN
  body = jStatement[]
    {
      if (!(body instanceof JBlock || body instanceof JEmptyStatement)) {
	reportTrouble(new CWarning(sourceRef, KjcMessages.ENCLOSE_LOOP_BODY_IN_BLOCK, null));
      }
      self = new JForStatement(sourceRef, init, cond, iter, body, getStatementComment());
    }
;

// The initializer for a for loop
jForInit []
  returns [JStatement self = null]
{
  int			modifiers;
  JExpression[]		list;
}
:
(
  // if it looks like a declaration, it is
  ( jModifiers[] jTypeSpec[] IDENT ) =>
  modifiers = jModifiers[] self = jLocalVariableDeclaration[modifiers]
|
  // otherwise it could be an expression list...
  list = jExpressionList[]
    { self = new JExpressionListStatement(buildTokenReference(), list, getStatementComment()); }
)?
;

jForCond []
  returns [JExpression expr = null]
:
  ( expr = jExpression[] )?
;

jForIter []
  returns [JExpressionListStatement self = null]
{
  JExpression[] list;
}
:
  (
   list = jExpressionList[]
     { self = new JExpressionListStatement(buildTokenReference(), list, null); }
  )?
;

// an exception handler try/catch block
jTryBlock []
  returns [JStatement self = null]
{
  JBlock		tryClause = null;
  JStatement[]		compound;
  Vector		catchClauses = new Vector();
  JBlock		finallyClause = null;
  JCatchClause		catcher = null;
  TokenReference	sourceRef = buildTokenReference();
}
:
  "try"
  compound = jCompoundStatement[]
    { tryClause = new JBlock(sourceRef, compound, null); }
  (
    catcher = jHandler[]
      { catchClauses.addElement(catcher); }
  )*
  (
    "finally" compound = jCompoundStatement[]
      { finallyClause = new JBlock(sourceRef, compound, null); }
  )?
    {
      if (catchClauses.size() > 0) {
	self = new JTryCatchStatement(sourceRef,
				      tryClause,
				      (JCatchClause[])at.dms.util.Utils.toArray(catchClauses, JCatchClause.class),
				      finallyClause == null ? getStatementComment() : null);
      }
      if (finallyClause != null) {
	// If both catch and finally clauses are present,
	// the try-catch is embedded as try clause into a
	// try-finally statement.
	if (self != null) {
	  tryClause = new JBlock(sourceRef, new JStatement[] {self}, null);
	}
	self = new JTryFinallyStatement(sourceRef, tryClause, finallyClause, getStatementComment());
      }

      if (self == null) {
	// try without catch or finally: error
	reportTrouble(new PositionedError(sourceRef, KjcMessages.TRY_NOCATCH, null));
	self = tryClause;
      }
    }
;


// an exception handler
jHandler []
  returns [JCatchClause self = null]
{
  JFormalParameter	param;
  JStatement[]		body;
  TokenReference	sourceRef = buildTokenReference();
}
:
  "catch" LPAREN param = jParameterDeclaration[JLocalVariable.DES_CATCH_PARAMETER] RPAREN body = jCompoundStatement[]
    {
      self = new JCatchClause(sourceRef,
			      param,
			      new JBlock(sourceRef, body, null));
    }
;


// expressions
// Note that most of these expressions follow the pattern
//   thisLevelExpression :
//       nextHigherPrecedenceExpression
//           (OPERATOR nextHigherPrecedenceExpression)*
// which is a standard recursive definition for a parsing an expression.
// The operators in java have the following precedences:
//    lowest  (13)  = *= /= %= += -= <<= >>= >>>= &= ^= |=
//            (12)  ?:
//            (11)  ||
//            (10)  &&
//            ( 9)  |
//            ( 8)  ^
//            ( 7)  &
//            ( 6)  == !=
//            ( 5)  < <= > >=
//            ( 4)  << >>
//            ( 3)  +(binary) -(binary)
//            ( 2)  * / %
//            ( 1)  ++ -- +(unary) -(unary)  ~  !  (type)
//                  []   () (method call)  . (dot -- identifier qualification)
//                  new   ()  (explicit parenthesis)
//
// the last two are not usually on a precedence chart; I put them in
// to point out that new has a higher precedence than '.', so you
// can validy use
//     new Frame().show()
//
// Note that the above precedence levels map to the rules below...
// Once you have a precedence chart, writing the appropriate rules as below
//   is usually very straightfoward



// the mother of all expressions
jExpression []
  returns [JExpression self = null]
:
  self = jAssignmentExpression[]
;


// This is a list of expressions.
jExpressionList []
  returns [JExpression[] self = null]
{
  JExpression		expr;
  Vector		vect = new Vector();
}
:
  expr = jExpression[]
    { vect.addElement(expr); }
  (
    COMMA expr = jExpression[]
      { vect.addElement(expr); }
  )*
    { self = (JExpression[])at.dms.util.Utils.toArray(vect, JExpression.class); }
;

// assignment expression (level 13)
jAssignmentExpression []
  returns [JExpression self = null]
{
  int			oper = -1;
  JExpression		right;
}
:
  self = jConditionalExpression[]
  (
    ASSIGN right = jAssignmentExpression[]
      { self = new JAssignmentExpression(self.getTokenReference(), self, right); }
  |
    oper = jCompoundAssignmentOperator[] right = jAssignmentExpression[]
      { self = new JCompoundAssignmentExpression(self.getTokenReference(), oper, self, right); }
  )?
;

jCompoundAssignmentOperator []
  returns [int self = -1]
:
  PLUS_ASSIGN { self = Constants.OPE_PLUS; }
|
  MINUS_ASSIGN { self = Constants.OPE_MINUS; }
|
  STAR_ASSIGN { self = Constants.OPE_STAR; }
|
  SLASH_ASSIGN { self = Constants.OPE_SLASH; }
|
  PERCENT_ASSIGN { self = Constants.OPE_PERCENT; }
|
  SR_ASSIGN { self = Constants.OPE_SR; }
|
  BSR_ASSIGN { self = Constants.OPE_BSR; }
|
  SL_ASSIGN { self = Constants.OPE_SL; }
|
  BAND_ASSIGN { self = Constants.OPE_BAND; }
|
  BXOR_ASSIGN { self = Constants.OPE_BXOR; }
|
  BOR_ASSIGN { self = Constants.OPE_BOR; }
;


// conditional test (level 12)
jConditionalExpression []
  returns [JExpression self = null]
{
  JExpression		middle;
  JExpression		right;
}
:
  self = jLogicalOrExpression[]
  (
    QUESTION middle = jConditionalExpression[] COLON right = jConditionalExpression[]
      { self = new JConditionalExpression(self.getTokenReference(), self, middle, right); }
  )?
;

// logical or (||)  (level 11)
jLogicalOrExpression []
  returns [JExpression self = null]
{
  JExpression		right;
}
:
  self = jLogicalAndExpression[]
  (
    LOR right = jLogicalAndExpression[]
      { self = new JConditionalOrExpression(self.getTokenReference(), self, right); }
  )*
;

// logical and (&&)  (level 10)
jLogicalAndExpression []
  returns [JExpression self = null]
{
  JExpression		right;
}
:
  self = jInclusiveOrExpression[]
  (
    LAND right = jInclusiveOrExpression[]
      { self = new JConditionalAndExpression(self.getTokenReference(), self, right); }
  )*
;

// bitwise or non-short-circuiting or (|)  (level 9)
jInclusiveOrExpression []
  returns [JExpression self = null]
{
  JExpression		right;
}
:
  self = jExclusiveOrExpression[]
  (
    BOR right = jExclusiveOrExpression[]
      { self = new JBitwiseExpression(self.getTokenReference(), Constants.OPE_BOR, self, right); }
  )*
;

// exclusive or (^)  (level 8)
jExclusiveOrExpression []
  returns [JExpression self = null]
{
  JExpression		right;
}
:
  self = jAndExpression[]
  (
    BXOR right = jAndExpression[]
      { self = new JBitwiseExpression(self.getTokenReference(), Constants.OPE_BXOR, self, right); }
  )*
;

// bitwise or non-short-circuiting and (&)  (level 7)
jAndExpression []
  returns [JExpression self = null]
{
  JExpression		right;
}
:
  self = jEqualityExpression[]
  (
    BAND right = jEqualityExpression[]
      { self = new JBitwiseExpression(self.getTokenReference(), Constants.OPE_BAND, self, right); }
  )*
;

// equality/inequality (==/!=) (level 6)
jEqualityExpression []
  returns [JExpression self = null]
{
  JExpression		right;
}
:
  self = jRelationalExpression[]
  (
    NOT_EQUAL right = jRelationalExpression[]
      { self = new JEqualityExpression(self.getTokenReference(), false, self, right); }
  |
    EQUAL right = jRelationalExpression[]
      { self = new JEqualityExpression(self.getTokenReference(), true, self, right); }
  )*
;


// boolean relational expressions (level 5)
jRelationalExpression []
  returns [JExpression self = null]
{
  int			operator = -1;
  JExpression		right;
  CType			type;
}
:
  self = jShiftExpression[]
  (
    (
      (
        LT { operator = Constants.OPE_LT; }
      |
	GT { operator = Constants.OPE_GT; }
      |
	LE { operator = Constants.OPE_LE; }
      |
	GE { operator = Constants.OPE_GE; }
      )
      right = jShiftExpression[]
        { self = new JRelationalExpression(self.getTokenReference(), operator, self, right); }
    )*
  |
    "instanceof" type = jTypeSpec[]
      { self = new JInstanceofExpression(self.getTokenReference(), self, type); }
  )
;


// bit shift expressions (level 4)
jShiftExpression []
  returns [JExpression self = null]
{
  int			operator = -1;
  JExpression		right;
}
:
  self = jAdditiveExpression[]
  (
    (
      SL { operator = Constants.OPE_SL; }
    |
      SR { operator = Constants.OPE_SR; }
    |
      BSR { operator = Constants.OPE_BSR; }
    )
    right = jAdditiveExpression[]
      { self = new JShiftExpression(self.getTokenReference(), operator, self, right); }
  )*
;


// binary addition/subtraction (level 3)
jAdditiveExpression []
  returns [JExpression self = null]
{
  JExpression		right;
}
:
  self = jMultiplicativeExpression[]
  (
    PLUS right = jMultiplicativeExpression[]
      { self = new JAddExpression(self.getTokenReference(), self, right); }
  |
    MINUS right = jMultiplicativeExpression[]
      { self = new JMinusExpression(self.getTokenReference(), self, right); }
  )*
;


// multiplication/division/modulo (level 2)
jMultiplicativeExpression []
  returns [JExpression self = null]
{
  JExpression		right;
}
:
  self = jUnaryExpression[]
  (
    STAR right = jUnaryExpression[]
      { self = new JMultExpression(self.getTokenReference(), self, right); }
  |
    SLASH right = jUnaryExpression[]
      { self = new JDivideExpression(self.getTokenReference(), self, right); }
  |
    PERCENT right = jUnaryExpression[]
      { self = new JModuloExpression(self.getTokenReference(), self, right); }
  )*
;

jUnaryExpression []
  returns [JExpression self = null]
{
  TokenReference	sourceRef = buildTokenReference();
}
:
  INC self = jUnaryExpression[]
    { self = new JPrefixExpression(sourceRef, Constants.OPE_PREINC, self); }
|
  DEC self = jUnaryExpression[]
    { self = new JPrefixExpression(sourceRef, Constants.OPE_PREDEC, self); }
|
  MINUS self = jUnaryExpression[]
    { self = new JUnaryMinusExpression(sourceRef, self); }
|
  PLUS self = jUnaryExpression[]
    { self = new JUnaryPlusExpression(sourceRef, self); }
|
  self = jUnaryExpressionNotPlusMinus[]
;

jUnaryExpressionNotPlusMinus []
  returns [JExpression self = null]
{
  JExpression		expr;
  CType			dest;
  TokenReference	sourceRef = buildTokenReference();
}
:
  BNOT self = jUnaryExpression[]
    { self = new JBitwiseComplementExpression(sourceRef, self); }
|
  LNOT self = jUnaryExpression[]
    { self = new JLogicalComplementExpression(sourceRef, self); }
|
  (
    // subrule allows option to shut off warnings
    options {
      // "(int" ambig with postfixExpr due to lack of sequence
      // info in linear approximate LL(k).  It's ok.  Shut up.
      generateAmbigWarnings=false;
    } :
    // If typecast is built in type, must be numeric operand
    // Also, no reason to backtrack if type keyword like int, float...
    LPAREN dest = jBuiltInTypeSpec[] RPAREN
    expr = jUnaryExpression[]
      { self = new JCastExpression(sourceRef, expr, dest); }
    // Have to backtrack to see if operator follows.  If no operator
    // follows, it's a typecast.  No semantic checking needed to parse.
    // if it _looks_ like a cast, it _is_ a cast; else it's a "(expr)"
  |
    (LPAREN jClassTypeSpec[] RPAREN jUnaryExpressionNotPlusMinus[])=>
    LPAREN dest = jClassTypeSpec[] RPAREN
    expr = jUnaryExpressionNotPlusMinus[]
      { self = new JCastExpression(sourceRef, expr, dest); }
  |
    self = jPostfixExpression[]
  )
;

// qualified names, array expressions, method invocation, post inc/dec
jPostfixExpression[]
  returns [JExpression self = null]
{
  int			bounds = 0;
  JExpression		expr;
  JExpression[]		args = null;
  CType			type;
  TokenReference	sourceRef = buildTokenReference();
}
:
  self = jPrimaryExpression[] // start with a primary
  (
    // qualified id (id.id.id.id...) -- build the name
    DOT
    (
      ident : IDENT
        { self = new JNameExpression(sourceRef, self, ident.getText()); }
    |
      "this"
        { self = new JThisExpression(sourceRef, self); }
    |
      "class"
        { self = new JClassExpression(sourceRef, self, 0); }
    |
      self = jQualifiedNewExpression[self]
    )
  |
    // allow ClassName[].class
    ( LBRACK RBRACK { bounds++; } )+
    DOT "class"
      { self = new JClassExpression(sourceRef, self, bounds); }
  |
    LBRACK expr = jExpression[] RBRACK
      { self = new JArrayAccessExpression(sourceRef, self, expr); }
  |
    LPAREN args = jArgList[] RPAREN
      {
	if (! (self instanceof JNameExpression)) {
	  reportTrouble(new PositionedError(sourceRef, KjcMessages.INVALID_METHOD_NAME, null));
	} else {
	  self = new JMethodCallExpression(sourceRef,
					   ((JNameExpression)self).getPrefix(),
					   ((JNameExpression)self).getName(),
					   args);
	}
      }
  )*
  (
    INC
      { self = new JPostfixExpression(sourceRef, Constants.OPE_POSTINC, self); }
  |
    DEC
      { self = new JPostfixExpression(sourceRef, Constants.OPE_POSTDEC, self); }
  |
    // nothing
  )
;

// the basic element of an expression
jPrimaryExpression []
  returns [JExpression self = null]
{
  int			bounds = 0;
  CType			type;
  TokenReference	sourceRef = buildTokenReference();
}
:
  ident : IDENT
    { self = new JNameExpression(sourceRef, ident.getText()); }
|
  self = jUnqualifiedNewExpression[]
|
  self = jLiteral[]
|
  "super"
    { self = new JSuperExpression(sourceRef); }
|
  "true"
    { self = new JBooleanLiteral(sourceRef, true); }
|
  "false"
    { self = new JBooleanLiteral(sourceRef, false); }
|
  "this"
    { self = new JThisExpression(sourceRef); }
|
  "null"
    { self = new JNullLiteral(sourceRef); }
|
  LPAREN self = jAssignmentExpression[] RPAREN
    { self = new JParenthesedExpression(sourceRef, self); }
|
  type = jBuiltInType[]
  ( LBRACK RBRACK { bounds++; } )*
  DOT "class"
    { self = new JClassExpression(buildTokenReference(), type, bounds); }
;

jUnqualifiedNewExpression []
  returns [JExpression self = null]
{
  CType				type;
  JExpression[]			args;
  JArrayInitializer		init = null;
  JClassDeclaration		decl = null;
  CParseClassContext		context = null;
  TokenReference		sourceRef = buildTokenReference();
}
:
  "new"
  (
    type = jBuiltInType[]
    args = jNewArrayDeclarator[] ( init = jArrayInitializer[] )?
      { self = new JNewArrayExpression(sourceRef, type, args, init); }
  |
    type = jTypeName[]
    (
      args = jNewArrayDeclarator[] ( init = jArrayInitializer[] )?
        { self = new JNewArrayExpression(sourceRef, type, args, init); }
    |
      LPAREN args = jArgList[] RPAREN
      (
        { context = CParseClassContext.getInstance(); }
        jClassBlock[context]
          {
	    decl = new JClassDeclaration(sourceRef,
					 at.dms.kjc.Constants.ACC_FINAL, // JLS 15.9.5
					 ((CClassType)type).getQualifiedName(),
					 null,
					 CClassType.EMPTY,
					 context.getFields(),
					 context.getMethods(),
					 context.getInnerClasses(),
					 context.getBody(),
					 getJavadocComment(),
					 getStatementComment());
	    context.release();
	  }
          { self = new JUnqualifiedAnonymousCreation(sourceRef, (CClassType)type, args, decl); }
      |
	// epsilon
        { self = new JUnqualifiedInstanceCreation(sourceRef, (CClassType)type, args); }
      )
    )
  )
;

jQualifiedNewExpression [JExpression prefix]
  returns [JExpression self = null]
{
  CType				type;
  JExpression[]			args;
  JArrayInitializer		init = null;
  JClassDeclaration		decl = null;
  CParseClassContext		context = null;
  TokenReference		sourceRef = buildTokenReference();
}
:
  "new" ident : IDENT
  LPAREN args = jArgList[] RPAREN
  (
    { context = CParseClassContext.getInstance(); }
    jClassBlock[context]
      {
	decl = new JClassDeclaration(sourceRef,
				     at.dms.kjc.Constants.ACC_FINAL, // JLS 15.9.5
				     ident.getText(),
				     null,
				     CClassType.EMPTY,
				     context.getFields(),
				     context.getMethods(),
				     context.getInnerClasses(),
				     context.getBody(),
				     getJavadocComment(),
				     getStatementComment());
	context.release();
      }
      { self = new JQualifiedAnonymousCreation(sourceRef, prefix, ident.getText(), args, decl); }
  |
    { self = new JQualifiedInstanceCreation(sourceRef, prefix, ident.getText(), args); }
  )
;

jArgList[]
  returns [JExpression[] self = JExpression.EMPTY]
:
  ( self = jExpressionList[] )?
;


// TODO : do these checks in semantic analysis
jNewArrayDeclarator[]
  returns [JExpression[] self = null]
{
  Vector		container = new Vector();
  JExpression		exp = null;
}
:
  (
    // CONFLICT:
    // newExpression is a primaryExpression which can be
    // followed by an array index reference.  This is ok,
    // as the generated code will stay in this loop as
    // long as it sees an LBRACK (proper behavior)
    options { warnWhenFollowAmbig = false; } :
    LBRACK
    ( exp = jExpression[] )?
    RBRACK
      { container.addElement(exp); exp = null; }
  )+
    { self = (JExpression[])at.dms.util.Utils.toArray(container, JExpression.class); }
;

jLiteral []
  returns [JLiteral self = null]
:
  self = jIntegerLiteral[]
|
  self = jCharLiteral[]
|
  self = jStringLiteral[]
|
  self = jRealLiteral[]
;

jIntegerLiteral []
  returns [JLiteral self = null]
:
  i : INTEGER_LITERAL
    {
      try {
	self = JLiteral.parseInteger(buildTokenReference(), i.getText());
      } catch (PositionedError e) {
	reportTrouble(e);
	// allow parsing to continue
	self = new JIntLiteral(TokenReference.NO_REF, 0);
      }
    }
;

jCharLiteral []
  returns [JLiteral self = null]
:
  c : CHARACTER_LITERAL
    {
      try {
	self = new JCharLiteral(buildTokenReference(), c.getText());
      } catch (PositionedError e) {
	reportTrouble(e);
	// allow parsing to continue
	self = new JCharLiteral(TokenReference.NO_REF, '\0');
      }
    }
;

jStringLiteral []
  returns [JLiteral self = null]
:
  s : STRING_LITERAL
    { self = new JStringLiteral(buildTokenReference(), s.getText()); }
;

jRealLiteral []
  returns [JLiteral self = null]
:
  r : REAL_LITERAL
    {
      try {
	self = JLiteral.parseReal(buildTokenReference(), r.getText());
      } catch (PositionedError e) {
	reportTrouble(e);
	// allow parsing to continue
	self = new JFloatLiteral(TokenReference.NO_REF, 0f);
      }
    }
;

jNameList []
  returns [CClassType[] self = null]
{
  CClassType	name;
  Vector	container = new Vector();
}
:
  name = jTypeName[] { container.addElement(name); }
  (
    COMMA
    name = jTypeName[] { container.addElement(name); }
  )*
    { self = (CClassType[])at.dms.util.Utils.toArray(container, CClassType.class); }
;

jTypeName []
  returns [CClassType self = null]
{
  String	name;
}
:
  name = jIdentifier[]
    { self = CClassType.lookup(name); }
;
