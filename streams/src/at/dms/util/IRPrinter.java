/*
 * IRPrinter.java: Print Kopi IR
 * David Maze <dmaze@cag.lcs.mit.edu>
 */

package at.dms.util;

import java.io.*;
import java.util.List;
import java.util.ListIterator;

import at.dms.kjc.SLIRVisitor;
import at.dms.compiler.JavaStyleComment;
import at.dms.compiler.JavadocComment;
import at.dms.util.Utils;
import at.dms.kjc.*;
import at.dms.kjc.sir.*;
import at.dms.kjc.lir.*;

public class IRPrinter extends Utils implements SLIRVisitor
{
    /**
     * Amount the current line of text should be indented by
     */
    protected int indent;
    
    /**
     * Where printed text should go
     */
    protected BufferedWriter p;

    /**
     * Default constructor.  Writes IR to standard output.
     */
    public IRPrinter()
    {
        indent = 0;
        p = new BufferedWriter(new OutputStreamWriter(System.out));
    }

    /**
     * Build an IRPrinter for a particular file.
     *
     * @arg filename  Name of the file to write IR to
     */
    public IRPrinter(String filename)
    {
        indent = 0;
        try
        {
            p = new BufferedWriter(new FileWriter(filename));
        }
        catch (IOException e)
        {
            System.err.println(e);
            System.exit(1);
        }
    }

    /**
     * Flush any pending output.
     */
    public void close()
    {
        try
        {
            p.newLine();
            p.flush();
        }
        catch (IOException e)
        {
            System.err.println(e);
            System.exit(1);
        }
    }

    /**
     * Print a newline and the current indent.
     */
    protected void printNewline()
    {
        int i;
        try
        {
            p.newLine();
            for (i = 0; i < indent; i++) {
                p.write(' ');
	    }
	    p.flush();
        }
        catch (IOException e)
        {
            System.err.println(e);
            System.exit(1);
        }
    }
    
    /**
     * Print the start of a block.
     */
    protected void printStart(char delim, String name)
    {
        printNewline();
        printData(delim);
        printData(name);
        indent += 2;
    }

    /**
     * Print arbitrary string data to p.
     *
     * @arg data  Data to write
     */
    protected void printData(String data)
    {
        try
        {
            p.write(data);
	    p.flush();
        }
        catch (IOException e)
        {
            System.err.println(e);
            System.exit(1);
        }
    }

    /**
     * Print an arbitrary single character.
     *
     * @arg data  Character to write
     */
    protected void printData(char data)
    {
        try
        {
            p.write(data);
	    p.flush();
        }
        catch (IOException e)
        {
            System.err.println(e);
            System.exit(1);
        }
    }
    
    /**
     * Print the end of a block.
     */
    protected void printEnd(char delim)
    {
        printData(delim);
        indent -= 2;
    }

    /**
     * Begin an object block.
     */
    protected void blockStart(String name)
    {
        printStart('[', name);
    }
    
    /**
     * End an object block.
     */
    protected void blockEnd()
    {
        printEnd(']');
    }

    /**
     * Begin an attribute block.
     */
    protected void attrStart(String name)
    {
        printStart('(', name);
    }
    
    /**
     * End an attribute block.
     */
    protected void attrEnd()
    {
        printEnd(')');
    }
    
    /**
     * Print a single-line attribute block.
     */
    protected void attrPrint(String name, String body)
    {
        attrStart(name);
        printData(" ");
        if (body == null) {
	    printData("NULL");
	} else {
	    printData(body);
	}
        attrEnd();
    }

    /**
     * Print an attribute block for a Kjc object.
     */
    protected void attrPrint(String name, JPhylum body)
    {
        if (body == null)
            return;

        attrStart(name);
        body.accept(this);
        attrEnd();
    }

    /**
     * Print a multi-line attribute block corresponding to a list
     * of objects.
     */
    protected void attrList(String name, Object[] body)
    {
        if (body == null || body.length == 0)
            return;
        
        attrStart(name);
        for (int i = 0; i < body.length; i++)
        {
            printNewline();
            printData(body[i].toString());
        }
        attrEnd();
    }

    // ----------------------------------------------------------------------
    // COMPILATION UNIT
    // ----------------------------------------------------------------------

    /**
     * visits a compilation unit
     */
    public void visitCompilationUnit(JCompilationUnit self,
                                     JPackageName packageName,
                                     JPackageImport[] importedPackages,
                                     JClassImport[] importedClasses,
                                     JTypeDeclaration[] typeDeclarations)
    {
        blockStart("CompilationUnit");

        if (packageName.getName().length() > 0)
            packageName.accept(this);

        for (int i = 0; i < importedPackages.length ; i++)
            importedPackages[i].accept(this);

        for (int i = 0; i < importedClasses.length ; i++)
            importedClasses[i].accept(this);

        for (int i = 0; i < typeDeclarations.length ; i++)
            typeDeclarations[i].accept(this);

        blockEnd();
    }

    // ----------------------------------------------------------------------
    // TYPE DECLARATION
    // ----------------------------------------------------------------------

    /**
     * visits a class declaration
     */
    public void visitClassDeclaration(JClassDeclaration self,
                                      int modifiers,
                                      String ident,
                                      String superName,
                                      CClassType[] interfaces,
                                      JPhylum[] body,
				      JFieldDeclaration[] fields,
                                      JMethodDeclaration[] methods,
                                      JTypeDeclaration[] decls)
    {
        blockStart("ClassDeclaration");
        
        attrPrint("modifiers", CModifier.toString(modifiers));
        attrPrint("class", ident);
        attrPrint("extends", superName);
        attrList("implements", interfaces);

        visitClassBody(decls, fields, methods, body);
        
        blockEnd();
    }
    
    /**
     * visits a class body
     */
    public void visitClassBody(JTypeDeclaration[] decls,
			       JFieldDeclaration[] fields,
                               JMethodDeclaration[] methods,
                               JPhylum[] body)
    {
        blockStart("ClassBody");

	for (int i = 0; i < decls.length ; i++)
	    decls[i].accept(this);
        for (int i = 0; i < methods.length ; i++)
	    methods[i].accept(this);
        for (int i = 0; i < fields.length ; i++)
            fields[i].accept(this);
	if (body!=null) {
	    for (int i = 0; i < body.length ; i++)
		body[i].accept(this);
	}

        blockEnd();
    }

    /**
   * visits a class declaration
   */
    public void visitInnerClassDeclaration(JClassDeclaration self,
                                           int modifiers,
                                           String ident,
                                           String superName,
                                           CClassType[] interfaces,
                                           JTypeDeclaration[] decls,
                                           JPhylum[] body,
                                           JFieldDeclaration[] fields,
                                           JMethodDeclaration[] methods)
    {
        blockStart("InnerClassDeclaration");
        
        attrPrint("modifiers", CModifier.toString(modifiers));
        attrPrint("class", ident);
        attrPrint("extends", superName);
        attrList("implements", interfaces);

        visitClassBody(decls, fields, methods, body);
        
        blockEnd();
    }
    

    /**
     * visits an interface declaration
     */
    public void visitInterfaceDeclaration(JInterfaceDeclaration self,
                                          int modifiers,
                                          String ident,
                                          CClassType[] interfaces,
                                          JPhylum[] body,
                                          JMethodDeclaration[] methods)
    {
        blockStart("InterfaceDeclaration");
        attrPrint("modifiers", String.valueOf(modifiers));
        attrPrint("name", ident);
        attrList("implements", interfaces);
        visitClassBody(new JTypeDeclaration[0], 
		       JFieldDeclaration.EMPTY(),
		       methods, body);
        blockEnd();
    }

    // ----------------------------------------------------------------------
    // METHODS AND FIELDS
    // ----------------------------------------------------------------------

    /**
     * visits a field declaration
     */
    public void visitFieldDeclaration(JFieldDeclaration self,
                                      int modifiers,
                                      CType type,
                                      String ident,
                                      JExpression expr)
    {
        blockStart("FieldDeclaration");
        attrPrint("modifiers", CModifier.toString(modifiers));
        attrPrint("type", type.toString());
        attrPrint("name", ident);
        if (expr != null) expr.accept(this);
        blockEnd();
    }

    /**
     * visits a method declaration
     */
    public void visitMethodDeclaration(JMethodDeclaration self,
                                       int modifiers,
                                       CType returnType,
                                       String ident,
                                       JFormalParameter[] parameters,
                                       CClassType[] exceptions,
                                       JBlock body)
    {
        blockStart("MethodDeclaration");
        attrPrint("modifiers", CModifier.toString(modifiers));
        attrPrint("returns", returnType.toString());
        attrPrint("name", ident);
        attrStart("parameters");
        for (int i = 0; i < parameters.length; i++)
            parameters[i].accept(this);
        attrEnd();
        attrList("throws", exceptions);
        if (body != null)
            body.accept(this);
        blockEnd();
    }
    
    /**
     * visits a method declaration
     */
    public void visitConstructorDeclaration(JConstructorDeclaration self,
                                            int modifiers,
                                            String ident,
                                            JFormalParameter[] parameters,
                                            CClassType[] exceptions,
                                            JConstructorBlock body)
    {
        blockStart("ConstructorDeclaration");
        attrPrint("modifiers", String.valueOf(modifiers));
        attrPrint("name", ident);
        attrStart("parameters");
        for (int i = 0; i < parameters.length; i++)
            parameters[i].accept(this);
        attrEnd();
        attrList("exceptions", exceptions);
        body.accept(this);
        blockEnd();
    }

    // ----------------------------------------------------------------------
    // STATEMENTS
    // ----------------------------------------------------------------------

    /**
     * visits a while statement
     */
    public void visitWhileStatement(JWhileStatement self,
                                    JExpression cond,
                                    JStatement body)
    {
        blockStart("WhileStatement");
        cond.accept(this);
        body.accept(this);
        blockEnd();
    }

    /**
   * visits a variable declaration statement
   */
    public void visitVariableDeclarationStatement
        (JVariableDeclarationStatement self,
         JVariableDefinition[] vars)
    {
        blockStart("VariableDeclarationStatement");
        for (int i = 0; i < vars.length; i++)
            vars[i].accept(this);
        blockEnd();
    }

    /**
     * visits a variable declaration statement
     */
    public void visitVariableDefinition(JVariableDefinition self,
                                        int modifiers,
                                        CType type,
                                        String ident,
                                        JExpression expr)
    {
        blockStart("VariableDefinition");
        attrPrint("modifiers", String.valueOf(modifiers));
        attrPrint("type", type.toString());
        attrPrint("name", ident);
        if (expr != null) expr.accept(this);
        blockEnd();
    }
    
    /**
     * visits a try-catch statement
     */
    public void visitTryCatchStatement(JTryCatchStatement self,
                                       JBlock tryClause,
                                       JCatchClause[] catchClauses)
    {
        blockStart("TryCatchStatement");
        tryClause.accept(this);
        for (int i = 0; i < catchClauses.length; i++)
            catchClauses[i].accept(this);
        blockEnd();
    }

    /**
     * visits a try-finally statement
     */
    public void visitTryFinallyStatement(JTryFinallyStatement self,
                                         JBlock tryClause,
                                         JBlock finallyClause)
    {
        blockStart("TryFinallyStatement");
        tryClause.accept(this);
        finallyClause.accept(this);
        blockEnd();
    }

    /**
     * visits a throw statement
     */
    public void visitThrowStatement(JThrowStatement self,
                                    JExpression expr)
    {
        blockStart("ThrowStatement");
        expr.accept(this);
        blockEnd();
    }

    /**
     * visits a synchronized statement
     */
    public void visitSynchronizedStatement(JSynchronizedStatement self,
                                           JExpression cond,
                                           JStatement body)
    {
        blockStart("SynchronizedStatement");
        cond.accept(this);
        body.accept(this);
        blockEnd();
    }

    /**
     * visits a switch statement
     */
    public void visitSwitchStatement(JSwitchStatement self,
                                     JExpression expr,
                                     JSwitchGroup[] body)
    {
        blockStart("SwitchStatement");
        expr.accept(this);
        for (int i = 0; i < body.length; i++)
            body[i].accept(this);
        blockEnd();
    }

    /**
     * visits a return statement
     */
    public void visitReturnStatement(JReturnStatement self,
                                     JExpression expr)
    {
        blockStart("Return");
        if (expr != null) expr.accept(this);
        blockEnd();
    }

    /**
     * visits a labeled statement
     */
    public void visitLabeledStatement(JLabeledStatement self,
                                      String label,
                                      JStatement stmt)
    {
        blockStart("LabeledStatement");
        attrPrint("label", label);
        stmt.accept(this);
        blockEnd();
    }

    /**
     * visits a if statement
     */
    public void visitIfStatement(JIfStatement self,
                                 JExpression cond,
                                 JStatement thenClause,
                                 JStatement elseClause)
    {
        blockStart("IfStatement");
        cond.accept(this);
        thenClause.accept(this);
        if (elseClause != null) elseClause.accept(this);
        blockEnd();
    }

    /**
   * visits a for statement
   */
    public void visitForStatement(JForStatement self,
                                  JStatement init,
                                  JExpression cond,
                                  JStatement incr,
                                  JStatement body)
    {
        blockStart("ForStatement");
        attrPrint("init", init);
        attrPrint("cond", cond);
        attrPrint("incr", incr);
        body.accept(this);
        blockEnd();
    }

    /**
   * visits a compound statement
   */
    public void visitCompoundStatement(JCompoundStatement self,
                                       JStatement[] body)
    {
        blockStart("CompoundStatement");
        for (int i = 0; i < body.length; i++)
            body[i].accept(this);
        blockEnd();
    }

    /**
     * visits an expression statement
     */
    public void visitExpressionStatement(JExpressionStatement self,
                                         JExpression expr)
    {
        blockStart("ExpressionStatement");
        expr.accept(this);
        blockEnd();
    }

    /**
     * visits an expression list statement
     */
    public void visitExpressionListStatement(JExpressionListStatement self,
                                             JExpression[] expr)
    {
        blockStart("ExpressionListStatement");
        for (int i = 0; i < expr.length; i++)
            expr[i].accept(this);
        blockEnd();
    }

    /**
   * visits a empty statement
   */
    public void visitEmptyStatement(JEmptyStatement self)
    {
        blockStart("EmptyStatement");
        blockEnd();
    }

    /**
   * visits a do statement
   */
    public void visitDoStatement(JDoStatement self,
                                 JExpression cond,
                                 JStatement body)
    {
        blockStart("DoStatement");
        attrPrint("cond", cond);
        body.accept(this);
        blockEnd();
    }

    /**
     * visits a continue statement
     */
    public void visitContinueStatement(JContinueStatement self,
                                       String label)
    {
        blockStart("ContinueStatement");
        attrPrint("label", label);
        blockEnd();
    }

    /**
     * visits a break statement
     */
    public void visitBreakStatement(JBreakStatement self,
                                    String label)
    {
        blockStart("BreakStatement");
        attrPrint("label", label);
        blockEnd();
    }

    /**
     * visits an expression statement
     */
    public void visitBlockStatement(JBlock self,
                                    JavaStyleComment[] comments)
    {
        blockStart("BlockStatement");
	for (ListIterator it = self.getStatementIterator(); it.hasNext(); ) {
            ((JStatement)it.next()).accept(this);
	}
        // comments
        blockEnd();
    }

    /**
     * visits a type declaration statement
     */
    public void visitTypeDeclarationStatement(JTypeDeclarationStatement self,
                                              JTypeDeclaration decl)
    {
        blockStart("TypeDeclarationStatement");
        decl.accept(this);
        blockEnd();
    }

    // ----------------------------------------------------------------------
    // EXPRESSION
    // ----------------------------------------------------------------------

    /**
     * visits an unary plus expression
     */
    public void visitUnaryPlusExpression(JUnaryExpression self,
                                         JExpression expr)
    {
        blockStart("UnaryPlusExpression");
        expr.accept(this);
        blockEnd();
    }

    /**
     * visits an unary minus expression
     */
    public void visitUnaryMinusExpression(JUnaryExpression self,
                                          JExpression expr)
    {
        blockStart("UnaryMinusExpression");
        expr.accept(this);
        blockEnd();
    }

    /**
     * visits a bitwise complement expression
     */
    public void visitBitwiseComplementExpression(JUnaryExpression self,
                                                 JExpression expr)
    {
        blockStart("BitwiseComplementExpression");
        expr.accept(this);
        blockEnd();
    }

    /**
     * visits a logical complement expression
     */
    public void visitLogicalComplementExpression(JUnaryExpression self,
                                                 JExpression expr)
    {
        blockStart("LogicalComplementExpression");
        expr.accept(this);
        blockEnd();
    }

    /**
     * visits a type name expression
     */
    public void visitTypeNameExpression(JTypeNameExpression self,
                                        CType type)
    {
        blockStart("TypeNameExpression");
        printData(' ');
        printData(type.toString());
        blockEnd();
    }

    /**
     * visits a this expression
     */
    public void visitThisExpression(JThisExpression self,
                                    JExpression prefix)
    {
        blockStart("ThisExpression");
        attrPrint("prefix", prefix);
        blockEnd();
    }

    /**
     * visits a super expression
     */
    public void visitSuperExpression(JSuperExpression self)
    {
        blockStart("SuperExpression");
        blockEnd();
    }

    /**
     * visits a shift expression
     */
    public void visitShiftExpression(JShiftExpression self,
                                     int oper,
                                     JExpression left,
                                     JExpression right)
    {
        blockStart("ShiftExpression");
        attrPrint("oper", String.valueOf(oper));
        attrPrint("left", left);
        attrPrint("right", right);
        blockEnd();
    }

    /**
     * visits a shift expressiona
     */
    public void visitRelationalExpression(JRelationalExpression self,
                                          int oper,
                                          JExpression left,
                                          JExpression right)
    {
        blockStart("RelationalExpression");
        attrPrint("oper", String.valueOf(oper));
        attrPrint("left", left);
        attrPrint("right", right);
        blockEnd();
    }

    /**
     * visits a prefix expression
     */
    public void visitPrefixExpression(JPrefixExpression self,
                                      int oper,
                                      JExpression expr)
    {
        blockStart("PrefixExpression");
        attrPrint("oper", String.valueOf(oper));
        expr.accept(this);
        blockEnd();
    }

    /**
     * visits a postfix expression
     */
    public void visitPostfixExpression(JPostfixExpression self,
                                       int oper,
                                       JExpression expr)
    {
        blockStart("PostfixExpression");
        attrPrint("oper", String.valueOf(oper));
        expr.accept(this);
        blockEnd();
    }

    /**
     * visits a parenthesed expression
     */
    public void visitParenthesedExpression(JParenthesedExpression self,
                                           JExpression expr)
    {
        // Not parenthesized?
        blockStart("ParenthesedExpression");
        expr.accept(this);
        blockEnd();
    }

    /**
     * Visits an unqualified anonymous class instance creation expression.
     */
    public void visitQualifiedAnonymousCreation
        (JQualifiedAnonymousCreation self,
         JExpression prefix,
         String ident,
         JExpression[] params,
         JClassDeclaration decl)
    {
        blockStart("QualifiedAnonymousCreation");
        attrPrint("prefix", prefix);
        attrPrint("name", ident);
        attrStart("params");
        for (int i = 0; i < params.length; i++)
            params[i].accept(this);
        attrEnd();
        attrPrint("decl", decl);
        blockEnd();
    }

    /**
     * Visits an unqualified instance creation expression.
     */
    public void visitQualifiedInstanceCreation(JQualifiedInstanceCreation self,
                                               JExpression prefix,
                                               String ident,
                                               JExpression[] params)
    {
        blockStart("QualifiedInstanceCreation");
        attrPrint("prefix", prefix);
        attrPrint("name", ident);
        attrStart("params");
        for (int i = 0; i < params.length; i++)
            params[i].accept(this);
        attrEnd();
        blockEnd();
    }
    
    /**
     * Visits an unqualified anonymous class instance creation expression.
     */
    public void visitUnqualifiedAnonymousCreation
        (JUnqualifiedAnonymousCreation self,
         CClassType type,
         JExpression[] params,
         JClassDeclaration decl)
    {
        blockStart("UnqualifiedAnonymousCreation");
        attrPrint("type", type.toString());
        attrStart("params");
        for (int i = 0; i < params.length; i++)
            params[i].accept(this);
        attrEnd();
        attrPrint("decl", decl);
        blockEnd();
    }

    /**
     * Visits an unqualified instance creation expression.
     */
    public void visitUnqualifiedInstanceCreation
        (JUnqualifiedInstanceCreation self,
         CClassType type,
         JExpression[] params)
    {
        blockStart("UnqualifiedInstanceCreation");
        attrPrint("type", type.toString());
        attrStart("params");
        for (int i = 0; i < params.length; i++)
            params[i].accept(this);
        attrEnd();
        blockEnd();
    }

    /**
     * visits an array allocator expression
     */
    public void visitNewArrayExpression(JNewArrayExpression self,
                                        CType type,
                                        JExpression[] dims,
                                        JArrayInitializer init)
    {
        blockStart("NewArrayExpression");
        attrPrint("type", type.toString());
        attrStart("dims");
        for (int i = 0; i < dims.length; i++) {
	    // could be null if you're doing something like "new int[10][]"
	    if (dims[i]!=null) {
		dims[i].accept(this);
	    }
	}
        attrEnd();
        attrPrint("init", init);
        blockEnd();
    }

    /**
     * visits a name expression
     */
    public void visitNameExpression(JNameExpression self,
                                    JExpression prefix,
                                    String ident)
    {
        blockStart("NameExpression");
        attrPrint("prefix", prefix);
        attrPrint("name", ident);
        blockEnd();
    }

    /**
     * visits an array allocator expression
     */
    public void visitBinaryExpression(JBinaryExpression self,
                                      String oper,
                                      JExpression left,
                                      JExpression right)
    {
        blockStart("BinaryExpression");
        printData(' ');
        printData(oper);
        left.accept(this);
        right.accept(this);
        blockEnd();
    }

    /**
     * visits a method call expression
     */
    public void visitMethodCallExpression(JMethodCallExpression self,
                                          JExpression prefix,
                                          String ident,
                                          JExpression[] args)
    {
        blockStart("MethodCallExpression");
        attrPrint("prefix", prefix);
        attrPrint("name", ident);
        attrStart("args");
        for (int i = 0; i < args.length; i++)
            args[i].accept(this);
        attrEnd();
        blockEnd();
    }
    
    /**
   * visits a local variable expression
   */
    public void visitLocalVariableExpression(JLocalVariableExpression self,
                                             String ident)
    {
        blockStart("LocalVariableExpression");
        printData(' ');
        printData(ident);
        blockEnd();
    }

    /**
   * visits an instanceof expression
   */
    public void visitInstanceofExpression(JInstanceofExpression self,
                                          JExpression expr,
                                          CType dest)
    {
        blockStart("InstanceOfExpression");
        attrPrint("type", dest.toString());
        expr.accept(this);
        blockEnd();
    }

    /**
     * visits an equality expression
     */
    public void visitEqualityExpression(JEqualityExpression self,
                                        boolean equal,
                                        JExpression left,
                                        JExpression right)
    {
        blockStart("EqualityExpression");
        attrPrint("equal", String.valueOf(equal));
        attrPrint("left", left);
        attrPrint("right", right);
        blockEnd();
    }

    /**
     * visits a conditional expression
     */
    public void visitConditionalExpression(JConditionalExpression self,
                                           JExpression cond,
                                           JExpression left,
                                           JExpression right)
    {
        blockStart("ConditionalExpression");
        attrPrint("cond", cond);
        attrPrint("left", left);
        attrPrint("right", right);
        blockEnd();
    }

    /**
     * visits a compound expression
     */
    public void visitCompoundAssignmentExpression
        (JCompoundAssignmentExpression self,
         int oper,
         JExpression left,
         JExpression right)
    {
        blockStart("CompoundAssignmentExpression");
        attrPrint("oper", String.valueOf(oper));
        attrPrint("left", left);
        attrPrint("right", right);
        blockEnd();
    }

    /**
     * visits a field expression
     */
    public void visitFieldExpression(JFieldAccessExpression self,
                                     JExpression left,
                                     String ident)
    {
        blockStart("FieldExpression");
        attrPrint("left", left);
        attrPrint("name", ident);
        blockEnd();
    }

    /**
   * visits a class expression
   */
    public void visitClassExpression(JClassExpression self,
                                     CType type)
    {
        blockStart("ClassExpression");
        printData(' ');
        printData(type.toString());
        blockEnd();
    }

    /**
     * visits a cast expression
     */
    public void visitCastExpression(JCastExpression self,
                                    JExpression expr,
                                    CType type)
    {
        blockStart("CastExpression");
        attrPrint("type", type.toString());
        expr.accept(this);
        blockEnd();
    }

    /**
     * visits a cast expression
     */
    public void visitUnaryPromoteExpression(JUnaryPromote self,
                                            JExpression expr,
                                            CType type)
    {
        blockStart("UnaryPromoteExpression");
        attrPrint("type", type.toString());
        expr.accept(this);
        blockEnd();
    }

    /**
     * visits a compound assignment expression
     */
    public void visitBitwiseExpression(JBitwiseExpression self,
                                       int oper,
                                       JExpression left,
                                       JExpression right)
    {
        blockStart("BitwiseExpression");
        attrPrint("oper", String.valueOf(oper));
        attrPrint("left", left);
        attrPrint("right", right);
        blockEnd();
    }
    
    /**
     * visits an assignment expression
     */
    public void visitAssignmentExpression(JAssignmentExpression self,
                                          JExpression left,
                                          JExpression right)
    {
        blockStart("AssignmentExpression");
        left.accept(this);
        right.accept(this);
        blockEnd();
    }

    /**
     * visits an array length expression
     */
    public void visitArrayLengthExpression(JArrayLengthExpression self,
                                           JExpression prefix)
    {
        blockStart("ArrayLengthExpression");
        prefix.accept(this);
        blockEnd();
    }

    /**
     * visits an array length expression
     */
    public void visitArrayAccessExpression(JArrayAccessExpression self,
                                           JExpression prefix,
                                           JExpression accessor)
    {
        blockStart("ArrayAccessExpression");
        attrPrint("prefix", prefix);
        attrPrint("accessor", accessor);
        blockEnd();
    }

    /**
     * visits an array length expression
     */
    public void visitComments(JavaStyleComment[] comments) { }

    /**
     * visits an array length expression
     */
    public void visitComment(JavaStyleComment comment) { }

    /**
     * visits an array length expression
     */
    public void visitJavadoc(JavadocComment comment) { }

    // ----------------------------------------------------------------------
    // OTHERS
    // ----------------------------------------------------------------------

    /**
     * visits an array length expression
     */
    public void visitSwitchLabel(JSwitchLabel self,
                                 JExpression expr)
    {
        blockStart("SwitchLabel");
        expr.accept(this);
        blockEnd();
    }

    /**
     * visits an array length expression
     */
    public void visitSwitchGroup(JSwitchGroup self,
                                 JSwitchLabel[] labels,
                                 JStatement[] stmts)
    {
        blockStart("SwitchGroup");
        attrStart("labels");
        for (int i = 0; i < labels.length; i++)
            labels[i].accept(this);
        attrEnd();
        attrStart("stmts");
        for (int i = 0; i < stmts.length; i++)
            stmts[i].accept(this);
        attrEnd();
        blockEnd();
    }

    /**
     * visits an array length expression
     */
    public void visitCatchClause(JCatchClause self,
                                 JFormalParameter exception,
                                 JBlock body)
    {
        blockStart("CatchClause");
        exception.accept(this);
        body.accept(this);
        blockEnd();
    }

    /**
     * visits an array length expression
     */
    public void visitFormalParameters(JFormalParameter self,
                                      boolean isFinal,
                                      CType type,
                                      String ident)
    {
        blockStart("FormalParameter");
        if (isFinal) attrPrint("final", "true");
        attrPrint("type", type.toString());
        attrPrint("name", ident);
        blockEnd();
    }

    /**
     * visits an array length expression
     */
    public void visitConstructorCall(JConstructorCall self,
                                     boolean functorIsThis,
                                     JExpression[] params)
    {
        blockStart("ConstructorCall");
        attrPrint("this", String.valueOf(functorIsThis));
        attrStart("params");
        for (int i = 0; i < params.length; i++)
            params[i].accept(this);
        attrEnd();
        blockEnd();
    }

    /**
     * visits an array initializer expression
     */
    public void visitArrayInitializer(JArrayInitializer self,
                                      JExpression[] elems)
    {
        blockStart("ArrayInitializer");
        for (int i = 0; i < elems.length; i++)
            elems[i].accept(this);
        blockEnd();
    }

    /**
     * visits a boolean literal
     */
    public void visitBooleanLiteral(boolean value)
    {
        blockStart("BooleanLiteral");
        if (value)
            printData(" true");
        else
            printData(" false");
        blockEnd();
    }

    /**
     * visits a byte literal
     */
    public void visitByteLiteral(byte value)
    {
        blockStart("ByteLiteral");
        printData(' ');
        printData(String.valueOf(value));
        blockEnd();
    }

    /**
     * visits a character literal
     */
    public void visitCharLiteral(char value)
    {
        blockStart("CharLiteral");
        printData(' ');
        printData(value);
        blockEnd();
    }

    /**
     * visits a double literal
     */
    public void visitDoubleLiteral(double value)
    {
        blockStart("DoubleLiteral");
        printData(' ');
        printData(String.valueOf(value));
        blockEnd();
    }

    /**
     * visits a float literal
     */
    public void visitFloatLiteral(float value)
    {
        blockStart("FloatLiteral");
        printData(' ');
        printData(String.valueOf(value));
        blockEnd();
    }

    /**
     * visits a int literal
     */
    public void visitIntLiteral(int value)
    {
        blockStart("IntLiteral");
        printData(' ');
        printData(String.valueOf(value));
        blockEnd();
    }

    /**
     * visits a long literal
     */
    public void visitLongLiteral(long value)
    {
        blockStart("LongLiteral");
        printData(' ');
        printData(String.valueOf(value));
        blockEnd();
    }

    /**
     * visits a short literal
     */
    public void visitShortLiteral(short value)
    {
        blockStart("ShortLiteral");
        printData(' ');
        printData(String.valueOf(value));
        blockEnd();
    }

    /**
     * visits a string literal
     */
    public void visitStringLiteral(String value)
    {
        blockStart("StringLiteral");
        printData(' ');
        printData('\"');
        printData(value);
        printData('\"');
        blockEnd();
    }

    /**
     * visits a null literal
     */
    public void visitNullLiteral()
    {
        blockStart("NullLiteral");
        blockEnd();
    }

    /**
     * visits a package name declaration
     */
    public void visitPackageName(String name)
    {
        blockStart("PackageName");
        printData(" ");
        printData(name);
        blockEnd();
    }

    /**
     * visits a package import declaration
     */
    public void visitPackageImport(String name)
    {
        blockStart("PackageImport");
        printData(" ");
        printData(name);
        blockEnd();
    }

    /**
     * visits a class import declaration
     */
    public void visitClassImport(String name)
    {
        blockStart("ClassImport");
        printData(" ");
        printData(name);
        blockEnd();
    }

    /**
     * SIR NODES
     */

    /**
     * Visits an init statement.
     */
    public void visitInitStatement(SIRInitStatement self,
				   SIRStream target) {
	blockStart("SIRInitStatement");
	attrPrint("target ", target.toString());
	attrStart("args");
	List args = self.getArgs();
	for (int i=0; i<args.size(); i++) {
	    ((JExpression)args.get(i)).accept(this);
	}
	attrEnd();
	blockEnd();
    }

    /**
     * Visits an interface table.
     */
    public void visitInterfaceTable(SIRInterfaceTable self)
    {
        blockStart("SIRInterfaceTable");
        attrPrint("interface", self.getIface().getIdent());
        attrStart("methods");
        for (int i = 0; i < self.getMethods().length; i++)
            printData(self.getMethods()[i].getName());
        attrEnd();
        blockEnd();
    }

    /**
     * Visits a latency.
     */
    public void visitLatency(SIRLatency self) {
	blockStart("SIRLatency");
	if (self==SIRLatency.BEST_EFFORT) {
	    printData("BEST EFFORT");
	}
	blockEnd();
    }


    /**
     * Visits a max latency.
     */
    public void visitLatencyMax(SIRLatencyMax self) {
	blockStart("SIRLatencyMax");
	attrPrint("max", String.valueOf(self.getMax()));
	blockEnd();
    }


    /**
     * Visits a latency range.
     */
    public void visitLatencyRange(SIRLatencyRange self) {
	blockStart("SIRLatencyRange");
	attrPrint("max", String.valueOf(self.getMax()));
	attrPrint("min", String.valueOf(self.getMin()));
	blockEnd();
    }


    /**
     * Visits a latency set.
     */
    public void visitLatencySet(SIRLatencySet self) {
	blockStart("SIRLatencySet");
	Utils.fail("Printing list of latencies not implemented yet.");
	blockEnd();
    }


    /**
     * Visits a message statement.
     */
    public void visitMessageStatement(SIRMessageStatement self,
				      JExpression portal,
                                      String iname,
                                      String ident,
				      JExpression[] args,
				      SIRLatency latency) {
	blockStart("SIRMessageStatement");
	attrStart("portal");
	portal.accept(this);
	attrEnd();
        attrPrint("iname", iname);
        attrPrint("ident", ident);
	attrStart("args");
	for (int i = 0; i < args.length; i++)
	    args[i].accept(this);
	attrEnd();
	attrStart("latency");
	latency.accept(this);
	attrEnd();
	blockEnd();
    }


    /**
     * Visits a peek expression.
     */
    public void visitPeekExpression(SIRPeekExpression self,
                                    CType tapeType,
			     JExpression arg) {
	blockStart("SIRPeekExpression");
	attrStart("arg");
	arg.accept(this);
	attrEnd();
	blockEnd();
    }


    /**
     * Visits a pop expression.
     */
    public void visitPopExpression(SIRPopExpression self,
                                   CType tapeType) {
	blockStart("SIRPopExpression");
	blockEnd();
    }

    /**
     * Visits a message-receiving portal.
     */
    public void visitPortal(SIRPortal self)
    {
        blockStart("SIRPortal");
        blockEnd();
    }

    /**
     * Visits a print statement.
     */
    public void visitPrintStatement(SIRPrintStatement self,
			     JExpression arg) {
	blockStart("SIRPrintStatement");
	attrStart("arg");
	arg.accept(this);
	attrEnd();
	blockEnd();
    }

    public void visitCreatePortalExpression(SIRCreatePortal self) {
	blockStart("SIRCreatePortalExpression");
	blockEnd();
    }
    

    /**
     * Visits a push expression.
     */
    public void visitPushExpression(SIRPushExpression self,
                                    CType tapeType,
			     JExpression arg) {
	blockStart("SIRPushExpression");
	attrStart("arg");
	arg.accept(this);
	attrEnd();
	blockEnd();
    }

    /**
     * Visit a phase-invocation statement.
     */
    public void visitPhaseInvocation(SIRPhaseInvocation self,
                                     JMethodCallExpression call,
                                     JExpression peek,
                                     JExpression pop,
                                     JExpression push)
    {
        blockStart("SIRPhaseInvocation");
        attrPrint("call", call);
        attrPrint("peek", peek);
        attrPrint("pop", pop);
        attrPrint("push", push);
        blockEnd();
    }

    /**
     * Visits a register-receiver statement.
     */
    public void visitRegReceiverStatement(SIRRegReceiverStatement self,
					  JExpression portal,
					  SIRStream receiver,
					  JMethodDeclaration[] methods) {
	blockStart("SIRRegReceiveStatement");
	attrStart("portal");
	portal.accept(this);
	attrEnd();
	attrStart("methods");
	for (int i = 0; i < methods.length; i++)
	    methods[i].accept(this);
	attrEnd();
	blockEnd();
    }


    /**
     * Visits a register-sender statement.
     */
    public void visitRegSenderStatement(SIRRegSenderStatement self,
				 String portal,
				 SIRLatency latency) {
	Utils.fail("Printing reg. sender statements unimplemented");
    }


    /**
     * LIR NODES.
     */

    /**
     * Visits a function pointer.
     */
    public void visitFunctionPointer(LIRFunctionPointer self,
				     String name) {
	blockStart("LIRFunctionPointer");
	attrPrint("name", name);
	blockEnd();
    }

    
    /**
     * Visits an LIR node.
     */
    public void visitNode(LIRNode self) {
	blockStart("LIRNode");
	blockEnd();
    }

    /**
     * Visits an LIR register-receiver statement.
     */
    public void visitRegisterReceiver(LIRRegisterReceiver self,
                                      JExpression streamContext,
                                      SIRPortal portal,
                                      String childName,
                                      SIRInterfaceTable itable) {
        blockStart("LIRRegisterReceiver");
        attrStart("parentContext");
        streamContext.accept(this);
        attrEnd();
        attrStart("portal");
        portal.accept(this);
        attrEnd();
        attrPrint("childName", childName);
        attrStart("itable");
        itable.accept(this);
        attrEnd();
        blockEnd();
    }

    /**
     * Visits a child registration node.
     */
    public void visitSetChild(LIRSetChild self,
			      JExpression streamContext,
			      String childType,
			      String childName) {
	blockStart("LIRSetChild");
	attrStart("parentContext");
	streamContext.accept(this);
	attrEnd();
	attrPrint("childType", childType);
	attrPrint("childName", childName);
	blockEnd();
    }

    
    /**
     * Visits a decoder registration node.
     */
    public void visitSetDecode(LIRSetDecode self,
                        JExpression streamContext,
                        LIRFunctionPointer fp) {
	blockStart("LIRSetDecode");
	attrStart("streamContext");
	streamContext.accept(this);
	attrEnd();
	attrStart("decode_function");
	fp.accept(this);
	attrEnd();
	blockEnd();
    }


    /**
     * Visit a feedback loop delay node.
     */
    public void visitSetDelay(LIRSetDelay self,
                              JExpression data,
                              JExpression streamContext,
                              int delay,
                              CType type,
                              LIRFunctionPointer fp)
    {
        blockStart("LIRSetDelay");
        attrPrint("data", data);
        attrPrint("streamContext", streamContext);
        attrPrint("delay", String.valueOf(delay));
        attrPrint("type", type.toString());
        attrStart("fp");
        fp.accept(this);
        attrEnd();
        blockEnd();
    }

    
    /**
     * Visits an encoder registration node.
     */
    public void visitSetEncode(LIRSetEncode self,
			       JExpression streamContext,
			       LIRFunctionPointer fp) {
	blockStart("LIRSetEncode");
	attrStart("streamContext");
	streamContext.accept(this);
	attrEnd();
	attrStart("encode_function");
	fp.accept(this);
	attrEnd();
	blockEnd();
    }

    /**
     * Visits a joiner-setting node.
     */
    public void visitSetJoiner(LIRSetJoiner self,
                            JExpression streamContext,
                            SIRJoinType type,
                            int ways,
                            int[] weights) {
        blockStart("LIRSetJoiner");
        attrStart("streamContext");
        streamContext.accept(this);
        attrEnd();
        attrPrint("type", type.toString());
        attrPrint("ways", String.valueOf(ways));
        if (weights != null)
        {
            attrStart("weights");
            for (int i = 0; i < ways; i++)
                printData(String.valueOf(weights[i]));
            attrEnd();
        }
        blockEnd();
    }
    
    /**
     * Visits a peek-rate-setting node.
     */
    public void visitSetPeek(LIRSetPeek self,
			     JExpression streamContext,
			     int peek) {
	blockStart("LIRSetPeek");
	attrStart("streamContext");
	streamContext.accept(this);
	attrEnd();
	attrPrint("peek_count", String.valueOf(peek));
	blockEnd();
    }

    
    /**
     * Visits a pop-rate-setting node.
     */
    public void visitSetPop(LIRSetPop self,
                     JExpression streamContext,
                     int pop) {
	blockStart("LIRSetPop");
	attrStart("streamContext");
	streamContext.accept(this);
	attrEnd();
	attrPrint("pop_count", String.valueOf(pop));
	blockEnd();
    }

    
    /**
     * Visits a push-rate-setting node.
     */
    public void visitSetPush(LIRSetPush self,
                      JExpression streamContext,
                      int push) {
	blockStart("LIRSetPush");
	attrStart("streamContext");
	streamContext.accept(this);
	attrEnd();
	attrPrint("push_count", String.valueOf(push));
	blockEnd();
    }

    /**
     * Visits a splitter-setting node.
     */
    public void visitSetSplitter(LIRSetSplitter self,
                                 JExpression streamContext,
                                 SIRSplitType type,
                                 int ways,
                                 int[] weights) {
        blockStart("LIRSetSplitter");
        attrStart("streamContext");
        streamContext.accept(this);
        attrEnd();
        attrPrint("type", type.toString());
        attrPrint("ways", String.valueOf(ways));
        if (weights != null)
        {
            attrStart("weights");
            for (int i = 0; i < ways; i++)
                printData(String.valueOf(weights[i]));
            attrEnd();
        }
        blockEnd();
    }
    

    /**
     * Visits a stream-type-setting node.
     */
    public void visitSetStreamType(LIRSetStreamType self,
                            JExpression streamContext,
                            LIRStreamType streamType) {
	blockStart("LIRSetStreamType");
	attrStart("streamContext");
	streamContext.accept(this);
	attrEnd();
	attrPrint("stream_type", streamType.toString());
	blockEnd();
    }

    
    /**
     * Visits a work-function-setting node.
     */
    public void visitSetWork(LIRSetWork self,
                      JExpression streamContext,
                      LIRFunctionPointer fn) {
	blockStart("LIRSetWork");
	attrStart("streamContext");
	streamContext.accept(this);
	attrEnd();
	attrStart("work_function");
	fn.accept(this);
	attrEnd();
	blockEnd();
    }

    /**
     * Visits a tape-setter.
     */
    public void visitSetTape(LIRSetTape self,
			     JExpression streamContext,
			     JExpression srcStruct,
			     JExpression dstStruct,
			     CType type,
			     int size) {
	blockStart("LIRSetTape");
	attrStart("streamContext");
	streamContext.accept(this);
	attrEnd();
	attrStart("srcStruct");
	srcStruct.accept(this);
	attrEnd();
	attrStart("dstStruct");
	dstStruct.accept(this);
	attrEnd();
	attrPrint("type", type.toString());
	attrPrint("size", String.valueOf(size));
	blockEnd();
    }

    /**
     * Visits a main function contents.
     */
    public void visitMainFunction(LIRMainFunction self,
				  String typeName,
				  LIRFunctionPointer init,
				  List initStatements) {
	blockStart("LIRMainFunction");
	attrPrint("typeName", typeName);
	attrStart("init");
	init.accept(this);
	attrEnd();
	printData("init statements:");
	for (ListIterator it = initStatements.listIterator(); it.hasNext(); ) {
	    ((JStatement)it.next()).accept(this);
	}
	blockEnd();
    }

    /**
     * Visits a set body of feedback loop.
     */
    public void visitSetBodyOfFeedback(LIRSetBodyOfFeedback self,
				       JExpression streamContext,
                                       JExpression childContext,
				       CType inputType,
				       CType outputType,
				       int inputSize,
				       int outputSize) {
	blockStart("LIRSetBodyOfFeedback");
	attrStart("streamContext");
	streamContext.accept(this);
	attrEnd();
        attrStart("childContext");
        childContext.accept(this);
        attrEnd();
	attrPrint("input type", inputType.toString());
	attrPrint("output type", outputType.toString());
	attrPrint("input size", String.valueOf(inputSize));
	attrPrint("output size", String.valueOf(outputSize));
	blockEnd();
    }

    /**
     * Visits a set loop of feedback loop.
     */
    public void visitSetLoopOfFeedback(LIRSetLoopOfFeedback self,
				       JExpression streamContext,
                                       JExpression childContext,
				       CType inputType,
				       CType outputType,
				       int inputSize,
				       int outputSize) {
	blockStart("LIRSetLoopOfFeedback");
	attrStart("streamContext");
	streamContext.accept(this);
	attrEnd();
        attrStart("childContext");
        childContext.accept(this);
        attrEnd();
	attrPrint("input type", inputType.toString());
	attrPrint("output type", outputType.toString());
	attrPrint("input size", String.valueOf(inputSize));
	attrPrint("output size", String.valueOf(outputSize));
	blockEnd();
    }


    /**
     * Visits a file reader.
     */
    public void visitFileReader(LIRFileReader self) {
	blockStart("LIRFileReader");
	attrStart("streamContext");
	self.getStreamContext().accept(this);
	attrEnd();
	attrPrint("file name", self.getFileName());
	blockEnd();
    }
    
    /**
     * Visits a file writer.
     */
    public void visitFileWriter(LIRFileWriter self) {
	blockStart("LIRFileWriter");
	attrStart("streamContext");
	self.getStreamContext().accept(this);
	attrEnd();
	attrPrint("file name", self.getFileName());
	blockEnd();
    }
    
    /**
     * Visits an identity creator.
     */
    public void visitIdentity(LIRIdentity self) {
        blockStart("LIRIdentity");
        attrStart("streamContext");
        self.getStreamContext().accept(this);
        attrEnd();
        blockEnd();
    }
    
    /**
     * Visits a set a parallel stream.
     */
    public void visitSetParallelStream(LIRSetParallelStream self,
				       JExpression streamContext,
                                       JExpression childContext,
				       int position,
				       CType inputType,
				       CType outputType,
				       int inputSize,
				       int outputSize) {
	blockStart("LIRSetParallelStream");
	attrStart("streamContext");
	streamContext.accept(this);
	attrEnd();
        attrStart("childContext");
        childContext.accept(this);
        attrEnd();
	attrPrint("position", String.valueOf(position));
	attrPrint("input type", inputType.toString());
	attrPrint("output type", outputType.toString());
	attrPrint("input size", String.valueOf(inputSize));
	attrPrint("output size", String.valueOf(outputSize));
	blockEnd();
    }

    /**
     * Visits a work function entry.
     */
    public void visitWorkEntry(LIRWorkEntry self)
    {
        blockStart("LIRWorkEntry");
        attrStart("streamContext");
        self.getStreamContext().accept(this);
        attrEnd();
        blockEnd();
    }

    /**
     * Visits a work function exit.
     */
    public void visitWorkExit(LIRWorkExit self)
    {
        blockStart("LIRWorkExit");
        attrStart("streamContext");
        self.getStreamContext().accept(this);
        attrEnd();
        blockEnd();
    }
}
