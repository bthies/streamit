package at.dms.kjc.common;
//import java.io.StringWriter;
//import java.util.Vector;
import java.util.Iterator;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
//import java.util.HashMap;
import at.dms.kjc.*;
import at.dms.kjc.sir.*;
//import at.dms.kjc.sir.lowering.LoweringConstants;
//import at.dms.compiler.TabbedPrintWriter;
import at.dms.compiler.JavaStyleComment;
import at.dms.kjc.common.CommonUtils;
/**
 * Somewhat artificial class to provide common code for 
 * at.dms.kjc.common.ToC and at.dms.kjc.lir.LIRToC
 *
 * Hopefully keep from having to fix some bugs twice.
 *
 * @author Allyn Dimock
 */

public abstract class ToCCommon extends SLIREmptyVisitor {
    
    static public boolean alternatePrintsForTiming = true;

    /**
     *  Controls visitPrintStatement. Can override for a backend by writing
     *  to this variable. initialized by a static block.  Can be
     *  overridden in a static block in a subclass by 
     *  printPrefixMap.clear();
     *  printPrefixMap.put...
     *  
     *  If there is more than one printer inheriting from ToCCommon and 
     *  your inheritance structure is not linear then you may have
     *  to write to printPrefixMap from a constructor rather than from
     *  a static block. 
     *
     *  If your backend does more on prints than the common
     *  backend, you will probably want to override printExp.
     */

    static protected Map<String,String> printPrefixMap;
    
    /**
     * Print postfixes: defaults to ");" useful for printing boolean.
     */
    static protected Map<String,String> printPostfixMap;

    static {
        printPrefixMap = new java.util.HashMap<String,String>();
        printPostfixMap = new java.util.HashMap<String,String>();
        // Set up standard prefixes for visitPrintStatement.
        // a subclass may override by:
        // printPrefixMap.clear(); printPrefixMap.put...
        printPrefixMap.put("boolean", "printf( \"%s\", ");
        printPostfixMap.put("boolean", " ? \"true\" : \"false\");");
        printPrefixMap.put("byte", "printf( \"%d\", ");
        printPrefixMap.put("char", "printf( \"%c\", ");
        printPrefixMap.put("double", "printf( \"%f\", ");
        printPrefixMap.put("float", "printf( \"%f\", ");
        printPrefixMap.put("int", "printf( \"%d\", ");
        printPrefixMap.put("long", "printf( \"%d\", ");
        printPrefixMap.put("short", "printf( \"%d\", ");
        printPrefixMap.put("java.lang.String", "printf( \"%s\", ");
        // we don't currently print: bit, or any CCLassType other
        // than String

    }
    
    /** Needed to pass info from assignment to visitNewArray * */
    protected JExpression lastLeft;  // LITtoC gave package visibility

    /** Object with useful print routines **/
    protected CodegenPrintWriter p;

    /** Make sure anyone can get the printer to insert code generated 
     * outside of a descendant of this class.
     */
    public CodegenPrintWriter getPrinter() { return p; }

    /**
     * For C code generation, the Java type 'boolean' will be printed as 'int'
     * For C++ code generation, set hasBoolType = true; to print the Java
     * type 'boolean' as 'bool'
     */
    protected boolean hasBoolType = false; 
    /**
     * With no parameters: create a new string TabbedPrintWriter for output
     */
    protected ToCCommon() {
        this.p = new CodegenPrintWriter();
    }

    /**
     * With a TabbedPrintWriter: use the given TabbedPrintWriter for output
     * and start off with no indentation yet.
     */
    protected ToCCommon(CodegenPrintWriter p) {
        this.p = p;
        p.setIndentation(0);        // Reset indentation to 0.  Why?
    }

    /**
     * Print a left parenthesis if not in statement context.
     */
    protected void printLParen() {
        p.print("(");
    }

    /**
     * Print a right parenthesis if not in statement context.
     */
    protected void printRParen() {
        p.print(")");
    }

    /**
     * Generate a code string for a JExpression or JStatement
     * 
     * <p>usage:<br/ >
     * (new mySubClass()).makeString(e);
     * </p>
     * Relies on the subclass's nullary constructor doing something sensible.
     * 
     * @param e a JPhylum (superclass of Jexpression and Jstatement)
     * @return code generated for the expression.
     */
    public String makeString(JPhylum e) {
        ToCCommon toc;
        try {
            toc = this.getClass().newInstance();
        } catch (/*IllegalAccess,Instantiation*/Exception exn) {
            System.err.println("Error in creating class " + this.getClass().getName());
            throw new Error (exn.getCause());
        } 
        e.accept(toc);
        return toc.getPrinter().getString();
    }
    
/**
 * Generate an array of code strings for an array of JPhylums.
 * Usually used to generate code for each of CArrayType's dimensions.
 * 
 * <p>usual usage:<br/ >
 * String[] dims_code = (new mySubClass()).makeArrayStrings(((CArrayType)type).getDims());
 * <br/ > or, if called from a within an object method of a subclass:<br/ >
 * String[] dims_code = this.makeArrayStrings(((CArrayType)type).getDims());
 * </p>
 * Relies on the subclass's nullary constructor doing something sensible.
 *
 * @param dims  an array of JExpression
 * @return array of String conatining generated code for each expression
 */
    public String[] makeArrayStrings(JPhylum[] dims) {
        String[] ret = new String[dims.length];
        
        for (int i = 0; i < dims.length; i++) {
            ret[i] = makeString(dims[i]);
        }
        return ret;
         
    }
    // ------------------------------------------------------------------------
    // More substantial common methods.
    // ------------------------------------------------------------------------

    /**
     * prints an array allocator expression
     *
     * Uses malloc or calloc based on setting of KjcOptions.malloczeros
     * We seem to always have KjcOptions.malloczeros == true
     * The RAW backend has problems with non-zeroed arrays so don't
     *  set KjcOptions.malloczeros to false!
     */
    //     public void visitNewArrayExpression(JNewArrayExpression self,
    //                                         CType type,
    //                                         JExpression[] dims,
    //                                         JArrayInitializer init
    //                   ) {
    //     }
    //     {
    //  p.print(" /* ToCCommon visitNewArrayExpression "
    //                + this.getClass().getName() + "*/ ");
    //  //the memory allocator to use: 
    //  String memory_alloc = KjcOptions.malloczeros ? 
    //      "malloc" : "calloc";
    //  //malloc takes one arg, calloc two, so use a different sep between
    //  //size and elements
    //  String mem_alloc_sep = KjcOptions.malloczeros ? 
    //      " * " : ", ";


    //  int derefs = dims.length; // number of *'s after element type
    //  Vector suffixes = new Vector();
    //  for (int dim = 0; dim < dims.length; dim++) {
    //      p.print("(");       // cast return type of allocator
    //      printType(type);
    //      for (int i = 0; i < derefs; i++) { p.print("*"); }
    //      p.print(")");
    //      p.print(memory_alloc + "("); // allocation
    //      dims[dim].accept(this);
    //      p.print(mem_alloc_sep + "sizeof(");
    //      printType(type);
    //      for (int i = 0; i < derefs - 1; i++) { p.print("*"); }
    //      p.print("));");
    //      p.newLine();
    //      // now either allocate subarrays if not last dimension
    //      // or optionally initialize if last dimension;
    //      if (dim == dims.length - 1) {
    //      // gotten to data in array: 
    //      // code creating initialization code not yet written
    //      if (init != null) {
    //          p.print("/* initialize with "); 
    //          init.accept(this);
    //          p.print("*/");
    //          p.print(" no initialization code! ");
    //          p.newLine();
    //      }
    //      break;
    //             }
    //      // initialize sub-arrays in for loop so that large arrays
    //      // don't result in large amounts of C code
    //      String indx = LoweringConstants.getUniqueVarName();
    //      p.print("{");
    //      p.newLine();
    //      p.indent();
    //      p.print("int " + indx + ";");
    //      p.newLine();
    //      p.print("for (" + indx + "= 0; " 
    //        + indx + " < ");
    //      derefs--;       // drop a level of indirection each time
    //             dims[dim].accept(this);
    //             p.print("; " 
    //        + indx + "++) {");
    //      p.newLine();
    //      p.indent();
    //      suffixes.add("[" + indx + "]");
    //      lastLeft.accept(this);
    //      for (int i = 0; i < suffixes.size(); i++) {
    //      p.print((String)(suffixes.get(i)));
    //      }
    //             p.print(" = ");
    //  }
        
    //  // close off any for loops created.
    //  for (int dim = 0; dim < dims.length-1; dim++) {
    //      p.outdent();    // end of emitted for loop
    //      p.print("}");
    //      p.newLine();
    //      p.outdent();    // end of emitted block declaring loop variable
    //      p.print("}");
    //      p.newLine();
    //  }
    //     }

    // ----------------------------------------------------------------------------
    // Statements common to ToC, LIRToC up to white space
    // ----------------------------------------------------------------------------

    /**
     * prints a while statement
     */
    public void visitWhileStatement(JWhileStatement self,
                                    JExpression cond,
                                    JStatement body) {
        p.newLine();
        p.print("while (");
        cond.accept(this);
        p.print(") ");
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

    /**
     * prints a switch statement
     */
    public void visitSwitchStatement(JSwitchStatement self,
                                     JExpression expr,
                                     JSwitchGroup[] body) {
        p.print("switch (");
        expr.accept(this);
        p.print(") {");
        for (int i = 0; i < body.length; i++) {
            body[i].accept(this);
        }
        p.newLine();
        p.print("}");
    }

    /**
     * prints a return statement
     */
    public void visitReturnStatement(JReturnStatement self,
                                     JExpression expr) {
        p.print("return");
        if (expr != null) {
            p.print(" ");
            expr.accept(this);
        }
        p.print(";");
    }

    /**
     * prints a labeled statement
     */
    public void visitLabeledStatement(JLabeledStatement self,
                                      String label,
                                      JStatement stmt) {
        p.print(label + ":");
        stmt.accept(this);
    }

    /**
     * prints a compound statement: 2-argument form
     */
    public void visitCompoundStatement(JCompoundStatement self,
                                       JStatement[] body) {
        visitCompoundStatement(body);
    }

    /**
     * prints an expression statement
     */
    public void visitExpressionStatement(JExpressionStatement self,
                                         JExpression expr) {
        expr.accept(this);
        p.print(";");
    }

    /**
     * prints an expression list statement
     */
    public void visitExpressionListStatement(JExpressionListStatement self,
                                             JExpression[] expr) {
        // Want expressions parenthesized here to not have problems with
        // relative precedence with ","
        for (int i = 0; i < expr.length; i++) {
            if (i != 0) {
                p.print(", ");
            }
            expr[i].accept(this);
        }
        p.print(";");
    }

    /**
     * prints a do statement
     */
    public void visitDoStatement(JDoStatement self,
                                 JExpression cond,
                                 JStatement body) {
        p.newLine();
        p.print("do ");
        body.accept(this);
        p.print("");
        p.print("while (");
        cond.accept(this);
        p.print(");");
    }

    /**
     * prints a continue statement
     */
    public void visitContinueStatement(JContinueStatement self,
                                       String label) {
        p.newLine();
        p.print("continue");
        if (label != null) {
            p.print(" " + label);
        }
        p.print(";");
    }

    /**
     * prints a break statement
     */
    public void visitBreakStatement(JBreakStatement self,
                                    String label) {
        p.newLine();
        p.print("break");
        if (label != null) {
            p.print(" " + label);
        }
        p.print(";");
    }


    /**
     * prints a compound statement
     */
    public void visitCompoundStatement(JStatement[] body) {
        for (int i = 0; i < body.length; i++) {
            if (!(body[i] instanceof JEmptyStatement))
                p.newLine();
            body[i].accept(this);
        }
    }

    /**
     * prints an block statement
     */
    public void visitBlockStatement(JBlock self,
                                    JavaStyleComment[] comments) {
        p.print("{");
        p.indent();
        visitCompoundStatement(self.getStatementArray());
        p.outdent();
        p.newLine();
        p.print("}");
    }

    /**
     * prints a type declaration statement
     */
    public void visitTypeDeclarationStatement(JTypeDeclarationStatement self,
                                              JTypeDeclaration decl) {
    
        decl.accept(this);
    }

    public void visitEmittedText(JEmittedText self) {
        p.print(self.getText());
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
        p.print("(");
        p.print("+");
        expr.accept(this);
        p.print(")");
    }

    /**
     * prints an unary minus expression
     */
    public void visitUnaryMinusExpression(JUnaryExpression self,
                                          JExpression expr)
    {
        p.print("(");
        p.print("-");
        expr.accept(this);
        p.print(")");
    }

    /**
     * prints a bitwise complement expression
     */
    public void visitBitwiseComplementExpression(JUnaryExpression self,
                                                 JExpression expr)
    {
        p.print("(");
        p.print("~");
        expr.accept(this);
        p.print(")");
    }

    /**
     * prints a logical complement expression
     */
    public void visitLogicalComplementExpression(JUnaryExpression self,
                                                 JExpression expr)
    {
        p.print("(");
        p.print("!");
        expr.accept(this);
        p.print(")");
    }

    /**
     * prints a type name expression
     */
    public void visitTypeNameExpression(JTypeNameExpression self,
                                        CType type) {
        p.print("(");
        printType(type);
        p.print(")");
    }


    /**
     * prints a shift expression
     */
    public void visitShiftExpression(JShiftExpression self,
                                     int oper,
                                     JExpression left,
                                     JExpression right) {
        p.print("(");
        left.accept(this);
        if (oper == OPE_SL) {
            p.print(" << ");
        } else if (oper == OPE_SR) {
            p.print(" >> ");
        } else {
            p.print(" >>> ");
        }
        right.accept(this);
        p.print(")");
    }

    /**
     * prints a prefix expression
     */
    public void visitPrefixExpression(JPrefixExpression self,
                                      int oper,
                                      JExpression expr) {
        printLParen();
        if (oper == OPE_PREINC) {
            p.print("++");
        } else {
            p.print("--");
        }
        expr.accept(this);
        printRParen();
    }

    /**
     * prints a postfix expression
     */
    public void visitPostfixExpression(JPostfixExpression self,
                                       int oper,
                                       JExpression expr) {
        printLParen();
        expr.accept(this);
        if (oper == OPE_POSTINC) {
            p.print("++");
        } else {
            p.print("--");
        }
        printRParen();
    }

    /**
     * prints a parenthesed expression
     */
    public void visitParenthesedExpression(JParenthesedExpression self,
                                           JExpression expr) {
        p.print("(");
        expr.accept(this);
        p.print(")");
    }


    /**
     * prints a local variable expression
     */
    public void visitLocalVariableExpression(JLocalVariableExpression self,
                                             String ident) {
        p.print(ident);
    }
    /**
     * prints an equality expression
     */
    public void visitEqualityExpression(JEqualityExpression self,
                                        boolean equal,
                                        JExpression left,
                                        JExpression right) {
        printLParen();
        left.accept(this);
        p.print(equal ? " == " : " != ");
        right.accept(this);
        printRParen();
    }

    /**
     * prints a conditional expression
     */
    public void visitConditionalExpression(JConditionalExpression self,
                                           JExpression cond,
                                           JExpression left,
                                           JExpression right) {
        printLParen();
        cond.accept(this);
        p.print(" ? ");
        left.accept(this);
        p.print(" : ");
        right.accept(this);
        printRParen();
    }

    /**
     * prints a compound expression
     */
    public void visitCompoundAssignmentExpression(JCompoundAssignmentExpression self,
                                                  int oper,
                                                  JExpression left,
                                                  JExpression right) {
        printLParen();
        left.accept(this);
        switch (oper) {
        case OPE_STAR:
            p.print(" *= ");
            break;
        case OPE_SLASH:
            p.print(" /= ");
            break;
        case OPE_PERCENT:
            p.print(" %= ");
            break;
        case OPE_PLUS:
            p.print(" += ");
            break;
        case OPE_MINUS:
            p.print(" -= ");
            break;
        case OPE_SL:
            p.print(" <<= ");
            break;
        case OPE_SR:
            p.print(" >>= ");
            break;
        case OPE_BSR:
            p.print(" >>>= ");
            break;
        case OPE_BAND:
            p.print(" &= ");
            break;
        case OPE_BXOR:
            p.print(" ^= ");
            break;
        case OPE_BOR:
            p.print(" |= ");
            break;
        }
        right.accept(this);
        printRParen();
    }

    /**
     * prints a cast expression
     */
    public void visitCastExpression(JCastExpression self,
                                    JExpression expr,
                                    CType type)
    {
        printLParen();
        //suppress generation of casts for multidimensional arrays
        //when generating C code because they are meaningless
        //and if we try to access a multi-dim array after it has been cast to 
        //(type **) they dimensions are unknown...
        if (!(type.isArrayType() 
              && ((CArrayType)type).getElementType().isArrayType())) {
            p.print("(");
            printType(type);
            p.print(")");
        }
        p.print("(");
        expr.accept(this);
        p.print(")");
        printRParen();
    }
    
    /**
     * prints a cast expression
     */
    public void visitUnaryPromoteExpression(JUnaryPromote self,
                                            JExpression expr,
                                            CType type)
    {
        printLParen();
        p.print("(");
        printType(type);
        p.print(")");
        p.print("(");
        expr.accept(this);
        p.print(")");
        printRParen();
    }

    /**
     * Split expression into list of expressions for print.
     * 
     * The C backends are not set up to perform Java-like string 
     * concatenation, and the C++ backends often can not perform
     * string concatenation because of semantic diffrences between
     * the C++ + operator and the Java + operator.
     * 
     * We solve this by looking for all string contatenations reachable 
     * from the root of the expression without going through any operator
     * other than string concatenation and return a List of expressions
     * -- in left-to-right order -- that were connected by string 
     * concatenation in the original expression.
     * 
     * I am following the belief that there are some expressions for which
     * no type can be found.  As per previous implementations, such expressions
     * do not cause an uncaught exception, but they may generate bad code...
     * 
     * @param exp
     * @return list of expressions that had been string concatenated.
     */
    protected List<JExpression> splitForPrint(JExpression exp) {
        List<JExpression> exprs = new ArrayList<JExpression>(1);
        if (exp instanceof JAddExpression) {
            JExpression l, r;
            CType lt = null; 
            CType rt = null;
            
            l = ((JAddExpression)exp).getLeft();
            r = ((JAddExpression)exp).getRight();
            try {
                lt = l.getType();
            } catch (Exception e) { /* leave Null if type not recorded */ }
            try {
                rt = r.getType();
            } catch (Exception e) { /* leave Null if type not recorded */ }

            if ((lt != null && lt.equals(CStdType.String))
                || (rt != null && rt.equals(CStdType.String) )) {
                exprs.addAll(splitForPrint(l));
                exprs.addAll(splitForPrint(r));
            } else {
                exprs.add(exp);
            }
        } else {
            exprs.add(exp);
        }
        return exprs;
    }

    protected boolean printExp(JExpression expr) {

        List exps = splitForPrint(expr);

        // for timing runs we want an easily recognized bit of code 
        // that can not be optimized away by gcc
        // produced in an easliy recognisable comment.
        if (ToCCommon.alternatePrintsForTiming) {
            p.newline();
            p.print("// TIMER_PRINT_CODE: ");
            for (Iterator i = exps.iterator(); i.hasNext();) {
                JExpression exp = ((JExpression)i.next());
                p.print("__print_sink__ += (int)(");
                exp.accept(this);
                p.print("); ");
            }
            p.newline();
        }
        
        // now print for real...
        boolean printedOK = true;
        for (Iterator i = exps.iterator(); i.hasNext();) {
            JExpression exp = ((JExpression)i.next());
            CType t = null;
            try {
                t = exp.getType();
            } catch (Exception e) {
                System.err.println("Cannot get type for print statement" + exp.toString());
                printedOK = false;
                continue;
            }
            if (t == null) {
                System.err.println("Null type for print statement " + exp.toString());
                printedOK = false;
                continue;
            }
            String typeString = t.toString();
            String printPrefix = (String)(printPrefixMap.get(typeString));
            if (printPrefix == null) {
                System.err.println("Print statement does not support type "
                                   + t);
                printedOK = false;
            } else {
                p.print(printPrefix);
                exp.accept(this);
                String printPostfix = 
                    (String)(printPostfixMap.get(typeString));
                if (printPostfix == null) {
                    printPostfix = "); ";
                }
                p.print(printPostfix);
            }
        }
        return printedOK;
    }
    
    /**
     * Process a Print statment, table driven to allow several backends
     * Deals with the problem of string concatenation in Java not translating
     * to our output languages C or C++
     */
    
    public void visitPrintStatement(SIRPrintStatement self,
                                    JExpression exp) { 
        printExp(exp);
        if (self.getNewline()) {
            p.print("printf(\"\\n\");\n");
        }
    }


    /**
     * Print marker as a comment.
     */
    public void visitMarker(SIRMarker self) {
        if (self instanceof SIRBeginMarker) {
            p.println("// mark begin: " + ((SIRBeginMarker)self).getName());
        }
        if (self instanceof SIREndMarker) {
            p.println("// mark end: " + ((SIREndMarker)self).getName());
        }
    }

    /**
     * Prints a declaration for the given type with the given
     * identifier.  Prints int x[10][10] for arrays.
     * 
     * @param s      the type to declare
     * @param ident  the identifier to declare
     */
    protected void printDecl(CType s, String ident) {
        p.print(CommonUtils.declToString(s, ident, hasBoolType));
    }

    /**
     * Print a class Name.
     *
     * @param self    a JClassExpression
     * @param type    s CType, presumably a CCLassNameType....
     */

    public void visitClassExpression(JClassExpression self,
				     CType type) {
	printType(type);	// should be instance of CClassNameType
    }

    /**
     * Print a CType.
     *
     * @param s    a CType to be printed.
     *
     */
    protected void printType(CType s) {
        p.print(CommonUtils.CTypeToString(s, hasBoolType));
    }
}
