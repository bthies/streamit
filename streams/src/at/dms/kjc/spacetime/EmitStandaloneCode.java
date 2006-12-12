// $Id
package at.dms.kjc.spacetime;

import at.dms.kjc.*;
import at.dms.kjc.sir.*;
import at.dms.kjc.common.*;
import at.dms.kjc.slicegraph.*;

/**
 * @author dimock
 *
 */
public class EmitStandaloneCode extends ToC implements SLIRVisitor,CodeGenerator {

    // variable name prefix for copying arrays.
    private static final String ARRAY_COPY = "__array_copy__";

    // counts nesting of for loops to get separator correct
    private int forLoopHeader = 0;
    
    /**
     * Emit C code for a slice graph with exactly one FilterSlice
     * @param sliceGraph
     */
    public static void emitForSingleSlice(Slice[] sliceGraph) {
        assert sliceGraph.length == 1;
        // make sure we have a single Slice in the SliceGraph
        Slice slice = sliceGraph[0];
        // sanity check that SliceGraph really is a single slice
        assert slice.getHead().getWidth() == 0;
        assert slice.getTail().getWidth() == 0;
        // make sure that this slice contains a single Filter
        assert slice.getHead().getNext().isFilterSlice();
        assert slice.getTail().getPrevious().isFilterSlice();
        assert slice.getHead().getNext() == slice.getTail().getPrevious();
        
        // having checked, emit code into a CodegenPrintWriter
        EmitStandaloneCode emitter = new EmitStandaloneCode();
        emitter.singleSlice(slice);
        
        // now copy emitted code out to a file
        try {
            java.io.FileWriter fw = new java.io.FileWriter("standalone.c");
            fw.write(emitter.getPrinter().getString());
            fw.close();
        } catch (Exception e) {
            System.err.println("Error writing code to file.");
            e.printStackTrace();
            System.exit(-1);
        }
    }
    
    /**
     * 
     */
    public EmitStandaloneCode() {
        super();
    }
    
    /**
     * called for emitForSingleSlice after an object is set up.
     * @param slice 
     */
    public void singleSlice(Slice slice) {
        
        FilterInfo.canUse();
        FilterSliceNode filternode = (FilterSliceNode)slice.getHead().getNext();
        FilterContent filtercontent = filternode.getFilter();
        
        (new at.dms.kjc.sir.lowering.FinalUnitOptimize()).optimize(filtercontent);

        
        
//        System.err.println("// str before codegen");
//        for (JFieldDeclaration field : filtercontent.getFields()) { 
//            SIRToStreamIt.run(field);
//        }
//        for (JMethodDeclaration method : filtercontent.getMethods()) {
//            SIRToStreamIt.run(method);
//        }
//        System.err.println("// END str before codegen");

        
        FilterInfo filterinfo = FilterInfo.getFilterInfo(filternode);
        
        this.hasBoolType = false;  // emitting C, not C++, so no "boolean"

        // write out fixed header information
        generateHeader();
        
        // generate function prototypes for methods
        this.setDeclOnly(true);
        for (JMethodDeclaration method : filtercontent.getMethods()) {
            method.accept(this);
        }
        this.setDeclOnly(false);
        p.println("");
        
        // generate globals for fields
        for (JFieldDeclaration field : filtercontent.getFields()) {
            field.accept(this);
        }
        p.println("");
        
        // generate functions for methods
        this.setDeclOnly(false);
        for (JMethodDeclaration method : filtercontent.getMethods()) {
            method.accept(this);
        }
        
        // generate a main() function
        generateMain();
        
    }
    
    /**
     * Standard code for front of the file here.
     *
     */
    private void generateHeader() {
        p.println("// Global Header Code Here");
        p.println("#include <math.h>");
    }

    /**
     * Generate a "main" function.
     */
    private void generateMain() {
        p.println();
        p.println();
        p.println("// main() Function Here");
        p.println(
"/* helper routines to parse command line arguments */\n"+
"#include <unistd.h>\n" +
"\n"+
"/* retrieve iteration count for top level driver */\n"+
"static int __getIterationCounter(int argc, char** argv) {\n"+
"    int flag;\n"+
"    while ((flag = getopt(argc, argv, \"i:\")) != -1)\n"+
"       if (flag == 'i') return atoi(optarg);\n"+
"    return -1; /* default iteration count (run indefinitely) */\n"+
"}"+
"\n"+
"int main(int argc, char** argv) {\n"+
"  int __iterationCounter = __getIterationCounter(argc, argv);\n"+
"  init();\n"+
"  while ((__iterationCounter--)) {\n"+
"      work();\n"+
"  }\n"+
"  return 0;\n"+
"}\n"
        );
    }
    

    // Overridden methods from ToC, ToCCommon, SLIRVisitor
    
    @Override
    public void visitPeekExpression(SIRPeekExpression self,
            CType tapeType,
            JExpression num) {
        throw new AssertionError("Single Slice code emitter should see no peek expressions");
    }

    @Override
    public void visitPopExpression(SIRPopExpression self,
            CType tapeType) {
        throw new AssertionError("Single Slice code emitter should see no pop expressions");
    }

    @Override
    public void visitPushExpression(SIRPushExpression self,
            CType tapeType,
            JExpression val) {
        throw new AssertionError("Single Slice code emitter should see no push expressions");
    }
  
    @Override
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
            var = CommonUtils.lhsBaseExpr((JArrayAccessExpression)left);
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
            throw new AssertionError("Assigning an array to an unsupported expression of type " + left.getClass() + ": " + left);
    
        //  assert getDim(left.getType()) == getDim(right.getType()) :
        //    "Array dimensions of variables of array assignment do not match";
    
        //find the number of dimensions
        int bound = ((CArrayType)right.getType()).getArrayBound();
        //find the extent of each dimension
        int[] dims = CommonUtils.makeArrayInts(((CArrayType)var.getType()).getDims());
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
            p.print(EmitStandaloneCode.ARRAY_COPY + i + ", ");
        p.print(EmitStandaloneCode.ARRAY_COPY + (bound - 1));
        p.print(";\n");
        for (int i = 0; i < bound; i++) {
            p.print("for (" + EmitStandaloneCode.ARRAY_COPY + i + " = 0; " + EmitStandaloneCode.ARRAY_COPY + i +  
                    " < " + dims[i + diff] + "; " + EmitStandaloneCode.ARRAY_COPY + i + "++)\n");
        }
        left.accept(this);
        for (int i = 0; i < bound; i++)
            p.print("[" + EmitStandaloneCode.ARRAY_COPY + i + "]");
        p.print(" = ");
        right.accept(this);
        for (int i = 0; i < bound; i++)
            p.print("[" + EmitStandaloneCode.ARRAY_COPY + i + "]");
        p.print(";\n}\n");
        return;
    }

    @Override
    public void visitVariableDefinition(JVariableDefinition self,
                                        int modifiers,
                                        CType type,
                                        String ident,
                                        JExpression expr) {
        if ((modifiers & ACC_STATIC) != 0) {
            p.print ("static ");
            if ((modifiers & ACC_FINAL) != 0) {
                p.print ("const ");
            }
        }

        if (expr instanceof JArrayInitializer) {
            declareInitializedArray(type, ident, expr);
        } else {

            printDecl (type, ident);

            if (expr != null && !(expr instanceof JNewArrayExpression)) {
                p.print (" = ");
                expr.accept (this);
	    }
	}
        p.print(";");
    }

    /**
     * prints a method call expression
     */
    public void visitMethodCallExpression(JMethodCallExpression self,
                                          JExpression prefix,
                                          String ident,
                                          JExpression[] args) {
        // RMR { math functions are converted to use their floating-point counterparts;
        // to do this, some function names are prepended with a 'f', and others have an
        // 'f' appended to them
        if (at.dms.util.Utils.isMathMethod(prefix, ident) 
            && (at.dms.util.Utils.mathMethodRequiresFloatPrefix(prefix, ident))) {
            p.print("f");
            p.print(ident);
        }
        else {
            p.print(ident);

            //we want single precision versions of the math functions
            if (at.dms.util.Utils.isMathMethod(prefix, ident)) {
                p.print("f");
            }
        }
        // } RMR
        
        p.print("(");
        visitArgs(args, 0);
        p.print(")");
    }


    /**
     * prints a method declaration
     */
    @Override
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
        printType(returnType);
        p.print(" ");
        p.print(ident);
    
        p.print("(");
        int count = 0;
    
        for (int i = 0; i < parameters.length; i++) {
            if (count != 0) {
                p.print(", ");
            }
            parameters[i].accept(this);
            count++;
        }
        p.print(")");

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

    /**
     * prints a empty statement
     */
    @Override
    public void visitEmptyStatement(JEmptyStatement self) {
        //if we are inside a for loop header, we need to print 
        //the ; of an empty statement
        if (forLoopHeader > 0) {
            p.newLine();
            p.print(";");
        }
    }

    @Override
    public void visitForStatement(JForStatement self, 
				  JStatement init,
                                  JExpression cond, 
				  JStatement incr, 
				  JStatement body) {
        // be careful, if you return prematurely, decrement me
        forLoopHeader++;

        p.print("for (");
        if (init != null) {
            init.accept(this);
            // the ; will print in a statement visitor
        }

        p.print(" ");
        if (cond != null) {
            cond.accept(this);
        }
        // cond is an expression so print the ;
        p.print("; ");
        if (incr != null) {
            EmitStandaloneCode l2c = new EmitStandaloneCode();
            incr.accept(l2c);
            // get String
            String str = l2c.p.getString();
            // leave off the trailing semicolon if there is one
            if (str.endsWith(";")) {
                p.print(str.substring(0, str.length() - 1));
            } else {
                p.print(str);
            }
        }

        forLoopHeader--;
        p.print(") ");

        p.print("{");
        p.indent();
        body.accept(this);
        p.outdent();
        p.newLine();
        p.print("}");
    }
}
