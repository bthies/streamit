/*
 * HelloWorld6LIR.java: Produce StreaMIT Low IR for HelloWorld6
 * $Id: HelloWorld6LIR.java,v 1.1 2001-10-03 18:41:38 dmaze Exp $
 */

import at.dms.compiler.*;
import at.dms.kjc.*;
import at.dms.kjc.lir.*;
import at.dms.kjc.sir.*;
import at.dms.kjc.sir.lowering.*;
import at.dms.util.IRPrinter;

import java.util.LinkedList;

class HelloWorld6LIR
{
    public static void main(String args[])
    {
	System.out.println("/*");
        SIRStream hello6 = SIRBuilder.buildHello6();
        JClassDeclaration flat = Flattener.flatten(hello6);
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
