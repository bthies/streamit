/*
 * LIRToC.java: convert StreaMIT low IR to C
 * $Id: LIRToC.java,v 1.85 2003-10-04 02:26:04 jasperln Exp $
 */

package at.dms.kjc.lir;

import java.io.*;
import java.util.Iterator;
import java.util.StringTokenizer;
import java.util.List;
import at.dms.util.InconsistencyException;

import at.dms.kjc.sir.lowering.LoweringConstants;
import at.dms.kjc.sir.*;
import at.dms.kjc.*;
import at.dms.compiler.*;

public class LIRToC
    extends at.dms.util.Utils
    implements Constants, SLIRVisitor
{
    //Needed to pass info from assignment to visitNewArray
    JExpression lastLeft;

    //Name of the "this" parameter to init and work functions
    public static final String THIS_NAME = LoweringConstants.STATE_PARAM_NAME;
    public static final String CONTEXT_NAME =
        LoweringConstants.CONTEXT_VAR_NAME;
    public static final String THIS_CONTEXT_NAME =
        THIS_NAME + "->" + CONTEXT_NAME;

    // ----------------------------------------------------------------------
    // CONSTRUCTORS
    // ----------------------------------------------------------------------

    /**
     * construct a pretty printer object for java code
     */
    private LIRToC() {
        this.str = new StringWriter();
        this.p = new TabbedPrintWriter(str);
    }

    /**
     * Generates code for <flatClass> and sends to System.out.
     */
    public static void generateCode(JClassDeclaration flat) {
	System.out.println("*/");	
	System.out.println("#include \"streamit.h\"");
	System.out.println("#include <stdio.h>");
	System.out.println("#include <stdlib.h>");
	System.out.println("#include <math.h>");
	printAtlasInterface();
	LIRToC l2c = new LIRToC(new TabbedPrintWriter(new PrintWriter(System.out)));
	flat.accept(l2c);
	l2c.close();
    }

    // include literal ATLAS interface if appropriate
    private static void printAtlasInterface() {
	if (KjcOptions.atlas) {
	    // copy this over instead of doing an include reference so
	    // that 1) the generated C file can move around, wherever
	    // there's an ATLAS install, and 2) atlas-interface.c is short
	    try {
		String filename = (getEnvironmentVariable("STREAMIT_HOME") +
				   File.separator + "include" + 
				   File.separator + "atlas" + 
				   File.separator + "atlas-interface.c");
		System.out.println("\n/* Starting include of file " + filename + " ---------- */\n");
		BufferedReader br = new BufferedReader(new FileReader(filename));
		String line;
		while ((line = br.readLine()) != null) {
		    System.out.println(line);
		}
		br.close();
		System.out.println("\n/* Done with include of " + filename + " ---------- */");
	    } catch (IOException e) {
		e.printStackTrace();
		fail("IOException looking for atlas-interface.c");
	    }
	}
    }

    /**
     * construct a pretty printer object for java code
     * @param	fileName		the file into the code is generated
     */
    private LIRToC(TabbedPrintWriter p) {
        this.p = p;
        this.str = null;
        this.pos = 0;
    }

    /**
     * Close the stream at the end
     */
    public void close() {
        p.close();
    }

    public void setPos(int pos) {
        this.pos = pos;
    }

    public String getString() {
        if (str != null)
            return str.toString();
        else
            return null;
    }

    // ----------------------------------------------------------------------
    // TYPE DECLARATION
    // ----------------------------------------------------------------------

    /**
     * prints a compilation unit
     */
    public void visitCompilationUnit(JCompilationUnit self,
                                     JPackageName packageName,
                                     JPackageImport[] importedPackages,
                                     JClassImport[] importedClasses,
                                     JTypeDeclaration[] typeDeclarations) {
        if (packageName.getName().length() > 0) {
            packageName.accept(this);
            if (importedPackages.length + importedClasses.length > 0) {
                newLine();
            }
        }

        for (int i = 0; i < importedPackages.length ; i++) {
            if (!importedPackages[i].getName().equals("java/lang")) {
                importedPackages[i].accept(this);
                newLine();
            }
        }

        for (int i = 0; i < importedClasses.length ; i++) {
            importedClasses[i].accept(this);
            newLine();
        }

        for (int i = 0; i < typeDeclarations.length ; i++) {
            newLine();
            typeDeclarations[i].accept(this);
            newLine();
        }
    }

    // ----------------------------------------------------------------------
    // TYPE DECLARATION
    // ----------------------------------------------------------------------

    /**
     * prints a class declaration
     */
    public void visitClassDeclaration(JClassDeclaration self,
                                      int modifiers,
                                      String ident,
                                      String superName,
                                      CClassType[] interfaces,
                                      JPhylum[] body,
                                      JFieldDeclaration[] fields,
                                      JMethodDeclaration[] methods,
                                      JTypeDeclaration[] decls) {
        LIRToC that = new LIRToC(this.p);
        that.className = ident;
        that.isStruct = ((modifiers & ACC_STATIC) == ACC_STATIC);
        that.visitClassBody(decls, fields, methods, body);
    }

    /**
     *
     */
    public void visitClassBody(JTypeDeclaration[] decls,
                               JFieldDeclaration[] fields,
                               JMethodDeclaration[] methods,
                               JPhylum[] body) {
        // Visit type declarations that happen to be for StreamIt
        // structure types first.
        for (int i = 0; i < decls.length ; i++) {
            if (decls[i] instanceof JClassDeclaration &&
                (decls[i].getModifiers() & ACC_STATIC) == ACC_STATIC)
                decls[i].accept(this);
        }        
        for (int i = 0; i < decls.length ; i++) {
            if (!(decls[i] instanceof JClassDeclaration &&
                  (decls[i].getModifiers() & ACC_STATIC) == ACC_STATIC))
            decls[i].accept(this);
        }
        if (body != null) {
            for (int i = 0; i < body.length ; i++) {
                if (!(body[i] instanceof JFieldDeclaration)) {
                    body[i].accept(this);
                }
            }
        }

        newLine();
        print("typedef struct " + className + " {");
        pos += TAB_SIZE;
        if (body != null) {
            for (int i = 0; i < body.length ; i++) {
                if (body[i] instanceof JFieldDeclaration &&
                    !isFieldInterfaceTable((JFieldDeclaration)body[i])) {
                    body[i].accept(this);
                }
            }
        }
        for (int i = 0; i < fields.length; i++) {
            if (!isFieldInterfaceTable(fields[i]))
                fields[i].accept(this);
        }    

        pos -= TAB_SIZE;
        newLine();
        if (isStruct)
            print("} " + className + ";");
        else
            print("} _" + className + ", *" + className + ";");

        // Print function prototypes for each of the methods.
        declOnly = true;
        for (int i = 0; i < methods.length; i++) {
            methods[i].accept(this);
        }

        // Print any interface tables there might be.
        if (body != null) {
            for (int i = 0; i < body.length ; i++) {
                if (body[i] instanceof JFieldDeclaration &&
                    isFieldInterfaceTable((JFieldDeclaration)body[i])) {
                    expandInterfaceTable((JFieldDeclaration)body[i]);
                }
            }
        }
        for (int i = 0; i < fields.length; i++) {
            if (isFieldInterfaceTable(fields[i])) {
                expandInterfaceTable(fields[i]);
            }
        }        

        declOnly = false;
        for (int i = 0; i < methods.length ; i++) {
            methods[i].accept(this);
        }
    }

    /**
     * Checks to see whether or not a field declaration is initialized
     * to an SIRInterfaceTable.
     */
    private boolean isFieldInterfaceTable(JFieldDeclaration field)
    {
        JVariableDefinition vardef = field.getVariable();
        if (!vardef.hasInitializer()) return false;
        JExpression init = vardef.getValue();
        return (init instanceof SIRInterfaceTable);
    }

    /**
     * Prints the functions needed for an interface table.  This
     * writes out the actual code for the receiver wrapper; it
     * unpackages the structure argument and calls the specified
     * function with the correct parameters.
     */
    private void expandInterfaceTable(JFieldDeclaration field)
    {
        JVariableDefinition vardef = field.getVariable();
        JExpression init = vardef.getValue();
        SIRInterfaceTable self = (SIRInterfaceTable)init;
        CClassType iface = self.getIface();
        CMethod[] imethods = iface.getCClass().getMethods();
        String iname = iface.getIdent();
        JMethodDeclaration[] methods = self.getMethods();
        for (int i = 0; i < methods.length; i++)
        {
            newLine();
            print("void " + iname + "_" + methods[i].getName() +
                  "(void *" + THIS_NAME + ", void *params)");
            newLine();
            print("{");
            pos += TAB_SIZE;
            newLine();
            // The method matches the corresponding method in the
            // interface.
            CMethod im = imethods[i];
            String imName = iname + "_" + im.getIdent();
            // Cast the parameters struct...
            print(imName + "_params q = params;");
            // Call the actual function.
            newLine();
            print(methods[i].getName() + "(" + THIS_NAME);
            for (int j = 0; j < im.getParameters().length; j++)
                print(", q->p" + j);
            print(");");
            pos -= TAB_SIZE;
            newLine();
            print("}");
        }
        // Print the actual variable definition, too.  We can
        // just print the left-hand side explicitly and let the
        // visitor deal with the right.  Using the visitor for
        // the whole thing gets the wrong type (void).
        newLine();
        print("message_fn " + vardef.getIdent() + "[] = ");
        self.accept(this);
        print(";");
        // While we're at it, save the variable definition in the
        // interface table structure.
        self.setVarDecl(vardef);
    }

    /**
     * prints a class declaration
     */
    public void visitInnerClassDeclaration(JClassDeclaration self,
                                           int modifiers,
                                           String ident,
                                           String superName,
                                           CClassType[] interfaces,
                                           JTypeDeclaration[] decls,
                                           JPhylum[] body,
                                           JFieldDeclaration[] fields,
                                           JMethodDeclaration[] methods) {
        print(" {");
        pos += TAB_SIZE;
        for (int i = 0; i < decls.length ; i++) {
            decls[i].accept(this);
        }
        for (int i = 0; i < fields.length; i++) {
            fields[i].accept(this);
        }
        for (int i = 0; i < methods.length ; i++) {
            methods[i].accept(this);
        }
        for (int i = 0; i < body.length ; i++) {
            body[i].accept(this);
        }
        pos -= TAB_SIZE;
        newLine();
        print("}");
    }

    /**
     * prints an interface declaration
     */
    public void visitInterfaceDeclaration(JInterfaceDeclaration self,
                                          int modifiers,
                                          String ident,
                                          CClassType[] interfaces,
                                          JPhylum[] body,
                                          JMethodDeclaration[] methods) {
        /* If an interface declaration gets through this far, it means
         * that there is a portal interface that can have messages sent
         * through it.  We can ignore everything about the interface
         * but the methods.  For each method, we need to generate a
         * parameter structure and a correct send_message() wrapper. */
        for (int i = 0; i < methods.length; i++) {
            CMethod mth = methods[i].getMethod();
            CType[] params = mth.getParameters();
            String name = ident + "_" + methods[i].getName();
            String pname = name + "_params";

            // Print the parameter structure.
            newLine();
            print("typedef struct " + pname + " {");
            pos += TAB_SIZE;
            for (int j = 0; j < params.length; j++) {
                newLine();
                print(params[j]);
                print(" p" + j + ";");
            }
            pos -= TAB_SIZE;
            newLine();
            print("} _" + pname + ", *" + pname + ";");

            // And now print a wrapper for send_message().
            newLine();
            print("void send_" + name + "(portal *p, latency l");
            for (int j = 0; j < params.length; j++) {
                print(", ");
                print(params[j]);
                print(" p" + j);
            }
            print(") {");
            pos += TAB_SIZE;
            newLine();
            print(pname + " q = malloc(sizeof(_" + pname + "));");
            for (int j = 0; j < params.length; j++) {
                newLine();
                print("q->p" + j + " = p" + j + ";");
            }
            newLine();
            print("send_message(p, " + i + ", l, q);");
            pos -= TAB_SIZE;
            newLine();
            print("}");
        }        
    }

    // ----------------------------------------------------------------------
    // METHODS AND FIELDS
    // ----------------------------------------------------------------------

    /**
     * prints a field declaration
     */
    public void visitFieldDeclaration(JFieldDeclaration self,
                                      int modifiers,
                                      CType type,
                                      String ident,
                                      JExpression expr) {
        /*
          if (ident.indexOf("$") != -1) {
          return; // dont print generated elements
          }
        */

	// if it is a final field (eg constant)
	// leave it out of the structure because constant propagation
	// will have removed all references to it.
	/*
	if ((expr == null ||
	     !(expr instanceof JNewArrayExpression)) &&
	    CModifier.contains(self.getVariable().getModifiers(),
			       CModifier.ACC_FINAL)) {
	    return;
	}
	*/


	
        newLine();
        // if printing a field decl and it's array and it's
        // initialized, I want to use a special printer that
        // will just declare an array, instead of a pointer
        if (expr == null || !(expr instanceof JNewArrayExpression))
        {
            // nope - not an array, or not initialized.
            // just print it normally - if an array, it'll become  a
            // pointer and the init function better allocate it	    
	    		
	    print (type);
            print (" ");
            print (ident);

	    // initializing vars is legal in C as well as java.
	    if (expr != null) {
		print ("\t= ");
		expr.accept (this);
	    }
	} else {
            // yep.  use the local printing functions to print
            // the correct type and array size
            printLocalType (type);
            print (" ");
            print (ident);
            print(" ");
            printLocalArrayDecl((JNewArrayExpression)expr);
        }
        print(";");
    }

    /**
     * prints a method declaration
     */
    public void visitMethodDeclaration(JMethodDeclaration self,
                                       int modifiers,
                                       CType returnType,
                                       String ident,
                                       JFormalParameter[] parameters,
                                       CClassType[] exceptions,
                                       JBlock body) {
        newLine();
        // Treat main() specially.
        if (ident.equals("main"))
        {
            print("int main(int argc, char **argv)");
        }
        else
        {
            // print(CModifier.toString(modifiers));
            print(returnType);
            print(" ");
            print(ident);
            print("(");
            int count = 0;
            
            for (int i = 0; i < parameters.length; i++) {
                if (count != 0) {
                    print(", ");
                }
                
                // if (!parameters[i].isGenerated()) {
                parameters[i].accept(this);
                count++;
                // }
            }
            print(")");
        }
        
        if (declOnly)
        {
            print(";");
            return;
        }

        print(" ");
        if (body != null) {
            body.accept(this);
        } else {
            print(";");
        }
        newLine();
    }

    /**
     * prints a method declaration
     */
    public void visitConstructorDeclaration(JConstructorDeclaration self,
                                            int modifiers,
                                            String ident,
                                            JFormalParameter[] parameters,
                                            CClassType[] exceptions,
                                            JConstructorBlock body)
    {
        newLine();
        print(CModifier.toString(modifiers));
        print(ident);
        print("_");
        print(ident);
        print("(");
        int count = 0;
        for (int i = 0; i < parameters.length; i++) {
            if (count != 0) {
                print(", ");
            }
            if (!parameters[i].isGenerated()) {
                parameters[i].accept(this);
                count++;
            }
        }
        print(")");
        /*
          for (int i = 0; i < exceptions.length; i++) {
          if (i != 0) {
          print(", ");
          } else {
          print(" throws ");
          }
          print(exceptions[i].toString());
          }
        */
        print(" ");
        body.accept(this);
        newLine();
    }

    // ----------------------------------------------------------------------
    // STATEMENT
    // ----------------------------------------------------------------------

    /**
     * prints a while statement
     */
    public void visitWhileStatement(JWhileStatement self,
                                    JExpression cond,
                                    JStatement body) {
        print("while (");
        cond.accept(this);
        print(") ");

        body.accept(this);
    }

    /**
     * prints a variable declaration statement
     */
    public void visitVariableDeclarationStatement(JVariableDeclarationStatement self,
                                                  JVariableDefinition[] vars) {
        for (int i = 0; i < vars.length; i++) {
            vars[i].accept(this);
        }
    }

    private void printLocalArrayDecl(JNewArrayExpression expr) 
    {
	JExpression[] dims = expr.getDims();
	for (int i = 0 ; i < dims.length; i++) {
	    print("[");
	    LIRToC toC = new LIRToC(this.p);
	    dims[i].accept(toC);
	    print("]");
	}
    }
    

    protected void printLocalType(CType s) 
    {
	if (s instanceof CArrayType){
	    print(((CArrayType)s).getBaseType());
	}
        else if (s.getTypeID() == TID_BOOLEAN)
            print("int");
        else if (s.toString().endsWith("Portal"))
	    // ignore the specific type of portal in the C library
	    print("portal");
	else
            print(s.toString());
    }

    /**
     * prints a variable declaration statement
     */
    public void visitVariableDefinition(JVariableDefinition self,
                                        int modifiers,
                                        CType type,
                                        String ident,
                                        JExpression expr) {
        // print(CModifier.toString(modifiers));
	if (expr!=null) {
	    printLocalType(type);
	} else {
	    print(type);
	}	    
        print(" ");
        print(ident);
        if (expr != null) {
            if (expr instanceof JNewArrayExpression) {
		printLocalArrayDecl((JNewArrayExpression)expr);
	    }
	    else {
		print(" = ");
		expr.accept(this);
	    }
	}
        print(";");
    }

    /**
     * prints a try-catch statement
     */
    public void visitTryCatchStatement(JTryCatchStatement self,
                                       JBlock tryClause,
                                       JCatchClause[] catchClauses) {
        print("try ");
        tryClause.accept(this);
        for (int i = 0; i < catchClauses.length; i++) {
            catchClauses[i].accept(this);
        }
    }

    /**
     * prints a try-finally statement
     */
    public void visitTryFinallyStatement(JTryFinallyStatement self,
                                         JBlock tryClause,
                                         JBlock finallyClause) {
        print("try ");
        tryClause.accept(this);
        if (finallyClause != null) {
            print(" finally ");
            finallyClause.accept(this);
        }
    }

    /**
     * prints a throw statement
     */
    public void visitThrowStatement(JThrowStatement self,
                                    JExpression expr) {
        print("throw ");
        expr.accept(this);
        print(";");
    }

    /**
     * prints a synchronized statement
     */
    public void visitSynchronizedStatement(JSynchronizedStatement self,
                                           JExpression cond,
                                           JStatement body) {
        print("synchronized (");
        cond.accept(this);
        print(") ");
        body.accept(this);
    }

    /**
     * prints a switch statement
     */
    public void visitSwitchStatement(JSwitchStatement self,
                                     JExpression expr,
                                     JSwitchGroup[] body) {
        print("switch (");
        expr.accept(this);
        print(") {");
        for (int i = 0; i < body.length; i++) {
            body[i].accept(this);
        }
        newLine();
        print("}");
    }

    /**
     * prints a return statement
     */
    public void visitReturnStatement(JReturnStatement self,
                                     JExpression expr) {
        print("return");
        if (expr != null) {
            print(" ");
            expr.accept(this);
        }
        print(";");
    }

    /**
     * prints a labeled statement
     */
    public void visitLabeledStatement(JLabeledStatement self,
                                      String label,
                                      JStatement stmt) {
        print(label + ":");
        stmt.accept(this);
    }

    /**
     * prints a if statement
     */
    public void visitIfStatement(JIfStatement self,
                                 JExpression cond,
                                 JStatement thenClause,
                                 JStatement elseClause) {
        print("if (");
        cond.accept(this);
        print(") ");
        pos += thenClause instanceof JBlock ? 0 : TAB_SIZE;
        thenClause.accept(this);
        pos -= thenClause instanceof JBlock ? 0 : TAB_SIZE;
        if (elseClause != null) {
            if ((elseClause instanceof JBlock) || (elseClause instanceof JIfStatement)) {
                print(" ");
            } else {
                newLine();
            }
            print("else ");
            pos += elseClause instanceof JBlock || elseClause instanceof JIfStatement ? 0 : TAB_SIZE;
            elseClause.accept(this);
            pos -= elseClause instanceof JBlock || elseClause instanceof JIfStatement ? 0 : TAB_SIZE;
        }
    }

    /**
     * prints a for statement
     */
    public void visitForStatement(JForStatement self,
                                  JStatement init,
                                  JExpression cond,
                                  JStatement incr,
                                  JStatement body) {
        print("for (");
        //forInit = true;
        if (init != null) {
            init.accept(this);
	} else {
	    print(";");
	}
	//forInit = false;

        print(" ");
        if (cond != null) {
            cond.accept(this);
        }
        print("; ");

        if (incr != null) {
	    LIRToC l2c = new LIRToC();
            incr.accept(l2c);
	    // get String
	    String str = l2c.getString();
	    // leave off the trailing semicolon if there is one
	    if (str.endsWith(";")) {
		print(str.substring(0, str.length()-1));
	    } else { 
		print(str);
	    }
        }
        print(") {\n");

        pos += TAB_SIZE;
        body.accept(this);
        pos -= TAB_SIZE;
        newLine();
        print("}");
    }

    /**
     * prints a compound statement
     */
    public void visitCompoundStatement(JCompoundStatement self,
                                       JStatement[] body) {
        visitCompoundStatement(body);
    }

    /**
     * prints a compound statement
     */
    public void visitCompoundStatement(JStatement[] body) {
        for (int i = 0; i < body.length; i++) {
            if (body[i] instanceof JIfStatement &&
                i < body.length - 1 &&
                !(body[i + 1] instanceof JReturnStatement)) {
                newLine();
            }
            if (body[i] instanceof JReturnStatement && i > 0) {
                newLine();
            }

            newLine();
            body[i].accept(this);

            if (body[i] instanceof JVariableDeclarationStatement &&
                i < body.length - 1 &&
                !(body[i + 1] instanceof JVariableDeclarationStatement)) {
                newLine();
            }
        }
    }

    /**
     * prints an expression statement
     */
    public void visitExpressionStatement(JExpressionStatement self,
                                         JExpression expr) {
        expr.accept(this);
        //if (!forInit) {
            print(";");
	    //}
    }

    /**
     * prints an expression list statement
     */
    public void visitExpressionListStatement(JExpressionListStatement self,
                                             JExpression[] expr) {
        for (int i = 0; i < expr.length; i++) {
            if (i != 0) {
                print(", ");
            }
            expr[i].accept(this);
        }
	print(";");
    }

    /**
     * prints a empty statement
     */
    public void visitEmptyStatement(JEmptyStatement self) {
        newLine();
        print(";");
    }

    /**
     * prints a do statement
     */
    public void visitDoStatement(JDoStatement self,
                                 JExpression cond,
                                 JStatement body) {
        newLine();
        print("do ");
        body.accept(this);
        print("");
        print("while (");
        cond.accept(this);
        print(");");
    }

    /**
     * prints a continue statement
     */
    public void visitContinueStatement(JContinueStatement self,
                                       String label) {
        newLine();
        print("continue");
        if (label != null) {
            print(" " + label);
        }
        print(";");
    }

    /**
     * prints a break statement
     */
    public void visitBreakStatement(JBreakStatement self,
                                    String label) {
        newLine();
        print("break");
        if (label != null) {
            print(" " + label);
        }
        print(";");
    }

    /**
     * prints an expression statement
     */
    public void visitBlockStatement(JBlock self,
                                    JavaStyleComment[] comments) {
        print("{");
        pos += TAB_SIZE;
        visitCompoundStatement(self.getStatementArray());
        if (comments != null) {
            visitComments(comments);
        }
        pos -= TAB_SIZE;
        newLine();
        print("}");
    }

    /**
     * prints a type declaration statement
     */
    public void visitTypeDeclarationStatement(JTypeDeclarationStatement self,
                                              JTypeDeclaration decl) {
        decl.accept(this);
    }

    // ----------------------------------------------------------------------
    // EXPRESSION
    // ----------------------------------------------------------------------

    /**
     * prints an unary plus expression
     */
    public void visitUnaryPlusExpression(JUnaryExpression self,
                                         JExpression expr)
    {
	print("(");
        print("+");
        expr.accept(this);
	print(")");
    }

    /**
     * prints an unary minus expression
     */
    public void visitUnaryMinusExpression(JUnaryExpression self,
                                          JExpression expr)
    {
	print("(");
        print("-");
        expr.accept(this);
	print(")");
    }

    /**
     * prints a bitwise complement expression
     */
    public void visitBitwiseComplementExpression(JUnaryExpression self,
						 JExpression expr)
    {
	print("(");
        print("~");
        expr.accept(this);
	print(")");
    }

    /**
     * prints a logical complement expression
     */
    public void visitLogicalComplementExpression(JUnaryExpression self,
						 JExpression expr)
    {
	print("(");
        print("!");
        expr.accept(this);
	print(")");
    }

    /**
     * prints a type name expression
     */
    public void visitTypeNameExpression(JTypeNameExpression self,
                                        CType type) {
	print("(");
        print(type);
	print(")");
    }

    /**
     * prints a this expression
     */
    public void visitThisExpression(JThisExpression self,
                                    JExpression prefix) {
        if (prefix != null) {
            prefix.accept(this);
            print("." + THIS_NAME);
        } else {
            print(THIS_NAME);
        }
    }

    /**
     * prints a super expression
     */
    public void visitSuperExpression(JSuperExpression self) {
        print("super");
    }

    /**
     * prints a shift expression
     */
    public void visitShiftExpression(JShiftExpression self,
                                     int oper,
                                     JExpression left,
                                     JExpression right) {
	print("(");
        left.accept(this);
        if (oper == OPE_SL) {
            print(" << ");
        } else if (oper == OPE_SR) {
            print(" >> ");
        } else {
            print(" >>> ");
        }
        right.accept(this);
	print(")");
    }

    /**
     * prints a shift expressiona
     */
    public void visitRelationalExpression(JRelationalExpression self,
                                          int oper,
                                          JExpression left,
                                          JExpression right) {
	print("(");
        left.accept(this);
        switch (oper) {
        case OPE_LT:
            print(" < ");
            break;
        case OPE_LE:
            print(" <= ");
            break;
        case OPE_GT:
            print(" > ");
            break;
        case OPE_GE:
            print(" >= ");
            break;
        default:
            throw new InconsistencyException();
        }
        right.accept(this);
	print(")");
    }

    /**
     * prints a prefix expression
     */
    public void visitPrefixExpression(JPrefixExpression self,
                                      int oper,
                                      JExpression expr) {
	print("(");
        if (oper == OPE_PREINC) {
            print("++");
        } else {
            print("--");
        }
        expr.accept(this);
	print(")");
    }

    /**
     * prints a postfix expression
     */
    public void visitPostfixExpression(JPostfixExpression self,
                                       int oper,
                                       JExpression expr) {
	print("(");
        expr.accept(this);
        if (oper == OPE_POSTINC) {
            print("++");
        } else {
            print("--");
        }
	print(")");
    }

    /**
     * prints a parenthesed expression
     */
    public void visitParenthesedExpression(JParenthesedExpression self,
                                           JExpression expr) {
        print("(");
        expr.accept(this);
        print(")");
    }

    /**
     * Prints an unqualified anonymous class instance creation expression.
     */
    public void visitQualifiedAnonymousCreation(JQualifiedAnonymousCreation self,
                                                JExpression prefix,
                                                String ident,
                                                JExpression[] params,
                                                JClassDeclaration decl)
    {
        prefix.accept(this);
        print(".new " + ident + "(");
        visitArgs(params, 0);
        print(")");
        // decl.genInnerJavaCode(this);
    }

    /**
     * Prints an unqualified instance creation expression.
     */
    public void visitQualifiedInstanceCreation(JQualifiedInstanceCreation self,
                                               JExpression prefix,
                                               String ident,
                                               JExpression[] params)
    {
        prefix.accept(this);
        print(".new " + ident + "(");
        visitArgs(params, 0);
        print(")");
    }

    /**
     * Prints an unqualified anonymous class instance creation expression.
     */
    public void visitUnqualifiedAnonymousCreation(JUnqualifiedAnonymousCreation self,
                                                  CClassType type,
                                                  JExpression[] params,
                                                  JClassDeclaration decl)
    {
        print("new " + type + "(");
        visitArgs(params, 0);
        print(")");
        // decl.genInnerJavaCode(this);
    }

    /**
     * Prints an unqualified instance creation expression.
     */
    public void visitUnqualifiedInstanceCreation(JUnqualifiedInstanceCreation self,
                                                 CClassType type,
                                                 JExpression[] params)
    {
        print("new " + type + "(");
        visitArgs(params, 0);
        print(")");
    }

    /**
     * prints an array allocator expression
     */
    public void visitNewArrayExpression(JNewArrayExpression self,
                                        CType type,
                                        JExpression[] dims,
                                        JArrayInitializer init)
    {
        print("calloc(");
        dims[0].accept(this);
        print(", sizeof(");
        print(type);
	if(dims.length>1)
	    print("*");
        print("))");
	if(dims.length>1) {
	    for(int off=0;off<(dims.length-1);off++) {
		//Right now only handles JIntLiteral dims
		//If cast expression then probably a failure to reduce
		int num=((JIntLiteral)dims[off]).intValue();
		for(int i=0;i<num;i++) {
		    print(",\n");
		    //If lastLeft null then didn't come right after an assignment
		    lastLeft.accept(this);
		    print("["+i+"]=calloc(");
		    dims[off+1].accept(this);
		    print(", sizeof(");
		    print(type);
		    if(off<(dims.length-2))
			print("*");
		    print("))");
		}
	    }
	}
        if (init != null) {
            init.accept(this);
        }
    }

    /**
     * prints a name expression
     */
    public void visitNameExpression(JNameExpression self,
                                    JExpression prefix,
                                    String ident) {
	print("(");
        if (prefix != null) {
            prefix.accept(this);
            print("->");
        }
        print(ident);
	print(")");
    }

    /**
     * prints an array allocator expression
     */
    public void visitBinaryExpression(JBinaryExpression self,
                                      String oper,
                                      JExpression left,
                                      JExpression right) {
	print("(");
        left.accept(this);
        print(" ");
        print(oper);
        print(" ");
        right.accept(this);
	print(")");
    }

    /**
     * prints a method call expression
     */
    public void visitMethodCallExpression(JMethodCallExpression self,
                                          JExpression prefix,
                                          String ident,
                                          JExpression[] args) {
        /*
          if (ident != null && ident.equals(JAV_INIT)) {
          return; // we do not want generated methods in source code
          }
        */

        print(ident);
        print("(");
        int i = 0;
        /* Ignore prefix, since it's just going to be a Java class name.
        if (prefix != null) {
            prefix.accept(this);
            i++;
        }
        */
        visitArgs(args, i);
        print(")");
    }

    /**
     * prints a local variable expression
     */
    public void visitLocalVariableExpression(JLocalVariableExpression self,
                                             String ident) {
        print(ident);
    }

    /**
     * prints an instanceof expression
     */
    public void visitInstanceofExpression(JInstanceofExpression self,
                                          JExpression expr,
                                          CType dest) {
        expr.accept(this);
        print(" instanceof ");
        print(dest);
    }

    /**
     * prints an equality expression
     */
    public void visitEqualityExpression(JEqualityExpression self,
                                        boolean equal,
                                        JExpression left,
                                        JExpression right) {
	print("(");
        left.accept(this);
        print(equal ? " == " : " != ");
        right.accept(this);
	print(")");
    }

    /**
     * prints a conditional expression
     */
    public void visitConditionalExpression(JConditionalExpression self,
                                           JExpression cond,
                                           JExpression left,
                                           JExpression right) {
	print("(");
        cond.accept(this);
        print(" ? ");
        left.accept(this);
        print(" : ");
        right.accept(this);
	print(")");
    }

    /**
     * prints a compound expression
     */
    public void visitCompoundAssignmentExpression(JCompoundAssignmentExpression self,
                                                  int oper,
                                                  JExpression left,
                                                  JExpression right) {
	print("(");
        left.accept(this);
        switch (oper) {
        case OPE_STAR:
            print(" *= ");
            break;
        case OPE_SLASH:
            print(" /= ");
            break;
        case OPE_PERCENT:
            print(" %= ");
            break;
        case OPE_PLUS:
            print(" += ");
            break;
        case OPE_MINUS:
            print(" -= ");
            break;
        case OPE_SL:
            print(" <<= ");
            break;
        case OPE_SR:
            print(" >>= ");
            break;
        case OPE_BSR:
            print(" >>>= ");
            break;
        case OPE_BAND:
            print(" &= ");
            break;
        case OPE_BXOR:
            print(" ^= ");
            break;
        case OPE_BOR:
            print(" |= ");
            break;
        }
        right.accept(this);
	print(")");
    }

    /**
     * prints a field expression
     */
    public void visitFieldExpression(JFieldAccessExpression self,
                                     JExpression left,
                                     String ident)
    {
	/*
	System.err.println("!!! self=" + self);
	System.err.println(" left=" + left);
	System.err.println(" ident=" + ident);
	System.err.println(" left.getType()==" + left.getType());
	System.err.println(" getCClass()=" +left.getType().getCClass());
	*/
        if (ident.equals(JAV_OUTER_THIS)) {
            // This identifier is used for the enclosing instance of
            // inner classes; see JLS 8.1.2.
            print("((" + left.getType().getCClass().getOwner().getType() +
                  ")(" + THIS_CONTEXT_NAME + "->parent->stream_data))");
            return;
        }
        int		index = ident.indexOf("_$");
        if (index != -1) {
            print(ident.substring(0, index));      // local var
        } else {
	    print("(");
            left.accept(this);
            // I hate Kopi.  getType() doesn't necessarily work, since some
            // things (e.g. JFieldAccessExpressions) determine their CType
            // based on the CType of their components, which might not be
            // well-defined.  So let's do this...
            boolean isStructField = false;
            try
            {
		CType ctype = left.getType();
		// only class types have a proper cclass; the other's assert
                if (ctype instanceof CClassType && ctype.getCClass().getSuperClass().getIdent().equals("Structure"))
                    isStructField = true;
            }
            catch (NullPointerException e)
            {
                // do nothing
            }
            if (isStructField)
                print(".");
            else
                print("->");
            print(ident);
	    print(")");
        }
    }

    /**
     * prints a class expression
     */
    public void visitClassExpression(JClassExpression self, CType type) {
        print(type);
        print(".class");
    }

    /**
     * prints a cast expression
     */
    public void visitCastExpression(JCastExpression self,
                                    JExpression expr,
                                    CType type)
    {
	print("(");
        print("(");
        print(type);
        print(")");
        expr.accept(this);
	print(")");
    }

    /**
     * prints a cast expression
     */
    public void visitUnaryPromoteExpression(JUnaryPromote self,
                                            JExpression expr,
                                            CType type)
    {
	print("(");
        print("(");
        print(type);
        print(")");
        print("(");
        expr.accept(this);
        print(")");
        print(")");
    }

    /**
     * prints a compound assignment expression
     */
    public void visitBitwiseExpression(JBitwiseExpression self,
                                       int oper,
                                       JExpression left,
                                       JExpression right) {
        print("(");
        left.accept(this);
        switch (oper) {
        case OPE_BAND:
            print(" & ");
            break;
        case OPE_BOR:
            print(" | ");
            break;
        case OPE_BXOR:
            print(" ^ ");
            break;
        default:
            throw new InconsistencyException();
        }
        right.accept(this);
        print(")");
    }

    /**
     * prints an assignment expression
     */
    public void visitAssignmentExpression(JAssignmentExpression self,
                                          JExpression left,
                                          JExpression right) {
        /*
          if ((left instanceof JFieldAccessExpression) &&
          ((JFieldAccessExpression)left).getField().getIdent().equals(Constants.JAV_OUTER_THIS)) {
          return;
          }
        */

	lastLeft=left;
        print("(");
        left.accept(this);
        print(" = ");
        right.accept(this);
        print(")");
	lastLeft=null;
    }

    /**
     * prints an array length expression
     */
    public void visitArrayLengthExpression(JArrayLengthExpression self,
                                           JExpression prefix) {
        prefix.accept(this);
        print(".length");
    }

    /**
     * prints an array length expression
     */
    public void visitArrayAccessExpression(JArrayAccessExpression self,
                                           JExpression prefix,
                                           JExpression accessor) {
        print("(");
        prefix.accept(this);
        print("[");
        accessor.accept(this);
        print("]");
        print(")");
    }

    /**
     * prints an array length expression
     */
    public void visitComments(JavaStyleComment[] comments) {
        for (int i = 0; i < comments.length; i++) {
            if (comments[i] != null) {
                visitComment(comments[i]);
            }
        }
    }

    /**
     * prints an array length expression
     */
    public void visitComment(JavaStyleComment comment) {
        StringTokenizer	tok = new StringTokenizer(comment.getText(), "\n");

        if (comment.hadSpaceBefore()) {
            newLine();
        }

        if (comment.isLineComment()) {
            print("//");
	    if (tok.hasMoreTokens())
	      print(tok.nextToken().trim());
            p.println();
        } else {
            if (p.getLine() > 0) {
                if (!nl) {
                    newLine();
                }
                newLine();
            }
            print("/*");
            while (tok.hasMoreTokens()){
                String comm = tok.nextToken().trim();
                if (comm.startsWith("*")) {
                    comm = comm.substring(1).trim();
                }
                if (tok.hasMoreTokens() || comm.length() > 0) {
                    newLine();
                    print(" * " + comm);
                }
            }
            newLine();
            print(" */");
            newLine();
        }

        if (comment.hadSpaceAfter()) {
            newLine();
        }
    }

    /**
     * prints an array length expression
     */
    public void visitJavadoc(JavadocComment comment) {
        StringTokenizer	tok = new StringTokenizer(comment.getText(), "\n");
        boolean		isFirst = true;

        if (!nl) {
            newLine();
        }
        newLine();
        print("/**");
        while (tok.hasMoreTokens()) {
            String	text = tok.nextToken().trim();
            String	type = null;
            boolean	param = false;
            int	idx = text.indexOf("@param");
            if (idx >= 0) {
                type = "@param";
                param = true;
            }
            if (idx < 0) {
                idx = text.indexOf("@exception");
                if (idx >= 0) {
                    type = "@exception";
                    param = true;
                }
            }
            if (idx < 0) {
                idx = text.indexOf("@exception");
                if (idx >= 0) {
                    type = "@exception";
                    param = true;
                }
            }
            if (idx < 0) {
                idx = text.indexOf("@author");
                if (idx >= 0) {
                    type = "@author";
                }
            }
            if (idx < 0) {
                idx = text.indexOf("@see");
                if (idx >= 0) {
                    type = "@see";
                }
            }
            if (idx < 0) {
                idx = text.indexOf("@version");
                if (idx >= 0) {
                    type = "@version";
                }
            }
            if (idx < 0) {
                idx = text.indexOf("@return");
                if (idx >= 0) {
                    type = "@return";
                }
            }
            if (idx < 0) {
                idx = text.indexOf("@deprecated");
                if (idx >= 0) {
                    type = "@deprecated";
                }
            }
            if (idx >= 0) {
                newLine();
                isFirst = false;
                if (param) {
                    text = text.substring(idx + type.length()).trim();
                    idx = Math.min(text.indexOf(" ") == -1 ? Integer.MAX_VALUE : text.indexOf(" "),
                                   text.indexOf("\t") == -1 ? Integer.MAX_VALUE : text.indexOf("\t"));
                    if (idx == Integer.MAX_VALUE) {
                        idx = 0;
                    }
                    String	before = text.substring(0, idx);
                    print(" * " + type);
                    pos += 12;
                    print(before);
                    pos += 20;
                    print(text.substring(idx).trim());
                    pos -= 20;
                    pos -= 12;
                } else {
                    text = text.substring(idx + type.length()).trim();
                    print(" * " + type);
                    pos += 12;
                    print(text);
                    pos -= 12;
                }
            } else {
                text = text.substring(text.indexOf("*") + 1);
                if (tok.hasMoreTokens() || text.length() > 0) {
                    newLine();
                    print(" * ");
                    pos += isFirst ? 0 : 32;
                    print(text.trim());
                    pos -= isFirst ? 0 : 32;
                }
            }
        }
        newLine();
        print(" */");
    }

    // ----------------------------------------------------------------------
    // STREAMIT IR HANDLERS
    // ----------------------------------------------------------------------

    public void visitCreatePortalExpression(SIRCreatePortal self) {
        print("create_portal()");
    }

    public void visitInitStatement(SIRInitStatement self,
                                   SIRStream stream)
    {
        print("/* InitStatement */");
    }

    public void visitInterfaceTable(SIRInterfaceTable self)
    {
        String iname = self.getIface().getIdent();
        JMethodDeclaration[] methods = self.getMethods();
        boolean first = true;
        
        print("{ ");
        for (int i = 0; i < methods.length; i++)
        {
            if (!first) print(", ");
            first = false;
            print(iname + "_" + methods[i].getName());
        }
        print("}");
    }
    
    public void visitLatency(SIRLatency self)
    {
        print("LATENCY_BEST_EFFORT");
    }
    
    public void visitLatencyMax(SIRLatencyMax self)
    {
        print("LATENCY_BEST_EFFORT");
    }
    
    public void visitLatencyRange(SIRLatencyRange self)
    {
        print("LATENCY_BEST_EFFORT");
    }
    
    public void visitLatencySet(SIRLatencySet self)
    {
        print("LATENCY_BEST_EFFORT");
    }

    public void visitMessageStatement(SIRMessageStatement self,
                                      JExpression portal,
                                      String iname,
                                      String ident,
                                      JExpression[] params,
                                      SIRLatency latency)
    {
	print("send_" + iname + "_" + ident + "(");
        portal.accept(this);
        print(", ");
        latency.accept(this);
        if (params != null)
            for (int i = 0; i < params.length; i++)
                if (params[i] != null)
                {
                    print(", ");
                    params[i].accept(this);
                }
        print(");");
    }

    public void visitPeekExpression(SIRPeekExpression self,
                                    CType tapeType,
                                    JExpression num)
    {
        print("(PEEK_DEFAULTB(");
        if (tapeType != null)
            print(tapeType);
        else
            print("/* null tapeType! */ int");
        print(", ");
        num.accept(this);
        print("))");
    }
    
    public void visitPopExpression(SIRPopExpression self,
                                   CType tapeType)
    {
	if (self.getNumPop()>1) {
	    print("(POP_DEFAULTB_N( ");
	    if (tapeType != null)
		if(tapeType instanceof CArrayType)
		    print(tapeType.toString());
		else
		    print(tapeType);
	    else
		print("/* null tapeType! */ int");
	    print(", " + self.getNumPop() + " ))");
	} else {
	    print("(POP_DEFAULTB( ");
	    if (tapeType != null)
		if(tapeType instanceof CArrayType)
		    print(tapeType.toString());
		else
		    print(tapeType);
	    else
		print("/* null tapeType! */ int");
	    print("))");
	}
    }
    
    public void visitPrintStatement(SIRPrintStatement self,
                                    JExpression exp)
    {
        CType type = exp.getType();
        
        if (type.equals(CStdType.Boolean))
        {
            print("printf(\"%s\\n\", ");
            exp.accept(this);
            print(" ? \"true\" : \"false\");");
        }
        else if (type.equals(CStdType.Byte) ||
                 type.equals(CStdType.Integer) ||
                 type.equals(CStdType.Short))
        {
            print("printf(\"%d\\n\", ");
            exp.accept(this);
            print(");");
        }
        else if (type.equals(CStdType.Char))
        {
            print("printf(\"%c\\n\", ");
            exp.accept(this);
            print(");");
        }
        else if (type.equals(CStdType.Float) ||
                 type.equals(CStdType.Double))
        {
            print("printf(\"%f\\n\", ");
            exp.accept(this);
            print(");");
        }
        else if (type.equals(CStdType.Long))
        {
            print("printf(\"%ld\\n\", ");
            exp.accept(this);
            print(");");
        }
        else
        {
            print("printf(\"(unprintable type: " + type + ")\\n\", ");
            exp.accept(this);
            print(");");
        }
    }
    
    public void visitPushExpression(SIRPushExpression self,
                                    CType tapeType,
                                    JExpression val)
    {
        print("(PUSH_DEFAULTB(");
        if (tapeType != null) {
	    if(tapeType instanceof CArrayType)
		print(tapeType.toString());
	    else
		print(tapeType);
        } else
            print("/* null tapeType! */ int");
        print(", ");
        val.accept(this);
        print("))");
    }
    
    public void visitPhaseInvocation(SIRPhaseInvocation self,
                                     JMethodCallExpression call,
                                     JExpression peek,
                                     JExpression pop,
                                     JExpression push)
    {
        print("/* phase invocation: ");
        call.accept(this);
        print("; */");
    }

    public void visitRegReceiverStatement(SIRRegReceiverStatement self,
                                          JExpression portal,
					  SIRStream receiver, 
					  JMethodDeclaration[] methods)
    {
        print("register_receiver(");
        portal.accept(this);
        print(", " + THIS_CONTEXT_NAME + ", ");
        print(self.getItable().getVarDecl().getIdent());
        print(", LATENCY_BEST_EFFORT);");
        // (But shouldn't there be a latency field in here?)
    }
    
    public void visitRegSenderStatement(SIRRegSenderStatement self,
                                        String fn,
                                        SIRLatency latency)
    {
        print("register_sender(this->context, ");
        print(fn);
        print(", ");
        latency.accept(this);
        print(");");
    }


    /**
     * Visits a file reader.
     */
    public void visitFileReader(LIRFileReader self) {
        String childName = THIS_NAME + "->" + self.getChildName();
        print(childName + " = malloc(sizeof(_ContextContainer));");
        newLine();
        print(childName + "->" + CONTEXT_NAME +
              " = streamit_filereader_create(\"" +
              self.getFileName() + "\");");
        newLine();
        print("register_child(");
        self.getStreamContext().accept(this);
        print(", " + childName + "->" + CONTEXT_NAME + ");");
    }

    /**
     * Visits a file writer.
     */
    public void visitFileWriter(LIRFileWriter self) {
        String childName = THIS_NAME + "->" + self.getChildName();
        print(childName + " = malloc(sizeof(_ContextContainer));");
        newLine();
        print(childName + "->" + CONTEXT_NAME +
              " = streamit_filewriter_create(\"" +
              self.getFileName() + "\");");
        newLine();
        print("register_child(");
        self.getStreamContext().accept(this);
        print(", " + childName + "->" + CONTEXT_NAME + ");");
    }

    /**
     * Visits an identity filter.
     */
    public void visitIdentity(LIRIdentity self) 
    {
        String childName = THIS_NAME + "->" + self.getChildName();
        print(childName + " = malloc(sizeof(_ContextContainer));");
        newLine();
        print(childName + "->" + CONTEXT_NAME +
              " = streamit_identity_create();");
        newLine();
        print("register_child(");
        self.getStreamContext().accept(this);
        print(", " + childName + "->" + CONTEXT_NAME + ");");
    }

    public void visitSetChild(LIRSetChild self,
                              JExpression streamContext,
                              String childType,
                              String childName)
    {
        // Pay attention, three statements!
        print(THIS_NAME + "->" + childName +
              " = malloc(sizeof(_" + childType + "));");
        newLine();
        print(THIS_NAME + "->" + childName + "->" + CONTEXT_NAME + " = " +
              "create_context(" + THIS_NAME + "->" + childName + ");");
        newLine();
        print("register_child(");
        streamContext.accept(this);
        print(", " + THIS_NAME + "->" + childName + "->" + CONTEXT_NAME +
              ");");
    }
    
    public void visitSetTape(LIRSetTape self,
                             JExpression streamContext,
                             JExpression srcStruct,
                             JExpression dstStruct,
                             CType type,
                             int size)
    {
        print("create_tape(");
        srcStruct.accept(this);
        print("->" + CONTEXT_NAME + ", ");
        dstStruct.accept(this);
        print("->" + CONTEXT_NAME + ", sizeof(");
        print(type);
        print("), " + size + ");");
    }
    
    /**
     * Visits a function pointer.
     */
    public void visitFunctionPointer(LIRFunctionPointer self,
                                     String name)
    {
        // This is an expression.
        print(name);
    }
    
    /**
     * Visits an LIR node.
     */
    public void visitNode(LIRNode self)
    {
        // This should never be called directly.
        print("/* Unexpected visitNode */");
    }

    /**
     * Visits a child registration node.
     */
    public void visitSetChild(LIRSetChild self,
                              JExpression streamContext,
                              JExpression childContext)
    {
        // This is a statement.
        newLine();
        print("register_child(");
        streamContext.accept(this);
        print(", ");
        childContext.accept(this);
        print(");");
    }
    
    /**
     * Visits a decoder registration node.
     */
    public void visitSetDecode(LIRSetDecode self,
                               JExpression streamContext,
                               LIRFunctionPointer fp)
    {
        print("set_decode(");
        streamContext.accept(this);
        print(", ");
        fp.accept(this);
        print(");");
    }

    /**
     * Visits a feedback loop delay node.
     */
    public void visitSetDelay(LIRSetDelay self,
                              JExpression data,
                              JExpression streamContext,
                              int delay,
                              CType type,
                              LIRFunctionPointer fp)
    {
        /* This doesn't work quite right yet, but it's closer. */
        print("FEEDBACK_DELAY(");
        data.accept(this);
        print(", ");
        streamContext.accept(this);
        print(", " + delay + ", ");
        print(type);
        print(", ");
        fp.accept(this);
        print(");");
    }
    
    /**
     * Visits an encoder registration node.
     */
    public void visitSetEncode(LIRSetEncode self,
                        JExpression streamContext,
                        LIRFunctionPointer fp)
    {
        print("set_encode(");
        streamContext.accept(this);
        print(", ");
        fp.accept(this);
        print(");");
    }
    
    /**
     * Visits a joiner-setting node.
     */
    public void visitSetJoiner(LIRSetJoiner self,
                               JExpression streamContext,
                               SIRJoinType type,
                               int ways,
                               int[] weights)
    {
        print("set_joiner(");
        streamContext.accept(this);
        print(", ");
        print(type);
        print(", " + String.valueOf(ways));
        if (weights != null)
        {
            for (int i = 0; i < weights.length; i++)
                print(", " + String.valueOf(weights[i]));
        }
        print(");");
    }

    /**
     * Visits a peek-rate-setting node.
     */
    public void visitSetPeek(LIRSetPeek self,
                      JExpression streamContext,
                      int peek)
    {
        print("set_peek(");
        streamContext.accept(this);
        print(", " + peek + ");");
    }
    
    /**
     * Visits a pop-rate-setting node.
     */
    public void visitSetPop(LIRSetPop self,
                     JExpression streamContext,
                     int pop)
    {
        print("set_pop(");
        streamContext.accept(this);
        print(", " + pop + ");");
    }
    
    /**
     * Visits a push-rate-setting node.
     */
    public void visitSetPush(LIRSetPush self,
                      JExpression streamContext,
                      int push)
    {
        print("set_push(");
        streamContext.accept(this);
        print(", " + push + ");");
    }

    /**
     * Visits a splitter-setting node.
     */
    public void visitSetSplitter(LIRSetSplitter self,
                                 JExpression streamContext,
                                 SIRSplitType type,
                                 int ways,
                                 int[] weights)
    {
        print("set_splitter(");
        streamContext.accept(this);
        print(", " + type + ", " + String.valueOf(ways));
        if (weights != null)
        {
            for (int i = 0; i < weights.length; i++)
                print(", " + String.valueOf(weights[i]));
        }
        print(");");
    }

    /**
     * Visits a stream-type-setting node.
     */
    public void visitSetStreamType(LIRSetStreamType self,
                            JExpression streamContext,
                            LIRStreamType streamType)
    {
        print("set_stream_type(");
        streamContext.accept(this);
        print(", " + streamType + ");");
    }
    
    /**
     * Visits a work-function-setting node.
     */
    public void visitSetWork(LIRSetWork self,
                      JExpression streamContext,
                      LIRFunctionPointer fn)
    {
        print("set_work(");
        streamContext.accept(this);
        print(", (work_fn)");
        fn.accept(this);
        print(");");
    }

    public void visitMainFunction(LIRMainFunction self,
                                  String typeName,
                                  LIRFunctionPointer init,
				  List initStatements)
    {
        print(typeName + " " + THIS_NAME +
              " = malloc(sizeof(_" + typeName + "));");
        newLine();
        print(THIS_CONTEXT_NAME + " = create_context(" + THIS_NAME + ");");
        newLine();
        init.accept(this);
        print("(" + THIS_NAME + ");");
        newLine();
        print("connect_tapes(" + THIS_CONTEXT_NAME + ");");
        newLine();
        Iterator iter = initStatements.iterator();
        while (iter.hasNext())
            ((JStatement)(iter.next())).accept(this);
        newLine();
        print("streamit_run(" + THIS_CONTEXT_NAME + ", argc, argv);");
        newLine();
        print("return 0;");
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
        /* Three things need to happen for feedback loop children:
         * they need to be registered, their input tapes need
         * to be created, and the output tapes need to be created.
         * LIRSetChild deals with the registration, so we just need
         * to take care of the tapes.  For a feedback loop body,
         * we're looking at the output of the joiner and the
         * input of the splitter. */
        print("create_splitjoin_tape(");
        streamContext.accept(this);
        print(", JOINER, OUTPUT, 0, ");
        childContext.accept(this);
        print(", sizeof(");
        print(inputType);
        print("), " + inputSize + ");");
        newLine();
        print("create_splitjoin_tape(");
        streamContext.accept(this);
        print(", SPLITTER, INPUT, 0, ");
        childContext.accept(this);
        print(", sizeof(");
        print(outputType);
        print("), " + outputSize + ");");
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
        /* The loop's output goes to input 1 of the joiner, and its
         * input comes from output 1 of the splitter.  (input/output
         * 0 are connected to the outside of the block.) */
        print("create_splitjoin_tape(");
        streamContext.accept(this);
        print(", SPLITTER, OUTPUT, 1, ");
        childContext.accept(this);
        print(", sizeof(");
        print(inputType);
        print("), " + inputSize + ");");
        newLine();
        print("create_splitjoin_tape(");
        streamContext.accept(this);
        print(", JOINER, INPUT, 1, ");
        childContext.accept(this);
        print(", sizeof(");
        print(outputType);
        print("), " + outputSize + ");");
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
        /* For  split/joins now.  Again, assume registration has
         * already happened; we just need to connect tapes.
         * Use the position'th slot on the splitter output and
         * joiner input. */
	if (inputSize!=0) {
	    print("create_splitjoin_tape(");
	    streamContext.accept(this);
	    print(", SPLITTER, OUTPUT, " + position + ", ");
	    childContext.accept(this);
	    print(", sizeof(");
            print(inputType);
            print("), " + inputSize + ");");
	    newLine();
	}
	if (outputSize!=0) {
	    print("create_splitjoin_tape(");
	    streamContext.accept(this);
	    print(", JOINER, INPUT, " + position + ", ");
	    childContext.accept(this);
	    print(", sizeof(");
            print(outputType);
            print("), " + outputSize + ");");
	}
    }

    /**
     * Visits a work function entry.
     */
    public void visitWorkEntry(LIRWorkEntry self)
    {
        print("VARS_DEFAULTB();");
        print("LOCALIZE_DEFAULTB(");
        self.getStreamContext().accept(this);
        print(");");
    }

    /**
     * Visits a work function exit.
     */
    public void visitWorkExit(LIRWorkExit self)
    {
        print("UNLOCALIZE_DEFAULTB(");
        self.getStreamContext().accept(this);
        print(");");
    }


    // ----------------------------------------------------------------------
    // UTILS
    // ----------------------------------------------------------------------

    /**
     * prints an array length expression
     */
    public void visitSwitchLabel(JSwitchLabel self,
                                 JExpression expr) {
        newLine();
        if (expr != null) {
            print("case ");
            expr.accept(this);
            print(": ");
        } else {
            print("default: ");
        }
    }

    /**
     * prints an array length expression
     */
    public void visitSwitchGroup(JSwitchGroup self,
                                 JSwitchLabel[] labels,
                                 JStatement[] stmts) {
        for (int i = 0; i < labels.length; i++) {
            labels[i].accept(this);
        }
        pos += TAB_SIZE;
        for (int i = 0; i < stmts.length; i++) {
            newLine();
            stmts[i].accept(this);
        }
        pos -= TAB_SIZE;
    }

    /**
     * prints an array length expression
     */
    public void visitCatchClause(JCatchClause self,
                                 JFormalParameter exception,
                                 JBlock body) {
        print(" catch (");
        exception.accept(this);
        print(") ");
        body.accept(this);
    }

    /**
     * prints a boolean literal
     */
    public void visitBooleanLiteral(boolean value) {
        if (value)
            print(1);
        else
            print(0);
    }

    /**
     * prints a byte literal
     */
    public void visitByteLiteral(byte value) {
        print("((byte)" + value + ")");
    }

    /**
     * prints a character literal
     */
    public void visitCharLiteral(char value) {
        switch (value) {
        case '\b':
            print("'\\b'");
            break;
        case '\r':
            print("'\\r'");
            break;
        case '\t':
            print("'\\t'");
            break;
        case '\n':
            print("'\\n'");
            break;
        case '\f':
            print("'\\f'");
            break;
        case '\\':
            print("'\\\\'");
            break;
        case '\'':
            print("'\\''");
            break;
        case '\"':
            print("'\\\"'");
            break;
        default:
            print("'" + value + "'");
        }
    }

    /**
     * prints a double literal
     */
    public void visitDoubleLiteral(double value) {
        print("((double)" + value + ")");
    }

    /**
     * prints a float literal
     */
    public void visitFloatLiteral(float value) {
        print("((float)" + value + ")");
    }

    /**
     * prints a int literal
     */
    public void visitIntLiteral(int value) {
        print(value);
    }

    /**
     * prints a long literal
     */
    public void visitLongLiteral(long value) {
        print("(" + value + "L)");
    }

    /**
     * prints a short literal
     */
    public void visitShortLiteral(short value) {
        print("((short)" + value + ")");
    }

    /**
     * prints a string literal
     */
    public void visitStringLiteral(String value) {
        print('"' + value + '"');
    }

    /**
     * prints a null literal
     */
    public void visitNullLiteral() {
        print("null");
    }

    /**
     * prints an array length expression
     */
    public void visitPackageName(String name) {
        // print("package " + name + ";");
        // newLine();
    }

    /**
     * prints an array length expression
     */
    public void visitPackageImport(String name) {
        // print("import " + name.replace('/', '.') + ".*;");
    }

    /**
     * prints an array length expression
     */
    public void visitClassImport(String name) {
        // print("import " + name.replace('/', '.') + ";");
    }

    /**
     * prints an array length expression
     */
    public void visitFormalParameters(JFormalParameter self,
                                      boolean isFinal,
                                      CType type,
                                      String ident) {
        print(type);
        if (ident.indexOf("$") == -1) {
            print(" ");
            print(ident);
        }
    }

    /**
     * prints an array length expression
     */
    public void visitArgs(JExpression[] args, int base) {
        if (args != null) {
            for (int i = 0; i < args.length; i++) {
                if (i + base != 0) {
                    print(", ");
                }
                args[i].accept(this);
            }
        }
    }

    /**
     * prints an array length expression
     */
    public void visitConstructorCall(JConstructorCall self,
                                     boolean functorIsThis,
                                     JExpression[] params)
    {
        newLine();
        print(functorIsThis ? "this" : "super");
        print("(");
        visitArgs(params, 0);
        print(");");
    }

    /**
     * prints an array initializer expression
     */
    public void visitArrayInitializer(JArrayInitializer self,
                                      JExpression[] elems)
    {
        newLine();
        print("{");
        for (int i = 0; i < elems.length; i++) {
            if (i != 0) {
                print(", ");
            }
            elems[i].accept(this);
        }
        print("}");
    }

    
    // ----------------------------------------------------------------------
    // PROTECTED METHODS
    // ----------------------------------------------------------------------

    protected void newLine() {
        p.println();
    }

    // Special case for CTypes, to map some Java types to C types.
    protected void print(CType s) {
        if (s instanceof CArrayType)
        {
            print(((CArrayType)s).getBaseType());
            JExpression[] dims = ((CArrayType)s).getDims();
	    if (dims != null)
		for (int i = 0; i < dims.length; i++)
		    {
			print("[");
			dims[i].accept(this);
			print("]");
		    }
	    else
		for (int i = 0; i < ((CArrayType)s).getArrayBound(); i++)
		    print("*");
        }
        else if (s.getTypeID() == TID_BOOLEAN)
            print("int");
        else if (s.toString().endsWith("Portal"))
	    // ignore the specific type of portal in the C library
	    print("portal");
	else
            print(s.toString());
    }

    protected void print(Object s) {
        print(s.toString());
    }

    protected void print(String s) {
        p.setPos(pos);
        p.print(s);
    }

    protected void print(boolean s) {
        print("" + s);
    }

    protected void print(int s) {
        print("" + s);
    }

    protected void print(char s) {
        print("" + s);
    }

    protected void print(double s) {
        print("" + s);
    }

    // ----------------------------------------------------------------------
    // DATA MEMBERS
    // ----------------------------------------------------------------------

    //protected boolean			forInit;	// is on a for init
    protected int				TAB_SIZE = 2;
    protected int				WIDTH = 80;
    protected int				pos;
    protected String                      className;
    protected boolean isStruct;

    protected TabbedPrintWriter		p;
    protected StringWriter                str;
    protected boolean			nl = true;
    protected boolean                   declOnly = false;
}
