package at.dms.kjc.raw;

import at.dms.util.IRPrinter;
import at.dms.util.SIRPrinter;
import at.dms.kjc.*;
import at.dms.kjc.iterator.*;
import at.dms.kjc.sir.*;
import at.dms.kjc.sir.lowering.*;
import at.dms.kjc.sir.lowering.partition.*;
import at.dms.kjc.sir.lowering.fusion.*;
import at.dms.kjc.sir.lowering.fission.*;
import at.dms.kjc.lir.*;
import java.util.*;
import java.io.*;
import at.dms.util.Utils;
import at.dms.kjc.flatgraph.*;

public class StructureIncludeFile implements FlatVisitor
{
    private HashSet passedTypes;
    
     /**
     * Create structures include file in current directory.
     */
    public static void doit(SIRStructure[] structs, FlatNode toplevel) 
    {
	doit(structs, toplevel, ".");
    }

    /**
     * Create structures include file in directory <dir>.
     */
    public static void doit(SIRStructure[] structs, FlatNode toplevel, String dir) 
    {
	if (structs.length == 0) 
	    return;
	
	new StructureIncludeFile(structs, toplevel, dir);
    }

    public StructureIncludeFile(SIRStructure[] structs, FlatNode toplevel, String dir) 
    {
	try {
	    FileWriter fw = new FileWriter(dir + "/structs.h");
	    passedTypes = new HashSet();

	    //determine which struct types are actually passed over channels
	    toplevel.accept(this, null, true);

	    createStructureDefs(structs, fw);
	    if (!KjcOptions.standalone)
		createPushPopFunctions(structs, fw);
	    fw.close();
	}
	catch (Exception e) {
	    e.printStackTrace();
	    System.err.println("Error creating structure include file");
	}
    }
    
    public void visitNode(FlatNode node) 
    {
	if (node.isFilter()) {
	    passedTypes.add(((SIRFilter)node.contents).getOutputType());
	    System.out.println(((SIRFilter)node.contents).getOutputType());
	    passedTypes.add(((SIRFilter)node.contents).getInputType());
	    System.out.println(((SIRFilter)node.contents).getInputType());
	}
    }
    

    /** 
     * create a c header file with all the structure definitions
     * as typedef'ed structs.
     **/
    private void createStructureDefs(SIRStructure[] structs, 
					    FileWriter fw) throws Exception
    {
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
		fw.write("inline void " + RawExecutionCode.structReceiveMethodPrefix + 
			 current.getIdent() + "(" + current.getIdent() + "*);\n\n");
	    }
	}
    }

    private void createPushPopFunctions(SIRStructure[] structs,
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
		    fw.write("\t" + Util.staticNetworkReceivePrefix());
		    fw.write("temp." + current.getFields()[j].getVariable().getIdent());
		    fw.write(Util.staticNetworkReceiveSuffix(current.getFields()[j].getType()) + "\n");
		}
	    }
	    fw.write("\treturn temp;\n}\n");

	    //create the pop functions that take a pointer argument
	    //these are more efficent, we use these when we can
	    fw.write("inline void " + RawExecutionCode.structReceiveMethodPrefix + 
		     current.getIdent() + "(" + 
		     current.getIdent() + "* temp) {\n");
	    for (int j = 0; j < current.getFields().length; j++) {
		if (current.getFields()[j].getType().isArrayType()) {
		    assert false;
		}
		else if (current.getFields()[j].getType().isClassType()) {
		    //if this is struct field, call the struct's popPointer method
		    fw.write("\t" + RawExecutionCode.structReceiveMethodPrefix + 
			     current.getFields()[j].getType() +
			     "(&temp->" + current.getFields()[j].getVariable().getIdent() +
			     ");\n");
		}
		else {
		    fw.write("\t" + Util.staticNetworkReceivePrefix());
		    fw.write("temp->" + current.getFields()[j].getVariable().getIdent());
		    fw.write(Util.staticNetworkReceiveSuffix(current.getFields()[j].getType()) + "\n");
		}
	    }
	    fw.write("}\n");
	    
	    //create the push functions
	    
	    fw.write("inline void push" + current.getIdent() + "(" + current.getIdent() +
		     "* temp) {\n");
	    for (int j = 0; j < current.getFields().length; j++) {
		//if this field is a struct type, use its method to push the field
		if (current.getFields()[j].getType().isArrayType()) {
		    System.out.println(((CArrayType)current.getFields()[j].getType()).getDims()[0]);
		    assert false;
		}
		else if (current.getFields()[j].getType().isClassType()) {
		    fw.write("push" + current.getFields()[j].getType() + "(&temp->" +
			     current.getFields()[j].getVariable().getIdent() + ");\n");
		}
		else {
		    fw.write("\t" + Util.staticNetworkSendPrefix(current.getFields()[j].getType()));
		    fw.write("temp->" + current.getFields()[j].getVariable().getIdent());
		    fw.write(Util.staticNetworkSendSuffix() + ";\n");
		}
	    }
	    fw.write("}\n");
	}
    }
    
}
