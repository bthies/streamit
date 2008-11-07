package at.dms.kjc.backendSupport;

import java.util.*;

import at.dms.kjc.*;
import at.dms.kjc.sir.*;
import at.dms.kjc.backendSupport.*;
import at.dms.kjc.slicegraph.*;
import at.dms.kjc.common.*;

    /**
    * Takes a ComputeNode collection, a collection of Channel's, 
    * and a mapping from Channel x end -> ComputeNode and emits code for the ComputeNode. 
    *
    * Inner class CodeGen is the visitor that emits the code for a JPhylum.
    * @author dimock
    */
public class EmitCode {
    // variable name prefix for copying arrays.
    protected static final String ARRAY_COPY = "__array_copy__";
    protected BackEndFactory backendbits;
    protected CodeGen codegen;

    /**
     * Constructor.
     * @param backendbits indicates BackEndFactory containing all useful info.
     */
    public EmitCode (BackEndFactory backendbits) {
        super();
        this.backendbits = backendbits;
        codegen = null;
    }
    
    public void emitCodeForComputeNode (ComputeNode n, CodegenPrintWriter p) {
        codegen = new CodeGen(p);
        emitCodeForComputeNode(n, p, codegen);
    }

    /**
     * Given a ComputeNode and a CodegenPrintWrite, print all code for the ComputeNode.
     * Channel information relevant to the ComputeNode is printed based on data in the
     * BackEndFactory passed when this class was instantiated.
     * @param n The ComputeNode to emit code for.
     * @param p The CodegenPrintWriter (left open on return).
     */
    public void emitCodeForComputeNode (ComputeNode n, 
            CodegenPrintWriter p, CodeGen codegen) {
        
        SIRCodeUnit fieldsAndMethods = n.getComputeCode();
        emitCodeForComputeStore(fieldsAndMethods, n, p, codegen);
    }
    
    public void emitCodeForComputeStore (SIRCodeUnit fieldsAndMethods,
            ComputeNode n, CodegenPrintWriter p, CodeGen codegen) {
        
        // Standard final optimization of a code unit before code emission:
        // unrolling and constant prop as allowed, DCE, array destruction into scalars.
        (new at.dms.kjc.sir.lowering.FinalUnitOptimize()).optimize(fieldsAndMethods);
        
        p.println("// code for processor " + n.getUniqueId());
        
        // generate function prototypes for methods so that they can call each other
        // in C.
        codegen.setDeclOnly(true);
        for (JMethodDeclaration method : fieldsAndMethods.getMethods()) {
            method.accept(codegen);
        }
        p.println("");
        codegen.setDeclOnly(false);

        // generate code for ends of channels that connect to code on this ComputeNode
        Set<Channel> upstreamEnds = getUpstreamEnds(n);
        Set<Channel> downstreamEnds = getDownstreamEnds(n);
        
        // externs
        for (Channel c : upstreamEnds) {
            if (c.writeDeclsExtern() != null) {
                for (JStatement d : c.writeDeclsExtern()) { d.accept(codegen); }
            }
        }
       
        for (Channel c : downstreamEnds) {
            if (c.readDeclsExtern() != null) {
                for (JStatement d : c.readDeclsExtern()) { d.accept(codegen); }
            }
        }

        for (Channel c : upstreamEnds) {
            if (c.dataDecls() != null) {
                // wrap in #ifndef for case where different ends have
                // are in different files that eventually get concatenated.
                p.println();
                p.println("#ifndef " + c.getIdent() + "_CHANNEL_DATA");
                for (JStatement d : c.dataDecls()) { d.accept(codegen); }
                p.println();
                p.println("#define " + c.getIdent() + "_CHANNEL_DATA");
                p.println("#endif");
            }
        }
        
        for (Channel c : downstreamEnds) {
            if (c.dataDecls() != null && ! upstreamEnds.contains(c)) {
                p.println("#ifndef " + c.getIdent() + "_CHANNEL_DATA");
                for (JStatement d : c.dataDecls()) { d.accept(codegen); }
                p.println();
                p.println("#define " + c.getIdent() + "_CHANNEL_DATA");
                p.println("#endif");
            }
        }

        for (Channel c : upstreamEnds) {
            p.println("/* upstream end of " + c + "(" + c.getIdent() + ") */");
            p.println("/* " + upstreamDescription(c) + " -> " + downstreamDescription(c) + " */");
            if (c.writeDecls() != null) {
                for (JStatement d : c.writeDecls()) { d.accept(codegen); }
            }
            if (c.pushMethod() != null) { c.pushMethod().accept(codegen); }
        }

        for (Channel c : downstreamEnds) {
            p.println("/* downstream end of " + c + "(" + c.getIdent() + ") */");
            p.println("/* " + upstreamDescription(c) + " -> " + downstreamDescription(c) + " */");
            if (c.readDecls() != null) {
                for (JStatement d : c.readDecls()) { d.accept(codegen); }
            }
            if (c.peekMethod() != null) { c.peekMethod().accept(codegen); }
            if (c.assignFromPeekMethod() != null) { c.assignFromPeekMethod().accept(codegen); }
            if (c.popMethod() != null) { c.popMethod().accept(codegen); }
            if (c.assignFromPopMethod() != null) { c.assignFromPopMethod().accept(codegen); }
            if (c.popManyMethod() != null) { c.popManyMethod().accept(codegen); }
         }
        p.println("");
        
        // generate declarations for fields
        for (JFieldDeclaration field : fieldsAndMethods.getFields()) {
            field.accept(codegen);
        }
        p.println("");
        
        // generate functions for methods
        codegen.setDeclOnly(false);
        for (JMethodDeclaration method : fieldsAndMethods.getMethods()) {
            method.accept(codegen);
        }
    }
    
    /**
     * Get all channels having an upstream end on ComputeNode <b>n</b>.
     * @param n 
     * @return A collection of channels.
     */
    private Set<Channel> getUpstreamEnds (ComputeNode n) {
        Set<Channel> retval = new HashSet<Channel>();
        Layout l = backendbits.getLayout();
        Collection<Channel> channels = backendbits.getChannels();
        for (Channel c : channels) {
            SliceNode s = c.getSource();
            if (l.getComputeNode(s) == n) {
                retval.add(c);
            }
        }
        return retval;
    }
    
    
    /**
     * Get all channels having an downstream end on ComputeNode <b>n</b>.
     * @param n 
     * @return A collection of channels.
     */
    private Set<Channel> getDownstreamEnds (ComputeNode n) {
        Set<Channel> retval = new HashSet<Channel>();
        Layout l = backendbits.getLayout();
        Collection<Channel> channels = backendbits.getChannels();
        for (Channel c : channels) {
            SliceNode s = c.getDest();
            if (l.getComputeNode(s) == n) {
                retval.add(c);
            }
        }
        return retval;
    }
    
    /** representation of upstream end of channel for debugging */
    private String upstreamDescription(Channel c) {
        return c.getSource().toString();
    }
    
    /** representation of downstream end of channel for debugging */
    private String downstreamDescription(Channel c) {
        return c.getDest().toString();
    }
    
    /**
     * Standard code for front of a C file here.
     * Override!
     */
    public void generateCHeader(CodegenPrintWriter p) {
        codegen.getPrinter();
        p.println("// Global Header Code Here");
        p.println("#include <math.h>");
    }
    
    /**
     * Generate a "main" function.
     * Override!
     */
    public void generateMain(CodegenPrintWriter p) {
        p.println();
        p.println();
        p.println("// main() Function Here");
        // dumb template to override
        p.println(
"int main(int argc, char** argv) {\n");
        p.indent();
        p.println(
backendbits.getComputeNodes().getNthComputeNode(0).getComputeCode().getMainFunction().getName()
+ "();");
        p.outdent();
        p.println("}");
    }

    
    /**
     * Class to actually emit code.
     * @author dimock
     *
     */
    protected class CodeGen extends ToC implements SLIRVisitor,CodeGenerator {
    // Overridden methods from ToC, ToCCommon, SLIRVisitor
      
    /** set to true when declarations are local (so in methods, not in fields).  
     * Tested to determine whether to add declarations. */
    protected boolean declsAreLocal = false;

    
    protected CodeGen(CodegenPrintWriter p) {
        super(p);
        // emitting C, not C++, so no "boolean"
        hasBoolType = false;
    }
        
    @Override
    public void visitPeekExpression(SIRPeekExpression self,
            CType tapeType,
            JExpression num) {
        throw new AssertionError("emitter should see no peek expressions");
    }

    @Override
    public void visitPopExpression(SIRPopExpression self,
            CType tapeType) {
        throw new AssertionError("emitter should see no pop expressions");
    }

    @Override
    public void visitPushExpression(SIRPushExpression self,
            CType tapeType,
            JExpression val) {
        throw new AssertionError("emitter should see no push expressions");
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
    
        
        if ((left.getType() != null && left.getType().isArrayType()) ||
            (right.getType() != null && right.getType().isArrayType())) {
        
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
        //the var access expression
        JExpression var = left;
    
        //if this is an array access expression, get the variable access
        if (left instanceof JArrayAccessExpression) {
            var = CommonUtils.lhsBaseExpr((JArrayAccessExpression)left);
        }

        //copying arrays inside of structs is not currently supported.
        assert (var instanceof JFieldAccessExpression 
                || var instanceof JLocalVariableExpression) :
            "Assigning an array to an unsupported expression of type " +
            left.getClass() + ": " + left;
    
        //  assert getDim(left.getType()) == getDim(right.getType()) :
        //    "Array dimensions of variables of array assignment do not match";
    
        //find the number of dimensions
        int rbound = ((CArrayType)right.getType()).getArrayBound();
        int lbound = ((CArrayType)var.getType()).getArrayBound();
        //if we are assigning elements from a lower dimension array to a higher
        //dim array, remember the difference
        int diff = lbound - rbound;
        //find the extent of each dimension
        int dims[];
        try {
            dims = CommonUtils.makeArrayInts(((CArrayType)var.getType()).getDims());
        } catch (AssertionError e) {
            // Constant prop didn't get to lhs (can happen in field array = local array).
            // get dimensions from rhs.
            dims = CommonUtils.makeArrayInts(((CArrayType)right.getType()).getDims());
            // In this case, so far has been safe to assume that arrays have same number of dimensions.
            assert lbound == rbound;
        }
    
        assert diff >= 0 : "Error in array copy: " + left + " = " + right;

        assert rbound > 0;

        //print out a loop that will perform the element-wise copy
        p.print("{\n");
        p.print("int ");
        //print the index var decls
        for (int i = 0; i < rbound -1; i++)
            p.print(EmitCode.ARRAY_COPY + i + ", ");
        p.print(EmitCode.ARRAY_COPY + (rbound - 1));
        p.print(";\n");
        for (int i = 0; i < rbound; i++) {
            p.print("for (" + EmitCode.ARRAY_COPY + i + " = 0; " + EmitCode.ARRAY_COPY + i +  
                    " < " + dims[i + diff] + "; " + EmitCode.ARRAY_COPY + i + "++)\n");
        }
        left.accept(this);
        for (int i = 0; i < rbound; i++)
            p.print("[" + EmitCode.ARRAY_COPY + i + "]");
        p.print(" = ");
        right.accept(this);
        for (int i = 0; i < rbound; i++)
            p.print("[" + EmitCode.ARRAY_COPY + i + "]");
        p.print(";\n}\n");
        return;
    }

    /**
     * Simplify code for variable definitions.
     * Be able to emit "static" "const" if need be.
     * Do not attempt to initialize variables to default values.
     */
    @Override
    public void visitVariableDefinition(JVariableDefinition self,
                                        int modifiers,
                                        CType type,
                                        String ident,
                                        JExpression expr) {
        if (KjcOptions.tilera > 0 && type == CStdType.Double) {
            type = CStdType.Float;
            self.setType(CStdType.Float);
        }
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
            } else if (declsAreLocal) {
                // C stack allocation: StreamIt variables are initialized to 0
                // (StreamIt Language Specification 2.1, section 3.3.3) but C
                // does not automatically zero out variables on the stack so we
                // need to do it here.
                // TODO: gcc does not always eliminate array initialization code
                // in the situation where all elements are written before they are read.
                // we should probably put that check here.
                if (type.isOrdinal()) { p.print(" = 0"); }
                else if (type.isFloatingPoint()) {p.print(" = 0.0f"); }
                else if (type.isArrayType()) {
                    // gcc 4.1.1 will not zero out an array of vectors using this syntax!
                    if (! (((CArrayType)type).getBaseType() instanceof CVectorType)
                     && ! (((CArrayType)type).getBaseType() instanceof CVectorTypeLow)) {
                            p.print(" = {0}");
                        } 
                    }
                else if (type.isClassType()) {
                    if (((CClassType)type).toString().equals("java.lang.String")) {
                        p.print(" = NULL;"); 
                    } else {
                        p.print(" = {0}");
                    }
                }

            }
        }
        p.print(";");
    }

    /**
     * Prints a method call expression.
     */
    @Override
    public void visitMethodCallExpression(JMethodCallExpression self,
                                          JExpression prefix,
                                          String ident,
                                          JExpression[] args) {
        // math functions are converted to use their floating-point counterparts;
        if (at.dms.util.Utils.isMathMethod(prefix, ident) && !KjcOptions.fixedpoint) {
            p.print(at.dms.util.Utils.cMathEquivalent(prefix, ident));
        } else {
            p.print(ident);
        }
        
        p.print("(");
        visitArgs(args, 0);
        p.print(")");
    }


    /**
     * prints a method declaration or prototype depending.
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
        declsAreLocal = true;
        if (! this.isDeclOnly()) { p.newLine(); } // some extra space if not just declaration.
        p.newLine();
        if ((modifiers & at.dms.kjc.Constants.ACC_PUBLIC) == 0) {
            p.print("static ");
        }
        if ((modifiers & at.dms.kjc.Constants.ACC_INLINE) != 0) {
            p.print("inline ");
        }
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
            declsAreLocal = false;
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
        declsAreLocal = false;
        method = null;
    }
    }
}

