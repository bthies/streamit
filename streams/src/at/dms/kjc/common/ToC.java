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
import java.io.*;
import at.dms.compiler.*;
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
public abstract class ToC extends ToCCommon implements SLIRVisitor,CodeGenerator
{
    /** set to true to only print declarations of methods when visiting them **/
    public boolean declOnly = true;
    /** true if we are currently visiting the init function **/
    protected boolean isInit = false;
    /** the current function we are visiting **/
    protected JMethodDeclaration method;

    public ToC() { super(); }

    public ToC(TabbedPrintWriter p) { super(p); }
    
    /**
     * Prints initialization for an array with static initializer, e.g., "int A[2] = {1,2};"
     *
     * To promote code reuse with other backends, inputs a visitor to
     * do the recursive call.
     */
    protected void declareInitializedArray(CType type, String ident, JExpression expr) {
	// note this calls print(CType), not print(String)
	print(((CArrayType)type).getBaseType()); 
	
	print(" " + ident);
	JArrayInitializer init = (JArrayInitializer)expr;
	while (true) {
	    int length = init.getElems().length;
	    print("[" + length + "]");
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
	print(" = ");
	expr.accept(this);
	print(";");
	return;
    }

    protected abstract void stackAllocateArray(String ident);
   
    			
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

        newLine();
        // print(CModifier.toString(modifiers));

	//only stack allocate singe dimension arrays
	if (expr instanceof JNewArrayExpression) {
	    //print the basetype
	    print(((CArrayType)type).getBaseType());
	    print(" ");
	    //print the field identifier
	    print(ident);
	    //print the dims
	    stackAllocateArray(ident);
	    print(";");
	    return;
	} else if (expr instanceof JArrayInitializer) {
	    declareInitializedArray(type, ident, expr);
	    return;
	}

        print(type);
        print(" ");
        print(ident);

        if (expr != null) {
            print("\t= ");
	    expr.accept(this);
        }   //initialize all fields to 0
	else if (type.isOrdinal())
	    print (" = 0");
	else if (type.isFloatingPoint())
	    print(" = 0.0f");
	else if (type.isArrayType()) {
	    print(" = 0");
	}
	

        print(";");
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

	boolean oldStatementContext = statementContext;
        print("if (");
	statementContext = false;
        cond.accept(this);
        print(") {");
	statementContext = true;
        pos += thenClause instanceof JBlock ? 0 : TAB_SIZE;
        thenClause.accept(this);
        pos -= thenClause instanceof JBlock ? 0 : TAB_SIZE;
        if (elseClause != null) {
            if ((elseClause instanceof JBlock) || (elseClause instanceof JIfStatement)) {
                print(" ");
            } else {
                newLine();
            }
            print("} else {");
            pos += elseClause instanceof JBlock || elseClause instanceof JIfStatement ? 0 : TAB_SIZE;
            elseClause.accept(this);
            pos -= elseClause instanceof JBlock || elseClause instanceof JIfStatement ? 0 : TAB_SIZE;
        }
	print("}");
	statementContext = oldStatementContext;
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
	boolean oldStatementContext = statementContext;
	statementContext = false;
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
            Utils.fail("Unknown relational expression"); // only difference from LIRToC
	}
        right.accept(this);
	print(")");
	statementContext = oldStatementContext;
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
	Utils.fail("Name Expression");
	
	boolean oldStatementContext = statementContext;
	statementContext = false;
	print("(");
        if (prefix != null) {
            prefix.accept(this);
            print("->");
        }
        print(ident);
	print(")");
	statementContext = oldStatementContext;
    }

    /**
     * prints an binary expression
     */
    public void visitBinaryExpression(JBinaryExpression self,
                                      String oper,
                                      JExpression left,
                                      JExpression right) {
	printLParen();
	boolean oldStatementContext = statementContext;
	statementContext = false;
        left.accept(this);
        print(" ");
        print(oper);
        print(" ");
        right.accept(this);
	statementContext = oldStatementContext;
	printRParen();
    }


    /**
     * prints a field expression
     */
    public void visitFieldExpression(JFieldAccessExpression self,
                                     JExpression left,
                                     String ident)
    {
	boolean oldStatementContext = statementContext;
	statementContext = false;
        if (ident.equals(JAV_OUTER_THIS)) {// don't generate generated fields
            print(left.getType().getCClass().getOwner().getType() + "->this");
	    statementContext = oldStatementContext;
            return;
        }
        int		index = ident.indexOf("_$");
        if (index != -1) {
            print(ident.substring(0, index));      // local var
        } else {
	    print("(");
            left.accept(this);
	    if (!(left instanceof JThisExpression))
		print(".");
            print(ident);
	    print(")");
        }
	statementContext = oldStatementContext;
    }

    /**
     * prints a bitwise expression
     */
    public void visitBitwiseExpression(JBitwiseExpression self,
                                       int oper,
                                       JExpression left,
                                       JExpression right) {
	printLParen();
	boolean oldStatementContext = statementContext;
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
	    Utils.fail("Unknown relational expression"); // only difference with LIRToC
        }
        right.accept(this);
	statementContext = oldStatementContext;
	printRParen();
    }

    /**
     * prints an array length expression
     */
    public void visitArrayLengthExpression(JArrayLengthExpression self,
                                           JExpression prefix) {
	Utils.fail("Array length expression not supported in streamit");
	
        prefix.accept(this);
        print(".length");
    }

    /**
     * prints an array access expression
     */
    public void visitArrayAccessExpression(JArrayAccessExpression self,
                                           JExpression prefix,
                                           JExpression accessor) {
	printLParen();
	boolean oldStatementContext = statementContext;
	statementContext = false;
        prefix.accept(this);
        print("[(int)");	// cast to int is only difference with LIRToC
        accessor.accept(this);
        print("]");
	statementContext = oldStatementContext;
	printRParen();
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
        print("((float)" + value + ")");
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

    // Special case for CTypes, to map some Java types to C types.
    protected void print(CType s) {
	if (s instanceof CArrayType){
            print(((CArrayType)s).getElementType());
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

    /** clear the internal String that represents the code generated so far **/
    public void clear() 
    {
	this.str = new StringWriter();
        this.p = new TabbedPrintWriter(str);
    }
    
}
