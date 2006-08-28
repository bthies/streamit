package at.dms.kjc.cluster;

import at.dms.kjc.*;
import at.dms.kjc.sir.*;
import java.util.*;
import java.io.*;

import at.dms.kjc.common.CommonConstants;
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
    private static HashSet<CType> passedTypes;

    public static boolean debugging = false;

    private StructureIncludeFile() {}
    
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
    public static void doit(SIRStructure[] structs, StreamGraph sg, String dir) 
    {
        if (structs.length == 0) 
            return;
        passedTypes = new HashSet<CType>();
        try {
            FileWriter fw = new FileWriter(dir + "/structs.h");
            passedTypes = new HashSet<CType>();

            //determine which struct types are actually passed over channels
            getPassedStructs(sg);

            createStructureDefs(structs, fw);
            if (!KjcOptions.standalone)
                createPushPopFunctions(structs, fw);
            fw.close();
        }
        catch (Exception e) {
            e.printStackTrace();
            System.err.println("Error creating structure include file");
        }
        passedTypes = null;  // clean up static data structure
    }

    private static void getPassedStructs (StreamGraph sg) {
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
     * Don't even think of it!
     * 
     * Only public to avoid using an inner class.
     */
    public void visitNode(FlatNode node) 
    {
        if (node.isFilter()) {
            SIRFilter fnode = (SIRFilter)node.contents;
            if (debugging) {
                System.err.println(this.getClass().getName() + " processing filter " 
                                   + fnode.getName() + ": " 
                                   +  fnode.getInputType() + "->" + fnode.getOutputType());
            }
            passedTypes.add(fnode.getOutputType());
            passedTypes.add(fnode.getInputType());
        }
    }
    

    /** 
     * create a c header file with all the structure definitions
     * as typedef'ed structs.
     **/
    private static void createStructureDefs(SIRStructure[] structs, 
                                     FileWriter fw) throws Exception
    {
        fw.write("#ifndef __STRUCTS_H\n");
        fw.write("#define __STRUCTS_H\n");
        for (int i = 0; i < structs.length; i++) {
            SIRStructure current = structs[i];
            fw.write("typedef struct __" + current.getIdent() + " {\n");
            for (int j = 0; j < current.getFields().length; j++) {
                fw.write("\t" + current.getFields()[j].getType() + " " +
                         current.getFields()[j].getVariable().getIdent() +
                         ";\n");
            }
            fw.write("} " + current.getIdent() + ";\n");
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
                                        FileWriter fw) throws Exception
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
