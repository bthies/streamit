package at.dms.kjc.rstream;

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


/**
 * This class generates the c struct definition for any 
 * structures used in the StreamIt code.
 *
 *
 * @author Michael Gordon
 * 
 */
public class StructureIncludeFile
{

    /**
     * Create structures include file in current directory.
     * @param structs The structures used in the programs 
     */
    public static void doit(SIRStructure[] structs) 
    {
	doit(structs, ".");
    }

    /**
     * Create structures include file in directory <dir>.
     * @param structs The structures used in the programs 
     * @param dir The directory to store the include file.
     */
    public static void doit(SIRStructure[] structs, String dir) 
    {
	if (structs.length == 0) 
	    return;
	
	try {
	    FileWriter fw = new FileWriter(dir + "/structs.h");
	    createStructureDefs(structs, fw);
	    fw.close();
	}
	catch (Exception e) {
	    e.printStackTrace();
	    System.err.println("Error creating structure include file");
	}
    }
    
    /** 
     * create a c header file with all the structure definitions
     * as typedef'ed structs.
     **/
    private static void createStructureDefs(SIRStructure[] structs, 
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
	}
    }
}
