package at.dms.kjc.common;

//import at.dms.kjc.flatgraph.FlatNode;

import at.dms.kjc.*;
import at.dms.kjc.sir.*;
//import at.dms.kjc.iterator.*;
import at.dms.util.Utils;
//import java.util.List;
//import java.util.ListIterator;
//import java.util.Iterator;
//import java.util.LinkedList;
//import java.util.HashMap;
//import java.io.*;
//import at.dms.compiler.*;
//import at.dms.kjc.sir.lowering.*;
//import java.util.Hashtable;
//import at.dms.util.SIRPrinter;

/**
 * This class converts the Stream IR (which references the Kopi Java IR)
 * to C code and dumps it to a file, str.c.    
 *
 *
 * @author Michael Gordon
 */
public class ToC extends ToCCommon implements SLIRVisitor,CodeGenerator
{
    /** true to only print declarations of methods when visiting them **/
    private boolean declOnly = true;
    /** true if we are currently visiting the init function **/
    protected boolean isInit = false;
    /** the current function we are visiting **/
    protected JMethodDeclaration method;

    public ToC() { super(); }

    public ToC(CodegenPrintWriter p) { super(p); }
    
    /**
     * Prints initialization for an array with static initializer, e.g., "int A[2] = {1,2};"
     *
     * To promote code reuse with other backends, inputs a visitor to
     * do the recursive call.
     */
    protected void declareInitializedArray(CType type, String ident, JExpression expr) {
        JArrayInitializer init = (JArrayInitializer)expr;

        if (type instanceof CVectorTypeLow || type instanceof CVectorType) {
            printType(type);
            p.print(" " + ident);
        } else {
            assert type instanceof CArrayType;
            printType(((CArrayType)type).getBaseType());  
            p.print(" " + ident);
        
            while (true) {
                int length = init.getElems().length;
                p.print("[" + length + "]");
                if (length==0) { 
                    // hope that we have a 1-dimensional array in
                    // this case. Otherwise we won't currently
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
        }
        p.print(" = ");
        expr.accept(this);
        p.print(";");
        return;
    }

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

        p.newLine();
        // p.print(CModifier.toString(modifiers));

        //only stack allocate singe dimension arrays
        if (expr instanceof JNewArrayExpression) {
            /* Do not expect to have any JNewArrayExpressions any more */
            Utils.fail("Unexpected new array expression in codegen, for field: " + self);
            /*
            //print the basetype
            printType(((CArrayType)type).getBaseType());
            p.print(" ");
            //print the field identifier
            p.print(ident);
            //print the dims
            stackAllocateArray(ident);
            p.print(";");
            return;
            */
        } else if (expr instanceof JArrayInitializer) {
            declareInitializedArray(type, ident, expr);
            return;
        }

        printDecl (type, ident);

        if (expr != null) {
            p.print("\t= ");
            expr.accept(this);
        }   //initialize all fields to 0
        else if (type.isOrdinal())
            p.print (" = 0");
        else if (type.isFloatingPoint())
            p.print(" = 0.0f");
        else if (type.isArrayType()) {
            p.print(" = {0}");
        }
    

        p.print(";");
    }




    // ----------------------------------------------------------------------
    // STATEMENT
    // ----------------------------------------------------------------------
    
    /*
     * prints a while statement visitWhileStatement in ToCCommon
     */

    /*
     * prints a variable declaration statement
     * visitVariableDeclarationStatement in ToCCommon
     */
   
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
        p.print(") {");
        if (!(thenClause instanceof JBlock)) {p.indent(); }
        thenClause.accept(this);
        if (!(thenClause instanceof JBlock)) {p.outdent(); }
        if (elseClause != null) {
            if ((elseClause instanceof JBlock) || (elseClause instanceof JIfStatement)) {
                p.print(" ");
            } else {
                p.newLine();
            }
            p.print("} else {");
            if (!(elseClause instanceof JBlock 
                  || elseClause instanceof JIfStatement)) { p.indent(); }
            elseClause.accept(this);
            if (!(elseClause instanceof JBlock 
                  || elseClause instanceof JIfStatement)) { p.outdent(); }
        }
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
        //Utils.fail("This Expression encountered");
    }

    /**
     * prints a super expression
     */
    public void visitSuperExpression(JSuperExpression self) {
        Utils.fail("Super Expression Encountered");
    }

    /*
     * prints a shift expression
     */

    /**
     * prints a relational expression
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
            Utils.fail("Unknown relational expression"); // only difference from LIRToC
        }
        right.accept(this);
        p.print(")");
    }


    /*
     * prints an array allocator expression
     *
     *  public void visitNewArrayExpression
     *  see at/dms/kjc/common.ToCCommon
     */

    
    /**
     * prints a name expression
     *
     * There should be no surviving JNameExpression's from the front end.
     * We use them in the backend for two purposes when coding in SIR
     * but wanting to know that a C / C++ code generator will produce.
     * <p>
     * (1) Name with no prefix -- used to print an untyped string:
     *     such as a symbolic constant 'PI'.
     *
     * (2) Name with a prefix:  The uniprocessor backend co-opted this 
     *     to print 'prefix->string'
     *     TODO:  It seems that they should have used a BinaryExpression
     */
    public void visitNameExpression(JNameExpression self, JExpression prefix,
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
     * prints a field expression
     */
    public void visitFieldExpression(JFieldAccessExpression self,
                                     JExpression left,
                                     String ident)
    {
        if (ident.equals(JAV_OUTER_THIS)) {// don't generate generated fields
            p.print(left.getType().getCClass().getOwner().getType() + "->this");
            return;
        }
        int index = ident.indexOf("_$");
        if (index != -1) {
            p.print(ident.substring(0, index));      // local var
        } else {
            p.print("(");
            left.accept(this);
            if (!(left instanceof JThisExpression)) {
                p.print(".");
            }
            p.print(ident);
            p.print(")");
        }
    }


    /**
     * prints a bitwise expression
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
            Utils.fail("Unknown relational expression"); // only difference with LIRToC
        }
        right.accept(this);
        printRParen();
    }

    /**
     * prints an array length expression
     */
    public void visitArrayLengthExpression(JArrayLengthExpression self,
                                           JExpression prefix) {
        Utils.fail("Array length expression not supported in streamit");
    
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
        p.print("[(int)");  // cast to int is only difference with LIRToC
        accessor.accept(this);
        p.print("]");
        printRParen();
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
    
    public JExpression passParentheses(JExpression exp) 
    {
        while (exp instanceof JParenthesedExpression)
            exp = ((JParenthesedExpression)exp).getExpr();
    
        return exp;
    }
    

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
     * prints a boolean literal
     */
    public void visitBooleanLiteral(boolean value) {
        if (value)
            p.print(this.hasBoolType ? "true" :  "1");
        else
            p.print(this.hasBoolType ? "false" :  "0");
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
        p.print("((float)" + value + ")");
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
        // RMR { do not insert a new line before array initialization
    	// p.newLine();
    	// } RMR
        p.print("{");
        for (int i = 0; i < elems.length; i++) {
            if (i != 0) {
                p.print(", ");
            }
            elems[i].accept(this);
        }
        p.print("}");
    }

    
    // ----------------------------------------------------------------------
    // Misc methods for manipulating fields
    // ----------------------------------------------------------------------


    /** clear the internal String that represents the code generated so far **/
    public void clear() 
    {
        this.p = new CodegenPrintWriter();
    }

    /**
     * @param declOnly The declOnly to set.
     */
    public void setDeclOnly(boolean declOnly) {
        this.declOnly = declOnly;
    }

    /**
     * @return Returns the declOnly.
     */
    public boolean isDeclOnly() {
        return declOnly;
    }
    
}
