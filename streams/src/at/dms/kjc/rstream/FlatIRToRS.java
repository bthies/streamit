package at.dms.kjc.rstream;

import at.dms.kjc.common.*;
import at.dms.kjc.flatgraph.FlatNode;
import at.dms.kjc.flatgraph.FlatVisitor;
import at.dms.kjc.*;
import at.dms.kjc.sir.*;
import at.dms.kjc.iterator.*;
import at.dms.util.Utils;
import java.util.List;
import java.util.ListIterator;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.HashSet;
import java.util.HashMap;
import java.io.*;
import at.dms.compiler.*;
import at.dms.kjc.sir.lowering.*;
import java.util.Hashtable;
import at.dms.util.SIRPrinter;

/**
 * This class converts the Stream IR (which references the Kopi Java IR)
 * to C code and dumps it to a file, str.c.    
 *
 *
 * @author Michael Gordon
 */
public class FlatIRToRS extends ToC 
{
    
    /** the hashmap of for loops -> do loops **/   
    private HashMap doloops;
    /** the current filter we are visiting **/
    private SIRFilter filter;
    /** > 0 if in a for loop header during visit **/
    private int forLoopHeader = 0;

    public int doLoops = 0;
    public int staticDoLoops = 0;
    

    private static final String ARRAY_COPY = "__array_copy__";
    
    public FlatIRToRS() 
    {
        super();
        doloops = new HashMap();
    }
    
    /**
     * prints an assignment expression
     */
    public void visitAssignmentExpression(JAssignmentExpression self,
                                          JExpression left,
                                          JExpression right) {

        //do not print class creation expression
        if (passParentheses(right) instanceof JQualifiedInstanceCreation ||
            passParentheses(right) instanceof JUnqualifiedInstanceCreation ||
            passParentheses(right) instanceof JQualifiedAnonymousCreation ||
            passParentheses(right) instanceof JUnqualifiedAnonymousCreation)
            return;

        //we are assigning an array to an array in C, we want to do 
        //element-wise copy!!
    
        if (!KjcOptions.absarray && 
            ((left.getType() != null && left.getType().isArrayType()) ||
             (right.getType() != null && right.getType().isArrayType()))) {
        
            arrayCopy(left, right);
            return;
        }

        lastLeft=left;
        printLParen();
        left.accept(this);
        p.print(" = ");
        right.accept(this);
        printRParen();
    }

    /**
     * prints a field declaration
     */
    public void visitFieldDeclaration(JFieldDeclaration self,
                                      int modifiers,
                                      CType type,
                                      String ident,
                                      JExpression expr) {
        p.newLine();
        assert !(expr instanceof JNewArrayExpression) :
            "New Array expression in field declaration";

        /*
          if (expr instanceof JArrayInitializer) {
          declareInitializedArray(type, ident, expr);
          return;
          }
        */
    
        //we have an array declaration
        if (type.isArrayType()) {
            handleArrayDecl(ident, (CArrayType)type);   
        }
        else {
            printType(type);
            p.print(" ");
            p.print(ident);
        
            if (expr != null) {
                p.print(" = ");
                expr.accept(this);
            } else { //initialize all fields to 0
                if (type instanceof CVectorType ||
                    type instanceof CVectorTypeLow ||
                    type instanceof CArrayType && 
                        (((CArrayType)type).getBaseType() instanceof CVectorType ||
                         ((CArrayType)type).getBaseType() instanceof CVectorTypeLow)) {
                    // Do not print out initializer in these cases.  The underlying type is a union except in the
                    // case of CVectorTypeLow, and gcc4.1 gives errors on attempts to initialize.
                } else {
                if (type.isOrdinal())
                    p.print (" = 0");
                else if (type.isFloatingPoint())
                    p.print(" = 0.0f");
                else if (type.isArrayType())
                    p.print(" = {0}");
                }
            }
        
        }
        p.print(";");
    }

    /**
     * print an abstract array declaration and return the number of dimensions
     **/
    private void handleArrayDecl(String ident, CArrayType type)
    {
        String decl1 = CommonUtils.declToString(type, ident, false);
        // change brackets to double brackets
        String decl2 = Utils.replaceAll(decl1, "]", "]]");
        String decl3 = Utils.replaceAll(decl2, "[", "[[");

        p.print(decl3);
    }
    
    
    private void printArrayType(CArrayType type) 
    {
        assert false : "Should not be printing an array type";

        printType(type.getBaseType());
        p.print(" ");

        // print brackets
        for (int i=0; i<type.getDims().length; i++) {
            p.print("[[]]");
        }
    }
    
    /**
     * prints a variable declaration statement
     */
    public void visitVariableDefinition(JVariableDefinition self,
                                        int modifiers,
                                        CType type,
                                        String ident,
                                        JExpression expr) {
    
        /*if (expr instanceof JArrayInitializer) {
          declareInitializedArray(type, ident, expr);
          return;
          }*/
    
        //we have an array declaration
        if (type.isArrayType() && KjcOptions.absarray) {
            handleArrayDecl(ident, (CArrayType)type);
        } else {
            printDecl(type, ident);
        
            if (expr != null) {
                p.print(" = ");
                expr.accept(this);
            } else {
                if (type instanceof CVectorType ||
                        type instanceof CVectorTypeLow ||
                        type instanceof CArrayType && 
                            (((CArrayType)type).getBaseType() instanceof CVectorType ||
                             ((CArrayType)type).getBaseType() instanceof CVectorTypeLow)) {
                        // Do not print out initializer in these cases.  The underlying type is a union except in the
                        // case of CVectorTypeLow, and gcc4.1 gives errors on attempts to initialize.
                  } else {
                if (type.isOrdinal())
                    p.print (" = 0");
                else if (type.isFloatingPoint())
                    p.print(" = 0.0f");
                else if (type.isArrayType())
                    p.print(" = {0}");
              }
            }
        }
        p.print(";");
    }

    //     /**
    //      * prints an array allocator expression
    //      */
    //     public void visitNewArrayExpression(JNewArrayExpression self,
    //                                         CType type,
    //                                         JExpression[] dims,
    //                                         JArrayInitializer init)
    //     {
    //  //we should see no zero dimension arrays
    //  assert dims.length > 0 : "Zero Dimension array" ;
    //  //and no initializer
    //  assert init == null : "Initializers of Abstract Arrays not supported in RStream yet";
    //  if (KjcOptions.absarray) {
    //      //we are generating abstract arrays
    //      //print the absarray call with the dimensions...
    //      /*old absarray stuff 
    //        p.print(" absarray" + dims.length + "(");
    //      dims[0].accept(this);
    //      for (int i = 1; i < dims.length; i++) {
    //      p.print(",");
    //      dims[i].accept(this);
    //      }
    //      p.print(")");
    //      */
    //      //new abs array declaration
    //      assert dims.length > 0;     
    //      p.print("[[");
    //      dims[0].accept(this);
    //      for (int i = 1; i < dims.length; i++) {
    //        // RMR { syntax change in rstream 2.1: multidimmensional arrays do not use commas
    //        // p.print(", ");
    //        p.print("]][[");
    //          // } RMR
    //      dims[i].accept(this);
    //      }
    //      p.print("]]");
    //  }
    //  else {
    //      //normal c arrays
    //      for (int i = 0; i < dims.length; i++) {
    //      p.print("[");
    //      dims[i].accept(this);
    //      p.print("]");
    //      }
    //  }
    //     }
    
    private int[] getDims(CArrayType type) 
    {
        int dims[] = new int[type.getDims().length];
    
        for (int i = 0; i < dims.length; i++) {
            assert type.getDims()[i] instanceof JIntLiteral;
            dims[i] = ((JIntLiteral)type.getDims()[i]).intValue();
        }
        return dims;
    }
    
    /**
     * prints an expression statement
     */
    public void visitBlockStatement(JBlock self,
                                    JavaStyleComment[] comments) {
        if (self instanceof Jrstream_pr) {
            // RMR { did the rstream C language extensions change?
            // replace rstream_pr with a pragma so that rstream 2.1 doesn't barf
            //p.print("rstream_pr ");
            p.print("#pragma res parallel");
            p.newLine();
            // } RMR
        }

        p.print("{");
        p.indent();
        visitCompoundStatement(self.getStatementArray());
        if (comments != null) {
            visitComments(comments);
        }
        p.outdent();
        p.newLine();
        p.print("}");
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
            MacroConversion.doConvert(self, isDeclOnly(), this);
            return;
        }

        p.newLine();
        // print(CModifier.toString(modifiers));
        printType(returnType);
        p.print(" ");
    
        //just print initPath() instead of initPath<Type>
        //if (ident.startsWith("initPath"))
        //    p.print("initPath"); 
        //else
    
        p.print(ident);
    
        p.print("(");
        int count = 0;
    
        // RMR { for the main function, do not use abstract array syntax
        // (maybe there is a better way to do this)
        boolean savedKjcOption_absarray = KjcOptions.absarray;
        if (KjcOptions.absarray)
            if (ident == GenerateCCode.MAINMETHOD)
                KjcOptions.absarray = false;
        // } RMR

        for (int i = 0; i < parameters.length; i++) {
            if (count != 0) {
                p.print(", ");
            }
            parameters[i].accept(this);
            count++;
        }
        p.print(")");

        // RMR { restore saved KjcOption for abstract arrays
        KjcOptions.absarray = savedKjcOption_absarray;
        // } RMR
        //print the declaration then return
        if (isDeclOnly()) {
            p.print(";");
            return;
        }

        //set the current method we are visiting
        method = self;
    
        p.print(" ");
        if (body != null) 
            body.accept(this);
        else 
            p.print(";");

        p.newLine();
        method = null;
    }
    
    
    

    // ----------------------------------------------------------------------
    // STATEMENT
    // ----------------------------------------------------------------------   

    public void visitDoLoopStatement(JDoLoopStatement self) 
    {
        assert self.countUp() : "Currently we only handle doloops with positive increment";

        doLoops++;
        if (self.staticBounds())
            staticDoLoops++;

        p.print("doloop (");
        p.print(self.getInduction().getType() + " ");
        p.print(self.getInduction().getIdent());
        p.print(" = ");
        self.getInitValue().accept(this);
        p.print("; ");
        // RMR { did the rstream C language extensions change?
        // the following were added so that rstream 2.1 doesn't barf
        // the output will now more closely resemble a for loop
        p.print(self.getInduction().getIdent());
        // note assumption: always less than
        p.print(" < ");
        // } RMR
        self.getCondValue().accept(this);
        p.print("; ");
        // RMR { added so that rstream 2.1 doesn't barf; see note above
        p.print(self.getInduction().getIdent());
        // note assumption: always increment
        p.print(" += ");
        // } RMR
        self.getIncrValue().accept(this);
        p.print(") ");

    
        p.newLine();
        p.indent();
        self.getBody().accept(this);
        p.outdent();
        p.newLine();
    }
    

    /**
     * prints a for statement
     */
    public void visitForStatement(JForStatement self,
                                  JStatement init,
                                  JExpression cond,
                                  JStatement incr,
                                  JStatement body) {

        if (KjcOptions.doloops && self instanceof JDoLoopStatement) {
            visitDoLoopStatement((JDoLoopStatement)self);
            return;
        }
    
        //be careful, if you return prematurely, decrement me
        forLoopHeader++;


        p.print("for (");
    
        if (init != null) {
            init.accept(this);
            //the ; will print in a statement visitor
        }
    
        p.print(" ");
        if (cond != null) {
            cond.accept(this);
        }
        //cond is an expression so print the ;
        p.print("; ");
        if (incr != null) {
            FlatIRToRS l2c = new FlatIRToRS();
            l2c.doloops = this.doloops;
            incr.accept(l2c);
            // get String
            String str = l2c.getPrinter().getString();
            // leave off the trailing semicolon if there is one
            if (str.endsWith(";")) {
                p.print(str.substring(0, str.length()-1));
            } else { 
                p.print(str);
            }
        }
        forLoopHeader--;
        p.print(") ");
    
        //p.print("{");
        p.newLine();
        p.indent();
        body.accept(this);
        p.outdent();
        p.newLine();
        //p.print("}");
    }



    /**
     * prints an array access expression
     */
    public void visitArrayAccessExpression(JArrayAccessExpression self,
                                           JExpression prefix,
                                           JExpression accessor) {
        if (KjcOptions.absarray) {
            String access = "[[";
            JExpression exp = prefix;
        
            //if this is a multidimensional access, convert to the 
            //comma'ed form
            while (exp instanceof JArrayAccessExpression) {
                JArrayAccessExpression arr = (JArrayAccessExpression)exp;
                FlatIRToRS toRS = new FlatIRToRS();
                arr.getAccessor().accept(toRS);
        
                // RMR { syntax change in rstream 2.1: multidimmensional arrays do not use commas
                // access = access + toRS.getString() + ", ";
                access = access + toRS.getPrinter().getString() + "]][[";
                // } RMR
                exp = arr.getPrefix();
            }
            //visit the var access
            exp.accept(this);
            p.print(access);
            accessor.accept(this);
            p.print("]]");
        }
    
        else {
            //normal c arrays
            String access = "";
            JExpression exp = prefix;
            while (exp instanceof JArrayAccessExpression) {
                JArrayAccessExpression arr = (JArrayAccessExpression)exp;
                FlatIRToRS toRS = new FlatIRToRS();
                arr.getAccessor().accept(toRS);
        
                access = access + "[" + toRS.getPrinter().getString() + "]";
                exp = arr.getPrefix();
            }
            exp.accept(this);
            p.print(access);
            p.print("[");
            accessor.accept(this);
            p.print("]");
        }
    }
    

    /**
     * prints a method call expression
     */
    public void visitMethodCallExpression(JMethodCallExpression self,
                                          JExpression prefix,
                                          String ident,
                                          JExpression[] args) {

        assert (!ident.equals(Names.receiveMethod)) :
            "Error: RStream code generation should not see network receive method";

        // RMR { math functions are converted to use their floating-point counterparts;
        // to do this, some function names are prepended with a 'f', and others have an
        // 'f' appended to them
        if (Utils.isMathMethod(prefix, ident)) {
            p.print(Utils.cMathEquivalent(prefix, ident));
        } else {
            p.print(ident);
        }
        // } RMR
        
        p.print("(");
        if (args != null) {
            for (int i = 0; i < args.length; i++) {
                if (i != 0) {
                    p.print(", ");
                }
                /* this is a hack but there is no other way to do it,
                   if we are currently visiting fscanf and we are at the 3rd
                   argument, prepend an & to get the address and pass the pointer 
                   to the fscanf
                */
                if (ident.equals(Names.fscanf) && i == 2)
                    p.print("&");
                args[i].accept(this);
            }
        }
        p.print(")");
    }

    public JExpression passParentheses(JExpression exp) 
    {
        while (exp instanceof JParenthesedExpression)
            exp = ((JParenthesedExpression)exp).getExpr();
    
        return exp;
    }
    

    public void visitPeekExpression(SIRPeekExpression self,
                                    CType tapeType,
                                    JExpression num)
    {
        assert false : "RStream code generation should not see a pop statement";
    }
    
    public void visitPopExpression(SIRPopExpression self,
                                   CType tapeType)
    {
        assert false : "RStream code generation should not see a pop statement";
    }
    
    // visitPrintStatement innerited from ToCCommon
    
    public void visitPushExpression(SIRPushExpression self,
                                    CType tapeType,
                                    JExpression val)
    {
        assert false : "RStream Front-end should not see a push statement";
    }    

    
    

    // Special case for CTypes, to map some Java types to C types.
    protected void print(CType s) {
        if (s instanceof CArrayType){
            printArrayType((CArrayType)s);
        }
        else if (s.getTypeID() == TID_BOOLEAN)
            p.print("int");
        else if (s.toString().endsWith("Portal"))
            // ignore the specific type of portal in the C library
            p.print("portal");
        else
            p.print(s.toString());
    }
    

    /** This function is called if we have an assignment expression of array types 
        and we are generating C code.  This will generate code to perform an 
        element-wise copy **/
    private void arrayCopy(JExpression left, 
                           JExpression right) 
    {
        String ident = "";
        //this is used to find the new array expression
        //it is either a string for fields or JVarDef for locals
        Object varDef = null;
        //the var access expression
        JExpression var = left;
    
        //if this is an array access expression, get the variable access
        if (left instanceof JArrayAccessExpression) {
            var = Util.getVar((JArrayAccessExpression)left);
        }
    

        if (var instanceof JFieldAccessExpression) {
            varDef = ((JFieldAccessExpression)var).getIdent();
            ident = ((JFieldAccessExpression)var).getIdent();
        }
        else if (var instanceof JLocalVariableExpression) {
            varDef = ((JLocalVariableExpression)var).getVariable();
            ident = ((JLocalVariableExpression)var).getVariable().getIdent();
        }
        else 
            Utils.fail("Assigning an array to an unsupported expression of type " + left.getClass() + ": " + left);
    
        //  assert getDim(left.getType()) == getDim(right.getType()) :
        //    "Array dimensions of variables of array assignment do not match";
    
        //find the number of dimensions
        int bound = ((CArrayType)right.getType()).getArrayBound();
        //find the extent of each dimension
        int[] dims = getDims((CArrayType)var.getType());
        //if we are assigning elements from a lower dimension array to a higher
        //dim array, remember the difference
        int diff = dims.length - bound;
    
        assert diff >= 0 : "Error in array copy: " + left + " = " + right;

        assert bound > 0;

        //print out a loop that will perform the element-wise copy
        p.print("{\n");
        p.print("int ");
        //print the index var decls
        for (int i = 0; i < bound -1; i++)
            p.print(FlatIRToRS.ARRAY_COPY + i + ", ");
        p.print(FlatIRToRS.ARRAY_COPY + (bound - 1));
        p.print(";\n");
        for (int i = 0; i < bound; i++) {
            p.print("for (" + FlatIRToRS.ARRAY_COPY + i + " = 0; " + FlatIRToRS.ARRAY_COPY + i +  
                    " < " + dims[i + diff] + "; " + FlatIRToRS.ARRAY_COPY + i + "++)\n");
        }
        left.accept(this);
        for (int i = 0; i < bound; i++)
            p.print("[" + FlatIRToRS.ARRAY_COPY + i + "]");
        p.print(" = ");
        right.accept(this);
        for (int i = 0; i < bound; i++)
            p.print("[" + FlatIRToRS.ARRAY_COPY + i + "]");
        p.print(";\n}\n");
        return;
    }
    

    /**
    
    */
    public void visitComments(JavaStyleComment[] comments) {
        for (int i = 0; i < comments.length; i++)
            visitComment(comments[i]);
    }
    
    /**
    
    */
    public void visitComment(JavaStyleComment comment) {
        //don't print random comments, only sir comments
        if (!comment.getText().startsWith("SIR"))
            return;
    
        String str = "";
        if (comment.isLineComment())
            str = "\n";
        if (comment.hadSpaceBefore())
            str = str + " ";
        str = str + "/*" + comment.getText() + "*/";
        if (comment.hadSpaceAfter())
            str = str + " ";
    
        p.print(str);
    }
    

    /**
     * prints a cast expression
     */
    public void visitCastExpression(JCastExpression self,
                                    JExpression expr,
                                    CType type)
    {
        //hack, if we not generating abstract arrays
        //then don't print array casts for C
        if (!KjcOptions.absarray && type.isArrayType()) {
            expr.accept(this);
            return;
        }
    
        
        printLParen();
        p.print("(");
        printType(type);
        p.print(")");
        p.print("(");
        expr.accept(this);
        p.print(")");
        printRParen();
    }
    

    // ----------------------------------------------------------------------
    // UNUSED STREAM VISITORS
    // ----------------------------------------------------------------------

    /* pre-visit a pipeline */
    public void preVisitPipeline(SIRPipeline self,
                                 SIRPipelineIter iter) 
    {
    }
    

    /* pre-visit a splitjoin */
    public void preVisitSplitJoin(SIRSplitJoin self,
                                  SIRSplitJoinIter iter)
    {
    }
    

    /* pre-visit a feedbackloop */
    public void preVisitFeedbackLoop(SIRFeedbackLoop self,
                                     SIRFeedbackLoopIter iter)
    {
    }
    

    /**
     * POST-VISITS 
     */
        
    /* post-visit a pipeline */
    public void postVisitPipeline(SIRPipeline self,
                                  SIRPipelineIter iter) {
    }
    

    /* post-visit a splitjoin */
    public void postVisitSplitJoin(SIRSplitJoin self,
                                   SIRSplitJoinIter iter) {
    }
    

    /* post-visit a feedbackloop */
    public void postVisitFeedbackLoop(SIRFeedbackLoop self,
                                      SIRFeedbackLoopIter iter) {
    } 

    public void visitPhasedFilter(SIRPhasedFilter self,
                                  SIRPhasedFilterIter iter) {
        // This is a stub; it'll get filled in once we figure out how phased
        // filters should actually work.
    }

    protected void stackAllocateArray(String str) 
    {
        assert false : "Should not be called";
    }

    /**
     * prints a empty statement
     */
    public void visitEmptyStatement(JEmptyStatement self) {
        //if we are inside a for loop header, we need to print 
        //the ; of an empty statement
        if (forLoopHeader > 0) {
            p.newLine();
            p.print(";");
        }
    }

    //     /**
    //      * The main entry point of the visiting done by this class. 
    //      * print out c includes, visit the methods, and then generate
    //      * the main function in the c code that calls the driver function
    //      * that controls execution.
    //      *
    //      * @param self The filter we are visiting
    //      *
    //      */

    //     public void visitFilter(SIRFilter self,
    //              SIRFilterIter iter) {
    //  assert false : "Don't call me!";
    
    //  //Entry point of the visitor

    //  //p.print("#include <stdlib.h>\n");
    //  //p.print("#include <math.h>\n\n");

    //  //if there are structures in the code, include
    //  //the structure definition header files
    //  if (StrToRStream.structures.length > 0) 
    //      p.print("#include \"structs.h\"\n");
    
    //  printExterns();
    //  //Visit fields declared in the filter class
    //  JFieldDeclaration[] fields = self.getFields();
    //  for (int i = 0; i < fields.length; i++)
    //     fields[i].accept(this);
    
    //  //visit methods of filter, print the declaration first
    //  declOnly = true;
    //  JMethodDeclaration[] methods = self.getMethods();
    //  for (int i =0; i < methods.length; i++)
    //      methods[i].accept(this);
    
    //  //now print the functions with body
    //  declOnly = false;
    //  for (int i =0; i < methods.length; i++) {
    //      methods[i].accept(this);    
    //  }
    
    //  p.print("int main() {\n");
    //  //generate array initializer blocks for fields...
    //  p.printFieldArrayInits();
    
    //  //execute the main function
    //  p.print(Names.main + "();\n");
    
    //  //return 0 even though this should never return!
    //  p.print("  return 0;\n");
    //  //closes main()
    //  p.print("}\n");
       
    //  createFile();
    //     }

    //     /**
    //      * The entry method to this C conversion pass.  Given a flatnode containing
    //      * the single fused filter of the application, optimize the SIR code, if
    //      * enabled, and then generate then convert to C code and dump to a file.
    //      *
    //      * @param node The flatnode containing the single filter of the application.
    //      *
    //      */
    //     public static void generateCode(FlatNode node) 
    //     {
    //  assert false : "don't call me";
    
    //  //FlatIRToRS toC = new FlatIRToRS((SIRFilter)node.contents);
        
    //  //optimizations...
    //  System.out.println
    //      ("Optimizing SIR ...");

    //  ArrayDestroyer arrayDest=new ArrayDestroyer();

    //  //iterate over all the methods, calling the magic below...
    //  for (int i = 0; i < ((SIRFilter)node.contents).getMethods().length; i++) {
    //      JMethodDeclaration method=((SIRFilter)node.contents).getMethods()[i];
                
    //      if (!KjcOptions.nofieldprop) {
    //      Unroller unroller;
    //      do {
    //          do {
    //          unroller = new Unroller(new Hashtable());
    //          method.accept(unroller);
    //          } while (unroller.hasUnrolled());
            
    //          method.accept(new Propagator(new Hashtable()));
    //          unroller = new Unroller(new Hashtable());
    //          method.accept(unroller);
    //      } while(unroller.hasUnrolled());
        
    //      method.accept(new BlockFlattener());
    //      method.accept(new Propagator(new Hashtable()));
    //      } 
    //      else
    //      method.accept(new BlockFlattener());
    //      method.accept(arrayDest);
    //      method.accept(new VarDeclRaiser());
    //  }
    
    //  if(KjcOptions.destroyfieldarray)
    //     arrayDest.destroyFieldArrays((SIRFilter)node.contents);
    //     /*   
    //       try {
    //       SIRPrinter printer1 = new SIRPrinter();
    //       IterFactory.createFactory().createIter((SIRFilter)node.contents).accept(printer1);
    //       printer1.close();
    //       }
    //       catch (Exception e) 
    //       {
    //       }
    //  */
    //  //remove unused variables...
    //  RemoveUnusedVars.doit(node);
    //  //remove array initializers and remember them for placement later...
    //  toC.arrayInits = new ConvertArrayInitializers(node);
    //  //find all do loops, 
    //  toC.doloops = IDDoLoops.doit(node);
    //  //remove unnecessary do loops
    //  //RemoveDeadDoLoops.doit(node, toC.doloops);
    //  //now iterate over all the methods and generate the c code.
    //         IterFactory.createFactory().createIter((SIRFilter)node.contents).accept(toC);
    //     }


    
    //     private void createFile() {
    //  System.out.println("Code for application written to str.c");
    //  try {
    //      FileWriter fw = new FileWriter("str.c");
    //      fw.write(str.toString());
    //      fw.close();
    //  }
    //  catch (Exception e) {
    //      System.err.println("Unable to write tile code file for filter " +
    //                 filter.getName());
    //  }
    //     }

    //     //for now, just print all the common math functions as
    //     //external functions
    //     protected void printExterns() 
    //     {
    //  p.print("#define EXTERNC \n\n");
    //  p.print("extern EXTERNC int printf(char[], ...);\n");
    //  p.print("extern EXTERNC int fprintf(int, char[], ...);\n");
    //  p.print("extern EXTERNC int fopen(char[], char[]);\n");
    //  p.print("extern EXTERNC int fscanf(int, char[], ...);\n");
    //  p.print("extern EXTERNC float acosf(float);\n"); 
    //  p.print("extern EXTERNC float asinf(float);\n"); 
    //  p.print("extern EXTERNC float atanf(float);\n"); 
    //  p.print("extern EXTERNC float atan2f(float, float);\n"); 
    //  p.print("extern EXTERNC float ceilf(float);\n"); 
    //  p.print("extern EXTERNC float cosf(float);\n"); 
    //  p.print("extern EXTERNC float sinf(float);\n"); 
    //  p.print("extern EXTERNC float coshf(float);\n"); 
    //  p.print("extern EXTERNC float sinhf(float);\n"); 
    //  p.print("extern EXTERNC float expf(float);\n"); 
    //  p.print("extern EXTERNC float fabsf(float);\n"); 
    //  p.print("extern EXTERNC float modff(float, float *);\n"); 
    //  p.print("extern EXTERNC float fmodf(float, float);\n"); 
    //  p.print("extern EXTERNC float frexpf(float, int *);\n"); 
    //  p.print("extern EXTERNC float floorf(float);\n");        
    //  p.print("extern EXTERNC float logf(float);\n"); 
    //  p.print("extern EXTERNC float log10f(float, int);\n"); 
    //  p.print("extern EXTERNC float powf(float, float);\n"); 
    //  p.print("extern EXTERNC float rintf(float);\n"); 
    //  p.print("extern EXTERNC float sqrtf(float);\n"); 
    //  p.print("extern EXTERNC float tanhf(float);\n"); 
    //  p.print("extern EXTERNC float tanf(float);\n");
         
    //     }

}
