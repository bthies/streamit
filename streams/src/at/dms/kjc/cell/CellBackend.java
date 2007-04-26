package at.dms.kjc.cell;

import at.dms.kjc.JInterfaceDeclaration;
import at.dms.kjc.sir.SIRGlobal;
import at.dms.kjc.sir.SIRHelper;
import at.dms.kjc.sir.SIRInterfaceTable;
import at.dms.kjc.sir.SIRStream;
import at.dms.kjc.sir.SIRStructure;

public class CellBackend {

    public static void run(SIRStream str,
            JInterfaceDeclaration[] interfaces,
            SIRInterfaceTable[] interfaceTables,
            SIRStructure[]structs,
            SIRHelper[] helpers,
            SIRGlobal global) {
        System.out.println("Hi, I'm the new back end.");
        System.exit(0);
    }
}
