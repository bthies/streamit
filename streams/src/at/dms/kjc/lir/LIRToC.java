/*
 * LIRToC.java: convert StreaMIT low IR to C
 * $Id: LIRToC.java,v 1.114 2006-08-23 23:17:11 thies Exp $
 */

package at.dms.kjc.lir;

import java.io.*;
import java.util.Iterator;
import java.util.StringTokenizer;
import java.util.List;
import java.util.Map;
import at.dms.kjc.common.CommonConstants;
import at.dms.kjc.common.MacroConversion;
import at.dms.kjc.common.CodeGenerator;
import at.dms.util.InconsistencyException;
import at.dms.kjc.common.CodegenPrintWriter;
import at.dms.util.Utils;

import at.dms.kjc.sir.lowering.LoweringConstants;
import at.dms.kjc.sir.*;
import at.dms.kjc.*;
import at.dms.compiler.*;

public class LIRToC
    extends at.dms.kjc.common.ToCCommon
    implements SLIRVisitor,Constants, CodeGenerator
{
    /** >0 if in for loop header */
    private int forLoopHeader;

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
     * construct a pretty printer object for java code, with a given
     * set of array initializers defined.
     */
    private LIRToC(java.util.HashMap arrayInitializers) {
        super();
        this.arrayInitializers = arrayInitializers;
    }
    private LIRToC() {
        this(null);
    }

    /**
     * construct a pretty printer object for java code
     * @param   arrayInitializers       set of array initializers detected in code
     * @param   fileName                the file into the code is generated
     */
    private LIRToC(java.util.HashMap arrayInitializers, CodegenPrintWriter p) {
        super(p);
        this.portalCount = 0;
        this.portalNames = new java.util.HashMap();
        this.arrayInitializers = arrayInitializers;
    }

    /**
     * Generates code for <flatClass> and sends to System.out.
     */
    public static void generateCode(JClassDeclaration flat) {
        System.out.println("#include \"streamit.h\"");
        System.out.println("#include <stdio.h>");
        System.out.println("#include <stdlib.h>");
        System.out.println("#include <math.h>");
        printAtlasInterface();
        LIRToC l2c = new LIRToC(null, new CodegenPrintWriter(new PrintWriter(System.out)));

        // Print all of the portals.
        for (int i = 0; i < SIRPortal.getPortals().length; i++) {
            l2c.getPrinter().print("portal ");
            SIRPortal.getPortals()[i].accept(l2c);
            l2c.getPrinter().print(";");
            l2c.getPrinter().newLine();
        }

        // Print the static array initializers
        l2c.gatherArrayInitializers(flat);

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
                String filename = (Utils.getEnvironmentVariable("STREAMIT_HOME") +
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
                Utils.fail("IOException looking for atlas-interface.c");
            }
        }
    }

    /**
     * Close the stream at the end
     */
    public void close() {
        p.close();
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
                p.newLine();
            }
        }

        for (int i = 0; i < importedPackages.length ; i++) {
            if (!importedPackages[i].getName().equals("java/lang")) {
                importedPackages[i].accept(this);
                p.newLine();
            }
        }

        for (int i = 0; i < importedClasses.length ; i++) {
            importedClasses[i].accept(this);
            p.newLine();
        }

        for (int i = 0; i < typeDeclarations.length ; i++) {
            p.newLine();
            typeDeclarations[i].accept(this);
            p.newLine();
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
        LIRToC that = new LIRToC(arrayInitializers, this.p);
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

        p.newLine();
        p.print("typedef struct " + className + " {");
        p.indent();
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

        p.outdent();
        p.newLine();
        if (isStruct)
            p.print("} " + className + ";");
        else
            p.print("} _" + className + ", *" + className + ";");

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
                p.newLine();
                p.print("void " + iname + "_" + methods[i].getName() +
                        "(void *" + THIS_NAME + ", void *params)");
                p.newLine();
                p.print("{");
                p.indent();
                p.newLine();
                // The method matches the corresponding method in the
                // interface.
                CMethod im = imethods[i];
                String imName = iname + "_" + im.getIdent();
                // Cast the parameters struct...
                p.print(imName + "_params q = params;");
                // Call the actual function.
                p.newLine();
                p.print(methods[i].getName() + "(" + THIS_NAME);
                for (int j = 0; j < im.getParameters().length; j++)
                    p.print(", q->p" + j);
                p.print(");");
                p.outdent();
                p.newLine();
                p.print("}");
            }
        // Print the actual variable definition, too.  We can
        // just print the left-hand side explicitly and let the
        // visitor deal with the right.  Using the visitor for
        // the whole thing gets the wrong type (void).
        p.newLine();
        p.print("message_fn " + vardef.getIdent() + "[] = ");
        self.accept(this);
        p.print(";");
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
        p.print(" {");
        p.indent();
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
        p.outdent();
        p.newLine();
        p.print("}");
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
            p.newLine();
            p.print("typedef struct " + pname + " {");
            p.indent();
            for (int j = 0; j < params.length; j++) {
                p.newLine();
                printDecl(params[j], "p" + j);
                p.print(";");
            }
            p.outdent();
            p.newLine();
            p.print("} _" + pname + ", *" + pname + ";");

            // And now print a wrapper for send_message().
            p.newLine();
            p.print("void send_" + name + "(portal p, latency l");
            for (int j = 0; j < params.length; j++) {
                p.print(", ");
                printDecl(params[j], "p" + j);
            }
            p.print(") {");
            p.indent();
            p.newLine();
            p.print(pname + " q = malloc(sizeof(_" + pname + "));");
            for (int j = 0; j < params.length; j++) {
                p.newLine();
                p.print("q->p" + j + " = p" + j + ";");
            }
            p.newLine();
            p.print("send_message(p, " + i + ", l, q);");
            p.outdent();
            p.newLine();
            p.print("}");
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


        
        p.newLine();
        if (expr instanceof JArrayInitializer) {
            // just declare the beginning of array here; initializer copied over later
            declareInitializedArray(findBaseType((JArrayInitializer)expr),
                                    ident,
                                    expr,
                                    this,
                                    false);
        } else {
            printDecl (type, ident);
        
            if (expr != null && !(expr instanceof JNewArrayExpression)) {
                p.print ("\t= ");
                expr.accept (this);
            } 
            p.print(";");
        }
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

        // try converting to macro
        if (MacroConversion.shouldConvert(self)) {
            MacroConversion.doConvert(self, declOnly, this);
            return;
        }

        p.newLine();
        // Treat main() specially.
        if (ident.equals("main"))
            {
                p.print("int main(int argc, char **argv)");
            }
        else
            {
                // p.print(CModifier.toString(modifiers));
                printType(returnType);
                p.print(" ");
                p.print(ident);
                p.print("(");
                int count = 0;
            
                for (int i = 0; i < parameters.length; i++) {
                    if (count != 0) {
                        p.print(", ");
                    }
                
                    // if (!parameters[i].isGenerated()) {
                    parameters[i].accept(this);
                    count++;
                    // }
                }
                p.print(")");
            }
        
        if (declOnly)
            {
                p.print(";");
                return;
            }

        p.print(" ");
        if (body != null) {
            body.accept(this);
        } else {
            p.print(";");
        }
        p.newLine();
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
        p.newLine();
        p.print(CModifier.toString(modifiers));
        p.print(ident);
        p.print("_");
        p.print(ident);
        p.print("(");
        int count = 0;
        for (int i = 0; i < parameters.length; i++) {
            if (count != 0) {
                p.print(", ");
            }
            if (!parameters[i].isGenerated()) {
                parameters[i].accept(this);
                count++;
            }
        }
        p.print(")");
        /*
          for (int i = 0; i < exceptions.length; i++) {
          if (i != 0) {
          p.print(", ");
          } else {
          p.print(" throws ");
          }
          p.print(exceptions[i].toString());
          }
        */
        p.print(" ");
        body.accept(this);
        p.newLine();
    }

    // ----------------------------------------------------------------------
    // STATEMENT
    // ----------------------------------------------------------------------

    /**
     * prints a variable declaration statement
     */
    public void visitVariableDefinition(JVariableDefinition self,
                                        int modifiers,
                                        CType type,
                                        String ident,
                                        JExpression expr) {
        if (expr instanceof JArrayInitializer) {
            declareInitializedArray(findBaseType((JArrayInitializer)expr),
                                    ident,
                                    expr,
                                    this,
                                    true);
        } else {

            printDecl (type, ident);
        
            if (expr != null && !(expr instanceof JNewArrayExpression)) {
                p.print ("\t= ");
                expr.accept (this);
            } 
            p.print(";");
        }
    }

    /**
     * prints a try-catch statement
     */
    public void visitTryCatchStatement(JTryCatchStatement self,
                                       JBlock tryClause,
                                       JCatchClause[] catchClauses) {
        p.print("try ");
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
        p.print("try ");
        tryClause.accept(this);
        if (finallyClause != null) {
            p.print(" finally ");
            finallyClause.accept(this);
        }
    }

    /**
     * prints a throw statement
     */
    public void visitThrowStatement(JThrowStatement self,
                                    JExpression expr) {
        p.print("throw ");
        expr.accept(this);
        p.print(";");
    }

    /**
     * prints a synchronized statement
     */
    public void visitSynchronizedStatement(JSynchronizedStatement self,
                                           JExpression cond,
                                           JStatement body) {
        p.print("synchronized (");
        cond.accept(this);
        p.print(") ");
        body.accept(this);
    }

    /*
     * prints a switch statement
     * visitSwitchStatement in ToCCommon
     */
      
    /*
     * prints a return statement
     * visitReturnStatement in ToCCommon
     */

    /*
     * prints a labeled statement
     * visitLabeledStatement  in ToCCommon
     */


    /**
     * prints a if statement
     */
    public void visitIfStatement(JIfStatement self,
                                 JExpression cond,
                                 JStatement thenClause,
                                 JStatement elseClause) {
        p.print("if (");
        cond.accept(this);
        p.print(") ");
        if (!(thenClause instanceof JBlock)) p.indent();
        thenClause.accept(this);
        if (!(thenClause instanceof JBlock)) p.outdent();
        if (elseClause != null) {
            if ((elseClause instanceof JBlock) || (elseClause instanceof JIfStatement)) {
                p.print(" ");
            } else {
                p.newLine();
            }
            p.print("else ");
            if (!(elseClause instanceof JBlock 
                  || elseClause instanceof JIfStatement)) p.indent();
            elseClause.accept(this);
            if (!(elseClause instanceof JBlock 
                  || elseClause instanceof JIfStatement)) p.outdent();
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
        forLoopHeader++;
        p.print("for (");
        //forInit = true;
        if (init != null) {
            init.accept(this);
        } else {
            p.print(";");
        }
        //forInit = false;

        p.print(" ");
        if (cond != null) {
            cond.accept(this);
        }
        p.print("; ");

        if (incr != null) {
            LIRToC l2c = new LIRToC(arrayInitializers);
            incr.accept(l2c);
            // get String
            String str = l2c.getPrinter().getString();
            // leave off the trailing semicolon if there is one
            if (str.endsWith(";")) {
                p.print(str.substring(0, str.length() - 1));
            } else {
                p.print(str);
            }
        }
        forLoopHeader--;
        p.print(") {");
        p.newLine();
          
        p.indent();
        body.accept(this);
        p.outdent();
        p.newLine();
        p.print("}");
    }

    /*
     * prints a compound statement: 2-argument form
     * visitCompoundStatement in ToCCommon
     */


    /*
     * prints a compound statement
     * visitCompoundStatement in ToCCommon
     */

    /*
     * prints an expression statement
     * visitExpressionStatement in ToCCommon
     */

    /*
     * prints an expression list statement
     * visitExpressionListStatement in ToCCommon
     */

    /**
     * prints a empty statement
     */
    public void visitEmptyStatement(JEmptyStatement self) {
        //if we are inside a for loop header, we need to print 
        //the ; of an empty statement
        if (forLoopHeader > 0) {
            // RMR (don't offset the ; by a new line) // p.newLine();
            p.print(";");
        }
    }

    /*
     * prints a do statement 
     * visitDoStatement in ToCCommon
     */

    /*
     * prints a continue statement
     * visitContinueStatement in ToCCommon
     */

    /*
     * prints a break statement
     * visitBreakStatement in ToCCommon
     */

    /*
     * prints an expression statement
     * visitBlockStatement  in ToCCommon
     */

    /*
     * prints a type declaration statement
     * visitTypeDeclarationStatement  in ToCCommon
     */

    // ----------------------------------------------------------------------
    // EXPRESSION
    // ----------------------------------------------------------------------
    /**
     * prints a this expression
     */
    public void visitThisExpression(JThisExpression self,
                                    JExpression prefix) {
        if (prefix != null) {
            prefix.accept(this);
            p.print("." + THIS_NAME);
        } else {
            p.print(THIS_NAME);
        }
    }

    /**
     * prints a super expression
     */
    public void visitSuperExpression(JSuperExpression self) {
        p.print("super");
    }

    /**
     * prints a shift expressiona
     */
    public void visitRelationalExpression(JRelationalExpression self,
                                          int oper,
                                          JExpression left,
                                          JExpression right) {
        p.print("(");
        left.accept(this);
        switch (oper) {
        case OPE_LT:
            p.print(" < ");
            break;
        case OPE_LE:
            p.print(" <= ");
            break;
        case OPE_GT:
            p.print(" > ");
            break;
        case OPE_GE:
            p.print(" >= ");
            break;
        default:
            throw new InconsistencyException(); // only difference from ToC
        }
        right.accept(this);
        p.print(")");
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
        p.print(".new " + ident + "(");
        visitArgs(params, 0);
        p.print(")");
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
        p.print(".new " + ident + "(");
        visitArgs(params, 0);
        p.print(")");
    }

    /**
     * Prints an unqualified anonymous class instance creation expression.
     */
    public void visitUnqualifiedAnonymousCreation(JUnqualifiedAnonymousCreation self,
                                                  CClassType type,
                                                  JExpression[] params,
                                                  JClassDeclaration decl)
    {
        /* It appears this only occurs in dead cody ("new Complex()",
         * FFT6) and it doesn't make sense in C, so just removing
         * this.

         p.print("new " + type + "(");
         visitArgs(params, 0);
         p.print(")");
        */
        // decl.genInnerJavaCode(this);
    }

    /**
     * Prints an unqualified instance creation expression.
     */
    public void visitUnqualifiedInstanceCreation(JUnqualifiedInstanceCreation self,
                                                 CClassType type,
                                                 JExpression[] params)
    {
        /* It appears this only occurs in dead cody ("new Complex()",
         * FFT6) and it doesn't make sense in C, so just removing
         * this.

         p.print("new " + type + "(");
         visitArgs(params, 0);
         p.print(")");
        */
    }

    /*
     * prints an array allocator expression
     *
     *  public void visitNewArrayExpression
     *  see at/dms/kjc/common.ToCCommon
     */

    /**
     * prints a name expression
     */
    public void visitNameExpression(JNameExpression self,
                                    JExpression prefix,
                                    String ident) {
        p.print("(");
        if (prefix != null) {
            prefix.accept(this);
            p.print("->");
        }
        p.print(ident);
        p.print(")");
    }

    /**
     * prints an binary expression
     */
    public void visitBinaryExpression(JBinaryExpression self,
                                      String oper,
                                      JExpression left,
                                      JExpression right) {
        printLParen();
        left.accept(this);
        p.print(" ");
        p.print(oper);
        p.print(" ");
        right.accept(this);
        printRParen();
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

        p.print(ident);

        //we want single precision versions of the math functions
        if (Utils.isMathMethod(prefix, ident)) 
            p.print("f");

        p.print("(");
        int i = 0;
        /* Ignore prefix, since it's just going to be a Java class name.
           if (prefix != null) {
           prefix.accept(this);
           i++;
           }
        */
        visitArgs(args, i);
        p.print(")");
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
            p.print("((" + left.getType().getCClass().getOwner().getType() +
                    ")(" + THIS_CONTEXT_NAME + "->parent->stream_data))");
            return;
        }
        int             index = ident.indexOf("_$");
        if (index != -1) {
            p.print(ident.substring(0, index));      // local var
        } else {
            p.print("(");
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
                p.print(".");
            else
                p.print("->");
            p.print(ident);
            p.print(")");
        }
    }

    /**
     * prints a compound assignment expression
     */
    public void visitBitwiseExpression(JBitwiseExpression self,
                                       int oper,
                                       JExpression left,
                                       JExpression right) {
        printLParen();
        left.accept(this);
        switch (oper) {
        case OPE_BAND:
            p.print(" & ");
            break;
        case OPE_BOR:
            p.print(" | ");
            break;
        case OPE_BXOR:
            p.print(" ^ ");
            break;
        default:
            throw new InconsistencyException(); // only difference with ToC
        }
        right.accept(this);
        printRParen();
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
        if (right instanceof JArrayInitializer &&
            arrayInitializers.containsKey(right)) {
            // memcpy for arrays
            p.print("memcpy(");
            left.accept(this);
            p.print(",");
            String name = (String)arrayInitializers.get(right);
            p.print(name);
            p.print(",");
            p.print(findSize((JArrayInitializer)right) + " * sizeof(");
            printType(findBaseType((JArrayInitializer)right));
            p.print("))");
            return;
        } 

        // copy arrays element-wise
        boolean arrayType = ((left.getType()!=null && left.getType().isArrayType()) ||
                             (right.getType()!=null && right.getType().isArrayType()));
        if (arrayType && !(right instanceof JNewArrayExpression)) {

            CArrayType type = (CArrayType)right.getType();
            JExpression[] dims = type.getDims();

            p.print("{\n");
            p.print("int ");
            // print the index var decls
            for (int i = 0; i < dims.length - 1; i++)
                p.print(CommonConstants.ARRAY_COPY + i + ", ");
            p.print(CommonConstants.ARRAY_COPY + (dims.length - 1));
            p.print(";\n");
            for (int i = 0; i < dims.length; i++) {
                p.print("for (" + CommonConstants.ARRAY_COPY + i + " = 0; "
                        + CommonConstants.ARRAY_COPY + i + " < ");
                dims[i].accept(this);
                p.print("; " + CommonConstants.ARRAY_COPY + i + "++)\n");
            }
            left.accept(this);
            for (int i = 0; i < dims.length; i++)
                p.print("[" + CommonConstants.ARRAY_COPY + i + "]");
            p.print(" = ");
            right.accept(this);
            for (int i = 0; i < dims.length; i++)
                p.print("[" + CommonConstants.ARRAY_COPY + i + "]");
            p.print(";\n}\n");

            return;

        }

        lastLeft=left;
        printLParen();
        left.accept(this);
        p.print(" = ");
        right.accept(this);
        printRParen();
        lastLeft=null;
    }

    /**
     * prints an array length expression
     */
    public void visitArrayLengthExpression(JArrayLengthExpression self,
                                           JExpression prefix) {
        prefix.accept(this);
        p.print(".length");
    }

    /**
     * prints an array access expression
     */
    public void visitArrayAccessExpression(JArrayAccessExpression self,
                                           JExpression prefix,
                                           JExpression accessor) {
        printLParen();
        prefix.accept(this);
        p.print("[");
        accessor.accept(this);
        p.print("]");
        printRParen();
    }

    /**
     * prints a comment expression
     */
    public void visitComments(JavaStyleComment[] comments) {
        for (int i = 0; i < comments.length; i++) {
            if (comments[i] != null) {
                visitComment(comments[i]);
            }
        }
    }

    /**
     * prints a comment expression
     */
    public void visitComment(JavaStyleComment comment) {
        StringTokenizer tok = new StringTokenizer(comment.getText(), "\n");

        if (comment.hadSpaceBefore()) {
            p.newLine();
        }

        if (comment.isLineComment()) {
            p.print("//");
            if (tok.hasMoreTokens())
                p.print(tok.nextToken().trim());
            p.newLine();
        } else {
            if (p.getLine() > 0) {
                if (!nl) {
                    p.newLine();
                }
                p.newLine();
            }
            p.print("/*");
            while (tok.hasMoreTokens()){
                String comm = tok.nextToken().trim();
                if (comm.startsWith("*")) {
                    comm = comm.substring(1).trim();
                }
                if (tok.hasMoreTokens() || comm.length() > 0) {
                    p.newLine();
                    p.print(" * " + comm);
                }
            }
            p.newLine();
            p.print(" */");
            p.newLine();
        }

        if (comment.hadSpaceAfter()) {
            p.newLine();
        }
    }

    /**
     * prints a Javadoc expression
     */
    public void visitJavadoc(JavadocComment comment) {
        StringTokenizer tok = new StringTokenizer(comment.getText(), "\n");
        boolean         isFirst = true;

        if (!nl) {
            p.newLine();
        }
        p.newLine();
        p.print("/**");
        while (tok.hasMoreTokens()) {
            String      text = tok.nextToken().trim();
            String      type = null;
            boolean     param = false;
            int idx = text.indexOf("@param");
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
                p.newLine();
                isFirst = false;
                if (param) {
                    text = text.substring(idx + type.length()).trim();
                    idx = Math.min(text.indexOf(" ") == -1 ? Integer.MAX_VALUE : text.indexOf(" "),
                                   text.indexOf("\t") == -1 ? Integer.MAX_VALUE : text.indexOf("\t"));
                    if (idx == Integer.MAX_VALUE) {
                        idx = 0;
                    }
                    String      before = text.substring(0, idx);
                    p.print(" * " + type);
                    p.setIndentation(p.getIndentation() + 12);
                    p.print(before);
                    p.setIndentation(p.getIndentation() + 20);
                    p.print(text.substring(idx).trim());
                    p.setIndentation(p.getIndentation() - 20);
                    p.setIndentation(p.getIndentation() - 12);
                } else {
                    text = text.substring(idx + type.length()).trim();
                    p.print(" * " + type);
                    p.setIndentation(p.getIndentation() + 12);
                    p.print(text);
                    p.setIndentation(p.getIndentation() - 12);
                }
            } else {
                text = text.substring(text.indexOf("*") + 1);
                if (tok.hasMoreTokens() || text.length() > 0) {
                    p.newLine();
                    p.print(" * ");
                    if (isFirst) p.setIndentation(p.getIndentation() + 32);
                    p.print(text.trim());
                    if (isFirst) p.setIndentation(p.getIndentation() - 32);
                }
            }
        }
        p.newLine();
        p.print(" */");
    }

    // ----------------------------------------------------------------------
    // STREAMIT IR HANDLERS
    // ----------------------------------------------------------------------

    public void visitCreatePortalExpression(SIRCreatePortal self) {
        p.print("create_portal()");
    }

    public void visitInitStatement(SIRInitStatement self,
                                   SIRStream stream)
    {
        p.print("/* InitStatement */");
    }

    public void visitInterfaceTable(SIRInterfaceTable self)
    {
        String iname = self.getIface().getIdent();
        JMethodDeclaration[] methods = self.getMethods();
        boolean first = true;
        
        p.print("{ ");
        for (int i = 0; i < methods.length; i++)
            {
                if (!first) p.print(", ");
                first = false;
                p.print(iname + "_" + methods[i].getName());
            }
        p.print("}");
    }
    
    public void visitLatency(SIRLatency self)
    {
        p.print("LATENCY_BEST_EFFORT");
    }
    
    public void visitLatencyMax(SIRLatencyMax self)
    {
        p.print("LATENCY_BEST_EFFORT");
    }
    
    public void visitLatencyRange(SIRLatencyRange self)
    {
        p.print("LATENCY_BEST_EFFORT");
    }
    
    public void visitLatencySet(SIRLatencySet self)
    {
        p.print("LATENCY_BEST_EFFORT");
    }

    public void visitMessageStatement(SIRMessageStatement self,
                                      JExpression portal,
                                      String iname,
                                      String ident,
                                      JExpression[] params,
                                      SIRLatency latency)
    {
        p.print("send_" + iname + "_" + ident + "(");
        portal.accept(this);
        p.print(", ");
        latency.accept(this);
        if (params != null)
            for (int i = 0; i < params.length; i++)
                if (params[i] != null)
                    {
                        p.print(", ");
                        params[i].accept(this);
                    }
        p.print(");");
    }

    public void visitRangeExpression(SIRRangeExpression self) {
        assert false : "Do not yet support dynamic rates in uniprocessor backend.";
    }

    public void visitDynamicToken(SIRDynamicToken self) {
        assert false : "Do not yet support dynamic rates in uniprocessor backend.";
    }

    public void visitPeekExpression(SIRPeekExpression self,
                                    CType tapeType,
                                    JExpression num)
    {
        p.print("(PEEK_DEFAULTB(");
        if (tapeType != null)
            printType(tapeType);
        else
            p.print("/* null tapeType! */ int");
        p.print(", ");
        num.accept(this);
        p.print("))");
    }
    
    public void visitPopExpression(SIRPopExpression self,
                                   CType tapeType)
    {
        if (self.getNumPop()>1) {
            p.print("(POP_DEFAULTB_N( ");
            if (tapeType != null)
                printType(tapeType);
            else
                p.print("/* null tapeType! */ int");
            p.print(", " + self.getNumPop() + " ))");
        } else {
            p.print("(POP_DEFAULTB( ");
            if (tapeType != null)
                printType(tapeType);
            else
                p.print("/* null tapeType! */ int");
            p.print("))");
        }
    }
    
    public void visitPortal(SIRPortal self)
    {
        // Have we seen this portal before?
        if (!(portalNames.containsKey(self)))
            {
                portalCount++;
                String theName = "__portal_" + portalCount;
                portalNames.put(self, theName);
            }
        p.print((String)portalNames.get(self));
    }

    // visitPrintStatement inherited from ToCCommon.
    
    public void visitPushExpression(SIRPushExpression self,
                                    CType tapeType,
                                    JExpression val)
    {
        p.print("(PUSH_DEFAULTB(");
        if (tapeType != null) {
            printType(tapeType);
        } else
            p.print("/* null tapeType! */ int");
        p.print(", ");
        val.accept(this);
        p.print("))");
    }
    
    public void visitRegReceiverStatement(SIRRegReceiverStatement self,
                                          JExpression portal,
                                          SIRStream receiver, 
                                          JMethodDeclaration[] methods)
    {
        p.print("register_receiver(");
        portal.accept(this);
        p.print(", " + THIS_CONTEXT_NAME + ", ");
        p.print(self.getItable().getVarDecl().getIdent());
        p.print(", LATENCY_BEST_EFFORT);");
        // (But shouldn't there be a latency field in here?)
    }
    
    public void visitRegSenderStatement(SIRRegSenderStatement self,
                                        String fn,
                                        SIRLatency latency)
    {
        p.print("register_sender(this->context, ");
        p.print(fn);
        p.print(", ");
        latency.accept(this);
        p.print(");");
    }


    /**
     * Visits a file reader.
     */
    public void visitFileReader(LIRFileReader self) {
        String childName = THIS_NAME + "->" + self.getChildName();
        p.print(childName + " = malloc(sizeof(_ContextContainer));");
        p.newLine();
        p.print(childName + "->" + CONTEXT_NAME +
                " = streamit_filereader_create(\"" +
                self.getFileName() + "\");");
        p.newLine();
        p.print("register_child(");
        self.getStreamContext().accept(this);
        p.print(", " + childName + "->" + CONTEXT_NAME + ");");
    }

    /**
     * Visits a file writer.
     */
    public void visitFileWriter(LIRFileWriter self) {
        String childName = THIS_NAME + "->" + self.getChildName();
        p.print(childName + " = malloc(sizeof(_ContextContainer));");
        p.newLine();
        p.print(childName + "->" + CONTEXT_NAME +
                " = streamit_filewriter_create(\"" +
                self.getFileName() + "\");");
        p.newLine();
        p.print("register_child(");
        self.getStreamContext().accept(this);
        p.print(", " + childName + "->" + CONTEXT_NAME + ");");
    }

    /**
     * Visits an identity filter.
     */
    public void visitIdentity(LIRIdentity self) 
    {
        String childName = THIS_NAME + "->" + self.getChildName();
        p.print(childName + " = malloc(sizeof(_ContextContainer));");
        p.newLine();
        p.print(childName + "->" + CONTEXT_NAME +
                " = streamit_identity_create();");
        p.newLine();
        p.print("register_child(");
        self.getStreamContext().accept(this);
        p.print(", " + childName + "->" + CONTEXT_NAME + ");");
    }

    public void visitSetChild(LIRSetChild self,
                              JExpression streamContext,
                              String childType,
                              String childName)
    {
        // Pay attention, three statements!
        p.print(THIS_NAME + "->" + childName +
                " = malloc(sizeof(_" + childType + "));");
        p.newLine();
        p.print(THIS_NAME + "->" + childName + "->" + CONTEXT_NAME + " = " +
                "create_context(" + THIS_NAME + "->" + childName + ");");
        p.newLine();
        p.print("register_child(");
        streamContext.accept(this);
        p.print(", " + THIS_NAME + "->" + childName + "->" + CONTEXT_NAME +
                ");");
    }
    
    public void visitSetTape(LIRSetTape self,
                             JExpression streamContext,
                             JExpression srcStruct,
                             JExpression dstStruct,
                             CType type,
                             int size)
    {
        p.print("create_tape(");
        srcStruct.accept(this);
        p.print("->" + CONTEXT_NAME + ", ");
        dstStruct.accept(this);
        p.print("->" + CONTEXT_NAME + ", sizeof(");
        printType(type);
        p.print("), " + size + ");");
    }
    
    /**
     * Visits a function pointer.
     */
    public void visitFunctionPointer(LIRFunctionPointer self,
                                     String name)
    {
        // This is an expression.
        p.print(name);
    }
    
    /**
     * Visits an LIR node.
     */
    public void visitNode(LIRNode self)
    {
        // This should never be called directly.
        p.print("/* Unexpected visitNode */");
    }

    /**
     * Visits an LIR register-receiver statement.
     */
    public void visitRegisterReceiver(LIRRegisterReceiver self,
                                      JExpression streamContext,
                                      SIRPortal portal,
                                      String childName,
                                      SIRInterfaceTable itable)
    {
        p.print("register_receiver(");
        portal.accept(this);
        p.print(", ");
        p.print(THIS_NAME + "->" + childName + "->" + CONTEXT_NAME + ", ");
        p.print(itable.getVarDecl().getIdent());
        p.print(", LATENCY_BEST_EFFORT);");
        // (But shouldn't there be a latency field in here?)
    }

    /**
     * Visits a child registration node.
     */
    public void visitSetChild(LIRSetChild self,
                              JExpression streamContext,
                              JExpression childContext)
    {
        // This is a statement.
        p.newLine();
        p.print("register_child(");
        streamContext.accept(this);
        p.print(", ");
        childContext.accept(this);
        p.print(");");
    }
    
    /**
     * Visits a decoder registration node.
     */
    public void visitSetDecode(LIRSetDecode self,
                               JExpression streamContext,
                               LIRFunctionPointer fp)
    {
        p.print("set_decode(");
        streamContext.accept(this);
        p.print(", ");
        fp.accept(this);
        p.print(");");
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
        p.print("FEEDBACK_DELAY(");
        data.accept(this);
        p.print(", ");
        streamContext.accept(this);
        p.print(", " + delay + ", ");
        printType(type);
        p.print(", ");
        fp.accept(this);
        p.print(");");
    }
    
    /**
     * Visits an encoder registration node.
     */
    public void visitSetEncode(LIRSetEncode self,
                               JExpression streamContext,
                               LIRFunctionPointer fp)
    {
        p.print("set_encode(");
        streamContext.accept(this);
        p.print(", ");
        fp.accept(this);
        p.print(");");
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
        p.print("set_joiner(");
        streamContext.accept(this);
        p.print(", ");
        p.print(type.toString());
        p.print(", " + String.valueOf(ways));
        if (weights != null)
            {
                for (int i = 0; i < weights.length; i++)
                    p.print(", " + String.valueOf(weights[i]));
            }
        p.print(");");
    }

    /**
     * Visits a peek-rate-setting node.
     */
    public void visitSetPeek(LIRSetPeek self,
                             JExpression streamContext,
                             int peek)
    {
        p.print("set_peek(");
        streamContext.accept(this);
        p.print(", " + peek + ");");
    }
    
    /**
     * Visits a pop-rate-setting node.
     */
    public void visitSetPop(LIRSetPop self,
                            JExpression streamContext,
                            int pop)
    {
        p.print("set_pop(");
        streamContext.accept(this);
        p.print(", " + pop + ");");
    }
    
    /**
     * Visits a push-rate-setting node.
     */
    public void visitSetPush(LIRSetPush self,
                             JExpression streamContext,
                             int push)
    {
        p.print("set_push(");
        streamContext.accept(this);
        p.print(", " + push + ");");
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
        p.print("set_splitter(");
        streamContext.accept(this);
        p.print(", " + type + ", " + String.valueOf(ways));
        if (weights != null)
            {
                for (int i = 0; i < weights.length; i++)
                    p.print(", " + String.valueOf(weights[i]));
            }
        p.print(");");
    }

    /**
     * Visits a stream-type-setting node.
     */
    public void visitSetStreamType(LIRSetStreamType self,
                                   JExpression streamContext,
                                   LIRStreamType streamType)
    {
        p.print("set_stream_type(");
        streamContext.accept(this);
        p.print(", " + streamType + ");");
    }
    
    /**
     * Visits a work-function-setting node.
     */
    public void visitSetWork(LIRSetWork self,
                             JExpression streamContext,
                             LIRFunctionPointer fn)
    {
        p.print("set_work(");
        streamContext.accept(this);
        p.print(", (work_fn)");
        fn.accept(this);
        p.print(");");
    }

    public void visitMainFunction(LIRMainFunction self,
                                  String typeName,
                                  LIRFunctionPointer init,
                                  List initStatements)
    {
        p.print(typeName + " " + THIS_NAME +
                " = malloc(sizeof(_" + typeName + "));");
        p.newLine();
        p.print(THIS_CONTEXT_NAME + " = create_context(" + THIS_NAME + ");");
        p.newLine();
        init.accept(this);
        p.print("(" + THIS_NAME + ");");
        p.newLine();
        p.print("connect_tapes(" + THIS_CONTEXT_NAME + ");");
        p.newLine();
        Iterator iter = initStatements.iterator();
        while (iter.hasNext())
            ((JStatement)(iter.next())).accept(this);
        p.newLine();
        p.print("streamit_run(" + THIS_CONTEXT_NAME + ", argc, argv);");
        p.newLine();
        p.print("return 0;");
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
        p.print("create_splitjoin_tape(");
        streamContext.accept(this);
        p.print(", JOINER, OUTPUT, 0, ");
        childContext.accept(this);
        p.print(", sizeof(");
        printType(inputType);
        p.print("), " + inputSize + ");");
        p.newLine();
        p.print("create_splitjoin_tape(");
        streamContext.accept(this);
        p.print(", SPLITTER, INPUT, 0, ");
        childContext.accept(this);
        p.print(", sizeof(");
        printType(outputType);
        p.print("), " + outputSize + ");");
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
        p.print("create_splitjoin_tape(");
        streamContext.accept(this);
        p.print(", SPLITTER, OUTPUT, 1, ");
        childContext.accept(this);
        p.print(", sizeof(");
        printType(inputType);
        p.print("), " + inputSize + ");");
        p.newLine();
        p.print("create_splitjoin_tape(");
        streamContext.accept(this);
        p.print(", JOINER, INPUT, 1, ");
        childContext.accept(this);
        p.print(", sizeof(");
        printType(outputType);
        p.print("), " + outputSize + ");");
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
            p.print("create_splitjoin_tape(");
            streamContext.accept(this);
            p.print(", SPLITTER, OUTPUT, " + position + ", ");
            childContext.accept(this);
            p.print(", sizeof(");
            printType(inputType);
            p.print("), " + inputSize + ");");
            p.newLine();
        }
        if (outputSize!=0) {
            p.print("create_splitjoin_tape(");
            streamContext.accept(this);
            p.print(", JOINER, INPUT, " + position + ", ");
            childContext.accept(this);
            p.print(", sizeof(");
            printType(outputType);
            p.print("), " + outputSize + ");");
        }
    }

    /**
     * Visits a work function entry.
     */
    public void visitWorkEntry(LIRWorkEntry self)
    {
        p.print("VARS_DEFAULTB();");
        p.newLine();
        p.print("LOCALIZE_DEFAULTB(");
        self.getStreamContext().accept(this);
        p.print(");");
    }

    /**
     * Visits a work function exit.
     */
    public void visitWorkExit(LIRWorkExit self)
    {
        p.print("UNLOCALIZE_DEFAULTB(");
        self.getStreamContext().accept(this);
        p.print(");");
    }


    // ----------------------------------------------------------------------
    // UTILS
    // ----------------------------------------------------------------------

    /**
     * prints an array length expression
     */
    public void visitSwitchLabel(JSwitchLabel self,
                                 JExpression expr) {
        p.newLine();
        if (expr != null) {
            p.print("case ");
            expr.accept(this);
            p.print(": ");
        } else {
            p.print("default: ");
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
        p.indent();
        for (int i = 0; i < stmts.length; i++) {
            p.newLine();
            stmts[i].accept(this);
        }
        p.outdent();
    }

    /**
     * prints an array length expression
     */
    public void visitCatchClause(JCatchClause self,
                                 JFormalParameter exception,
                                 JBlock body) {
        p.print(" catch (");
        exception.accept(this);
        p.print(") ");
        body.accept(this);
    }

    /**
     * prints a boolean literal
     */
    public void visitBooleanLiteral(boolean value) {
        if (value)
            p.print(1);
        else
            p.print(0);
    }

    /**
     * prints a byte literal
     */
    public void visitByteLiteral(byte value) {
        p.print("((byte)" + value + ")");
    }

    /**
     * prints a character literal
     */
    public void visitCharLiteral(char value) {
        switch (value) {
        case '\b':
            p.print("'\\b'");
            break;
        case '\r':
            p.print("'\\r'");
            break;
        case '\t':
            p.print("'\\t'");
            break;
        case '\n':
            p.print("'\\n'");
            break;
        case '\f':
            p.print("'\\f'");
            break;
        case '\\':
            p.print("'\\\\'");
            break;
        case '\'':
            p.print("'\\''");
            break;
        case '\"':
            p.print("'\\\"'");
            break;
        default:
            p.print("'" + value + "'");
        }
    }

    /**
     * prints a double literal
     */
    public void visitDoubleLiteral(double value) {
        p.print("((double)" + value + ")");
    }

    /**
     * prints a float literal
     */
    public void visitFloatLiteral(float value) {
        p.print("((float)" + value + ")");
    }

    /**
     * prints a int literal
     */
    public void visitIntLiteral(int value) {
        p.print(value);
    }

    /**
     * prints a long literal
     */
    public void visitLongLiteral(long value) {
        p.print("(" + value + "L)");
    }

    /**
     * prints a short literal
     */
    public void visitShortLiteral(short value) {
        p.print("((short)" + value + ")");
    }

    /**
     * prints a string literal
     */
    public void visitStringLiteral(String value) {
        p.print('"' + value + '"');
    }

    /**
     * prints a null literal
     */
    public void visitNullLiteral() {
        p.print("null");
    }

    /**
     * prints an array length expression
     */
    public void visitPackageName(String name) {
        // p.print("package " + name + ";");
        // p.newLine();
    }

    /**
     * prints an array length expression
     */
    public void visitPackageImport(String name) {
        // p.print("import " + name.replace('/', '.') + ".*;");
    }

    /**
     * prints an array length expression
     */
    public void visitClassImport(String name) {
        // p.print("import " + name.replace('/', '.') + ";");
    }

    /**
     * prints an array length expression
     */
    public void visitFormalParameters(JFormalParameter self,
                                      boolean isFinal,
                                      CType type,
                                      String ident) {
        if (ident.indexOf("$") == -1) {
            printDecl(type, ident);
        } else {
            // does this case actually happen?  just preseving
            // semantics of previous version, but this doesn't make
            // sense to me.  --bft
            printType(type);
        }
    }

    /**
     * prints an array length expression
     */
    public void visitArgs(JExpression[] args, int base) {
        if (args != null) {
            for (int i = 0; i < args.length; i++) {
                if (i + base != 0) {
                    p.print(", ");
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
        p.newLine();
        p.print(functorIsThis ? "this" : "super");
        p.print("(");
        visitArgs(params, 0);
        p.print(");");
    }

    /**
     * prints an array initializer expression
     */
    public void visitArrayInitializer(JArrayInitializer self,
                                      JExpression[] elems)
    {
        /*
        // refer to previously declared static initializers
        if (arrayInitializers.containsKey(self)) {
        // cast to pointer type
        p.print("(");
        p.print(findBaseType(self));
        int dims = findNumDims(self);
        for (int i=0; i<dims; i++) {
        p.print("*");
        }
        p.print(")");
        // print name of array
        String name = (String)arrayInitializers.get(self);
        p.print(name);
        } else {
        */
        p.newLine();
        p.print("{");
        for (int i = 0; i < elems.length; i++) {
            if (i != 0) {
                p.print(", ");
            }
            elems[i].accept(this);
        }
        p.print("}");
        //}
    }

    
    // ----------------------------------------------------------------------
    // PROTECTED METHODS
    // ----------------------------------------------------------------------

    /**
     * Tries to find the number of dimensions of <self>.
     */
    protected int findNumDims(JArrayInitializer self) {
        int dims = 0;
        JExpression expr = self;
        while(true) {
            if (expr instanceof JArrayInitializer) {
                dims++;
                expr = ((JArrayInitializer)expr).getElems()[0];
            } else {
                break;
            }
        }
        return dims;
    }

    /**
     * Returns the total number of elements in a multi-dimensional
     * array.  For example, "A[2][2]" has 4 elements.
     */
    protected int findSize(JArrayInitializer self) {
        int count = 1;
        JExpression expr = self;
        while(true) {
            if (expr instanceof JArrayInitializer) {
                count *= ((JArrayInitializer)expr).getElems().length;
                expr = ((JArrayInitializer)expr).getElems()[0];
            } else {
                break;
            }
        }
        return count;
    }

    /**
     * Tries to find base type of <self> (sometimes getType() returns null)
     */
    protected CType findBaseType(JArrayInitializer self) {
        JExpression expr = self;
        while (true) {
            if (expr.getType()!=null && 
                expr.getType() instanceof CArrayType &&
                ((CArrayType)expr.getType()).getBaseType()!=null) {
                return ((CArrayType)expr.getType()).getBaseType();
            }
            if (expr.getType()!=null && 
                !(expr.getType() instanceof CArrayType)) {
                return expr.getType();
            }
            if (expr.getType()==null &&
                expr instanceof JArrayInitializer) {
                JExpression[] myElems = ((JArrayInitializer)expr).getElems();
                if (myElems.length==0) {
                    at.dms.util.Utils.fail("Can't find type of array " + self);
                } else {
                    expr = myElems[0];
                }
            } else {
                at.dms.util.Utils.fail("Can't find type of array " + self);
            }
        }
    }

    /**
     * If printInit is true, the initializer is actually printed;
     * otherwise just the declaration is printed.
     */
    protected void declareInitializedArray(CType baseType, String ident, JExpression expr, KjcVisitor visitor, boolean printInit) {
        printType(baseType); // note this calls print(CType), not print(String)
        p.print(" " + ident);
        JArrayInitializer init = (JArrayInitializer)expr;
        while (true) {
            int length = init.getElems().length;
            p.print("[" + length + "]");
            if (length==0) { 
                // hope that we have a 1-dimensional array in
                // this case.  Otherwise we won't currently
                // get the type declarations right for the
                // lower pieces.
                break;
            }
            // assume rectangular arrays
            JExpression next = (JExpression)init.getElems()[0];
            if (next instanceof JArrayInitializer) {
                init = (JArrayInitializer)next;
            } else {
                break;
            }
        }
        if (printInit) {
            p.print(" = ");
            expr.accept(visitor);
        }
        p.print(";");
        p.newLine();
    }

    /**
     * Finds any assignments of static arrays in functions and
     * replaces them with an assignment from a global pointer.  For
     * example, takes this original code:
     *
     * void Foo_init(Foo_1 data) {
     *  data->x = {1, 1, 1}
     * }
     *
     * And produces something like this (name of generated field may
     * differ):
     *
     * int _Foo_init_x[3] = {1, 1, 1}
     *
     * void Foo_init(Foo_1 data) {
     *  data->x = _Foo_init_x;
     * }
     * 
     * The declaration of the array is written to the output using
     * print() methods.  The array initializer is stored in a hashmap
     * for identification in later stages to print a reference to the
     * variable instead of printing the static array.
     */
    protected java.util.HashMap arrayInitializers; // JArrayInitializer -> String
    public void gatherArrayInitializers(JClassDeclaration flat) {
        arrayInitializers = new java.util.HashMap();
        flat.accept(new GatherArrayInitializers(this));
    }
    class GatherArrayInitializers extends SLIREmptyVisitor {
        /**
         * Name for init fields generated.
         */
        private static final String INITIALIZER_NAME = "__array_initializer_";
        /**
         * Counter for init fields generated.
         */
        private int INITIALIZER_COUNT = 0;
        /**
         * The visitor doing the actual printing of stuff.
         */
        private KjcVisitor printer;
        
        public GatherArrayInitializers(KjcVisitor printer) {
            this.printer = printer;
        }
        
        /**
         * Don't visit variable defs, because these declare local
         * arrays.  They are stack allocated automatically.
         */
        public void visitVariableDefinition(JVariableDefinition self,
                                            int modifiers,
                                            CType type,
                                            String ident,
                                            JExpression expr) {
            return;
        }

        public void visitAssignmentExpression(JAssignmentExpression self,
                                              JExpression left,
                                              JExpression right) {
            if (right instanceof JArrayInitializer) {
                JArrayInitializer init = (JArrayInitializer)right;
                // make new name for this reference
                String name = INITIALIZER_NAME + (INITIALIZER_COUNT++);
                CType baseType = findBaseType(init);
                int numDims = findNumDims(init);
                assert baseType!=null:"Can't find type of array " + init;
                
                declareInitializedArray(baseType, name, init, printer, true);
                // important to register the name after declaring the
                // array, so that the above statement prints the array
                // itself rather than a reference to the name
                arrayInitializers.put(init, name);          
            }
        }
    }

    // ----------------------------------------------------------------------
    // DATA MEMBERS
    // ----------------------------------------------------------------------

    //protected boolean                 forInit;        // is on a for init
    protected String                      className;
    protected boolean isStruct;

    protected boolean                   nl = true;
    protected boolean                   declOnly = false;

    protected int                       portalCount;
    protected Map                       portalNames;
}

