package at.dms.kjc.cluster;

import at.dms.kjc.*;
import at.dms.kjc.sir.*;
import java.util.*;
import java.io.*;

import at.dms.kjc.common.CommonConstants;
import at.dms.kjc.common.CommonUtils;
import at.dms.kjc.common.RawUtil;
import at.dms.kjc.flatgraph.*;
//import at.dms.util.IRPrinter;
//import at.dms.util.SIRPrinter;

/**
 * Create structs.h and its contents.
 *  
 *  Used in raw (space) and cluster backends.
 *  
 * @author Janis
 *
 */
public class StructureIncludeFile {
    public static boolean debugging = false;
   
    /**
     * Create structures include file in current directory.
     * 
     * If standalone switch is not set, also creates push and pop
     * routines for using structure fields.
     */
    public static void doit(SIRStructure[] structs, StreamGraph sg) 
    {
        doit(structs, sg, ".");
    }

    /**
     * Create structures include file in directory <dir>.
     * 
     * If standalone switch is not set, also creates push and pop
     * routines for using structure fields.
     */
    private static void doit(SIRStructure[] structs, StreamGraph sg, String dir) 
    {
        if (structs.length == 0) 
            return;
        Set<CType> passedTypes;
        try {
            FileWriter fw = new FileWriter(dir + "/structs.h");
            passedTypes = new HashSet<CType>();

            //determine what types are passed over tapes
            getPassedStructs(sg, passedTypes);

            // horrible mess for getting typedefs for all vector types in program into vectorTypeDefs
            final Set<String> vectorTypeDefs = new HashSet<String>();
            sg.getTopLevel().accept(new StreamGraphVisitor(){
                public void visitStaticStreamGraph(StaticStreamGraph ssg) {
                    for (FlatNode fn : ssg.getFlatNodes()) {
                        if (fn.isFilter()) {
                            SIRFilter filter = (SIRFilter)fn.contents;
                            for (JFieldDeclaration decl : filter.getFields()) {
                                decl.accept(new SLIREmptyVisitor(){
                                    @Override
                                    public void visitVariableDefinition(JVariableDefinition self,
                                            int modifiers, CType type, String ident, JExpression expr) {
                                        if (type instanceof CVectorType) {
                                            vectorTypeDefs.add(((CVectorType)type).typedefString());
                                        }
                                    }
                                });
                            }
                            for (JMethodDeclaration m : filter.getMethods()) {
                                m.accept(new SLIREmptyVisitor(){
                                    @Override
                                    public void visitVariableDefinition(JVariableDefinition self,
                                            int modifiers, CType type, String ident, JExpression expr) {
                                        if (type instanceof CVectorType) {
                                            vectorTypeDefs.add(((CVectorType)type).typedefString());
                                        }
                                    }
                                });
                            }
                        }
                    }
                }
            }, null, true);
            
            createStructureDefs(structs, fw, passedTypes, vectorTypeDefs);
            if (!KjcOptions.standalone)
                createPushPopFunctions(structs, fw, passedTypes);
            fw.close();
        }
        catch (Exception e) {
            e.printStackTrace();
            System.err.println("Error creating structure include file");
        }
    }

    private static void getPassedStructs (StreamGraph sg, Set<CType> passedTypes) {
        for (StaticStreamGraph ssg : sg.getStaticSubGraphs()) {
            for (FlatNode node : ssg.getFlatNodes()) {
                if (node.isFilter()) {
                    SIRFilter fnode = (SIRFilter)node.contents;
                    if (debugging) {
                        System.err.println("StructureIncludeFile processing filter " 
                                           + fnode.getName() + ": " 
                                           + fnode.getInputType() + "->" + fnode.getOutputType());
                    }
                    passedTypes.add(fnode.getOutputType());
                    passedTypes.add(fnode.getInputType());
                }
            }
        }
    }
    
    /** 
     * create a c header file with all the structure definitions
     * as typedef'ed structs.
     **/
    private static void createStructureDefs(SIRStructure[] structs, 
                                     FileWriter fw,
                                     Set<CType> passedTypes, 
                                     Set<String> vectorTypeDefs) throws Exception
    {
        fw.write("#ifndef __STRUCTS_H\n");
        fw.write("#define __STRUCTS_H\n");
        for (String typedef : vectorTypeDefs) {
            fw.write(typedef);
            fw.write("\n");
        }
        for (int i = 0; i < structs.length; i++) {
            SIRStructure current = structs[i];
            // write the typedef for the struct.
            fw.write(CommonUtils.structToTypedef(current,true));
            fw.write("\n");
            //write the defs for the push/pop functions
            if (!KjcOptions.standalone && passedTypes.contains(current)) {
                fw.write("inline void push" + current.getIdent() + "(" + current.getIdent() +
                         "*);\n");
                fw.write("inline " + current.getIdent() + " pop" + current.getIdent() + "();\n");
                fw.write("inline void " + CommonConstants.structReceiveMethodPrefix + 
                         current.getIdent() + "(" + current.getIdent() + "*);\n\n");
            }
        }
        /* RMR { moved typedef outside of loop */
        // FIXME put in kluge for 'bit'  We do not have bit handling so 
        // bit is defined here as 'unsigned char'
        // Further kluge: until can figure out how to get cluster to expand
        // from unsigned byte to integer we just use integer!
        //    fw.write("typedef unsigned char bit;\n");
        fw.write("typedef int bit;\n");
        /* } RMR */
        fw.write("#ifndef round\n#define round(x) (floor((x)+0.5))\n#endif\n");
        fw.write("#endif // __STRUCTS_H\n");
    }

    private static void createPushPopFunctions(SIRStructure[] structs,
                                        FileWriter fw, Set<CType> passedTypes) throws Exception
    {
    
        //create the pop functions
        for (int i = 0; i < structs.length; i++) {
            SIRStructure current = structs[i];
            //if this type is not passed over a channel, then don't generate the push
            //pop functions for it...
            if (!passedTypes.contains(current))
                continue;
            fw.write("inline " + current.getIdent() + " pop" + current.getIdent() + "() {\n");
            fw.write("\t" + current.getIdent() + " temp;\n");
            for (int j = 0; j < current.getFields().length; j++) {
                fw.write("\t//" + current.getFields()[j].getType() + "\n");
                if (current.getFields()[j].getType().isArrayType()) {
                    System.out.println(((CArrayType)current.getFields()[j].getType()).getDims());
                    assert false;
                }
                else if (current.getFields()[j].getType().isClassType()) {
         
                    fw.write("\ttemp." + current.getFields()[j].getVariable().getIdent() +
                             " = pop" + current.getFields()[j].getType() + "();\n");
                }
                else {
                    fw.write("\t" + RawUtil.staticNetworkReceivePrefix());
                    fw.write("temp." + current.getFields()[j].getVariable().getIdent());
                    fw.write(RawUtil.staticNetworkReceiveSuffix(current.getFields()[j].getType()) + "\n");
                }
            }
            fw.write("\treturn temp;\n}\n");

            //create the pop functions that take a pointer argument
            //these are more efficent, we use these when we can
            fw.write("inline void " + CommonConstants.structReceiveMethodPrefix + 
                     current.getIdent() + "(" + 
                     current.getIdent() + "* temp) {\n");
            for (int j = 0; j < current.getFields().length; j++) {
                if (current.getFields()[j].getType().isArrayType()) {
                    assert false;
                }
                else if (current.getFields()[j].getType().isClassType()) {
                    //if this is struct field, call the struct's popPointer method
                    fw.write("\t" + CommonConstants.structReceiveMethodPrefix + 
                             current.getFields()[j].getType() +
                             "(&temp->" + current.getFields()[j].getVariable().getIdent() +
                             ");\n");
                }
                else {
                    fw.write("\t" + RawUtil.staticNetworkReceivePrefix());
                    fw.write("temp->" + current.getFields()[j].getVariable().getIdent());
                    fw.write(RawUtil.staticNetworkReceiveSuffix(current.getFields()[j].getType()) + "\n");
                }
            }
            fw.write("}\n");
        
            //create the push functions
        
            fw.write("inline void push" + current.getIdent() + "(" + current.getIdent() +
                     "* temp) {\n");
            for (int j = 0; j < current.getFields().length; j++) {
                //if this field is a struct type, use its method to push the field
                if (current.getFields()[j].getType().isArrayType()) {
                    System.err.println(((CArrayType)current.getFields()[j].getType()).getDims()[0]);
                    assert false;
                }
                else if (current.getFields()[j].getType().isClassType()) {
                    fw.write("push" + current.getFields()[j].getType() + "(&temp->" +
                             current.getFields()[j].getVariable().getIdent() + ");\n");
                }
                else {
                    fw.write("\t" + RawUtil.staticNetworkSendPrefix(current.getFields()[j].getType()));
                    fw.write("temp->" + current.getFields()[j].getVariable().getIdent());
                    fw.write(RawUtil.staticNetworkSendSuffix() + ";\n");
                }
            }
            fw.write("}\n");
        }
    }
    
}
