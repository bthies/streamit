package at.dms.kjc;

import at.dms.kjc.sir.*;
import at.dms.kjc.sir.lowering.*;
import at.dms.kjc.lir.*;
import at.dms.kjc.raw.*;

/**
 * This provides the toplevel interface for StreaMIT.
 */
public class StreaMITMain {
    
    /**
     * For now, we just need one memoizer, so let's just keep it here.
     */
    public static final Memoizer memoizer = Memoizer.create();
    
    /**
     * Prints out C code for the program being compiled.
     */
    public static void compile(JCompilationUnit[] app) {
	System.out.println("/*");
	Kopi2SIR k2s = new Kopi2SIR(app);
	SIRStream stream = null;
	for (int i = 0; i < app.length; i++) {
	    SIRStream top = (SIRStream)app[i].accept(k2s);
	    if (top != null)
		stream = top;
	}

	SemanticChecker.doCheck(stream);
	
	if (StreamItOptions.rawRows != -1) {
	    System.out.println("*/");
	
	    /* Compiling for raw */
	    RawBackend.run(stream, 
			   k2s.getInterfaces(),
			   k2s.getInterfaceTables());
	    
	}
	else {
	    /* Compiling for uniprocessor */
	    JClassDeclaration flat = Flattener.flatten(stream, 
						       k2s.getInterfaces(),
						       k2s.getInterfaceTables(),
                                                       k2s.getStructures());
	    System.out.println("*/");
	
	    System.out.println("#include \"streamit.h\"");
	    System.out.println("#include <stdio.h>");
	    System.out.println("#include <stdlib.h>");
	    System.out.println("#include <math.h>");
	    LIRToC l2c = new LIRToC();
	    flat.accept(l2c);
	    l2c.close();
	    System.out.println(l2c.getString());
	}
    }
}
