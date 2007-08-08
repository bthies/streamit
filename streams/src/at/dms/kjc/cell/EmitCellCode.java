package at.dms.kjc.cell;

import at.dms.kjc.CArrayType;
import at.dms.kjc.CClassType;
import at.dms.kjc.CType;
import at.dms.kjc.CVectorType;
import at.dms.kjc.CVectorTypeLow;
import at.dms.kjc.JArrayInitializer;
import at.dms.kjc.JBlock;
import at.dms.kjc.JEmittedTextExpression;
import at.dms.kjc.JExpression;
import at.dms.kjc.JExpressionStatement;
import at.dms.kjc.JFieldDeclaration;
import at.dms.kjc.JFormalParameter;
import at.dms.kjc.JMethodCallExpression;
import at.dms.kjc.JMethodDeclaration;
import at.dms.kjc.JNewArrayExpression;
import at.dms.kjc.JStatement;
import at.dms.kjc.JVariableDefinition;
import at.dms.kjc.JWhileStatement;
import at.dms.kjc.backendSupport.ComputeNode;
import at.dms.kjc.backendSupport.EmitCode;
import at.dms.kjc.common.CodegenPrintWriter;
import at.dms.kjc.common.MacroConversion;
import at.dms.util.Utils;

public class EmitCellCode extends EmitCode {

    public EmitCellCode(CellBackendFactory backendBits) {
        super(backendBits);
    }
    
    @Override
    public void emitCodeForComputeNode (ComputeNode n, 
            CodegenPrintWriter p) {
        if (n instanceof SPU) {
            codegen = new CellSPUCodeGen(p);
            emitCodeForComputeNode(n, p, codegen);
        }
        else {
            codegen = new CellPPUCodeGen(p);
            emitCodeForComputeNode(n, p, codegen);
        }

    }
    
    protected class CellPPUCodeGen extends CodeGen {
        CellPPUCodeGen(CodegenPrintWriter p) {
            super(p);
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
                //p.print(" = {0}");
            }
        

            p.print(";");
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
            System.out.println(ident);
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
                                //p.print(" = {0}");
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
    }
    
    protected class CellSPUCodeGen extends CodeGen {
        
        CellSPUCodeGen(CodegenPrintWriter p) {
            super(p);
        }
        
        /**
         * Prints a method call expression.
         */
        @Override
        public void visitMethodCallExpression(JMethodCallExpression self,
                                              JExpression prefix,
                                              String ident,
                                              JExpression[] args) {
            if (ident.equals("push") || ident.equals("pop") || ident.equals("peek")
                    || ident.equals("popn")
                    || at.dms.util.Utils.isMathMethod(prefix, ident)) {
                super.visitMethodCallExpression(self, prefix, ident, args);
                return;
            }
            p.print("CALL_FUNC(");
            p.print(ident);
            if (args.length > 0) p.print(",");
            visitArgs(args, 0);
            p.print(")");
        }
        
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
//            if ((modifiers & at.dms.kjc.Constants.ACC_PUBLIC) == 0) {
//                p.print("static ");
//            }
//            if ((modifiers & at.dms.kjc.Constants.ACC_INLINE) != 0) {
//                p.print("inline ");
//            }
            
            if (ident.equals("__MAIN__")) {
                //print the declaration then return
                if (isDeclOnly()) {
                    declsAreLocal = false;
                    return;
                }
                p.println("BEGIN_WORK_FUNC");
            } else if (ident.equals("__INIT_FUNC__")) {
                if (isDeclOnly()) {
                    declsAreLocal = false;
                    return;
                }
                p.println("BEGIN_INIT_FUNC");
            } else {
                if (isDeclOnly()) {
                    p.print("DECLARE_FUNC(");
                } else {
                    p.print("BEGIN_FUNC(");
                }
                p.print(ident);
                p.print(", ");
                printType(returnType);
                for (int i = 0; i < parameters.length; i++) {
                    p.print(", ");
                    parameters[i].accept(this);
                }
                p.print(")");
            }
//            } else {
//                printType(returnType);
//                p.print(" ");
//                p.print(ident);
//            
//                p.print("(");
//                int count = 0;
//            
//                for (int i = 0; i < parameters.length; i++) {
//                    if (count != 0) {
//                        p.print(", ");
//                    }
//                    parameters[i].accept(this);
//                    count++;
//                }
//                p.print(")");
//    
//            }

            //print the declaration then return
            if (isDeclOnly()) {
                p.print(";");
                declsAreLocal = false;
                return;
            }
            //set the current method we are visiting
            method = self;
        
            //p.print(" ");
            if (body != null) 
                body.accept(this);
            else 
                p.print(";");

            p.newLine();
            declsAreLocal = false;
            method = null;
            if (ident.equals("__MAIN__")) p.println("END_WORK_FUNC");
            else if (ident.equals("__INIT_FUNC__")) p.println("END_INIT_FUNC");
            else p.println("END_FUNC");

        }
        
//        /**
//         * prints a while statement
//         */
//        public void visitWhileStatement(JWhileStatement self,
//                                        JExpression cond,
//                                        JStatement body) {
//            body.accept(this);
//        }
    }
}
