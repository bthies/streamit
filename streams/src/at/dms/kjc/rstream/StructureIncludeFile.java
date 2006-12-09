package at.dms.kjc.rstream;

//import at.dms.util.IRPrinter;
//import at.dms.util.SIRPrinter;
//import at.dms.kjc.*;
import at.dms.kjc.sir.*;
//import java.util.*;
import java.io.*;
//import at.dms.util.Utils;
import at.dms.kjc.common.CommonUtils;


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
     * @param other text to put in file
     */
    public static void doit(SIRStructure[] structs, String other) 
    {
        doit(structs, other, ".");
    }

    /**
     * Create structures include file in directory *dir*.
     * @param structs The structures used in the programs 
     * @param other text to put in file
    * @param dir The directory to store the include file.
     */
    public static void doit(SIRStructure[] structs, String other, String dir) 
    {
        if (structs.length == 0 && other.length() == 0) 
            return;
    
        try {
            FileWriter fw = new FileWriter(dir + "/structs.h");
            createStructureDefs(structs, fw);
            if (other != null) { fw.write(other); }
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
            // write the typedef for the struct.
            fw.write(CommonUtils.structToTypedef(current,true));
            fw.write("\n");
        }
    }
}
