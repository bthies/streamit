package at.dms.kjc;

import at.dms.kjc.sir.*;
import at.dms.kjc.sir.lowering.*;
import at.dms.kjc.lir.*;

/**
 * This provides the toplevel interface for StreaMIT.
 */
public class StreaMITMain {

    /**
     * Prints out C code for the program being compiled.
     */
    public static void compile(JCompilationUnit top) {
	System.out.println("/*");
	Kopi2SIR k2s = new Kopi2SIR();
        SIRStream stream = (SIRStream)top.accept(k2s);
        JClassDeclaration flat = Flattener.flatten(stream, 
						   k2s.getInterfaces());
        System.out.println("*/\n");
	System.out.println("#include \"streamit.h\"\n");
	System.out.println("#include <stdio.h>\n");
	System.out.println("#include <stdlib.h>\n");
        LIRToC l2c = new LIRToC();
        flat.accept(l2c);
        l2c.close();
	System.out.println(l2c.getString());
    }

}
